package com.graphhopper.reader;

import com.graphhopper.coll.GHLongIntBTree;
import com.graphhopper.coll.LongIntMap;
import com.graphhopper.reader.DataReader;
import com.graphhopper.reader.OSMElement;
import com.graphhopper.reader.OSMInputFile;
import com.graphhopper.reader.OSMNode;
import com.graphhopper.reader.OSMRelation;
import com.graphhopper.reader.OSMTurnRelation;
import com.graphhopper.reader.OSMWay;
import com.graphhopper.reader.PillarInfo;
import com.graphhopper.reader.dem.ElevationProvider;
import com.graphhopper.routing.util.DefaultEdgeFilter;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.TurnWeighting;
import com.graphhopper.storage.Graph;
import com.graphhopper.storage.GraphExtension;
import com.graphhopper.storage.GraphHopperStorage;
import com.graphhopper.storage.GraphStorage;
import com.graphhopper.storage.NodeAccess;
import com.graphhopper.storage.TurnCostExtension;
import com.graphhopper.util.DistanceCalc;
import com.graphhopper.util.DistanceCalc3D;
import com.graphhopper.util.DouglasPeucker;
import com.graphhopper.util.EdgeExplorer;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.Helper;
import com.graphhopper.util.PointList;
import com.graphhopper.util.StopWatch;
import com.graphhopper.util.shapes.GHPoint;
import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.TIntLongMap;
import gnu.trove.map.TLongLongMap;
import gnu.trove.map.hash.TIntLongHashMap;
import gnu.trove.map.hash.TLongLongHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.xml.stream.XMLStreamException;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

public class OSMReader implements DataReader {
   protected static final int EMPTY = -1;
   protected static final int PILLAR_NODE = 1;
   protected static final int TOWER_NODE = -2;
//   private static final Logger logger = LoggerFactory.getLogger(OSMReader.class);
   private long locations;
   private long skippedLocations;
   private final GraphStorage ghStorage;
   private final Graph graph;
   private final NodeAccess nodeAccess;
   private EncodingManager encodingManager = null;
   private int workerThreads = -1;
   protected long zeroCounter = 0L;
   private LongIntMap osmNodeIdToInternalNodeMap;
   private TLongLongHashMap osmNodeIdToNodeFlagsMap;
   private TLongLongHashMap osmWayIdToRouteWeightMap;
   private TLongHashSet osmWayIdSet = new TLongHashSet();
   private TIntLongMap edgeIdToOsmWayIdMap;
   private final TLongList barrierNodeIds = new TLongArrayList();
   protected PillarInfo pillarInfo;
   private final DistanceCalc distCalc;
   private final DistanceCalc3D distCalc3D;
   private final DouglasPeucker simplifyAlgo;
   private boolean doSimplify;
   private int nextTowerId;
   private int nextPillarId;
   private long newUniqueOsmId;
   private ElevationProvider eleProvider;
   private final boolean exitOnlyPillarNodeException;
   private File osmFile;
   private final Map outExplorerMap;
   private final Map inExplorerMap;

   public OSMReader(GraphHopperStorage ghStorage) {
      this.distCalc = Helper.DIST_EARTH;
      this.distCalc3D = Helper.DIST_3D;
      this.simplifyAlgo = new DouglasPeucker();
      this.doSimplify = true;
      this.nextTowerId = 0;
      this.nextPillarId = 0;
      this.newUniqueOsmId = -9223372036854775807L;
      this.eleProvider = ElevationProvider.NOOP;
      this.exitOnlyPillarNodeException = true;
      this.outExplorerMap = new HashMap();
      this.inExplorerMap = new HashMap();
      this.ghStorage = ghStorage;
      this.graph = ghStorage;
      this.nodeAccess = this.graph.getNodeAccess();
      this.osmNodeIdToInternalNodeMap = new GHLongIntBTree(200);
      this.osmNodeIdToNodeFlagsMap = new TLongLongHashMap(200, 0.5F, 0L, 0L);
      this.osmWayIdToRouteWeightMap = new TLongLongHashMap(200, 0.5F, 0L, 0L);
      this.pillarInfo = new PillarInfo(this.nodeAccess.is3D(), ghStorage.getDirectory());
   }

