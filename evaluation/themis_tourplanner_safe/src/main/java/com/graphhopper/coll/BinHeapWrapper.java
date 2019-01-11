package com.graphhopper.coll;

public interface BinHeapWrapper {
   void update(Object var1, Object var2);

   void insert(Object var1, Object var2);

   boolean isEmpty();

   int getSize();

   Object peekElement();

   Object peekKey();

   Object pollElement();

   void clear();

   void ensureCapacity(int var1);
}
