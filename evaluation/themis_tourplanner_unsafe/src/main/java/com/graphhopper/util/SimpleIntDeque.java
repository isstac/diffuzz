package com.graphhopper.util;

import java.util.Arrays;

public class SimpleIntDeque {
   private int[] arr;
   private float growFactor;
   private int frontIndex;
   private int endIndexPlusOne;

   public SimpleIntDeque() {
      this(100, 2.0F);
   }

   public SimpleIntDeque(int initSize) {
      this(initSize, 2.0F);
   }

   public SimpleIntDeque(int initSize, float growFactor) {
      if((int)((float)initSize * growFactor) <= initSize) {
         throw new RuntimeException("initial size or increasing grow-factor too low!");
      } else {
         this.growFactor = growFactor;
         this.arr = new int[initSize];
      }
   }

   int getCapacity() {
      return this.arr.length;
   }

   public void setGrowFactor(float factor) {
      this.growFactor = factor;
   }

   public boolean isEmpty() {
      return this.frontIndex >= this.endIndexPlusOne;
   }

   public int pop() {
      int tmp = this.arr[this.frontIndex];
      ++this.frontIndex;
      int smallerSize = (int)((float)this.arr.length / this.growFactor);
      if(this.frontIndex > smallerSize) {
         this.endIndexPlusOne = this.getSize();
         int[] newArr = new int[this.endIndexPlusOne + 10];
         System.arraycopy(this.arr, this.frontIndex, newArr, 0, this.endIndexPlusOne);
         this.arr = newArr;
         this.frontIndex = 0;
      }

      return tmp;
   }

   public int getSize() {
      return this.endIndexPlusOne - this.frontIndex;
   }

   public void push(int v) {
      if(this.endIndexPlusOne >= this.arr.length) {
         this.arr = Arrays.copyOf(this.arr, (int)((float)this.arr.length * this.growFactor));
      }

      this.arr[this.endIndexPlusOne] = v;
      ++this.endIndexPlusOne;
   }

   public String toString() {
      StringBuilder sb = new StringBuilder();

      for(int i = this.frontIndex; i < this.endIndexPlusOne; ++i) {
         if(i > this.frontIndex) {
            sb.append(", ");
         }

         sb.append(this.arr[i]);
      }

      return sb.toString();
   }
}
