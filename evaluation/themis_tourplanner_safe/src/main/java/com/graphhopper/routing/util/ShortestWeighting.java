package com.graphhopper.routing.util;

import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.Weighting;
import com.graphhopper.util.EdgeIteratorState;

public class ShortestWeighting implements Weighting {
   private final FlagEncoder flagEncoder;

   public ShortestWeighting(FlagEncoder flagEncoder) {
      this.flagEncoder = flagEncoder;
   }

   public FlagEncoder getFlagEncoder() {
      return this.flagEncoder;
   }

   public double getMinWeight(double currDistToGoal) {
      return currDistToGoal;
   }

   public double calcWeight(EdgeIteratorState edgeState, boolean reverse, int prevOrNextEdgeId) {
      return edgeState.getDistance();
   }

   public String toString() {
      return "SHORTEST|" + this.flagEncoder;
   }
}
