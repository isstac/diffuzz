package com.graphhopper.coll;

public interface LongIntMap {
   int put(long var1, int var3);

   int get(long var1);

   long getSize();

   void optimize();

   int getMemoryUsage();
}
