package com.graphhopper.routing;

import com.graphhopper.routing.Path;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.storage.Graph;
import com.graphhopper.util.EdgeWrapper;

public class PathBidir extends Path {
   public boolean switchWrapper = false;
   public int fromRef = -1;
   public int toRef = -1;
   private EdgeWrapper edgeWFrom;
   private EdgeWrapper edgeWTo;

   public PathBidir(Graph g, FlagEncoder encoder, EdgeWrapper edgesFrom, EdgeWrapper edgesTo) {
      super(g, encoder);
      this.edgeWFrom = edgesFrom;
      this.edgeWTo = edgesTo;
   }

   public Path extract() {
      if(this.fromRef >= 0 && this.toRef >= 0) {
         int nodeFrom;
         if(this.switchWrapper) {
            nodeFrom = this.fromRef;
            this.fromRef = this.toRef;
            this.toRef = nodeFrom;
         }

         nodeFrom = this.edgeWFrom.getNode(this.fromRef);
         int nodeTo = this.edgeWTo.getNode(this.toRef);
         if(nodeFrom != nodeTo) {
            throw new IllegalStateException("\'to\' and \'from\' have to be the same. " + this.toString());
         } else {
            int currRef;
            int edgeId;
            for(currRef = this.fromRef; currRef > 0; nodeFrom = this.edgeWFrom.getNode(currRef)) {
               edgeId = this.edgeWFrom.getEdgeId(currRef);
               if(edgeId < 0) {
                  break;
               }

               this.processEdge(edgeId, nodeFrom);
               currRef = this.edgeWFrom.getParent(currRef);
            }

            this.reverseOrder();
            this.setFromNode(nodeFrom);

            int tmpRef;
            for(currRef = this.toRef; currRef > 0; currRef = tmpRef) {
               edgeId = this.edgeWTo.getEdgeId(currRef);
               if(edgeId < 0) {
                  break;
               }

               tmpRef = this.edgeWTo.getParent(currRef);
               nodeTo = this.edgeWTo.getNode(tmpRef);
               this.processEdge(edgeId, nodeTo);
            }

            this.setEndNode(nodeTo);
            return this.setFound(true);
         }
      } else {
         return this;
      }
   }
}
