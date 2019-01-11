package com.graphhopper.coll;

import com.graphhopper.coll.LongIntMap;
import com.graphhopper.util.Helper;
import gnu.trove.map.hash.TLongIntHashMap;

public class BigLongIntMap implements LongIntMap {
   private TLongIntHashMap[] maps;

   public BigLongIntMap(long maxSize, int noNumber) {
      this(maxSize, Math.max(1, (int)(maxSize / 10000000L)), noNumber);
   }

   public BigLongIntMap(long maxSize, int minSegments, int noNumber) {
      if(maxSize < 0L) {
         throw new IllegalArgumentException("Maximum size illegal " + maxSize);
      } else if(minSegments < 1) {
         throw new IllegalArgumentException("Minimun segment number illegal " + minSegments);
      } else {
         minSegments = Math.max((int)(maxSize / 2147483647L), minSegments);
         this.maps = new TLongIntHashMap[minSegments];
         int size = (int)(maxSize / (long)minSegments) + 1;

         for(int i = 0; i < this.maps.length; ++i) {
            this.maps[i] = new TLongIntHashMap(size, 1.4F, (long)noNumber, noNumber);
         }

      }
   }

   public int put(long key, int value) {
      int segment = Math.abs((int)(key >> 32 ^ key)) % this.maps.length;
      return this.maps[segment].put(key, value);
   }

   public int get(long key) {
      int segment = Math.abs((int)(key >> 32 ^ key)) % this.maps.length;
      return this.maps[segment].get(key);
   }

   public long getCapacity() {
      long cap = 0L;

      for(int i = 0; i < this.maps.length; ++i) {
         cap += (long)this.maps[i].capacity();
      }

      return cap;
   }

   public long getSize() {
      long size = 0L;

      for(int i = 0; i < this.maps.length; ++i) {
         size += (long)this.maps[i].size();
      }

      return size;
   }

   public String toString() {
      String str = "";

      for(int i = 0; i < this.maps.length; ++i) {
         str = str + Helper.nf((long)this.maps[i].size()) + ", ";
      }

      return str;
   }

   public void clear() {
      for(int i = 0; i < this.maps.length; ++i) {
         this.maps[i].clear();
      }

   }

   public int getMemoryUsage() {
      return Math.round((float)(this.getCapacity() * 13L / 1048576L));
   }

   public void optimize() {
   }
}
