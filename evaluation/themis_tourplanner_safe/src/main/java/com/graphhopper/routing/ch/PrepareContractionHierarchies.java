package com.graphhopper.routing.ch;

import com.graphhopper.coll.GHTreeMapComposed;
import com.graphhopper.routing.AStarBidirection;
import com.graphhopper.routing.AbstractBidirAlgo;
import com.graphhopper.routing.AlgorithmOptions;
import com.graphhopper.routing.DijkstraBidirectionRef;
import com.graphhopper.routing.DijkstraOneToMany;
import com.graphhopper.routing.Path;
import com.graphhopper.routing.RoutingAlgorithm;
import com.graphhopper.routing.RoutingAlgorithmFactory;
import com.graphhopper.routing.ch.Path4CH;
import com.graphhopper.routing.ch.PreparationWeighting;
import com.graphhopper.routing.ch.PrepareEncoder;
import com.graphhopper.routing.util.AbstractAlgoPreparation;
import com.graphhopper.routing.util.DefaultEdgeFilter;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.LevelEdgeFilter;
import com.graphhopper.routing.util.TraversalMode;
import com.graphhopper.routing.util.Weighting;
import com.graphhopper.storage.CHGraph;
import com.graphhopper.storage.CHGraphImpl;
import com.graphhopper.storage.DataAccess;
import com.graphhopper.storage.Directory;
import com.graphhopper.storage.Graph;
import com.graphhopper.storage.GraphHopperStorage;
import com.graphhopper.storage.NodeAccess;
import com.graphhopper.util.CHEdgeExplorer;
import com.graphhopper.util.CHEdgeIterator;
import com.graphhopper.util.CHEdgeIteratorState;
import com.graphhopper.util.EdgeIterator;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.GHUtility;
import com.graphhopper.util.Helper;
import com.graphhopper.util.StopWatch;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

public class PrepareContractionHierarchies extends AbstractAlgoPreparation implements RoutingAlgorithmFactory {
//   private final Logger logger = LoggerFactory.getLogger(this.getClass());
   private final PreparationWeighting prepareWeighting;
   private final FlagEncoder prepareFlagEncoder;
   private final TraversalMode traversalMode;
   private CHEdgeExplorer vehicleInExplorer;
   private CHEdgeExplorer vehicleOutExplorer;
   private CHEdgeExplorer vehicleAllExplorer;
   private CHEdgeExplorer vehicleAllTmpExplorer;
   private CHEdgeExplorer calcPrioAllExplorer;
   private final LevelEdgeFilter levelFilter;
   private int maxLevel;
   private final GraphHopperStorage ghStorage;
   private final CHGraphImpl prepareGraph;
   private GHTreeMapComposed sortedNodes;
   private int[] oldPriorities;
   private final DataAccess originalEdges;
   private final Map shortcuts = new HashMap();
   private PrepareContractionHierarchies.IgnoreNodeFilter ignoreNodeFilter;
   private DijkstraOneToMany prepareAlgo;
   private long counter;
   private int newShortcuts;
   private long dijkstraCount;
   private double meanDegree;
   private final Random rand = new Random(123L);
   private StopWatch dijkstraSW = new StopWatch();
   private final StopWatch allSW = new StopWatch();
   private int periodicUpdatesPercentage = 20;
   private int lastNodesLazyUpdatePercentage = 10;
   private int neighborUpdatePercentage = 20;
   private int initialCollectionSize = 5000;
   private double nodesContractedPercentage = 100.0D;
   private double logMessagesPercentage = 20.0D;
   private double dijkstraTime;
   private double periodTime;
   private double lazyTime;
   private double neighborTime;
   private int maxEdgesCount;
   PrepareContractionHierarchies.AddShortcutHandler addScHandler = new PrepareContractionHierarchies.AddShortcutHandler();
   PrepareContractionHierarchies.CalcShortcutHandler calcScHandler = new PrepareContractionHierarchies.CalcShortcutHandler();

