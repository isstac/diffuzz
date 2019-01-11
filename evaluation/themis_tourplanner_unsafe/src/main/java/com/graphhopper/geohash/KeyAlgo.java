package com.graphhopper.geohash;

import com.graphhopper.util.shapes.GHPoint;

public interface KeyAlgo {
   KeyAlgo setBounds(double var1, double var3, double var5, double var7);

   long encode(GHPoint var1);

   long encode(double var1, double var3);

   void decode(long var1, GHPoint var3);
}
