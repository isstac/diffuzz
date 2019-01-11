package com.graphhopper.coll;

import com.graphhopper.coll.BinHeapWrapper;
import java.util.Arrays;

public class IntDoubleBinHeap implements BinHeapWrapper {
   private static final double GROW_FACTOR = 2.0D;
   private float[] keys;
   private int[] elem;
   private int size;
   private int capacity;

   public IntDoubleBinHeap() {
      this(1000);
   }

   public IntDoubleBinHeap(int capacity) {
      if(capacity < 10) {
         capacity = 10;
      }

      this.capacity = capacity;
      this.size = 0;
      this.elem = new int[capacity + 1];
      this.keys = new float[capacity + 1];
      this.keys[0] = Float.NEGATIVE_INFINITY;
   }

   public int getSize() {
      return this.size;
   }

   public int size() {
      return this.size;
   }

   public boolean isEmpty() {
      return this.size == 0;
   }

   public Double peekKey() {
      return Double.valueOf(this.peek_key());
   }

   public double peek_key() {
      if(this.size > 0) {
         return (double)this.keys[1];
      } else {
         throw new IllegalStateException("An empty queue does not have a minimum key.");
      }
   }

   public Integer peekElement() {
      return Integer.valueOf(this.peek_element());
   }

   public int peek_element() {
      if(this.size > 0) {
         return this.elem[1];
      } else {
         throw new IllegalStateException("An empty queue does not have a minimum value.");
      }
   }

   public Integer pollElement() {
      return Integer.valueOf(this.poll_element());
   }

   public int poll_element() {
      int minElem = this.elem[1];
      int lastElem = this.elem[this.size];
      double lastPrio = (double)this.keys[this.size];
      if(this.size <= 0) {
         throw new IllegalStateException("An empty queue does not have a minimum value.");
      } else {
         --this.size;

         int i;
         int child;
         for(i = 1; i * 2 <= this.size; i = child) {
            child = i * 2;
            if(child != this.size && this.keys[child + 1] < this.keys[child]) {
               ++child;
            }

            if(lastPrio <= (double)this.keys[child]) {
               break;
            }

            this.elem[i] = this.elem[child];
            this.keys[i] = this.keys[child];
         }

         this.elem[i] = lastElem;
         this.keys[i] = (float)lastPrio;
         return minElem;
      }
   }

   public void update(Object key, Object element) {
      this.update_(((Number)key).doubleValue(), ((Integer)element).intValue());
   }

   public boolean update_(double key, int element) {
      int i;
      for(i = 1; i <= this.size && this.elem[i] != element; ++i) {
         ;
      }

      if(i > this.size) {
         return false;
      } else {
         if(key > (double)this.keys[i]) {
            while(i * 2 <= this.size) {
               int child = i * 2;
               if(child != this.size && this.keys[child + 1] < this.keys[child]) {
                  ++child;
               }

               if(key <= (double)this.keys[child]) {
                  break;
               }

               this.elem[i] = this.elem[child];
               this.keys[i] = this.keys[child];
               i = child;
            }

            this.elem[i] = element;
            this.keys[i] = (float)key;
         } else {
            while((double)this.keys[i / 2] > key) {
               this.elem[i] = this.elem[i / 2];
               this.keys[i] = this.keys[i / 2];
               i /= 2;
            }

            this.elem[i] = element;
            this.keys[i] = (float)key;
         }

         return true;
      }
   }

   public void insert(Object key, Object element) {
      this.insert_(((Number)key).doubleValue(), ((Integer)element).intValue());
   }

   public void insert_(double key, int element) {
      ++this.size;
      if(this.size > this.capacity) {
         this.ensureCapacity((int)((double)this.capacity * 2.0D));
      }

      int i;
      for(i = this.size; (double)this.keys[i / 2] > key; i /= 2) {
         this.elem[i] = this.elem[i / 2];
         this.keys[i] = this.keys[i / 2];
      }

      this.elem[i] = element;
      this.keys[i] = (float)key;
   }

   public void ensureCapacity(int capacity) {
      if(capacity < this.size) {
         throw new IllegalStateException("BinHeap contains too many elements to fit in new capacity.");
      } else {
         this.capacity = capacity;
         this.keys = Arrays.copyOf(this.keys, capacity + 1);
         this.elem = Arrays.copyOf(this.elem, capacity + 1);
      }
   }

   public int getCapacity() {
      return this.capacity;
   }

   float getKey(int index) {
      return this.keys[index];
   }

   int getElement(int index) {
      return this.elem[index];
   }

   void set(int index, float key, int element) {
      this.keys[index] = key;
      this.elem[index] = element;
   }

   void trimTo(int toSize) {
      this.size = toSize++;
      Arrays.fill(this.keys, toSize, this.size + 1, 0.0F);
      Arrays.fill(this.elem, toSize, this.size + 1, 0);
   }

   public void clear() {
      this.trimTo(0);
   }

   public String toString() {
      StringBuilder sb = new StringBuilder();

      for(int i = 1; i <= this.size; ++i) {
         if(i > 1) {
            sb.append(", ");
         }

         sb.append(this.keys[i]).append(":").append(this.elem[i]);
      }

      return sb.toString();
   }

   public String toKeyString() {
      StringBuilder sb = new StringBuilder();

      for(int i = 1; i <= this.size; ++i) {
         if(i > 1) {
            sb.append(", ");
         }

         sb.append(this.keys[i]);
      }

      return sb.toString();
   }

   public int indexOfValue(int value) {
      for(int i = 0; i <= this.size; ++i) {
         if(this.elem[i] == value) {
            return i;
         }
      }

      return -1;
   }
}