   public PrepareContractionHierarchies(Directory dir, GraphHopperStorage ghStorage, CHGraph chGraph, FlagEncoder encoder, Weighting weighting, TraversalMode traversalMode) {
      this.ghStorage = ghStorage;
      this.prepareGraph = (CHGraphImpl)chGraph;
      this.traversalMode = traversalMode;
      this.prepareFlagEncoder = encoder;
      this.levelFilter = new LevelEdgeFilter(this.prepareGraph);
      this.prepareWeighting = new PreparationWeighting(weighting);
      this.originalEdges = dir.find("original_edges_" + this.prepareGraph.weightingToFileName(weighting));
      this.originalEdges.create(1000L);
   }

   public PrepareContractionHierarchies setPeriodicUpdates(int periodicUpdates) {
      if(periodicUpdates < 0) {
         return this;
      } else if(periodicUpdates > 100) {
         throw new IllegalArgumentException("periodicUpdates has to be in [0, 100], to disable it use 0");
      } else {
         this.periodicUpdatesPercentage = periodicUpdates;
         return this;
      }
   }

   public PrepareContractionHierarchies setLazyUpdates(int lazyUpdates) {
      if(lazyUpdates < 0) {
         return this;
      } else if(lazyUpdates > 100) {
         throw new IllegalArgumentException("lazyUpdates has to be in [0, 100], to disable it use 0");
      } else {
         this.lastNodesLazyUpdatePercentage = lazyUpdates;
         return this;
      }
   }

   public PrepareContractionHierarchies setNeighborUpdates(int neighborUpdates) {
      if(neighborUpdates < 0) {
         return this;
      } else if(neighborUpdates > 100) {
         throw new IllegalArgumentException("neighborUpdates has to be in [0, 100], to disable it use 0");
      } else {
         this.neighborUpdatePercentage = neighborUpdates;
         return this;
      }
   }

   public PrepareContractionHierarchies setLogMessages(double logMessages) {
      if(logMessages >= 0.0D) {
         this.logMessagesPercentage = logMessages;
      }

      return this;
   }

   public PrepareContractionHierarchies setContractedNodes(double nodesContracted) {
      if(nodesContracted < 0.0D) {
         return this;
      } else if(nodesContracted > 100.0D) {
         throw new IllegalArgumentException("setNodesContracted can be 100% maximum");
      } else {
         this.nodesContractedPercentage = nodesContracted;
         return this;
      }
   }

   public void setInitialCollectionSize(int initialCollectionSize) {
      this.initialCollectionSize = initialCollectionSize;
   }

   public void doWork() {
      if(this.prepareFlagEncoder == null) {
         throw new IllegalStateException("No vehicle encoder set.");
      } else if(this.prepareWeighting == null) {
         throw new IllegalStateException("No weight calculation set.");
      } else {
         this.allSW.start();
         super.doWork();
         this.initFromGraph();
         if(this.prepareNodes()) {
            this.contractNodes();
         }
      }
   }

   boolean prepareNodes() {
      int nodes = this.prepareGraph.getNodes();

      int node;
      for(node = 0; node < nodes; ++node) {
         this.prepareGraph.setLevel(node, this.maxLevel);
      }

      for(node = 0; node < nodes; ++node) {
         int priority = this.oldPriorities[node] = this.calculatePriority(node);
         this.sortedNodes.insert(node, priority);
      }

      return !this.sortedNodes.isEmpty();
   }

