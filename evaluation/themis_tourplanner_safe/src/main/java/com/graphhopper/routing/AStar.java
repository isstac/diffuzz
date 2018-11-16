package com.graphhopper.routing;

import com.graphhopper.routing.AbstractRoutingAlgorithm;
import com.graphhopper.routing.Path;
import com.graphhopper.routing.util.BeelineWeightApproximator;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.TraversalMode;
import com.graphhopper.routing.util.WeightApproximator;
import com.graphhopper.routing.util.Weighting;
import com.graphhopper.storage.EdgeEntry;
import com.graphhopper.storage.Graph;
import com.graphhopper.util.DistancePlaneProjection;
import com.graphhopper.util.EdgeExplorer;
import com.graphhopper.util.EdgeIterator;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import java.util.PriorityQueue;

public class AStar extends AbstractRoutingAlgorithm {
   private WeightApproximator weightApprox;
   private int visitedCount;
   private TIntObjectMap fromMap;
   private PriorityQueue prioQueueOpenSet;
   private AStar.AStarEdge currEdge;
   private int to1 = -1;

   public AStar(Graph g, FlagEncoder encoder, Weighting weighting, TraversalMode tMode) {
      super(g, encoder, weighting, tMode);
      this.initCollections(1000);
      BeelineWeightApproximator defaultApprox = new BeelineWeightApproximator(this.nodeAccess, weighting);
      defaultApprox.setDistanceCalc(new DistancePlaneProjection());
      this.setApproximation(defaultApprox);
   }

   public AStar setApproximation(WeightApproximator approx) {
      this.weightApprox = approx;
      return this;
   }

   protected void initCollections(int size) {
      this.fromMap = new TIntObjectHashMap();
      this.prioQueueOpenSet = new PriorityQueue(size);
   }

   public Path calcPath(int from, int to) {
      this.checkAlreadyRun();
      this.to1 = to;
      this.weightApprox.setGoalNode(to);
      double weightToGoal = this.weightApprox.approximate(from);
      this.currEdge = new AStar.AStarEdge(-1, from, 0.0D + weightToGoal, 0.0D);
      if(!this.traversalMode.isEdgeBased()) {
         this.fromMap.put(from, this.currEdge);
      }

      return this.runAlgo();
   }

   private Path runAlgo() {
      EdgeExplorer explorer = this.outEdgeExplorer;

      label63:
      do {
         int currVertex = this.currEdge.adjNode;
         ++this.visitedCount;
         if(this.isWeightLimitExceeded()) {
            return this.createEmptyPath();
         }

         if(this.finished()) {
            return this.extractPath();
         }

         EdgeIterator iter = explorer.setBaseNode(currVertex);

         while(true) {
            int neighborNode;
            int traversalId;
            float alreadyVisitedWeight;
            AStar.AStarEdge ase;
            do {
               do {
                  do {
                     if(!iter.next()) {
                        if(this.prioQueueOpenSet.isEmpty()) {
                           return this.createEmptyPath();
                        }

                        this.currEdge = (AStar.AStarEdge)this.prioQueueOpenSet.poll();
                        continue label63;
                     }
                  } while(!this.accept(iter, this.currEdge.edge));

                  neighborNode = iter.getAdjNode();
                  traversalId = this.traversalMode.createTraversalId(iter, false);
                  alreadyVisitedWeight = (float)(this.weighting.calcWeight(iter, false, this.currEdge.edge) + this.currEdge.weightOfVisitedPath);
               } while(Double.isInfinite((double)alreadyVisitedWeight));

               ase = (AStar.AStarEdge)this.fromMap.get(traversalId);
            } while(ase != null && ase.weightOfVisitedPath <= (double)alreadyVisitedWeight);

            double currWeightToGoal = this.weightApprox.approximate(neighborNode);
            double distEstimation = (double)alreadyVisitedWeight + currWeightToGoal;
            if(ase == null) {
               ase = new AStar.AStarEdge(iter.getEdge(), neighborNode, distEstimation, (double)alreadyVisitedWeight);
               this.fromMap.put(traversalId, ase);
            } else {
               assert ase.weight > distEstimation : "Inconsistent distance estimate";

               this.prioQueueOpenSet.remove(ase);
               ase.edge = iter.getEdge();
               ase.weight = distEstimation;
               ase.weightOfVisitedPath = (double)alreadyVisitedWeight;
            }

            ase.parent = this.currEdge;
            this.prioQueueOpenSet.add(ase);
            this.updateBestPath(iter, ase, traversalId);
         }
      } while(this.currEdge != null);

      throw new AssertionError("Empty edge cannot happen");
   }

   protected Path extractPath() {
      return (new Path(this.graph, this.flagEncoder)).setWeight(this.currEdge.weight).setEdgeEntry(this.currEdge).extract();
   }

   protected EdgeEntry createEdgeEntry(int node, double weight) {
      throw new IllegalStateException("use AStarEdge constructor directly");
   }

   protected boolean finished() {
      return this.currEdge.adjNode == this.to1;
   }

   public int getVisitedNodes() {
      return this.visitedCount;
   }

   protected boolean isWeightLimitExceeded() {
      return this.currEdge.weight > this.weightLimit;
   }

   public String getName() {
      return "astar";
   }

   public static class AStarEdge extends EdgeEntry {
      double weightOfVisitedPath;

      public AStarEdge(int edgeId, int adjNode, double weightForHeap, double weightOfVisitedPath) {
         super(edgeId, adjNode, weightForHeap);
         this.weightOfVisitedPath = (double)((float)weightOfVisitedPath);
      }
   }
}
