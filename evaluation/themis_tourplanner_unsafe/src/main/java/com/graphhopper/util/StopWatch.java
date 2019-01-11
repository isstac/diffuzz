package com.graphhopper.util;

import com.graphhopper.util.Helper;

public class StopWatch {
   private long lastTime;
   private long nanoTime;
   private String name = "";

   public StopWatch(String name) {
      this.name = name;
   }

   public StopWatch() {
   }

   public StopWatch setName(String name) {
      this.name = name;
      return this;
   }

   public StopWatch start() {
      this.lastTime = System.nanoTime();
      return this;
   }

   public StopWatch stop() {
      if(this.lastTime < 0L) {
         return this;
      } else {
         this.nanoTime += System.nanoTime() - this.lastTime;
         this.lastTime = -1L;
         return this;
      }
   }

   public long getTime() {
      return this.nanoTime / 1000000L;
   }

   public long getNanos() {
      return this.nanoTime;
   }

   public String toString() {
      String str = "";
      if(!Helper.isEmpty(this.name)) {
         str = str + this.name + " ";
      }

      return str + "time:" + this.getSeconds();
   }

   public float getSeconds() {
      return (float)this.nanoTime / 1.0E9F;
   }
}