   void contractNodes() {
      this.meanDegree = (double)(this.prepareGraph.getAllEdges().getMaxId() / this.prepareGraph.getNodes());
      int level = 1;
      this.counter = 0L;
      int initSize = this.sortedNodes.getSize();
      long logSize = Math.round(Math.max(10.0D, (double)(this.sortedNodes.getSize() / 100) * this.logMessagesPercentage));
      if(this.logMessagesPercentage == 0.0D) {
         logSize = 2147483647L;
      }

      boolean periodicUpdate = true;
      StopWatch periodSW = new StopWatch();
      int updateCounter = 0;
      long periodicUpdatesCount = Math.round(Math.max(10.0D, (double)this.sortedNodes.getSize() / 100.0D * (double)this.periodicUpdatesPercentage));
      if(this.periodicUpdatesPercentage == 0) {
         periodicUpdate = false;
      }

      long lastNodesLazyUpdates = Math.round((double)this.sortedNodes.getSize() / 100.0D * (double)this.lastNodesLazyUpdatePercentage);
      long nodesToAvoidContract = Math.round((100.0D - this.nodesContractedPercentage) / 100.0D * (double)this.sortedNodes.getSize());
      StopWatch lazySW = new StopWatch();
      boolean neighborUpdate = true;
      if(this.neighborUpdatePercentage == 0) {
         neighborUpdate = false;
      }

      StopWatch neighborSW = new StopWatch();

      while(!this.sortedNodes.isEmpty()) {
         int polledNode;
         int iter;
         int nn;
         if(periodicUpdate && this.counter > 0L && this.counter % periodicUpdatesCount == 0L) {
            periodSW.start();
            this.sortedNodes.clear();
            polledNode = this.prepareGraph.getNodes();

            for(iter = 0; iter < polledNode; ++iter) {
               if(this.prepareGraph.getLevel(iter) == this.maxLevel) {
                  nn = this.oldPriorities[iter] = this.calculatePriority(iter);
                  this.sortedNodes.insert(iter, nn);
               }
            }

            periodSW.stop();
            ++updateCounter;
            if(this.sortedNodes.isEmpty()) {
               throw new IllegalStateException("Cannot prepare as no unprepared nodes where found. Called preparation twice?");
            }
         }

         if(this.counter % logSize == 0L) {
            this.dijkstraTime += (double)this.dijkstraSW.getSeconds();
            this.periodTime += (double)periodSW.getSeconds();
            this.lazyTime += (double)lazySW.getSeconds();
            this.neighborTime += (double)neighborSW.getSeconds();
//            this.logger.info(Helper.nf(this.counter) + ", updates:" + updateCounter + ", nodes: " + Helper.nf((long)this.sortedNodes.getSize()) + ", shortcuts:" + Helper.nf((long)this.newShortcuts) + ", dijkstras:" + Helper.nf(this.dijkstraCount) + ", " + this.getTimesAsString() + ", meanDegree:" + (long)this.meanDegree + ", algo:" + this.prepareAlgo.getMemoryUsageAsString() + ", " + Helper.getMemInfo());
            this.dijkstraSW = new StopWatch();
            periodSW = new StopWatch();
            lazySW = new StopWatch();
            neighborSW = new StopWatch();
         }

         ++this.counter;
         polledNode = this.sortedNodes.pollKey();
         if(!this.sortedNodes.isEmpty() && (long)this.sortedNodes.getSize() < lastNodesLazyUpdates) {
            lazySW.start();
            iter = this.oldPriorities[polledNode] = this.calculatePriority(polledNode);
            if(iter > this.sortedNodes.peekValue()) {
               this.sortedNodes.insert(polledNode, iter);
               lazySW.stop();
               continue;
            }

            lazySW.stop();
         }

         this.newShortcuts += this.addShortcuts(polledNode);
         this.prepareGraph.setLevel(polledNode, level);
         ++level;
         if((long)this.sortedNodes.getSize() < nodesToAvoidContract) {
            break;
         }

         CHEdgeIterator var22 = this.vehicleAllExplorer.setBaseNode(polledNode);

         while(var22.next()) {
            nn = var22.getAdjNode();
            if(this.prepareGraph.getLevel(nn) == this.maxLevel) {
               if(neighborUpdate && this.rand.nextInt(100) < this.neighborUpdatePercentage) {
                  neighborSW.start();
                  int oldPrio = this.oldPriorities[nn];
                  int priority = this.oldPriorities[nn] = this.calculatePriority(nn);
                  if(priority != oldPrio) {
                     this.sortedNodes.update(nn, oldPrio, priority);
                  }

                  neighborSW.stop();
               }

               this.prepareGraph.disconnect(this.vehicleAllTmpExplorer, var22);
            }
         }
      }

      this.close();
      this.dijkstraTime += (double)this.dijkstraSW.getSeconds();
      this.periodTime += (double)periodSW.getSeconds();
      this.lazyTime += (double)lazySW.getSeconds();
      this.neighborTime += (double)neighborSW.getSeconds();
//      this.logger.info("took:" + (int)this.allSW.stop().getSeconds() + ", new shortcuts: " + Helper.nf((long)this.newShortcuts) + ", " + this.prepareWeighting + ", " + this.prepareFlagEncoder + ", dijkstras:" + this.dijkstraCount + ", " + this.getTimesAsString() + ", meanDegree:" + (long)this.meanDegree + ", initSize:" + initSize + ", periodic:" + this.periodicUpdatesPercentage + ", lazy:" + this.lastNodesLazyUpdatePercentage + ", neighbor:" + this.neighborUpdatePercentage + ", " + Helper.getMemInfo());
   }

