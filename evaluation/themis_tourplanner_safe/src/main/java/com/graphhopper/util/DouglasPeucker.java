package com.graphhopper.util;

import com.graphhopper.util.DistanceCalc;
import com.graphhopper.util.Helper;
import com.graphhopper.util.PointList;

public class DouglasPeucker {
   private double normedMaxDist;
   private DistanceCalc calc;
   private boolean approx;

   public DouglasPeucker() {
      this.setApproximation(true);
      this.setMaxDistance(1.0D);
   }

   public void setApproximation(boolean a) {
      this.approx = a;
      if(this.approx) {
         this.calc = Helper.DIST_PLANE;
      } else {
         this.calc = Helper.DIST_EARTH;
      }

   }

   public DouglasPeucker setMaxDistance(double dist) {
      this.normedMaxDist = this.calc.calcNormalizedDist(dist);
      return this;
   }

   public int simplify(PointList points) {
      int removed = 0;
      int size = points.getSize();
      if(this.approx) {
         short delta = 500;
         int segments = size / delta + 1;
         int start = 0;

         for(int i = 0; i < segments; ++i) {
            removed += this.simplify(points, start, Math.min(size - 1, start + delta));
            start += delta;
         }
      } else {
         removed = this.simplify(points, 0, size - 1);
      }

      this.compressNew(points, removed);
      return removed;
   }

   void compressNew(PointList points, int removed) {
      int freeIndex = -1;

      for(int currentIndex = 0; currentIndex < points.getSize(); ++currentIndex) {
         if(Double.isNaN(points.getLatitude(currentIndex))) {
            if(freeIndex < 0) {
               freeIndex = currentIndex;
            }
         } else if(freeIndex >= 0) {
            points.set(freeIndex, points.getLatitude(currentIndex), points.getLongitude(currentIndex), points.getElevation(currentIndex));
            points.set(currentIndex, Double.NaN, Double.NaN, Double.NaN);
            int max = currentIndex;
            int searchIndex = freeIndex + 1;

            for(freeIndex = currentIndex; searchIndex < max; ++searchIndex) {
               if(Double.isNaN(points.getLatitude(searchIndex))) {
                  freeIndex = searchIndex;
                  break;
               }
            }
         }
      }

      points.trimToSize(points.getSize() - removed);
   }

   int simplify(PointList points, int fromIndex, int lastIndex) {
      if(lastIndex - fromIndex < 2) {
         return 0;
      } else {
         int indexWithMaxDist = -1;
         double maxDist = -1.0D;
         double firstLat = points.getLatitude(fromIndex);
         double firstLon = points.getLongitude(fromIndex);
         double lastLat = points.getLatitude(lastIndex);
         double lastLon = points.getLongitude(lastIndex);

         int counter;
         for(counter = fromIndex + 1; counter < lastIndex; ++counter) {
            double i = points.getLatitude(counter);
            if(!Double.isNaN(i)) {
               double lon = points.getLongitude(counter);
               double dist = this.calc.calcNormalizedEdgeDistance(i, lon, firstLat, firstLon, lastLat, lastLon);
               if(maxDist < dist) {
                  indexWithMaxDist = counter;
                  maxDist = dist;
               }
            }
         }

         if(indexWithMaxDist < 0) {
            throw new IllegalStateException("maximum not found in [" + fromIndex + "," + lastIndex + "]");
         } else {
            counter = 0;
            if(maxDist < this.normedMaxDist) {
               for(int var22 = fromIndex + 1; var22 < lastIndex; ++var22) {
                  points.set(var22, Double.NaN, Double.NaN, Double.NaN);
                  ++counter;
               }
            } else {
               counter = this.simplify(points, fromIndex, indexWithMaxDist);
               counter += this.simplify(points, indexWithMaxDist, lastIndex);
            }

            return counter;
         }
      }
   }
}
