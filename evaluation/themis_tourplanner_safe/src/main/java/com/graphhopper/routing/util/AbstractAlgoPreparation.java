package com.graphhopper.routing.util;

public abstract class AbstractAlgoPreparation {
   private boolean prepared = false;

   public void doWork() {
      if(this.prepared) {
         throw new IllegalStateException("Call doWork only once!");
      } else {
         this.prepared = true;
      }
   }

   public boolean isPrepared() {
      return this.prepared;
   }
}
