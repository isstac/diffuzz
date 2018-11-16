package com.graphhopper.routing.ch;

import com.graphhopper.routing.PathBidirRef;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.storage.Graph;
import com.graphhopper.util.CHEdgeIteratorState;

public class Path4CH extends PathBidirRef {
   private final Graph routingGraph;

   public Path4CH(Graph routingGraph, Graph baseGraph, FlagEncoder encoder) {
      super(baseGraph, encoder);
      this.routingGraph = routingGraph;
   }

   protected final void processEdge(int tmpEdge, int endNode) {
      this.expandEdge((CHEdgeIteratorState)this.routingGraph.getEdgeIteratorState(tmpEdge, endNode), false);
   }

   private void expandEdge(CHEdgeIteratorState mainEdgeState, boolean reverse) {
      if(!mainEdgeState.isShortcut()) {
         double skippedEdge11 = mainEdgeState.getDistance();
         this.distance += skippedEdge11;
         long from1 = mainEdgeState.getFlags();
         this.time += this.calcMillis(skippedEdge11, from1, reverse);
         this.addEdge(mainEdgeState.getEdge());
      } else {
         int skippedEdge1 = mainEdgeState.getSkippedEdge1();
         int skippedEdge2 = mainEdgeState.getSkippedEdge2();
         int from = mainEdgeState.getBaseNode();
         int to = mainEdgeState.getAdjNode();
         if(reverse) {
            int iter = from;
            from = to;
            to = iter;
         }

         boolean empty;
         CHEdgeIteratorState iter1;
         if(this.reverseOrder) {
            iter1 = (CHEdgeIteratorState)this.routingGraph.getEdgeIteratorState(skippedEdge1, to);
            empty = iter1 == null;
            if(empty) {
               iter1 = (CHEdgeIteratorState)this.routingGraph.getEdgeIteratorState(skippedEdge2, to);
            }

            this.expandEdge(iter1, false);
            if(empty) {
               iter1 = (CHEdgeIteratorState)this.routingGraph.getEdgeIteratorState(skippedEdge1, from);
            } else {
               iter1 = (CHEdgeIteratorState)this.routingGraph.getEdgeIteratorState(skippedEdge2, from);
            }

            this.expandEdge(iter1, true);
         } else {
            iter1 = (CHEdgeIteratorState)this.routingGraph.getEdgeIteratorState(skippedEdge1, from);
            empty = iter1 == null;
            if(empty) {
               iter1 = (CHEdgeIteratorState)this.routingGraph.getEdgeIteratorState(skippedEdge2, from);
            }

            this.expandEdge(iter1, true);
            if(empty) {
               iter1 = (CHEdgeIteratorState)this.routingGraph.getEdgeIteratorState(skippedEdge1, to);
            } else {
               iter1 = (CHEdgeIteratorState)this.routingGraph.getEdgeIteratorState(skippedEdge2, to);
            }

            this.expandEdge(iter1, false);
         }

      }
   }
}
