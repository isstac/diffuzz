package com.graphhopper.coll;

import com.graphhopper.coll.BinHeapWrapper;
import java.util.Arrays;

public class IntIntBinHeap implements BinHeapWrapper {
   private static final double GROW_FACTOR = 2.0D;
   private int[] keys;
   private int[] elem;
   private int size;
   private int capacity;

   public IntIntBinHeap() {
      this(1000);
   }

   public IntIntBinHeap(int capacity) {
      if(capacity < 10) {
         capacity = 10;
      }

      this.capacity = capacity;
      this.size = 0;
      this.elem = new int[capacity + 1];
      this.keys = new int[capacity + 1];
      this.keys[0] = Integer.MIN_VALUE;
   }

   public int getSize() {
      return this.size;
   }

   public boolean isEmpty() {
      return this.size == 0;
   }

   public Integer peekKey() {
      return Integer.valueOf(this.peek_key());
   }

   public int peek_key() {
      if(this.size > 0) {
         return this.keys[1];
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
      int lastPrio = this.keys[this.size];
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

            if(lastPrio <= this.keys[child]) {
               break;
            }

            this.elem[i] = this.elem[child];
            this.keys[i] = this.keys[child];
         }

         this.elem[i] = lastElem;
         this.keys[i] = lastPrio;
         return minElem;
      }
   }

   public void update(Object key, Object value) {
      this.update_(((Number)key).intValue(), ((Integer)value).intValue());
   }

   public void update_(int key, int value) {
      int i;
      for(i = 1; i <= this.size && this.elem[i] != value; ++i) {
         ;
      }

      if(i <= this.size) {
         if(key > this.keys[i]) {
            while(i * 2 <= this.size) {
               int child = i * 2;
               if(child != this.size && this.keys[child + 1] < this.keys[child]) {
                  ++child;
               }

               if(key <= this.keys[child]) {
                  break;
               }

               this.elem[i] = this.elem[child];
               this.keys[i] = this.keys[child];
               i = child;
            }

            this.elem[i] = value;
            this.keys[i] = key;
         } else {
            while(this.keys[i / 2] > key) {
               this.elem[i] = this.elem[i / 2];
               this.keys[i] = this.keys[i / 2];
               i /= 2;
            }

            this.elem[i] = value;
            this.keys[i] = key;
         }

      }
   }

   public void reset() {
      this.size = 0;
   }

   public void insert(Object key, Object value) {
      this.insert_(((Number)key).intValue(), ((Integer)value).intValue());
   }

   public void insert_(int key, int value) {
      ++this.size;
      if(this.size > this.capacity) {
         this.ensureCapacity((int)((double)this.capacity * 2.0D));
      }

      int i;
      for(i = this.size; this.keys[i / 2] > key; i /= 2) {
         this.elem[i] = this.elem[i / 2];
         this.keys[i] = this.keys[i / 2];
      }

      this.elem[i] = value;
      this.keys[i] = key;
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

   public void clear() {
      this.size = 0;
      Arrays.fill(this.keys, 0);
      Arrays.fill(this.elem, 0);
   }
}
