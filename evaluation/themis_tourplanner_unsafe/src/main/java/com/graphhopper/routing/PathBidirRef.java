package com.graphhopper.routing;

import com.graphhopper.routing.Path;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.storage.EdgeEntry;
import com.graphhopper.storage.Graph;
import com.graphhopper.util.EdgeIterator;

public class PathBidirRef extends Path {
   protected EdgeEntry edgeTo;
   private boolean switchWrapper = false;

   public PathBidirRef(Graph g, FlagEncoder encoder) {
      super(g, encoder);
   }

   PathBidirRef(PathBidirRef p) {
      super(p);
      this.edgeTo = p.edgeTo;
      this.switchWrapper = p.switchWrapper;
   }

   public PathBidirRef setSwitchToFrom(boolean b) {
      this.switchWrapper = b;
      return this;
   }

   public PathBidirRef setEdgeEntryTo(EdgeEntry edgeTo) {
      this.edgeTo = edgeTo;
      return this;
   }

   public Path extract() {
      if(this.edgeEntry != null && this.edgeTo != null) {
         if(this.edgeEntry.adjNode != this.edgeTo.adjNode) {
            throw new IllegalStateException("Locations of the \'to\'- and \'from\'-Edge has to be the same." + this.toString() + ", fromEntry:" + this.edgeEntry + ", toEntry:" + this.edgeTo);
         } else {
            this.extractSW.start();
            EdgeEntry currEdge;
            if(this.switchWrapper) {
               currEdge = this.edgeEntry;
               this.edgeEntry = this.edgeTo;
               this.edgeTo = currEdge;
            }

            for(currEdge = this.edgeEntry; EdgeIterator.Edge.isValid(currEdge.edge); currEdge = currEdge.parent) {
               this.processEdge(currEdge.edge, currEdge.adjNode);
            }

            this.setFromNode(currEdge.adjNode);
            this.reverseOrder();
            currEdge = this.edgeTo;

            for(int tmpEdge = currEdge.edge; EdgeIterator.Edge.isValid(tmpEdge); tmpEdge = currEdge.edge) {
               currEdge = currEdge.parent;
               this.processEdge(tmpEdge, currEdge.adjNode);
            }

            this.setEndNode(currEdge.adjNode);
            this.extractSW.stop();
            return this.setFound(true);
         }
      } else {
         return this;
      }
   }
}
