package com.graphhopper.routing.util;

import com.graphhopper.routing.util.FastestWeighting;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.PMap;

public class PriorityWeighting extends FastestWeighting {
   public static final int KEY = 101;

   public PriorityWeighting(FlagEncoder encoder, PMap pMap) {
      super(encoder, pMap);
   }

   public PriorityWeighting(FlagEncoder encoder) {
      this(encoder, new PMap(0));
   }

   public double calcWeight(EdgeIteratorState edgeState, boolean reverse, int prevOrNextEdgeId) {
      double weight = super.calcWeight(edgeState, reverse, prevOrNextEdgeId);
      return Double.isInfinite(weight)?Double.POSITIVE_INFINITY:weight / (0.5D + this.flagEncoder.getDouble(edgeState.getFlags(), 101));
   }
}
