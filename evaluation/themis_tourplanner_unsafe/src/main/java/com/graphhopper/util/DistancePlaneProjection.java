package com.graphhopper.util;

import com.graphhopper.util.DistanceCalcEarth;

public class DistancePlaneProjection extends DistanceCalcEarth {
   public double calcDist(double fromLat, double fromLon, double toLat, double toLon) {
      double dLat = Math.toRadians(toLat - fromLat);
      double dLon = Math.toRadians(toLon - fromLon);
      double tmp = Math.cos(Math.toRadians((fromLat + toLat) / 2.0D)) * dLon;
      double normedDist = dLat * dLat + tmp * tmp;
      return 6371000.0D * Math.sqrt(normedDist);
   }

   public double calcDenormalizedDist(double normedDist) {
      return 6371000.0D * Math.sqrt(normedDist);
   }

   public double calcNormalizedDist(double dist) {
      double tmp = dist / 6371000.0D;
      return tmp * tmp;
   }

   public double calcNormalizedDist(double fromLat, double fromLon, double toLat, double toLon) {
      double dLat = Math.toRadians(toLat - fromLat);
      double dLon = Math.toRadians(toLon - fromLon);
      double left = Math.cos(Math.toRadians((fromLat + toLat) / 2.0D)) * dLon;
      return dLat * dLat + left * left;
   }

   public String toString() {
      return "PLANE_PROJ";
   }
}