   public long getDijkstraCount() {
      return this.dijkstraCount;
   }

   public double getLazyTime() {
      return this.lazyTime;
   }

   public double getPeriodTime() {
      return this.periodTime;
   }

   public double getDijkstraTime() {
      return this.dijkstraTime;
   }

   public double getNeighborTime() {
      return this.neighborTime;
   }

   public void close() {
      this.prepareAlgo.close();
      this.originalEdges.close();
      this.sortedNodes = null;
      this.oldPriorities = null;
   }

   private String getTimesAsString() {
      return "t(dijk):" + Helper.round2(this.dijkstraTime) + ", t(period):" + Helper.round2(this.periodTime) + ", t(lazy):" + Helper.round2(this.lazyTime) + ", t(neighbor):" + Helper.round2(this.neighborTime);
   }

   Set testFindShortcuts(int node) {
      this.findShortcuts(this.addScHandler.setNode(node));
      return this.shortcuts.keySet();
   }

   int calculatePriority(int v) {
      this.findShortcuts(this.calcScHandler.setNode(v));
      int originalEdgesCount = this.calcScHandler.originalEdgesCount;
      int contractedNeighbors = 0;
      int degree = 0;
      CHEdgeIterator iter = this.calcPrioAllExplorer.setBaseNode(v);

      while(iter.next()) {
         ++degree;
         if(iter.isShortcut()) {
            ++contractedNeighbors;
         }
      }

      int edgeDifference = this.calcScHandler.shortcuts - degree;
      return 10 * edgeDifference + originalEdgesCount + contractedNeighbors;
   }

