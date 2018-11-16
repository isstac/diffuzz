package com.graphhopper.coll;

import com.graphhopper.coll.LongIntMap;
import com.graphhopper.storage.DataAccess;
import com.graphhopper.storage.Directory;
import com.graphhopper.util.BitUtil;

public class OSMIDMap implements LongIntMap {
   private static final BitUtil bitUtil;
   private final DataAccess keys;
   private final DataAccess values;
   private long lastKey;
   private long size;
   private final int noEntryValue;
   private final Directory dir;

   public OSMIDMap(Directory dir) {
      this(dir, -1);
   }

   public OSMIDMap(Directory dir, int noNumber) {
      this.lastKey = Long.MIN_VALUE;
      this.dir = dir;
      this.noEntryValue = noNumber;
      this.keys = dir.find("osmidMapKeys");
      this.keys.create(2000L);
      this.values = dir.find("osmidMapValues");
      this.values.create(1000L);
   }

   public void remove() {
      this.dir.remove(this.keys);
   }

   public int put(long key, int value) {
      long doubleSize;
      if(key <= this.lastKey) {
         doubleSize = binarySearch(this.keys, 0L, this.getSize(), key);
         if(doubleSize < 0L) {
            throw new IllegalStateException("Cannot insert keys lower than the last key " + key + " < " + this.lastKey + ". Only updating supported");
         } else {
            doubleSize *= 4L;
            int longBytes1 = this.values.getInt(doubleSize);
            this.values.setInt(doubleSize, value);
            return longBytes1;
         }
      } else {
         this.values.ensureCapacity(this.size + 4L);
         this.values.setInt(this.size, value);
         doubleSize = this.size * 2L;
         this.keys.ensureCapacity(doubleSize + 8L);
         byte[] longBytes = bitUtil.fromLong(key);
         this.keys.setBytes(doubleSize, longBytes, 8);
         this.lastKey = key;
         this.size += 4L;
         return -1;
      }
   }

   public int get(long key) {
      long retIndex = binarySearch(this.keys, 0L, this.getSize(), key);
      return retIndex < 0L?this.noEntryValue:this.values.getInt(retIndex * 4L);
   }

   static long binarySearch(DataAccess da, long start, long len, long key) {
      long high = start + len;
      long low = start - 1L;
      byte[] longBytes = new byte[8];

      long highKey;
      long tmp;
      while(high - low > 1L) {
         long guess = high + low >>> 1;
         tmp = guess << 3;
         da.getBytes(tmp, longBytes, 8);
         highKey = bitUtil.toLong(longBytes);
         if(highKey < key) {
            low = guess;
         } else {
            high = guess;
         }
      }

      if(high == start + len) {
         return ~(start + len);
      } else {
         tmp = high << 3;
         da.getBytes(tmp, longBytes, 8);
         highKey = bitUtil.toLong(longBytes);
         if(highKey == key) {
            return high;
         } else {
            return ~high;
         }
      }
   }

   public long getSize() {
      return this.size / 4L;
   }

   public long getCapacity() {
      return this.keys.getCapacity();
   }

   public int getMemoryUsage() {
      return Math.round((float)(this.getCapacity() / 1048576L));
   }

   public void optimize() {
   }

   static {
      bitUtil = BitUtil.LITTLE;
   }
}