   public void readGraph() throws IOException {
      if(this.encodingManager == null) {
         throw new IllegalStateException("Encoding manager was not set.");
      } else if(this.osmFile == null) {
         throw new IllegalStateException("No OSM file specified");
      } else if(!this.osmFile.exists()) {
         throw new IllegalStateException("Your specified OSM file does not exist:" + this.osmFile.getAbsolutePath());
      } else {
         StopWatch sw1 = (new StopWatch()).start();
         this.preProcess(this.osmFile);
         sw1.stop();
         StopWatch sw2 = (new StopWatch()).start();
         this.writeOsm2Graph(this.osmFile);
         sw2.stop();
//         logger.info("time(pass1): " + (int)sw1.getSeconds() + " pass2: " + (int)sw2.getSeconds() + " total:" + (int)(sw1.getSeconds() + sw2.getSeconds()));
      }
   }

   void preProcess(File osmFile) {
      OSMInputFile in = null;

      try {
         in = (new OSMInputFile(osmFile)).setWorkerThreads(this.workerThreads).open();
         long ex = 1L;
         long tmpRelationCounter = 1L;

         OSMElement item;
         while((item = in.getNext()) != null) {
            if(item.isType(1)) {
               OSMWay relation = (OSMWay)item;
               boolean valid = this.filterWay(relation);
               if(valid) {
                  TLongList wayNodes = relation.getNodes();
                  int s = wayNodes.size();

                  for(int index = 0; index < s; ++index) {
                     this.prepareHighwayNode(wayNodes.get(index));
                  }

                  if(++ex % 5000000L == 0L) {
//                     logger.info(Helper.nf(ex) + " (preprocess), osmIdMap:" + Helper.nf(this.getNodeMap().getSize()) + " (" + this.getNodeMap().getMemoryUsage() + "MB) " + Helper.getMemInfo());
                  }
               }
            }

            if(item.isType(2)) {
               OSMRelation var18 = (OSMRelation)item;
               if(!var18.isMetaRelation() && var18.hasTag("type", "route")) {
                  this.prepareWaysWithRelationInfo(var18);
               }

               if(var18.hasTag("type", "restriction")) {
                  this.prepareRestrictionRelation(var18);
               }

               if(++tmpRelationCounter % 50000L == 0L) {
//                  logger.info(Helper.nf(tmpRelationCounter) + " (preprocess), osmWayMap:" + Helper.nf((long)this.getRelFlagsMap().size()) + " " + Helper.getMemInfo());
               }
            }
         }
      } catch (Exception var16) {
         throw new RuntimeException("Problem while parsing file", var16);
      } finally {
         Helper.close(in);
      }

   }

   private void prepareRestrictionRelation(OSMRelation relation) {
      OSMTurnRelation turnRelation = this.createTurnRelation(relation);
      if(turnRelation != null) {
         this.getOsmWayIdSet().add(turnRelation.getOsmIdFrom());
         this.getOsmWayIdSet().add(turnRelation.getOsmIdTo());
      }

   }

   private TLongSet getOsmWayIdSet() {
      return this.osmWayIdSet;
   }

   private TIntLongMap getEdgeIdToOsmWayIdMap() {
      if(this.edgeIdToOsmWayIdMap == null) {
         this.edgeIdToOsmWayIdMap = new TIntLongHashMap(this.getOsmWayIdSet().size(), 0.5F, -1, -1L);
      }

      return this.edgeIdToOsmWayIdMap;
   }

   boolean filterWay(OSMWay item) {
      return item.getNodes().size() < 2?false:(!item.hasTags()?false:this.encodingManager.acceptWay(item) > 0L);
   }