   void findShortcuts(PrepareContractionHierarchies.ShortcutHandler sch) {
      long tmpDegreeCounter = 0L;
      CHEdgeIterator incomingEdges = this.vehicleInExplorer.setBaseNode(sch.getNode());

      label58:
      while(true) {
         int u_fromNode;
         do {
            if(!incomingEdges.next()) {
               if(sch instanceof PrepareContractionHierarchies.AddShortcutHandler) {
                  this.meanDegree = (this.meanDegree * 2.0D + (double)tmpDegreeCounter) / 3.0D;
               }

               return;
            }

            u_fromNode = incomingEdges.getAdjNode();
         } while(this.prepareGraph.getLevel(u_fromNode) != this.maxLevel);

         double v_u_dist = incomingEdges.getDistance();
         double v_u_weight = this.prepareWeighting.calcWeight(incomingEdges, true, -1);
         int skippedEdge1 = incomingEdges.getEdge();
         int incomingEdgeOrigCount = this.getOrigEdgeCount(skippedEdge1);
         CHEdgeIterator outgoingEdges = this.vehicleOutExplorer.setBaseNode(sch.getNode());
         this.prepareAlgo.clear();
         ++tmpDegreeCounter;

         while(true) {
            int w_toNode;
            double existingDirectWeight;
            double existingDistSum;
            int endNode;
            do {
               do {
                  do {
                     do {
                        if(!outgoingEdges.next()) {
                           continue label58;
                        }

                        w_toNode = outgoingEdges.getAdjNode();
                     } while(this.prepareGraph.getLevel(w_toNode) != this.maxLevel);
                  } while(u_fromNode == w_toNode);

                  existingDirectWeight = v_u_weight + this.prepareWeighting.calcWeight(outgoingEdges, false, incomingEdges.getEdge());
                  if(Double.isNaN(existingDirectWeight)) {
                     throw new IllegalStateException("Weighting should never return NaN values, in:" + this.getCoords(incomingEdges, this.prepareGraph) + ", out:" + this.getCoords(outgoingEdges, this.prepareGraph) + ", dist:" + outgoingEdges.getDistance());
                  }
               } while(Double.isInfinite(existingDirectWeight));

               existingDistSum = v_u_dist + outgoingEdges.getDistance();
               this.prepareAlgo.setWeightLimit(existingDirectWeight);
               this.prepareAlgo.setLimitVisitedNodes((int)this.meanDegree * 100).setEdgeFilter(this.ignoreNodeFilter.setAvoidNode(sch.getNode()));
               this.dijkstraSW.start();
               ++this.dijkstraCount;
               endNode = this.prepareAlgo.findEndNode(u_fromNode, w_toNode);
               this.dijkstraSW.stop();
            } while(endNode == w_toNode && this.prepareAlgo.getWeight(endNode) <= existingDirectWeight);

            sch.foundShortcut(u_fromNode, w_toNode, existingDirectWeight, existingDistSum, outgoingEdges, skippedEdge1, incomingEdgeOrigCount);
         }
      }
   }

   int addShortcuts(int v) {
      this.shortcuts.clear();
      this.findShortcuts(this.addScHandler.setNode(v));
      int tmpNewShortcuts = 0;
      Iterator i$ = this.shortcuts.keySet().iterator();

      label43:
      while(i$.hasNext()) {
         PrepareContractionHierarchies.Shortcut sc = (PrepareContractionHierarchies.Shortcut)i$.next();
         boolean updatedInGraph = false;
         CHEdgeIterator iter = this.vehicleOutExplorer.setBaseNode(sc.from);

         while(iter.next()) {
            if(iter.isShortcut() && iter.getAdjNode() == sc.to && iter.canBeOverwritten(sc.flags)) {
               if(sc.weight >= this.prepareWeighting.calcWeight(iter, false, -1)) {
                  continue label43;
               }

               if(iter.getEdge() == sc.skippedEdge1 || iter.getEdge() == sc.skippedEdge2) {
                  throw new IllegalStateException("Shortcut cannot update itself! " + iter.getEdge() + ", skipEdge1:" + sc.skippedEdge1 + ", skipEdge2:" + sc.skippedEdge2 + ", edge " + iter + ":" + this.getCoords(iter, this.prepareGraph) + ", sc:" + sc + ", skippedEdge1: " + this.getCoords(this.prepareGraph.getEdgeIteratorState(sc.skippedEdge1, sc.from), this.prepareGraph) + ", skippedEdge2: " + this.getCoords(this.prepareGraph.getEdgeIteratorState(sc.skippedEdge2, sc.to), this.prepareGraph) + ", neighbors:" + GHUtility.getNeighbors(iter));
               }

               iter.setFlags(sc.flags);
               iter.setWeight(sc.weight);
               iter.setDistance(sc.dist);
               iter.setSkippedEdges(sc.skippedEdge1, sc.skippedEdge2);
               this.setOrigEdgeCount(iter.getEdge(), sc.originalEdges);
               updatedInGraph = true;
               break;
            }
         }

         if(!updatedInGraph) {
            CHEdgeIteratorState edgeState = this.prepareGraph.shortcut(sc.from, sc.to);
            edgeState.setFlags(sc.flags);
            edgeState.setWeight(sc.weight);
            edgeState.setDistance(sc.dist);
            edgeState.setSkippedEdges(sc.skippedEdge1, sc.skippedEdge2);
            this.setOrigEdgeCount(edgeState.getEdge(), sc.originalEdges);
            ++tmpNewShortcuts;
         }
      }

      return tmpNewShortcuts;
   }

