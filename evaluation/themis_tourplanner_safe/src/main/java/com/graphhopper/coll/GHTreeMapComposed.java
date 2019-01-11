package com.graphhopper.coll;

import com.graphhopper.util.BitUtil;
import java.util.TreeMap;

public class GHTreeMapComposed {
   private static final Integer NOT_EMPTY = new Integer(-3);
   private final BitUtil bitUtil;
   private final TreeMap map;

   public GHTreeMapComposed() {
      this.bitUtil = BitUtil.BIG;
      this.map = new TreeMap();
   }

   public void clear() {
      this.map.clear();
   }

   void remove(int key, int value) {
      long v = this.bitUtil.toLong(value, key);
      if(this.map.remove(Long.valueOf(v)) != NOT_EMPTY) {
         throw new IllegalStateException("cannot remove key " + key + " with value " + value + " - did you insert " + key + "," + value + " before?");
      }
   }

   public void update(int key, int oldValue, int value) {
      this.remove(key, oldValue);
      this.insert(key, value);
   }

   public void insert(int key, int value) {
      long v = this.bitUtil.toLong(value, key);
      this.map.put(Long.valueOf(v), NOT_EMPTY);
   }

   public int peekValue() {
      long key = ((Long)this.map.firstEntry().getKey()).longValue();
      return (int)(key >> 32);
   }

   public int peekKey() {
      long key = ((Long)this.map.firstEntry().getKey()).longValue();
      return (int)(key & 4294967295L);
   }

   public int pollKey() {
      if(this.map.isEmpty()) {
         throw new IllegalStateException("Cannot poll collection is empty!");
      } else {
         long key = ((Long)this.map.pollFirstEntry().getKey()).longValue();
         return (int)(key & 4294967295L);
      }
   }

   public int getSize() {
      return this.map.size();
   }

   public boolean isEmpty() {
      return this.map.isEmpty();
   }

   public String toString() {
      return this.map.toString();
   }
}
