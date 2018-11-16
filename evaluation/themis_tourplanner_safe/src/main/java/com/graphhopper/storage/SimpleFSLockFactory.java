package com.graphhopper.storage;

import com.graphhopper.storage.Lock;
import com.graphhopper.storage.LockFactory;
import java.io.File;
import java.io.IOException;

public class SimpleFSLockFactory implements LockFactory {
   private File lockDir;

   public SimpleFSLockFactory() {
   }

   public SimpleFSLockFactory(File dir) {
      this.lockDir = dir;
   }

   public void setLockDir(File lockDir) {
      this.lockDir = lockDir;
   }

   public synchronized Lock create(String fileName, boolean writeAccess) {
      if(this.lockDir == null) {
         throw new RuntimeException("Set lockDir before creating locks");
      } else {
         return new SimpleFSLockFactory.SimpleLock(this.lockDir, fileName);
      }
   }

   public synchronized void forceRemove(String fileName, boolean writeAccess) {
      if(this.lockDir.exists()) {
         File lockFile = new File(this.lockDir, fileName);
         if(lockFile.exists() && !lockFile.delete()) {
            throw new RuntimeException("Cannot delete " + lockFile);
         }
      }

   }

   static class SimpleLock implements Lock {
      private final File lockDir;
      private final File lockFile;
      private final String name;
      private IOException failedReason;

      public SimpleLock(File lockDir, String fileName) {
         this.name = fileName;
         this.lockDir = lockDir;
         this.lockFile = new File(lockDir, fileName);
      }

      public synchronized boolean tryLock() {
         if(!this.lockDir.exists() && !this.lockDir.mkdirs()) {
        	 System.out.println("Directory " + this.lockDir + " does not exist and cannot created to place lock file there: " + this.lockFile);
            throw new RuntimeException("Directory " + this.lockDir + " does not exist and cannot created to place lock file there: " + this.lockFile);
         } else if(!this.lockDir.isDirectory()) {
        	 System.out.println("lockDir has to be a directory: " + this.lockDir);
            throw new IllegalArgumentException("lockDir has to be a directory: " + this.lockDir);
         } else {
            try {
               return this.lockFile.createNewFile();
            } catch (IOException var2) {
               this.failedReason = var2;
               return false;
            }
         }
      }

      public synchronized boolean isLocked() {
         return this.lockFile.exists();
      }

      public synchronized void release() {
         if(this.isLocked() && this.lockFile.exists() && !this.lockFile.delete()) {
            throw new RuntimeException("Cannot release lock file: " + this.lockFile);
         }
      }

      public String getName() {
         return this.name;
      }

      public synchronized Exception getObtainFailedReason() {
         return this.failedReason;
      }

      public String toString() {
         return this.lockFile.toString();
      }
   }
}
