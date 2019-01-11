package com.graphhopper.coll;

import com.graphhopper.util.Helper;

public class SparseArray implements Cloneable {
   private static final Object DELETED = new Object();
   private boolean mGarbage;
   private int[] mKeys;
   private Object[] mValues;
   private int mSize;

   public SparseArray() {
      this(10);
   }

   public SparseArray(int initialCapacity) {
      this.mGarbage = false;
      initialCapacity = Helper.idealIntArraySize(initialCapacity);
      this.mKeys = new int[initialCapacity];
      this.mValues = new Object[initialCapacity];
      this.mSize = 0;
   }

   public SparseArray clone() {
      SparseArray clone = null;

      try {
         clone = (SparseArray)super.clone();
         clone.mKeys = (int[])this.mKeys.clone();
         clone.mValues = (Object[])this.mValues.clone();
      } catch (CloneNotSupportedException var3) {
         ;
      }

      return clone;
   }

   public Object get(int key) {
      return this.get(key, (Object)null);
   }

   public Object get(int key, Object valueIfKeyNotFound) {
      int i = binarySearch(this.mKeys, 0, this.mSize, key);
      return i >= 0 && this.mValues[i] != DELETED?this.mValues[i]:valueIfKeyNotFound;
   }

   public void remove(int key) {
      int i = binarySearch(this.mKeys, 0, this.mSize, key);
      if(i >= 0 && this.mValues[i] != DELETED) {
         this.mValues[i] = DELETED;
         this.mGarbage = true;
      }

   }

   public void removeAt(int index) {
      if(this.mValues[index] != DELETED) {
         this.mValues[index] = DELETED;
         this.mGarbage = true;
      }

   }

   private void gc() {
      int n = this.mSize;
      int o = 0;
      int[] keys = this.mKeys;
      Object[] values = this.mValues;

      for(int i = 0; i < n; ++i) {
         Object val = values[i];
         if(val != DELETED) {
            if(i != o) {
               keys[o] = keys[i];
               values[o] = val;
               values[i] = null;
            }

            ++o;
         }
      }

      this.mGarbage = false;
      this.mSize = o;
   }

   public void put(int key, Object value) {
      int i = binarySearch(this.mKeys, 0, this.mSize, key);
      if(i >= 0) {
         this.mValues[i] = value;
      } else {
         i = ~i;
         if(i < this.mSize && this.mValues[i] == DELETED) {
            this.mKeys[i] = key;
            this.mValues[i] = value;
            return;
         }

         if(this.mGarbage && this.mSize >= this.mKeys.length) {
            this.gc();
            i = ~binarySearch(this.mKeys, 0, this.mSize, key);
         }

         if(this.mSize >= this.mKeys.length) {
            int n = Helper.idealIntArraySize(this.mSize + 1);
            int[] nkeys = new int[n];
            Object[] nvalues = new Object[n];
            System.arraycopy(this.mKeys, 0, nkeys, 0, this.mKeys.length);
            System.arraycopy(this.mValues, 0, nvalues, 0, this.mValues.length);
            this.mKeys = nkeys;
            this.mValues = nvalues;
         }

         if(this.mSize - i != 0) {
            System.arraycopy(this.mKeys, i, this.mKeys, i + 1, this.mSize - i);
            System.arraycopy(this.mValues, i, this.mValues, i + 1, this.mSize - i);
         }

         this.mKeys[i] = key;
         this.mValues[i] = value;
         ++this.mSize;
      }

   }

   public int getSize() {
      if(this.mGarbage) {
         this.gc();
      }

      return this.mSize;
   }

   public int keyAt(int index) {
      if(this.mGarbage) {
         this.gc();
      }

      return this.mKeys[index];
   }

   public Object valueAt(int index) {
      if(this.mGarbage) {
         this.gc();
      }

      return this.mValues[index];
   }

   public void setValueAt(int index, Object value) {
      if(this.mGarbage) {
         this.gc();
      }

      this.mValues[index] = value;
   }

   public int indexOfKey(int key) {
      if(this.mGarbage) {
         this.gc();
      }

      return binarySearch(this.mKeys, 0, this.mSize, key);
   }

   public int indexOfValue(Object value) {
      if(this.mGarbage) {
         this.gc();
      }

      for(int i = 0; i < this.mSize; ++i) {
         if(this.mValues[i] == value) {
            return i;
         }
      }

      return -1;
   }

   public void clear() {
      this.trimTo(0);
   }

   public void trimTo(int size) {
      int max = Math.min(this.mSize, size);

      for(int i = max; i < this.mSize; ++i) {
         this.mValues[i] = null;
      }

      this.mSize = 0;
      this.mGarbage = false;
   }

   public void append(int key, Object value) {
      if(this.mSize != 0 && key <= this.mKeys[this.mSize - 1]) {
         this.put(key, value);
      } else {
         if(this.mGarbage && this.mSize >= this.mKeys.length) {
            this.gc();
         }

         int pos = this.mSize;
         if(pos >= this.mKeys.length) {
            int n = Helper.idealIntArraySize(pos + 1);
            int[] nkeys = new int[n];
            Object[] nvalues = new Object[n];
            System.arraycopy(this.mKeys, 0, nkeys, 0, this.mKeys.length);
            System.arraycopy(this.mValues, 0, nvalues, 0, this.mValues.length);
            this.mKeys = nkeys;
            this.mValues = nvalues;
         }

         this.mKeys[pos] = key;
         this.mValues[pos] = value;
         this.mSize = pos + 1;
      }
   }

   private static int binarySearch(int[] a, int start, int len, int key) {
      int high = start + len;
      int low = start - 1;

      while(high - low > 1) {
         int guess = high + low >>> 1;
         if(a[guess] < key) {
            low = guess;
         } else {
            high = guess;
         }
      }

      if(high == start + len) {
         return ~(start + len);
      } else if(a[high] == key) {
         return high;
      } else {
         return ~high;
      }
   }
}