   private void writeOsm2Graph(File osmFile) {
      int tmp = (int)Math.max(this.getNodeMap().getSize() / 50L, 100L);
//      logger.info("creating graph. Found nodes (pillar+tower):" + Helper.nf(this.getNodeMap().getSize()) + ", " + Helper.getMemInfo());
      this.ghStorage.create((long)tmp);
      long wayStart = -1L;
      long relationStart = -1L;
      long counter = 1L;
      OSMInputFile in = null;

      try {
         in = (new OSMInputFile(osmFile)).setWorkerThreads(this.workerThreads).open();
         LongIntMap ex = this.getNodeMap();

         OSMElement item;
         while((item = in.getNext()) != null) {
            switch(item.getType()) {
            case 0:
               if(ex.get(item.getId()) != -1) {
                  this.processNode((OSMNode)item);
               }
               break;
            case 1:
               if(wayStart < 0L) {
//                  logger.info(Helper.nf(counter) + ", now parsing ways");
                  wayStart = counter;
               }

               this.processWay((OSMWay)item);
               break;
            case 2:
               if(relationStart < 0L) {
//                  logger.info(Helper.nf(counter) + ", now parsing relations");
                  relationStart = counter;
               }

               this.processRelation((OSMRelation)item);
            }

            if(++counter % 100000000L == 0L) {
//               logger.info(Helper.nf(counter) + ", locs:" + Helper.nf(this.locations) + " (" + this.skippedLocations + ") " + Helper.getMemInfo());
            }
         }
      } catch (Exception var15) {
         throw new RuntimeException("Couldn\'t process file " + osmFile + ", error: " + var15.getMessage(), var15);
      } finally {
         Helper.close(in);
      }

      this.finishedReading();
      if(this.graph.getNodes() == 0) {
         throw new IllegalStateException("osm must not be empty. read " + counter + " lines and " + this.locations + " locations");
      }
   }

   void processWay(OSMWay way) {
      if(way.getNodes().size() >= 2) {
         if(way.hasTags()) {
            long wayOsmId = way.getId();
            long includeWay = this.encodingManager.acceptWay(way);
            if(includeWay != 0L) {
               long relationFlags = this.getRelFlagsMap().get(way.getId());
               TLongList osmNodeIds = way.getNodes();
               if(osmNodeIds.size() > 1) {
                  int wayFlags = this.getNodeMap().get(osmNodeIds.get(0));
                  int last = this.getNodeMap().get(osmNodeIds.get(osmNodeIds.size() - 1));
                  double createdEdges = this.getTmpLatitude(wayFlags);
                  double lastBarrier = this.getTmpLongitude(wayFlags);
                  double edge = this.getTmpLatitude(last);
                  double nodeFlags = this.getTmpLongitude(last);
                  if(!Double.isNaN(createdEdges) && !Double.isNaN(lastBarrier) && !Double.isNaN(edge) && !Double.isNaN(nodeFlags)) {
                     double newNodeId = this.distCalc.calcDist(createdEdges, lastBarrier, edge, nodeFlags);
                     way.setTag("estimated_distance", Double.valueOf(newNodeId));
                     way.setTag("estimated_center", new GHPoint((createdEdges + edge) / 2.0D, (lastBarrier + nodeFlags) / 2.0D));
                  }
               }

               long var23 = this.encodingManager.handleWayTags(way, includeWay, relationFlags);
               if(var23 != 0L) {
                  ArrayList var24 = new ArrayList();
                  int size = osmNodeIds.size();
                  int var25 = -1;

                  for(int i$ = 0; i$ < size; ++i$) {
                     long var28 = osmNodeIds.get(i$);
                     long var31 = this.getNodeFlagsMap().get(var28);
                     if(var31 > 0L && (var31 & var23) > 0L) {
                        this.getNodeFlagsMap().put(var28, 0L);
                        long var32 = this.addBarrierNode(var28);
                        if(i$ > 0) {
                           if(var25 < 0) {
                              var25 = 0;
                           }

                           long[] transfer = osmNodeIds.toArray(var25, i$ - var25 + 1);
                           transfer[transfer.length - 1] = var32;
                           TLongArrayList partIds = new TLongArrayList(transfer);
                           var24.addAll(this.addOSMWay(partIds, var23, wayOsmId));
                           var24.addAll(this.addBarrierEdge(var32, var28, var23, var31, wayOsmId));
                        } else {
                           var24.addAll(this.addBarrierEdge(var28, var32, var23, var31, wayOsmId));
                           osmNodeIds.set(0, var32);
                        }

                        var25 = i$;
                     }
                  }

                  if(var25 >= 0) {
                     if(var25 < size - 1) {
                        long[] var26 = osmNodeIds.toArray(var25, size - var25);
                        TLongArrayList var29 = new TLongArrayList(var26);
                        var24.addAll(this.addOSMWay(var29, var23, wayOsmId));
                     }
                  } else {
                     var24.addAll(this.addOSMWay(way.getNodes(), var23, wayOsmId));
                  }

                  Iterator var27 = var24.iterator();

                  while(var27.hasNext()) {
                     EdgeIteratorState var30 = (EdgeIteratorState)var27.next();
                     this.encodingManager.applyWayTags(way, var30);
                  }

               }
            }
         }
      }
   }

