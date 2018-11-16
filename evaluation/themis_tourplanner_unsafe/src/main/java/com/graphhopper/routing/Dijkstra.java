package com.graphhopper.routing;

import com.graphhopper.routing.AbstractRoutingAlgorithm;
import com.graphhopper.routing.Path;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.TraversalMode;
import com.graphhopper.routing.util.Weighting;
import com.graphhopper.storage.EdgeEntry;
import com.graphhopper.storage.Graph;
import com.graphhopper.util.EdgeExplorer;
import com.graphhopper.util.EdgeIterator;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import java.util.PriorityQueue;

public class Dijkstra extends AbstractRoutingAlgorithm {
   protected TIntObjectMap fromMap;
   protected PriorityQueue fromHeap;
   protected EdgeEntry currEdge;
   private int visitedNodes;
   private int to = -1;

   public Dijkstra(Graph g, FlagEncoder encoder, Weighting weighting, TraversalMode tMode) {
      super(g, encoder, weighting, tMode);
      this.initCollections(1000);
   }

   protected void initCollections(int size) {
      this.fromHeap = new PriorityQueue(size);
      this.fromMap = new TIntObjectHashMap(size);
   }

   public Path calcPath(int from, int to) {
      this.checkAlreadyRun();
      this.to = to;
      this.currEdge = this.createEdgeEntry(from, 0.0D);
      if(!this.traversalMode.isEdgeBased()) {
         this.fromMap.put(from, this.currEdge);
      }

      this.runAlgo();
      return this.extractPath();
   }

   protected void runAlgo() {
      EdgeExplorer explorer = this.outEdgeExplorer;

      label52:
      while(true) {
         ++this.visitedNodes;
         if(this.isWeightLimitExceeded() || this.finished()) {
            return;
         }

         int startNode = this.currEdge.adjNode;
         EdgeIterator iter = explorer.setBaseNode(startNode);

         while(true) {
            int traversalId;
            EdgeEntry nEdge;
            while(true) {
               double tmpWeight;
               do {
                  do {
                     if(!iter.next()) {
                        if(this.fromHeap.isEmpty()) {
                           return;
                        }

                        this.currEdge = (EdgeEntry)this.fromHeap.poll();
                        if(this.currEdge == null) {
                           throw new AssertionError("Empty edge cannot happen");
                        }
                        continue label52;
                     }
                  } while(!this.accept(iter, this.currEdge.edge));

                  traversalId = this.traversalMode.createTraversalId(iter, false);
                  tmpWeight = this.weighting.calcWeight(iter, false, this.currEdge.edge) + this.currEdge.weight;
               } while(Double.isInfinite(tmpWeight));

               nEdge = (EdgeEntry)this.fromMap.get(traversalId);
               if(nEdge == null) {
                  nEdge = new EdgeEntry(iter.getEdge(), iter.getAdjNode(), tmpWeight);
                  nEdge.parent = this.currEdge;
                  this.fromMap.put(traversalId, nEdge);
                  this.fromHeap.add(nEdge);
                  break;
               }

               if(nEdge.weight > tmpWeight) {
                  this.fromHeap.remove(nEdge);
                  nEdge.edge = iter.getEdge();
                  nEdge.weight = tmpWeight;
                  nEdge.parent = this.currEdge;
                  this.fromHeap.add(nEdge);
                  break;
               }
            }

            this.updateBestPath(iter, nEdge, traversalId);
         }
      }
   }

   protected boolean finished() {
      return this.currEdge.adjNode == this.to;
   }

   protected Path extractPath() {
      return this.currEdge != null && !this.isWeightLimitExceeded() && this.finished()?(new Path(this.graph, this.flagEncoder)).setWeight(this.currEdge.weight).setEdgeEntry(this.currEdge).extract():this.createEmptyPath();
   }

   public int getVisitedNodes() {
      return this.visitedNodes;
   }

   protected boolean isWeightLimitExceeded() {
      return this.currEdge.weight > this.weightLimit;
   }

   public String getName() {
      return "dijkstra";
   }
}
