package com.graphhopper.util;

import com.graphhopper.util.DistanceCalcEarth;

public class DistanceCalc2D extends DistanceCalcEarth {
   public double calcDist(double fromY, double fromX, double toY, double toX) {
      return Math.sqrt(this.calcNormalizedDist(fromY, fromX, toY, toX));
   }

   public double calcDenormalizedDist(double normedDist) {
      return Math.sqrt(normedDist);
   }

   public double calcNormalizedDist(double dist) {
      return dist * dist;
   }

   public double calcNormalizedDist(double fromY, double fromX, double toY, double toX) {
      double dX = fromX - toX;
      double dY = fromY - toY;
      return dX * dX + dY * dY;
   }

   public String toString() {
      return "2D";
   }
}