   public void processRelation(OSMRelation relation) throws XMLStreamException {
      if(relation.hasTag("type", "restriction")) {
         OSMTurnRelation turnRelation = this.createTurnRelation(relation);
         if(turnRelation != null) {
            GraphExtension extendedStorage = this.graph.getExtension();
            if(extendedStorage instanceof TurnCostExtension) {
               TurnCostExtension tcs = (TurnCostExtension)extendedStorage;
               Collection entries = this.analyzeTurnRelation(turnRelation);
               Iterator i$ = entries.iterator();

               while(i$.hasNext()) {
                  OSMTurnRelation.TurnCostTableEntry entry = (OSMTurnRelation.TurnCostTableEntry)i$.next();
                  tcs.addTurnInfo(entry.edgeFrom, entry.nodeVia, entry.edgeTo, entry.flags);
               }
            }
         }
      }

   }

   public Collection analyzeTurnRelation(OSMTurnRelation turnRelation) {
      TLongObjectHashMap entries = new TLongObjectHashMap();
      Iterator i$ = this.encodingManager.fetchEdgeEncoders().iterator();

      while(i$.hasNext()) {
         FlagEncoder encoder = (FlagEncoder)i$.next();
         Iterator i$1 = this.analyzeTurnRelation(encoder, turnRelation).iterator();

         while(i$1.hasNext()) {
            OSMTurnRelation.TurnCostTableEntry entry = (OSMTurnRelation.TurnCostTableEntry)i$1.next();
            OSMTurnRelation.TurnCostTableEntry oldEntry = (OSMTurnRelation.TurnCostTableEntry)entries.get(entry.getItemId());
            if(oldEntry != null) {
               oldEntry.flags |= entry.flags;
            } else {
               entries.put(entry.getItemId(), entry);
            }
         }
      }

      return entries.valueCollection();
   }

   public Collection analyzeTurnRelation(FlagEncoder encoder, OSMTurnRelation turnRelation) {
      if(!encoder.supports(TurnWeighting.class)) {
         return Collections.emptyList();
      } else {
         EdgeExplorer edgeOutExplorer = (EdgeExplorer)this.outExplorerMap.get(encoder);
         EdgeExplorer edgeInExplorer = (EdgeExplorer)this.inExplorerMap.get(encoder);
         if(edgeOutExplorer == null || edgeInExplorer == null) {
            edgeOutExplorer = this.graph.createEdgeExplorer(new DefaultEdgeFilter(encoder, false, true));
            this.outExplorerMap.put(encoder, edgeOutExplorer);
            edgeInExplorer = this.graph.createEdgeExplorer(new DefaultEdgeFilter(encoder, true, false));
            this.inExplorerMap.put(encoder, edgeInExplorer);
         }

         return turnRelation.getRestrictionAsEntries(encoder, edgeOutExplorer, edgeInExplorer, this);
      }
   }

   public long getOsmIdOfInternalEdge(int edgeId) {
      return this.getEdgeIdToOsmWayIdMap().get(edgeId);
   }

   public int getInternalNodeIdOfOsmNode(long nodeOsmId) {
      int id = this.getNodeMap().get(nodeOsmId);
      return id < -2?-id - 3:-1;
   }

   double getTmpLatitude(int id) {
      if(id == -1) {
         return Double.NaN;
      } else if(id < -2) {
         id = -id - 3;
         return this.nodeAccess.getLatitude(id);
      } else if(id > 2) {
         id -= 3;
         return this.pillarInfo.getLatitude(id);
      } else {
         return Double.NaN;
      }
   }

   double getTmpLongitude(int id) {
      if(id == -1) {
         return Double.NaN;
      } else if(id < -2) {
         id = -id - 3;
         return this.nodeAccess.getLongitude(id);
      } else if(id > 2) {
         id -= 3;
         return this.pillarInfo.getLon(id);
      } else {
         return Double.NaN;
      }
   }

   private void processNode(OSMNode node) {
      if(this.isInBounds(node)) {
         this.addNode(node);
         if(node.hasTags()) {
            long nodeFlags = this.encodingManager.handleNodeTags(node);
            if(nodeFlags != 0L) {
               this.getNodeFlagsMap().put(node.getId(), nodeFlags);
            }
         }

         ++this.locations;
      } else {
         ++this.skippedLocations;
      }

   }

