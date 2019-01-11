package com.graphhopper.routing.util;

import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.GHUtility;
import java.util.Arrays;

public enum TraversalMode {
   NODE_BASED(false, 1, false),
   EDGE_BASED_1DIR(true, 1, false),
   EDGE_BASED_2DIR(true, 2, false),
   EDGE_BASED_2DIR_UTURN(true, 2, true);

   private final boolean edgeBased;
   private final int noOfStates;
   private final boolean uTurnSupport;

   private TraversalMode(boolean edgeBased, int noOfStates, boolean uTurnSupport) {
      this.edgeBased = edgeBased;
      this.noOfStates = noOfStates;
      this.uTurnSupport = uTurnSupport;
      if(noOfStates != 1 && noOfStates != 2) {
         throw new IllegalArgumentException("Currently only 1 or 2 states allowed");
      }
   }

   public final int createTraversalId(EdgeIteratorState iterState, boolean reverse) {
      return this.edgeBased?(this.noOfStates == 1?iterState.getEdge():GHUtility.createEdgeKey(iterState.getBaseNode(), iterState.getAdjNode(), iterState.getEdge(), reverse)):iterState.getAdjNode();
   }

   public final int createTraversalId(int baseNode, int adjNode, int edgeId, boolean reverse) {
      return this.edgeBased?(this.noOfStates == 1?edgeId:GHUtility.createEdgeKey(baseNode, adjNode, edgeId, reverse)):adjNode;
   }

   public int reverseEdgeKey(int edgeKey) {
      return this.edgeBased && this.noOfStates > 1?GHUtility.reverseEdgeKey(edgeKey):edgeKey;
   }

   public int getNoOfStates() {
      return this.noOfStates;
   }

   public boolean isEdgeBased() {
      return this.edgeBased;
   }

   public final boolean hasUTurnSupport() {
      return this.uTurnSupport;
   }

   public static TraversalMode fromString(String name) {
      try {
         return valueOf(name.toUpperCase());
      } catch (Exception var2) {
         throw new IllegalArgumentException("TraversalMode " + name + " not supported. " + "Supported are: " + Arrays.asList(values()));
      }
   }
}
