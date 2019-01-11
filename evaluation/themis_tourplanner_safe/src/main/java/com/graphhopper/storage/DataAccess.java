package com.graphhopper.storage;

import com.graphhopper.storage.DAType;
import com.graphhopper.storage.Storable;

public interface DataAccess extends Storable {
   String getName();

   void rename(String var1);

   void setInt(long var1, int var3);

   int getInt(long var1);

   void setShort(long var1, short var3);

   short getShort(long var1);

   void setBytes(long var1, byte[] var3, int var4);

   void getBytes(long var1, byte[] var3, int var4);

   void setHeader(int var1, int var2);

   int getHeader(int var1);

   DataAccess create(long var1);

   boolean ensureCapacity(long var1);

   void trimTo(long var1);

   DataAccess copyTo(DataAccess var1);

   DataAccess setSegmentSize(int var1);

   int getSegmentSize();

   int getSegments();

   DAType getType();
}
