package com.graphhopper.routing.util;

import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.Weighting;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.PMap;

public class FastestWeighting implements Weighting {
   protected static final double SPEED_CONV = 3.6D;
   static final double DEFAULT_HEADING_PENALTY = 300.0D;
   private final double heading_penalty;
   protected final FlagEncoder flagEncoder;
   private final double maxSpeed;

   public FastestWeighting(FlagEncoder encoder, PMap pMap) {
      if(!encoder.isRegistered()) {
         throw new IllegalStateException("Make sure you add the FlagEncoder " + encoder + " to an EncodingManager before using it elsewhere");
      } else {
         this.flagEncoder = encoder;
         this.heading_penalty = pMap.getDouble("heading_penalty", 300.0D);
         this.maxSpeed = encoder.getMaxSpeed() / 3.6D;
      }
   }

   public FastestWeighting(FlagEncoder encoder) {
      this(encoder, new PMap(0));
   }

   public double getMinWeight(double distance) {
      return distance / this.maxSpeed;
   }

   public double calcWeight(EdgeIteratorState edge, boolean reverse, int prevOrNextEdgeId) {
      double speed = reverse?this.flagEncoder.getReverseSpeed(edge.getFlags()):this.flagEncoder.getSpeed(edge.getFlags());
      if(speed == 0.0D) {
         return Double.POSITIVE_INFINITY;
      } else {
         double time = edge.getDistance() / speed * 3.6D;
         boolean penalizeEdge = edge.getBoolean(-1, reverse, false);
         if(penalizeEdge) {
            time += this.heading_penalty;
         }

         return time;
      }
   }

   public FlagEncoder getFlagEncoder() {
      return this.flagEncoder;
   }

   public int hashCode() {
      byte hash = 7;
      int hash1 = 71 * hash + this.toString().hashCode();
      return hash1;
   }

   public boolean equals(Object obj) {
      if(obj == null) {
         return false;
      } else if(this.getClass() != obj.getClass()) {
         return false;
      } else {
         FastestWeighting other = (FastestWeighting)obj;
         return this.toString().equals(other.toString());
      }
   }

   public String toString() {
      return "FASTEST|" + this.flagEncoder;
   }
}
