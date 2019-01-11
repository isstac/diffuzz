package com.graphhopper.storage;

import java.io.Closeable;

public interface Storable extends Closeable {
   boolean loadExisting();

   Object create(long var1);

   void flush();

   void close();

   boolean isClosed();

   long getCapacity();
}