   String getCoords(EdgeIteratorState e, Graph g) {
      NodeAccess na = g.getNodeAccess();
      int base = e.getBaseNode();
      int adj = e.getAdjNode();
      return base + "->" + adj + " (" + e.getEdge() + "); " + na.getLat(base) + "," + na.getLon(base) + " -> " + na.getLat(adj) + "," + na.getLon(adj);
   }

   PrepareContractionHierarchies initFromGraph() {
      this.ghStorage.freeze();
      this.maxEdgesCount = this.ghStorage.getAllEdges().getMaxId();
      this.vehicleInExplorer = this.prepareGraph.createEdgeExplorer(new DefaultEdgeFilter(this.prepareFlagEncoder, true, false));
      this.vehicleOutExplorer = this.prepareGraph.createEdgeExplorer(new DefaultEdgeFilter(this.prepareFlagEncoder, false, true));
      final DefaultEdgeFilter allFilter = new DefaultEdgeFilter(this.prepareFlagEncoder, true, true);
      LevelEdgeFilter accessWithLevelFilter = new LevelEdgeFilter(this.prepareGraph) {
         public final boolean accept(EdgeIteratorState edgeState) {
            return !super.accept(edgeState)?false:allFilter.accept(edgeState);
         }
      };
      this.maxLevel = this.prepareGraph.getNodes() + 1;
      this.ignoreNodeFilter = new PrepareContractionHierarchies.IgnoreNodeFilter(this.prepareGraph, this.maxLevel);
      this.vehicleAllExplorer = this.prepareGraph.createEdgeExplorer(allFilter);
      this.vehicleAllTmpExplorer = this.prepareGraph.createEdgeExplorer(allFilter);
      this.calcPrioAllExplorer = this.prepareGraph.createEdgeExplorer(accessWithLevelFilter);
      this.sortedNodes = new GHTreeMapComposed();
      this.oldPriorities = new int[this.prepareGraph.getNodes()];
      this.prepareAlgo = new DijkstraOneToMany(this.prepareGraph, this.prepareFlagEncoder, this.prepareWeighting, this.traversalMode);
      return this;
   }

   public int getShortcuts() {
      return this.newShortcuts;
   }

   private void setOrigEdgeCount(int edgeId, int value) {
      edgeId -= this.maxEdgesCount;
      if(edgeId < 0) {
         if(value != 1) {
            throw new IllegalStateException("Trying to set original edge count for normal edge to a value = " + value + ", edge:" + (edgeId + this.maxEdgesCount) + ", max:" + this.maxEdgesCount + ", graph.max:" + this.ghStorage.getAllEdges().getMaxId());
         }
      } else {
         long tmp = (long)edgeId * 4L;
         this.originalEdges.ensureCapacity(tmp + 4L);
         this.originalEdges.setInt(tmp, value);
      }
   }

   private int getOrigEdgeCount(int edgeId) {
      edgeId -= this.maxEdgesCount;
      if(edgeId < 0) {
         return 1;
      } else {
         long tmp = (long)edgeId * 4L;
         this.originalEdges.ensureCapacity(tmp + 4L);
         return this.originalEdges.getInt(tmp);
      }
   }

