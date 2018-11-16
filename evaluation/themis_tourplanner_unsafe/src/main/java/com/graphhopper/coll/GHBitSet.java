package com.graphhopper.coll;

public interface GHBitSet {
   boolean contains(int var1);

   void add(int var1);

   int getCardinality();

   void clear();

   int next(int var1);

   GHBitSet copyTo(GHBitSet var1);
}