   boolean addNode(OSMNode node) {
      int nodeType = this.getNodeMap().get(node.getId());
      if(nodeType == -1) {
         return false;
      } else {
         double lat = node.getLat();
         double lon = node.getLon();
         double ele = this.getElevation(node);
         if(nodeType == -2) {
            this.addTowerNode(node.getId(), lat, lon, ele);
         } else if(nodeType == 1) {
            this.pillarInfo.setNode(this.nextPillarId, lat, lon, ele);
            this.getNodeMap().put(node.getId(), this.nextPillarId + 3);
            ++this.nextPillarId;
         }

         return true;
      }
   }

   protected double getElevation(OSMNode node) {
      return this.eleProvider.getEle(node.getLat(), node.getLon());
   }

   void prepareWaysWithRelationInfo(OSMRelation osmRelation) {
      if(this.encodingManager.handleRelationTags(osmRelation, 0L) != 0L) {
         int size = osmRelation.getMembers().size();

         for(int index = 0; index < size; ++index) {
            OSMRelation.Member member = (OSMRelation.Member)osmRelation.getMembers().get(index);
            if(member.type() == 1) {
               long osmId = member.ref();
               long oldRelationFlags = this.getRelFlagsMap().get(osmId);
               long newRelationFlags = this.encodingManager.handleRelationTags(osmRelation, oldRelationFlags);
               if(oldRelationFlags != newRelationFlags) {
                  this.getRelFlagsMap().put(osmId, newRelationFlags);
               }
            }
         }

      }
   }

   void prepareHighwayNode(long osmId) {
      int tmpIndex = this.getNodeMap().get(osmId);
      if(tmpIndex == -1) {
         this.getNodeMap().put(osmId, 1);
      } else if(tmpIndex > -1) {
         this.getNodeMap().put(osmId, -2);
      }

   }

   int addTowerNode(long osmId, double lat, double lon, double ele) {
      if(this.nodeAccess.is3D()) {
         this.nodeAccess.setNode(this.nextTowerId, lat, lon, ele);
      } else {
         this.nodeAccess.setNode(this.nextTowerId, lat, lon);
      }

      int id = -(this.nextTowerId + 3);
      this.getNodeMap().put(osmId, id);
      ++this.nextTowerId;
      return id;
   }

   Collection addOSMWay(TLongList osmNodeIds, long flags, long wayOsmId) {
      PointList pointList = new PointList(osmNodeIds.size(), this.nodeAccess.is3D());
      ArrayList newEdges = new ArrayList(5);
      int firstNode = -1;
      int lastIndex = osmNodeIds.size() - 1;
      int lastInBoundsPillarNode = -1;

      try {
         for(int ex = 0; ex < osmNodeIds.size(); ++ex) {
            long osmId = osmNodeIds.get(ex);
            int tmpNode = this.getNodeMap().get(osmId);
            if(tmpNode != -1 && tmpNode != -2) {
               if(tmpNode == 1) {
                  if(!pointList.isEmpty() && lastInBoundsPillarNode > 2) {
                     tmpNode = this.handlePillarNode(lastInBoundsPillarNode, osmId, (PointList)null, true);
                     tmpNode = -tmpNode - 3;
                     if(pointList.getSize() > 1 && firstNode >= 0) {
                        newEdges.add(this.addEdge(firstNode, tmpNode, pointList, flags, wayOsmId));
                        pointList.clear();
                        pointList.add(this.nodeAccess, tmpNode);
                     }

                     firstNode = tmpNode;
                     lastInBoundsPillarNode = -1;
                  }
               } else {
                  if(tmpNode <= 2 && tmpNode >= -2) {
                     throw new AssertionError("Mapped index not in correct bounds " + tmpNode + ", " + osmId);
                  }

                  if(tmpNode > 2) {
                     boolean convertToTowerNode = ex == 0 || ex == lastIndex;
                     if(!convertToTowerNode) {
                        lastInBoundsPillarNode = tmpNode;
                     }

                     tmpNode = this.handlePillarNode(tmpNode, osmId, pointList, convertToTowerNode);
                  }

                  if(tmpNode < -2) {
                     tmpNode = -tmpNode - 3;
                     pointList.add(this.nodeAccess, tmpNode);
                     if(firstNode >= 0) {
                        newEdges.add(this.addEdge(firstNode, tmpNode, pointList, flags, wayOsmId));
                        pointList.clear();
                        pointList.add(this.nodeAccess, tmpNode);
                     }

                     firstNode = tmpNode;
                  }
               }
            }
         }

         return newEdges;
      } catch (RuntimeException var16) {
//         logger.error("Couldn\'t properly add edge with osm ids:" + osmNodeIds, var16);
         throw var16;
      }
   }

