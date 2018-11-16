package com.graphhopper.routing;

import com.graphhopper.routing.VirtualEdgeIterator;
import com.graphhopper.routing.VirtualEdgeIteratorState;
import com.graphhopper.routing.util.AllEdgesIterator;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.storage.Graph;
import com.graphhopper.storage.GraphExtension;
import com.graphhopper.storage.NodeAccess;
import com.graphhopper.storage.TurnCostExtension;
import com.graphhopper.storage.index.QueryResult;
import com.graphhopper.util.AngleCalc;
import com.graphhopper.util.EdgeExplorer;
import com.graphhopper.util.EdgeIterator;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.GHUtility;
import com.graphhopper.util.Helper;
import com.graphhopper.util.PointList;
import com.graphhopper.util.shapes.BBox;
import com.graphhopper.util.shapes.GHPoint3D;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.procedure.TIntProcedure;
import gnu.trove.procedure.TObjectProcedure;
import gnu.trove.set.hash.TIntHashSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class QueryGraph implements Graph {
   private final Graph mainGraph;
   private final NodeAccess mainNodeAccess;
   private final int mainNodes;
   private final int mainEdges;
   private final QueryGraph baseGraph;
   private final GraphExtension wrappedExtension;
   private List queryResults;
   List virtualEdges;
   static final int VE_BASE = 0;
   static final int VE_BASE_REV = 1;
   static final int VE_ADJ = 2;
   static final int VE_ADJ_REV = 3;
   private PointList virtualNodes;
   private static final AngleCalc ac = new AngleCalc();
   private List modifiedEdges = new ArrayList(5);
   private final NodeAccess nodeAccess = new NodeAccess() {
      public void ensureNode(int nodeId) {
         QueryGraph.this.mainNodeAccess.ensureNode(nodeId);
      }

      public boolean is3D() {
         return QueryGraph.this.mainNodeAccess.is3D();
      }

      public int getDimension() {
         return QueryGraph.this.mainNodeAccess.getDimension();
      }

      public double getLatitude(int nodeId) {
         return QueryGraph.this.isVirtualNode(nodeId)?QueryGraph.this.virtualNodes.getLatitude(nodeId - QueryGraph.this.mainNodes):QueryGraph.this.mainNodeAccess.getLatitude(nodeId);
      }

      public double getLongitude(int nodeId) {
         return QueryGraph.this.isVirtualNode(nodeId)?QueryGraph.this.virtualNodes.getLongitude(nodeId - QueryGraph.this.mainNodes):QueryGraph.this.mainNodeAccess.getLongitude(nodeId);
      }

      public double getElevation(int nodeId) {
         return QueryGraph.this.isVirtualNode(nodeId)?QueryGraph.this.virtualNodes.getElevation(nodeId - QueryGraph.this.mainNodes):QueryGraph.this.mainNodeAccess.getElevation(nodeId);
      }

      public int getAdditionalNodeField(int nodeId) {
         return QueryGraph.this.isVirtualNode(nodeId)?0:QueryGraph.this.mainNodeAccess.getAdditionalNodeField(nodeId);
      }

      public void setNode(int nodeId, double lat, double lon) {
         throw new UnsupportedOperationException("Not supported yet.");
      }

      public void setNode(int nodeId, double lat, double lon, double ele) {
         throw new UnsupportedOperationException("Not supported yet.");
      }

      public void setAdditionalNodeField(int nodeId, int additionalValue) {
         throw new UnsupportedOperationException("Not supported yet.");
      }

      public double getLat(int nodeId) {
         return this.getLatitude(nodeId);
      }

      public double getLon(int nodeId) {
         return this.getLongitude(nodeId);
      }

      public double getEle(int nodeId) {
         return this.getElevation(nodeId);
      }
   };

   public QueryGraph(Graph graph) {
      this.mainGraph = graph;
      this.mainNodeAccess = graph.getNodeAccess();
      this.mainNodes = graph.getNodes();
      this.mainEdges = graph.getAllEdges().getMaxId();
      if(this.mainGraph.getExtension() instanceof TurnCostExtension) {
         this.wrappedExtension = new QueryGraph.QueryGraphTurnExt(this);
      } else {
         this.wrappedExtension = this.mainGraph.getExtension();
      }

      this.baseGraph = new QueryGraph(graph.getBaseGraph(), this);
   }

   private QueryGraph(Graph graph, QueryGraph superQueryGraph) {
      this.mainGraph = graph;
      this.baseGraph = this;
      this.wrappedExtension = superQueryGraph.wrappedExtension;
      this.mainNodeAccess = graph.getNodeAccess();
      this.mainNodes = superQueryGraph.mainNodes;
      this.mainEdges = superQueryGraph.mainEdges;
   }

   public QueryGraph lookup(QueryResult fromRes, QueryResult toRes) {
      ArrayList results = new ArrayList(2);
      results.add(fromRes);
      results.add(toRes);
      this.lookup(results);
      return this;
   }

   public void lookup(List resList) {
      if(this.isInitialized()) {
         throw new IllegalStateException("Call lookup only once. Otherwise you\'ll have problems for queries sharing the same edge.");
      } else {
         this.virtualEdges = new ArrayList(resList.size() * 2);
         this.virtualNodes = new PointList(resList.size(), this.mainNodeAccess.is3D());
         this.queryResults = new ArrayList(resList.size());
         this.baseGraph.virtualEdges = this.virtualEdges;
         this.baseGraph.virtualNodes = this.virtualNodes;
         this.baseGraph.queryResults = this.queryResults;
         TIntObjectHashMap edge2res = new TIntObjectHashMap(resList.size());
         Iterator i$ = resList.iterator();

         while(i$.hasNext()) {
            QueryResult res = (QueryResult)i$.next();
            if(res.getSnappedPosition() != QueryResult.Position.TOWER) {
               EdgeIteratorState closestEdge = res.getClosestEdge();
               if(closestEdge == null) {
                  throw new IllegalStateException("Do not call QueryGraph.lookup with invalid QueryResult " + res);
               }

               int base = closestEdge.getBaseNode();
               boolean doReverse = base > closestEdge.getAdjNode();
               PointList edgeId;
               if(base == closestEdge.getAdjNode()) {
                  edgeId = closestEdge.fetchWayGeometry(0);
                  if(edgeId.size() > 1) {
                     doReverse = edgeId.getLatitude(0) > edgeId.getLatitude(edgeId.size() - 1);
                  }
               }

               if(doReverse) {
                  closestEdge = closestEdge.detach(true);
                  edgeId = closestEdge.fetchWayGeometry(3);
                  res.setClosestEdge(closestEdge);
                  if(res.getSnappedPosition() == QueryResult.Position.PILLAR) {
                     res.setWayIndex(edgeId.getSize() - res.getWayIndex() - 1);
                  } else {
                     res.setWayIndex(edgeId.getSize() - res.getWayIndex() - 2);
                  }

                  if(res.getWayIndex() < 0) {
                     throw new IllegalStateException("Problem with wayIndex while reversing closest edge:" + closestEdge + ", " + res);
                  }
               }

               int edgeId1 = closestEdge.getEdge();
               Object list = (List)edge2res.get(edgeId1);
               if(list == null) {
                  list = new ArrayList(5);
                  edge2res.put(edgeId1, list);
               }

               ((List)list).add(res);
            }
         }

         edge2res.forEachValue(new TObjectProcedure() {
            public boolean execute(Object obj) {
               List results = (List) obj;
               EdgeIteratorState closestEdge = ((QueryResult)results.get(0)).getClosestEdge();
               final PointList fullPL = closestEdge.fetchWayGeometry(3);
               int baseNode = closestEdge.getBaseNode();
               Collections.sort(results, new Comparator() {
                  public int compare(Object obj1, Object obj2) {
                	 QueryResult o1 = (QueryResult) obj1;
                	 QueryResult o2 = (QueryResult) obj2;
                     int diff = o1.getWayIndex() - o2.getWayIndex();
                     if(diff == 0) {
                        GHPoint3D p1 = o1.getSnappedPoint();
                        GHPoint3D p2 = o2.getSnappedPoint();
                        if(p1.equals(p2)) {
                           return 0;
                        } else {
                           double fromLat = fullPL.getLatitude(o1.getWayIndex());
                           double fromLon = fullPL.getLongitude(o1.getWayIndex());
                           return Helper.DIST_PLANE.calcNormalizedDist(fromLat, fromLon, p1.lat, p1.lon) > Helper.DIST_PLANE.calcNormalizedDist(fromLat, fromLon, p2.lat, p2.lon)?1:-1;
                        }
                     } else {
                        return diff;
                     }
                  }
               });
               GHPoint3D prevPoint = fullPL.toGHPoint(0);
               int adjNode = closestEdge.getAdjNode();
               int origTraversalKey = GHUtility.createEdgeKey(baseNode, adjNode, closestEdge.getEdge(), false);
               int origRevTraversalKey = GHUtility.createEdgeKey(baseNode, adjNode, closestEdge.getEdge(), true);
               long reverseFlags = closestEdge.detach(true).getFlags();
               int prevWayIndex = 1;
               int prevNodeId = baseNode;
               int virtNodeId = QueryGraph.this.virtualNodes.getSize() + QueryGraph.this.mainNodes;
               boolean addedEdges = false;

               for(int counter = 0; counter < results.size(); ++counter) {
                  QueryResult res = (QueryResult)results.get(counter);
                  if(res.getClosestEdge().getBaseNode() != baseNode) {
                     throw new IllegalStateException("Base nodes have to be identical but were not: " + closestEdge + " vs " + res.getClosestEdge());
                  }

                  GHPoint3D currSnapped = res.getSnappedPoint();
                  if(prevPoint.equals(currSnapped)) {
                     res.setClosestNode(prevNodeId);
                  } else {
                     QueryGraph.this.queryResults.add(res);
                     QueryGraph.this.createEdges(origTraversalKey, origRevTraversalKey, prevPoint, prevWayIndex, res.getSnappedPoint(), res.getWayIndex(), fullPL, closestEdge, prevNodeId, virtNodeId, reverseFlags);
                     QueryGraph.this.virtualNodes.add(currSnapped.lat, currSnapped.lon, currSnapped.ele);
                     if(addedEdges) {
                        QueryGraph.this.virtualEdges.add(QueryGraph.this.virtualEdges.get(QueryGraph.this.virtualEdges.size() - 2));
                        QueryGraph.this.virtualEdges.add(QueryGraph.this.virtualEdges.get(QueryGraph.this.virtualEdges.size() - 2));
                     }

                     addedEdges = true;
                     res.setClosestNode(virtNodeId);
                     prevNodeId = virtNodeId;
                     prevWayIndex = res.getWayIndex() + 1;
                     prevPoint = currSnapped;
                     ++virtNodeId;
                  }
               }

               if(addedEdges) {
                  QueryGraph.this.createEdges(origTraversalKey, origRevTraversalKey, prevPoint, prevWayIndex, fullPL.toGHPoint(fullPL.getSize() - 1), fullPL.getSize() - 2, fullPL, closestEdge, virtNodeId - 1, adjNode, reverseFlags);
               }

               return true;
            }
         });
      }
   }

   public Graph getBaseGraph() {
      return this.baseGraph;
   }

   public boolean isVirtualEdge(int edgeId) {
      return edgeId >= this.mainEdges;
   }

   public boolean isVirtualNode(int nodeId) {
      return nodeId >= this.mainNodes;
   }

   private void createEdges(int origTraversalKey, int origRevTraversalKey, GHPoint3D prevSnapped, int prevWayIndex, GHPoint3D currSnapped, int wayIndex, PointList fullPL, EdgeIteratorState closestEdge, int prevNodeId, int nodeId, long reverseFlags) {
      int max = wayIndex + 1;
      PointList basePoints = new PointList(max - prevWayIndex + 1, this.mainNodeAccess.is3D());
      basePoints.add(prevSnapped.lat, prevSnapped.lon, prevSnapped.ele);

      for(int baseReversePoints = prevWayIndex; baseReversePoints < max; ++baseReversePoints) {
         basePoints.add(fullPL, baseReversePoints);
      }

      basePoints.add(currSnapped.lat, currSnapped.lon, currSnapped.ele);
      PointList var21 = basePoints.clone(true);
      double baseDistance = basePoints.calcDistance(Helper.DIST_PLANE);
      int virtEdgeId = this.mainEdges + this.virtualEdges.size();
      VirtualEdgeIteratorState baseEdge = new VirtualEdgeIteratorState(origTraversalKey, virtEdgeId, prevNodeId, nodeId, baseDistance, closestEdge.getFlags(), closestEdge.getName(), basePoints);
      VirtualEdgeIteratorState baseReverseEdge = new VirtualEdgeIteratorState(origRevTraversalKey, virtEdgeId, nodeId, prevNodeId, baseDistance, reverseFlags, closestEdge.getName(), var21);
      this.virtualEdges.add(baseEdge);
      this.virtualEdges.add(baseReverseEdge);
   }

   public boolean enforceHeading(int nodeId, double favoredHeading, boolean incoming) {
      if(!this.isInitialized()) {
         throw new IllegalStateException("QueryGraph.lookup has to be called in before heading enforcement");
      } else if(Double.isNaN(favoredHeading)) {
         return false;
      } else if(!this.isVirtualNode(nodeId)) {
         return false;
      } else {
         int virtNodeIDintern = nodeId - this.mainNodes;
         favoredHeading = ac.convertAzimuth2xaxisAngle(favoredHeading);
         List edgePositions = incoming?Arrays.asList(new Integer[]{Integer.valueOf(0), Integer.valueOf(3)}):Arrays.asList(new Integer[]{Integer.valueOf(1), Integer.valueOf(2)});
         boolean enforcementOccured = false;
         Iterator i$ = edgePositions.iterator();

         while(i$.hasNext()) {
            int edgePos = ((Integer)i$.next()).intValue();
            VirtualEdgeIteratorState edge = (VirtualEdgeIteratorState)this.virtualEdges.get(virtNodeIDintern * 4 + edgePos);
            PointList wayGeo = edge.fetchWayGeometry(3);
            double edgeOrientation;
            if(incoming) {
               int delta = wayGeo.getSize();
               edgeOrientation = ac.calcOrientation(wayGeo.getLat(delta - 2), wayGeo.getLon(delta - 2), wayGeo.getLat(delta - 1), wayGeo.getLon(delta - 1));
            } else {
               edgeOrientation = ac.calcOrientation(wayGeo.getLat(0), wayGeo.getLon(0), wayGeo.getLat(1), wayGeo.getLon(1));
            }

            edgeOrientation = ac.alignOrientation(favoredHeading, edgeOrientation);
            double delta1 = edgeOrientation - favoredHeading;
            if(Math.abs(delta1) > 1.74D) {
               edge.setVirtualEdgePreference(true);
               this.modifiedEdges.add(edge);
               VirtualEdgeIteratorState reverseEdge = (VirtualEdgeIteratorState)this.virtualEdges.get(virtNodeIDintern * 4 + this.getPosOfReverseEdge(edgePos));
               reverseEdge.setVirtualEdgePreference(true);
               this.modifiedEdges.add(reverseEdge);
               enforcementOccured = true;
            }
         }

         return enforcementOccured;
      }
   }

   public boolean enforceHeadingByEdgeId(int nodeId, int edgeId, boolean incoming) {
      if(!this.isVirtualNode(nodeId)) {
         return false;
      } else {
         VirtualEdgeIteratorState incomingEdge = (VirtualEdgeIteratorState)this.getEdgeIteratorState(edgeId, nodeId);
         VirtualEdgeIteratorState reverseEdge = (VirtualEdgeIteratorState)this.getEdgeIteratorState(edgeId, incomingEdge.getBaseNode());
         incomingEdge.setVirtualEdgePreference(true);
         this.modifiedEdges.add(incomingEdge);
         reverseEdge.setVirtualEdgePreference(true);
         this.modifiedEdges.add(reverseEdge);
         return true;
      }
   }

   public void clearUnfavoredStatus() {
      Iterator i$ = this.modifiedEdges.iterator();

      while(i$.hasNext()) {
         VirtualEdgeIteratorState edge = (VirtualEdgeIteratorState)i$.next();
         edge.setVirtualEdgePreference(false);
      }

   }

   public int getNodes() {
      return this.virtualNodes.getSize() + this.mainNodes;
   }

   public NodeAccess getNodeAccess() {
      return this.nodeAccess;
   }

   public BBox getBounds() {
      return this.mainGraph.getBounds();
   }

   public EdgeIteratorState getEdgeIteratorState(int origEdgeId, int adjNode) {
      if(!this.isVirtualEdge(origEdgeId)) {
         return this.mainGraph.getEdgeIteratorState(origEdgeId, adjNode);
      } else {
         int edgeId = origEdgeId - this.mainEdges;
         EdgeIteratorState eis = (EdgeIteratorState)this.virtualEdges.get(edgeId);
         if(eis.getAdjNode() != adjNode && adjNode != Integer.MIN_VALUE) {
            edgeId = this.getPosOfReverseEdge(edgeId);
            EdgeIteratorState eis2 = (EdgeIteratorState)this.virtualEdges.get(edgeId);
            if(eis2.getAdjNode() == adjNode) {
               return eis2;
            } else {
               throw new IllegalStateException("Edge " + origEdgeId + " not found with adjNode:" + adjNode + ". found edges were:" + eis + ", " + eis2);
            }
         } else {
            return eis;
         }
      }
   }

   private int getPosOfReverseEdge(int edgeId) {
      if(edgeId % 2 == 0) {
         ++edgeId;
      } else {
         --edgeId;
      }

      return edgeId;
   }

   public EdgeExplorer createEdgeExplorer(EdgeFilter edgeFilter) {
      if(!this.isInitialized()) {
         throw new IllegalStateException("Call lookup before using this graph");
      } else {
         final TIntObjectHashMap node2EdgeMap = new TIntObjectHashMap(this.queryResults.size() * 3);
         final EdgeExplorer mainExplorer = this.mainGraph.createEdgeExplorer(edgeFilter);
         TIntHashSet towerNodesToChange = new TIntHashSet(this.queryResults.size());

         for(int i = 0; i < this.queryResults.size(); ++i) {
            VirtualEdgeIterator virtEdgeIter = new VirtualEdgeIterator(2);
            EdgeIteratorState baseRevEdge = (EdgeIteratorState)this.virtualEdges.get(i * 4 + 1);
            if(edgeFilter.accept(baseRevEdge)) {
               virtEdgeIter.add(baseRevEdge);
            }

            EdgeIteratorState adjEdge = (EdgeIteratorState)this.virtualEdges.get(i * 4 + 2);
            if(edgeFilter.accept(adjEdge)) {
               virtEdgeIter.add(adjEdge);
            }

            int virtNode = this.mainNodes + i;
            node2EdgeMap.put(virtNode, virtEdgeIter);
            int towerNode = baseRevEdge.getAdjNode();
            if(!this.isVirtualNode(towerNode)) {
               towerNodesToChange.add(towerNode);
               this.addVirtualEdges(node2EdgeMap, edgeFilter, true, towerNode, i);
            }

            towerNode = adjEdge.getAdjNode();
            if(!this.isVirtualNode(towerNode)) {
               towerNodesToChange.add(towerNode);
               this.addVirtualEdges(node2EdgeMap, edgeFilter, false, towerNode, i);
            }
         }

         towerNodesToChange.forEach(new TIntProcedure() {
            public boolean execute(int value) {
               QueryGraph.this.fillVirtualEdges(node2EdgeMap, value, mainExplorer);
               return true;
            }
         });
         return new EdgeExplorer() {
            public EdgeIterator setBaseNode(int baseNode) {
               VirtualEdgeIterator iter = (VirtualEdgeIterator)node2EdgeMap.get(baseNode);
               return iter != null?iter.reset():mainExplorer.setBaseNode(baseNode);
            }
         };
      }
   }

   private void addVirtualEdges(TIntObjectMap node2EdgeMap, EdgeFilter filter, boolean base, int node, int virtNode) {
      VirtualEdgeIterator existingIter = (VirtualEdgeIterator)node2EdgeMap.get(node);
      if(existingIter == null) {
         existingIter = new VirtualEdgeIterator(10);
         node2EdgeMap.put(node, existingIter);
      }

      VirtualEdgeIteratorState edge = base?(VirtualEdgeIteratorState)this.virtualEdges.get(virtNode * 4 + 0):(VirtualEdgeIteratorState)this.virtualEdges.get(virtNode * 4 + 3);
      if(filter.accept(edge)) {
         existingIter.add(edge);
      }

   }

   void fillVirtualEdges(TIntObjectMap node2Edge, int towerNode, EdgeExplorer mainExpl) {
      if(this.isVirtualNode(towerNode)) {
         throw new IllegalStateException("Node should not be virtual:" + towerNode + ", " + node2Edge);
      } else {
         VirtualEdgeIterator vIter = (VirtualEdgeIterator)node2Edge.get(towerNode);
         TIntArrayList ignoreEdges = new TIntArrayList(vIter.count() * 2);

         while(vIter.next()) {
            EdgeIteratorState iter = ((QueryResult)this.queryResults.get(vIter.getAdjNode() - this.mainNodes)).getClosestEdge();
            ignoreEdges.add(iter.getEdge());
         }

         vIter.reset();
         EdgeIterator iter1 = mainExpl.setBaseNode(towerNode);

         while(iter1.next()) {
            if(!ignoreEdges.contains(iter1.getEdge())) {
               vIter.add(iter1.detach(false));
            }
         }

      }
   }

   private boolean isInitialized() {
      return this.queryResults != null;
   }

   public EdgeExplorer createEdgeExplorer() {
      return this.createEdgeExplorer(EdgeFilter.ALL_EDGES);
   }

   public AllEdgesIterator getAllEdges() {
      throw new UnsupportedOperationException("Not supported yet.");
   }

   public EdgeIteratorState edge(int a, int b) {
      throw this.exc();
   }

   public EdgeIteratorState edge(int a, int b, double distance, int flags) {
      throw this.exc();
   }

   public EdgeIteratorState edge(int a, int b, double distance, boolean bothDirections) {
      throw this.exc();
   }

   public Graph copyTo(Graph g) {
      throw this.exc();
   }

   public GraphExtension getExtension() {
      return this.wrappedExtension;
   }

   private UnsupportedOperationException exc() {
      return new UnsupportedOperationException("QueryGraph cannot be modified.");
   }

   class QueryGraphTurnExt extends TurnCostExtension {
      private final TurnCostExtension mainTurnExtension;

      public QueryGraphTurnExt(QueryGraph qGraph) {
         this.mainTurnExtension = (TurnCostExtension)QueryGraph.this.mainGraph.getExtension();
      }

      public long getTurnCostFlags(int edgeFrom, int nodeVia, int edgeTo) {
         if(QueryGraph.this.isVirtualNode(nodeVia)) {
            return 0L;
         } else if(!QueryGraph.this.isVirtualEdge(edgeFrom) && !QueryGraph.this.isVirtualEdge(edgeTo)) {
            return this.mainTurnExtension.getTurnCostFlags(edgeFrom, nodeVia, edgeTo);
         } else {
            if(QueryGraph.this.isVirtualEdge(edgeFrom)) {
               edgeFrom = ((QueryResult)QueryGraph.this.queryResults.get((edgeFrom - QueryGraph.this.mainEdges) / 4)).getClosestEdge().getEdge();
            }

            if(QueryGraph.this.isVirtualEdge(edgeTo)) {
               edgeTo = ((QueryResult)QueryGraph.this.queryResults.get((edgeTo - QueryGraph.this.mainEdges) / 4)).getClosestEdge().getEdge();
            }

            return this.mainTurnExtension.getTurnCostFlags(edgeFrom, nodeVia, edgeTo);
         }
      }
   }
}
