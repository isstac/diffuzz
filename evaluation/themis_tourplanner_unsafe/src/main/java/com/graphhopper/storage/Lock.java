package com.graphhopper.storage;

public interface Lock {
   String getName();

   boolean tryLock();

   boolean isLocked();

   void release();

   Exception getObtainFailedReason();
}