   EdgeIteratorState addEdge(int fromIndex, int toIndex, PointList pointList, long flags, long wayOsmId) {
      if(fromIndex >= 0 && toIndex >= 0) {
         if(pointList.getDimension() != this.nodeAccess.getDimension()) {
            throw new AssertionError("Dimension does not match for pointList vs. nodeAccess " + pointList.getDimension() + " <-> " + this.nodeAccess.getDimension());
         } else {
            double towerNodeDistance = 0.0D;
            double prevLat = pointList.getLatitude(0);
            double prevLon = pointList.getLongitude(0);
            double prevEle = pointList.is3D()?pointList.getElevation(0):Double.NaN;
            double ele = Double.NaN;
            PointList pillarNodes = new PointList(pointList.getSize() - 2, this.nodeAccess.is3D());
            int nodes = pointList.getSize();

            for(int iter = 1; iter < nodes; ++iter) {
               double lat = pointList.getLatitude(iter);
               double lon = pointList.getLongitude(iter);
               if(pointList.is3D()) {
                  ele = pointList.getElevation(iter);
                  towerNodeDistance += this.distCalc3D.calcDist(prevLat, prevLon, prevEle, lat, lon, ele);
                  prevEle = ele;
               } else {
                  towerNodeDistance += this.distCalc.calcDist(prevLat, prevLon, lat, lon);
               }

               prevLat = lat;
               prevLon = lon;
               if(nodes > 2 && iter < nodes - 1) {
                  if(pillarNodes.is3D()) {
                     pillarNodes.add(lat, lon, ele);
                  } else {
                     pillarNodes.add(lat, lon);
                  }
               }
            }

            if(towerNodeDistance < 1.0E-4D) {
               ++this.zeroCounter;
               towerNodeDistance = 1.0E-4D;
            }

            if(Double.isInfinite(towerNodeDistance) || Double.isNaN(towerNodeDistance)) {
//               logger.warn("Bug in OSM or GraphHopper. Illegal tower node distance " + towerNodeDistance + " reset to 1m, osm way " + wayOsmId);
               towerNodeDistance = 1.0D;
            }

            EdgeIteratorState var25 = this.graph.edge(fromIndex, toIndex).setDistance(towerNodeDistance).setFlags(flags);
            if(nodes > 2) {
               if(this.doSimplify) {
                  this.simplifyAlgo.simplify(pillarNodes);
               }

               var25.setWayGeometry(pillarNodes);
            }

            this.storeOsmWayID(var25.getEdge(), wayOsmId);
            return var25;
         }
      } else {
         throw new AssertionError("to or from index is invalid for this edge " + fromIndex + "->" + toIndex + ", points:" + pointList);
      }
   }

   private void storeOsmWayID(int edgeId, long osmWayId) {
      if(this.getOsmWayIdSet().contains(osmWayId)) {
         this.getEdgeIdToOsmWayIdMap().put(edgeId, osmWayId);
      }

   }

