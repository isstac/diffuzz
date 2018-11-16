package com.graphhopper.routing.ch;

import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.Weighting;
import com.graphhopper.util.CHEdgeIteratorState;
import com.graphhopper.util.EdgeIteratorState;

public class PreparationWeighting implements Weighting {
   private final Weighting userWeighting;

   public PreparationWeighting(Weighting userWeighting) {
      this.userWeighting = userWeighting;
   }

   public final double getMinWeight(double distance) {
      return this.userWeighting.getMinWeight(distance);
   }

   public double calcWeight(EdgeIteratorState edgeState, boolean reverse, int prevOrNextEdgeId) {
      CHEdgeIteratorState tmp = (CHEdgeIteratorState)edgeState;
      return tmp.isShortcut()?tmp.getWeight():this.userWeighting.calcWeight(edgeState, reverse, prevOrNextEdgeId);
   }

   public FlagEncoder getFlagEncoder() {
      return this.userWeighting.getFlagEncoder();
   }

   public String toString() {
      return "PREPARE+" + this.userWeighting.toString();
   }
}
