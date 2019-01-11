package com.graphhopper.util;

public interface PointAccess {
   boolean is3D();

   int getDimension();

   void ensureNode(int var1);

   void setNode(int var1, double var2, double var4);

   void setNode(int var1, double var2, double var4, double var6);

   double getLatitude(int var1);

   double getLat(int var1);

   double getLongitude(int var1);

   double getLon(int var1);

   double getElevation(int var1);

   double getEle(int var1);
}