   private int handlePillarNode(int tmpNode, long osmId, PointList pointList, boolean convertToTowerNode) {
      tmpNode -= 3;
      double lat = this.pillarInfo.getLatitude(tmpNode);
      double lon = this.pillarInfo.getLongitude(tmpNode);
      double ele = this.pillarInfo.getElevation(tmpNode);
      if(lat != Double.MAX_VALUE && lon != Double.MAX_VALUE) {
         if(convertToTowerNode) {
            this.pillarInfo.setNode(tmpNode, Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
            tmpNode = this.addTowerNode(osmId, lat, lon, ele);
         } else if(pointList.is3D()) {
            pointList.add(lat, lon, ele);
         } else {
            pointList.add(lat, lon);
         }

         return tmpNode;
      } else {
         throw new RuntimeException("Conversion pillarNode to towerNode already happended!? osmId:" + osmId + " pillarIndex:" + tmpNode);
      }
   }

   protected void finishedReading() {
      this.printInfo("way");
      this.pillarInfo.clear();
      this.eleProvider.release();
      this.osmNodeIdToInternalNodeMap = null;
      this.osmNodeIdToNodeFlagsMap = null;
      this.osmWayIdToRouteWeightMap = null;
      this.osmWayIdSet = null;
      this.edgeIdToOsmWayIdMap = null;
   }

   long addBarrierNode(long nodeId) {
      int graphIndex = this.getNodeMap().get(nodeId);
      OSMNode newNode;
      if(graphIndex < -2) {
         graphIndex = -graphIndex - 3;
         newNode = new OSMNode(this.createNewNodeId(), this.nodeAccess, graphIndex);
      } else {
         graphIndex -= 3;
         newNode = new OSMNode(this.createNewNodeId(), this.pillarInfo, graphIndex);
      }

      long id = newNode.getId();
      this.prepareHighwayNode(id);
      this.addNode(newNode);
      return id;
   }

   private long createNewNodeId() {
      return (long)(this.newUniqueOsmId++);
   }

   Collection addBarrierEdge(long fromId, long toId, long flags, long nodeFlags, long wayOsmId) {
      flags &= ~nodeFlags;
      this.barrierNodeIds.clear();
      this.barrierNodeIds.add(fromId);
      this.barrierNodeIds.add(toId);
      return this.addOSMWay(this.barrierNodeIds, flags, wayOsmId);
   }

   OSMTurnRelation createTurnRelation(OSMRelation relation) {
      OSMTurnRelation.Type type = OSMTurnRelation.Type.getRestrictionType(relation.getTag("restriction"));
      if(type != OSMTurnRelation.Type.UNSUPPORTED) {
         long fromWayID = -1L;
         long viaNodeID = -1L;
         long toWayID = -1L;
         Iterator i$ = relation.getMembers().iterator();

         while(i$.hasNext()) {
            OSMRelation.Member member = (OSMRelation.Member)i$.next();
            if(1 == member.type()) {
               if("from".equals(member.role())) {
                  fromWayID = member.ref();
               } else if("to".equals(member.role())) {
                  toWayID = member.ref();
               }
            } else if(0 == member.type() && "via".equals(member.role())) {
               viaNodeID = member.ref();
            }
         }

         if(fromWayID >= 0L && toWayID >= 0L && viaNodeID >= 0L) {
            return new OSMTurnRelation(fromWayID, viaNodeID, toWayID, type);
         }
      }

      return null;
   }

   boolean isInBounds(OSMNode node) {
      return true;
   }

   protected LongIntMap getNodeMap() {
      return this.osmNodeIdToInternalNodeMap;
   }

   protected TLongLongMap getNodeFlagsMap() {
      return this.osmNodeIdToNodeFlagsMap;
   }

   TLongLongHashMap getRelFlagsMap() {
      return this.osmWayIdToRouteWeightMap;
   }

   public OSMReader setEncodingManager(EncodingManager em) {
      this.encodingManager = em;
      return this;
   }

   public OSMReader setWayPointMaxDistance(double maxDist) {
      this.doSimplify = maxDist > 0.0D;
      this.simplifyAlgo.setMaxDistance(maxDist);
      return this;
   }

   public OSMReader setWorkerThreads(int numOfWorkers) {
      this.workerThreads = numOfWorkers;
      return this;
   }

   public OSMReader setElevationProvider(ElevationProvider eleProvider) {
      if(eleProvider == null) {
         throw new IllegalStateException("Use the NOOP elevation provider instead of null or don\'t call setElevationProvider");
      } else if(!this.nodeAccess.is3D() && ElevationProvider.NOOP != eleProvider) {
         throw new IllegalStateException("Make sure you graph accepts 3D data");
      } else {
         this.eleProvider = eleProvider;
         return this;
      }
   }

   public OSMReader setOSMFile(File osmFile) {
      this.osmFile = osmFile;
      return this;
   }

   private void printInfo(String str) {
//      logger.info("finished " + str + " processing." + " nodes: " + this.graph.getNodes() + ", osmIdMap.size:" + this.getNodeMap().getSize() + ", osmIdMap:" + this.getNodeMap().getMemoryUsage() + "MB" + ", nodeFlagsMap.size:" + this.getNodeFlagsMap().size() + ", relFlagsMap.size:" + this.getRelFlagsMap().size() + ", zeroCounter:" + this.zeroCounter + " " + Helper.getMemInfo());
   }

   public String toString() {
      return this.getClass().getSimpleName();
   }
}
