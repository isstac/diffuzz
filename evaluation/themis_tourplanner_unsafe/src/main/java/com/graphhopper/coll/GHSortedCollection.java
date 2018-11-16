package com.graphhopper.coll;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.set.hash.TIntHashSet;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.Map.Entry;

public class GHSortedCollection {
   private int size;
   private int slidingMeanValue;
   private TreeMap map;

   public GHSortedCollection() {
      this(0);
   }

   public GHSortedCollection(int size) {
      this.slidingMeanValue = 20;
      this.map = new TreeMap();
   }

   public void clear() {
      this.size = 0;
      this.map.clear();
   }

   void remove(int key, int value) {
      TIntHashSet set = (TIntHashSet)this.map.get(Integer.valueOf(value));
      if(set != null && set.remove(key)) {
         --this.size;
         if(set.isEmpty()) {
            this.map.remove(Integer.valueOf(value));
         }

      } else {
         throw new IllegalStateException("cannot remove key " + key + " with value " + value + " - did you insert " + key + "," + value + " before?");
      }
   }

   public void update(int key, int oldValue, int value) {
      this.remove(key, oldValue);
      this.insert(key, value);
   }

   public void insert(int key, int value) {
      TIntHashSet set = (TIntHashSet)this.map.get(Integer.valueOf(value));
      if(set == null) {
         this.map.put(Integer.valueOf(value), set = new TIntHashSet(this.slidingMeanValue));
      }

      if(!set.add(key)) {
         throw new IllegalStateException("use update if you want to update " + key);
      } else {
         ++this.size;
      }
   }

   public int peekValue() {
      if(this.size == 0) {
         throw new IllegalStateException("collection is already empty!?");
      } else {
         Entry e = this.map.firstEntry();
         if(((TIntHashSet)e.getValue()).isEmpty()) {
            throw new IllegalStateException("internal set is already empty!?");
         } else {
            return ((Integer)this.map.firstEntry().getKey()).intValue();
         }
      }
   }

   public int peekKey() {
      if(this.size == 0) {
         throw new IllegalStateException("collection is already empty!?");
      } else {
         TIntHashSet set = (TIntHashSet)this.map.firstEntry().getValue();
         if(set.isEmpty()) {
            throw new IllegalStateException("internal set is already empty!?");
         } else {
            return set.iterator().next();
         }
      }
   }

   public int pollKey() {
      --this.size;
      if(this.size < 0) {
         throw new IllegalStateException("collection is already empty!?");
      } else {
         Entry e = this.map.firstEntry();
         TIntHashSet set = (TIntHashSet)e.getValue();
         TIntIterator iter = set.iterator();
         if(set.isEmpty()) {
            throw new IllegalStateException("internal set is already empty!?");
         } else {
            int val = iter.next();
            iter.remove();
            if(set.isEmpty()) {
               this.map.remove(e.getKey());
            }

            return val;
         }
      }
   }

   public int getSize() {
      return this.size;
   }

   public boolean isEmpty() {
      return this.size == 0;
   }

   public int getSlidingMeanValue() {
      return this.slidingMeanValue;
   }

   public String toString() {
      int min = Integer.MAX_VALUE;
      int max = Integer.MIN_VALUE;
      Iterator str = this.map.entrySet().iterator();

      while(str.hasNext()) {
         Entry e = (Entry)str.next();
         int tmpSize = ((TIntHashSet)e.getValue()).size();
         if(min > tmpSize) {
            min = tmpSize;
         }

         if(max < tmpSize) {
            max = tmpSize;
         }
      }

      String str1 = "";
      if(!this.isEmpty()) {
         str1 = ", minEntry=(" + this.peekKey() + "=>" + this.peekValue() + ")";
      }

      return "size=" + this.size + ", treeMap.size=" + this.map.size() + ", averageNo=" + (float)this.size * 1.0F / (float)this.map.size() + ", minNo=" + min + ", maxNo=" + max + str1;
   }
}
