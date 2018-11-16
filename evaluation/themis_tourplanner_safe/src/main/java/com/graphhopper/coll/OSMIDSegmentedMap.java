package com.graphhopper.coll;

import com.graphhopper.coll.LongIntMap;
import com.graphhopper.coll.SparseLongLongArray;
import com.graphhopper.storage.VLongStorage;
import java.util.Arrays;

public class OSMIDSegmentedMap implements LongIntMap {
   private int bucketSize;
   private long[] keys;
   private VLongStorage[] buckets;
   private long lastKey;
   private long lastValue;
   private int currentBucket;
   private int currentIndex;
   private long size;

   public OSMIDSegmentedMap() {
      this(100, 10);
   }

   public OSMIDSegmentedMap(int initialCapacity, int maxEntryPerBucket) {
      this.lastKey = -1L;
      this.lastValue = -1L;
      this.currentBucket = 0;
      this.currentIndex = -1;
      this.bucketSize = maxEntryPerBucket;
      int cap = initialCapacity / this.bucketSize;
      this.keys = new long[cap];
      this.buckets = new VLongStorage[cap];
   }

   public void write(long key) {
      if(key <= this.lastKey) {
         throw new IllegalStateException("Not supported: key " + key + " is lower than last one " + this.lastKey);
      } else {
         ++this.currentIndex;
         if(this.currentIndex >= this.bucketSize) {
            ++this.currentBucket;
            this.currentIndex = 0;
         }

         if(this.currentBucket >= this.buckets.length) {
            int delta = (int)((float)this.currentBucket * 1.5F);
            this.buckets = (VLongStorage[])Arrays.copyOf(this.buckets, delta);
            this.keys = Arrays.copyOf(this.keys, delta);
         }

         if(this.buckets[this.currentBucket] == null) {
            this.keys[this.currentBucket] = key;
            if(this.currentBucket > 0) {
               this.buckets[this.currentBucket - 1].trimToSize();
            }

            this.buckets[this.currentBucket] = new VLongStorage(this.bucketSize);
         } else {
            long var5 = key - this.lastKey;
            this.buckets[this.currentBucket].writeVLong(var5);
         }

         ++this.size;
         this.lastKey = key;
      }
   }

   public int get(long key) {
      int retBucket = SparseLongLongArray.binarySearch(this.keys, 0, this.currentBucket + 1, key);
      if(retBucket >= 0) {
         return retBucket * this.bucketSize;
      } else {
         retBucket = ~retBucket;
         --retBucket;
         if(retBucket < 0) {
            return this.getNoEntryValue();
         } else {
            long storedKey = this.keys[retBucket];
            if(storedKey == key) {
               return retBucket * this.bucketSize;
            } else {
               VLongStorage buck = this.buckets[retBucket];
               long tmp = buck.getPosition();
               buck.seek(0L);
               int max = this.currentBucket == retBucket?this.currentIndex + 1:this.bucketSize;
               int ret = this.getNoEntryValue();

               for(int i = 1; i < max; ++i) {
                  storedKey += buck.readVLong();
                  if(storedKey == key) {
                     ret = retBucket * this.bucketSize + i;
                     break;
                  }

                  if(storedKey > key) {
                     break;
                  }
               }

               buck.seek(tmp);
               return ret;
            }
         }
      }
   }

   public int getNoEntryValue() {
      return -1;
   }

   public long getSize() {
      return this.size;
   }

   public void optimize() {
   }

   public int getMemoryUsage() {
      long bytes = 0L;

      for(int i = 0; i < this.buckets.length; ++i) {
         if(this.buckets[i] != null) {
            bytes += this.buckets[i].getLength();
         }
      }

      return Math.round((float)(((long)(this.keys.length * 4) + bytes) / 1048576L));
   }

   public int put(long key, int value) {
      throw new UnsupportedOperationException("Not supported yet.");
   }
}
