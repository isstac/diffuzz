package com.graphhopper.routing;

import com.graphhopper.coll.IntDoubleBinHeap;
import com.graphhopper.routing.AbstractRoutingAlgorithm;
import com.graphhopper.routing.Path;
import com.graphhopper.routing.PathNative;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.TraversalMode;
import com.graphhopper.routing.util.Weighting;
import com.graphhopper.storage.Graph;
import com.graphhopper.util.EdgeIterator;
import gnu.trove.list.array.TIntArrayList;
import java.util.Arrays;

public class DijkstraOneToMany extends AbstractRoutingAlgorithm {
   private static final int EMPTY_PARENT = -1;
   private static final int NOT_FOUND = -1;
   protected double[] weights;
   private final DijkstraOneToMany.TIntArrayListWithCap changedNodes;
   private int[] parents;
   private int[] edgeIds;
   private IntDoubleBinHeap heap;
   private int visitedNodes;
   private boolean doClear = true;
   private int limitVisitedNodes = Integer.MAX_VALUE;
   private int endNode;
   private int currNode;
   private int fromNode;
   private int to;

   public DijkstraOneToMany(Graph graph, FlagEncoder encoder, Weighting weighting, TraversalMode tMode) {
      super(graph, encoder, weighting, tMode);
      this.parents = new int[graph.getNodes()];
      Arrays.fill(this.parents, -1);
      this.edgeIds = new int[graph.getNodes()];
      Arrays.fill(this.edgeIds, -1);
      this.weights = new double[graph.getNodes()];
      Arrays.fill(this.weights, Double.MAX_VALUE);
      this.heap = new IntDoubleBinHeap();
      this.changedNodes = new DijkstraOneToMany.TIntArrayListWithCap();
   }

   public DijkstraOneToMany setLimitVisitedNodes(int nodes) {
      this.limitVisitedNodes = nodes;
      return this;
   }

   public Path calcPath(int from, int to) {
      this.fromNode = from;
      this.endNode = this.findEndNode(from, to);
      return this.extractPath();
   }

   public Path extractPath() {
      PathNative p = new PathNative(this.graph, this.flagEncoder, this.parents, this.edgeIds);
      if(this.endNode >= 0) {
         p.setWeight(this.weights[this.endNode]);
      }

      p.setFromNode(this.fromNode);
      return (Path)(this.endNode >= 0 && !this.isWeightLimitExceeded()?p.setEndNode(this.endNode).extract():p);
   }

   public DijkstraOneToMany clear() {
      this.doClear = true;
      return this;
   }

   public double getWeight(int endNode) {
      return this.weights[endNode];
   }

   public int findEndNode(int from, int to) {
      if(this.weights.length < 2) {
         return -1;
      } else {
         this.to = to;
         int iter;
         int adjNode;
         int prevEdgeId;
         if(this.doClear) {
            this.doClear = false;
            iter = this.changedNodes.size();

            for(adjNode = 0; adjNode < iter; ++adjNode) {
               prevEdgeId = this.changedNodes.get(adjNode);
               this.weights[prevEdgeId] = Double.MAX_VALUE;
               this.parents[prevEdgeId] = -1;
               this.edgeIds[prevEdgeId] = -1;
            }

            this.heap.clear();
            this.changedNodes.reset();
            this.currNode = from;
            if(!this.traversalMode.isEdgeBased()) {
               this.weights[this.currNode] = 0.0D;
               this.changedNodes.add(this.currNode);
            }
         } else {
            iter = this.parents[to];
            if(iter != -1 && this.weights[to] <= this.weights[this.currNode]) {
               return to;
            }

            if(this.heap.isEmpty() || this.visitedNodes >= this.limitVisitedNodes) {
               return -1;
            }

            this.currNode = this.heap.poll_element();
         }

         this.visitedNodes = 0;
         if(this.finished()) {
            return this.currNode;
         } else {
            while(true) {
               ++this.visitedNodes;
               EdgeIterator var10 = this.outEdgeExplorer.setBaseNode(this.currNode);

               while(var10.next()) {
                  adjNode = var10.getAdjNode();
                  prevEdgeId = this.edgeIds[adjNode];
                  if(this.accept(var10, prevEdgeId)) {
                     double tmpWeight = this.weighting.calcWeight(var10, false, prevEdgeId) + this.weights[this.currNode];
                     if(!Double.isInfinite(tmpWeight)) {
                        double w = this.weights[adjNode];
                        if(w == Double.MAX_VALUE) {
                           this.parents[adjNode] = this.currNode;
                           this.weights[adjNode] = tmpWeight;
                           this.heap.insert_(tmpWeight, adjNode);
                           this.changedNodes.add(adjNode);
                           this.edgeIds[adjNode] = var10.getEdge();
                        } else if(w > tmpWeight) {
                           this.parents[adjNode] = this.currNode;
                           this.weights[adjNode] = tmpWeight;
                           this.heap.update_(tmpWeight, adjNode);
                           this.changedNodes.add(adjNode);
                           this.edgeIds[adjNode] = var10.getEdge();
                        }
                     }
                  }
               }

               if(this.heap.isEmpty() || this.visitedNodes >= this.limitVisitedNodes || this.isWeightLimitExceeded()) {
                  return -1;
               }

               this.currNode = this.heap.peek_element();
               if(this.finished()) {
                  return this.currNode;
               }

               this.heap.poll_element();
            }
         }
      }
   }

   public boolean finished() {
      return this.currNode == this.to;
   }

   protected boolean isWeightLimitExceeded() {
      return this.weights[this.currNode] > this.weightLimit;
   }

   public void close() {
      this.weights = null;
      this.parents = null;
      this.edgeIds = null;
      this.heap = null;
   }

   public int getVisitedNodes() {
      return this.visitedNodes;
   }

   public String getName() {
      return "dijkstraOneToMany";
   }

   public String getMemoryUsageAsString() {
      long len = (long)this.weights.length;
      return (16L * len + (long)this.changedNodes.getCapacity() * 4L + (long)this.heap.getCapacity() * 8L) / 1048576L + "MB";
   }

   // $FF: synthetic class
   static class SyntheticClass_1 {
   }

   private static class TIntArrayListWithCap extends TIntArrayList {
      private TIntArrayListWithCap() {
      }

      public int getCapacity() {
         return this._data.length;
      }

      // $FF: synthetic method
      TIntArrayListWithCap(DijkstraOneToMany.SyntheticClass_1 x0) {
         this();
      }
   }
}
