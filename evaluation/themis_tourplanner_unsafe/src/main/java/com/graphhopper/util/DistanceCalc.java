package com.graphhopper.util;

import com.graphhopper.util.shapes.BBox;
import com.graphhopper.util.shapes.GHPoint;

public interface DistanceCalc {
   BBox createBBox(double var1, double var3, double var5);

   double calcCircumference(double var1);

   double calcDist(double var1, double var3, double var5, double var7);

   double calcNormalizedDist(double var1);

   double calcDenormalizedDist(double var1);

   double calcNormalizedDist(double var1, double var3, double var5, double var7);

   boolean validEdgeDistance(double var1, double var3, double var5, double var7, double var9, double var11);

   double calcNormalizedEdgeDistance(double var1, double var3, double var5, double var7, double var9, double var11);

   GHPoint calcCrossingPointToEdge(double var1, double var3, double var5, double var7, double var9, double var11);
}