   public RoutingAlgorithm createAlgo(final Graph graph, AlgorithmOptions opts) {
      Object algo;
      if("astarbi".equals(opts.getAlgorithm())) {
         AStarBidirection astarBi = new AStarBidirection(graph, this.prepareFlagEncoder, this.prepareWeighting, this.traversalMode) {
            protected void initCollections(int nodes) {
               super.initCollections(Math.min(PrepareContractionHierarchies.this.initialCollectionSize, nodes));
            }

            protected boolean finished() {
               return this.finishedFrom && this.finishedTo?true:this.currFrom.weight >= this.bestPath.getWeight() && this.currTo.weight >= this.bestPath.getWeight();
            }

            protected boolean isWeightLimitExceeded() {
               return this.currFrom.weight > this.weightLimit && this.currTo.weight > this.weightLimit;
            }

            protected Path createAndInitPath() {
               this.bestPath = new Path4CH(this.graph, this.graph.getBaseGraph(), this.flagEncoder);
               return this.bestPath;
            }

            public String getName() {
               return "astarbiCH";
            }

            public String toString() {
               return this.getName() + "|" + PrepareContractionHierarchies.this.prepareWeighting;
            }
         };
         algo = astarBi;
      } else {
         if(!"dijkstrabi".equals(opts.getAlgorithm())) {
            throw new UnsupportedOperationException("Algorithm " + opts.getAlgorithm() + " not supported for Contraction Hierarchies");
         }

         algo = new DijkstraBidirectionRef(graph, this.prepareFlagEncoder, this.prepareWeighting, this.traversalMode) {
            protected void initCollections(int nodes) {
               super.initCollections(Math.min(PrepareContractionHierarchies.this.initialCollectionSize, nodes));
            }

            public boolean finished() {
               return this.finishedFrom && this.finishedTo?true:this.currFrom.weight >= this.bestPath.getWeight() && this.currTo.weight >= this.bestPath.getWeight();
            }

            protected boolean isWeightLimitExceeded() {
               return this.currFrom.weight > this.weightLimit && this.currTo.weight > this.weightLimit;
            }

            protected Path createAndInitPath() {
               this.bestPath = new Path4CH(this.graph, this.graph.getBaseGraph(), this.flagEncoder);
               return this.bestPath;
            }

            public String getName() {
               return "dijkstrabiCH";
            }

            public String toString() {
               return this.getName() + "|" + PrepareContractionHierarchies.this.prepareWeighting;
            }
         };
      }

      ((AbstractBidirAlgo)algo).setEdgeFilter(this.levelFilter);
      return (RoutingAlgorithm)algo;
   }

   public String toString() {
      return "PREPARE|CH|dijkstrabi";
   }

   class Shortcut {
      int from;
      int to;
      int skippedEdge1;
      int skippedEdge2;
      double dist;
      double weight;
      int originalEdges;
      long flags = PrepareEncoder.getScFwdDir();

      public Shortcut(int from, int to, double weight, double dist) {
         this.from = from;
         this.to = to;
         this.weight = weight;
         this.dist = dist;
      }

      public int hashCode() {
         byte hash = 5;
         int hash1 = 23 * hash + this.from;
         hash1 = 23 * hash1 + this.to;
         return 23 * hash1 + (int)(Double.doubleToLongBits(this.weight) ^ Double.doubleToLongBits(this.weight) >>> 32);
      }

      public boolean equals(Object obj) {
         if(obj != null && this.getClass() == obj.getClass()) {
            PrepareContractionHierarchies.Shortcut other = (PrepareContractionHierarchies.Shortcut)obj;
            return this.from == other.from && this.to == other.to?Double.doubleToLongBits(this.weight) == Double.doubleToLongBits(other.weight):false;
         } else {
            return false;
         }
      }

      public String toString() {
         String str;
         if(this.flags == PrepareEncoder.getScDirMask()) {
            str = this.from + "<->";
         } else {
            str = this.from + "->";
         }

         return str + this.to + ", weight:" + this.weight + " (" + this.skippedEdge1 + "," + this.skippedEdge2 + ")";
      }
   }

   private static class PriorityNode implements Comparable {
      int node;
      int priority;

      public PriorityNode(int node, int priority) {
         this.node = node;
         this.priority = priority;
      }

      public String toString() {
         return this.node + " (" + this.priority + ")";
      }

      public int compareTo(Object o) {
         return this.priority - ((PrepareContractionHierarchies.PriorityNode)o).priority;
      }
   }

   static class IgnoreNodeFilter implements EdgeFilter {
      int avoidNode;
      int maxLevel;
      CHGraph graph;

      public IgnoreNodeFilter(CHGraph g, int maxLevel) {
         this.graph = g;
         this.maxLevel = maxLevel;
      }

      public PrepareContractionHierarchies.IgnoreNodeFilter setAvoidNode(int node) {
         this.avoidNode = node;
         return this;
      }

      public final boolean accept(EdgeIteratorState iter) {
         int node = iter.getAdjNode();
         return this.avoidNode != node && this.graph.getLevel(node) == this.maxLevel;
      }
   }

   class AddShortcutHandler implements PrepareContractionHierarchies.ShortcutHandler {
      int node;

      public int getNode() {
         return this.node;
      }

      public PrepareContractionHierarchies.AddShortcutHandler setNode(int n) {
         PrepareContractionHierarchies.this.shortcuts.clear();
         this.node = n;
         return this;
      }

      public void foundShortcut(int u_fromNode, int w_toNode, double existingDirectWeight, double existingDistSum, EdgeIterator outgoingEdges, int skippedEdge1, int incomingEdgeOrigCount) {
         PrepareContractionHierarchies.Shortcut sc = PrepareContractionHierarchies.this.new Shortcut(u_fromNode, w_toNode, existingDirectWeight, existingDistSum);
         if(!PrepareContractionHierarchies.this.shortcuts.containsKey(sc)) {
            PrepareContractionHierarchies.Shortcut tmpSc = PrepareContractionHierarchies.this.new Shortcut(w_toNode, u_fromNode, existingDirectWeight, existingDistSum);
            PrepareContractionHierarchies.Shortcut tmpRetSc = (PrepareContractionHierarchies.Shortcut)PrepareContractionHierarchies.this.shortcuts.get(tmpSc);
            if(tmpRetSc != null && tmpRetSc.skippedEdge2 == skippedEdge1 && tmpRetSc.skippedEdge1 == outgoingEdges.getEdge()) {
               tmpRetSc.flags = PrepareEncoder.getScDirMask();
            } else {
               PrepareContractionHierarchies.this.shortcuts.put(sc, sc);
               sc.skippedEdge1 = skippedEdge1;
               sc.skippedEdge2 = outgoingEdges.getEdge();
               sc.originalEdges = incomingEdgeOrigCount + PrepareContractionHierarchies.this.getOrigEdgeCount(outgoingEdges.getEdge());
            }
         }
      }
   }

   class CalcShortcutHandler implements PrepareContractionHierarchies.ShortcutHandler {
      int node;
      int originalEdgesCount;
      int shortcuts;

      public PrepareContractionHierarchies.CalcShortcutHandler setNode(int n) {
         this.node = n;
         this.originalEdgesCount = 0;
         this.shortcuts = 0;
         return this;
      }

      public int getNode() {
         return this.node;
      }

      public void foundShortcut(int u_fromNode, int w_toNode, double existingDirectWeight, double distance, EdgeIterator outgoingEdges, int skippedEdge1, int incomingEdgeOrigCount) {
         ++this.shortcuts;
         this.originalEdgesCount += incomingEdgeOrigCount + PrepareContractionHierarchies.this.getOrigEdgeCount(outgoingEdges.getEdge());
      }
   }

   interface ShortcutHandler {
      void foundShortcut(int var1, int var2, double var3, double var5, EdgeIterator var7, int var8, int var9);

      int getNode();
   }
}
