package com.graphhopper.storage;

import com.graphhopper.storage.Lock;
import com.graphhopper.storage.LockFactory;
import com.graphhopper.util.Helper;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

public class NativeFSLockFactory implements LockFactory {
   private File lockDir;

   public NativeFSLockFactory() {
   }

   public NativeFSLockFactory(File dir) {
      this.lockDir = dir;
   }

   public void setLockDir(File lockDir) {
      this.lockDir = lockDir;
   }

   public synchronized Lock create(String fileName, boolean writeAccess) {
      if(this.lockDir == null) {
         throw new RuntimeException("Set lockDir before creating " + (writeAccess?"write":"read") + " locks");
      } else {
         return new NativeFSLockFactory.NativeLock(this.lockDir, fileName, writeAccess);
      }
   }

   public synchronized void forceRemove(String fileName, boolean writeAccess) {
      if(this.lockDir.exists()) {
         this.create(fileName, writeAccess).release();
         File lockFile = new File(this.lockDir, fileName);
         if(lockFile.exists() && !lockFile.delete()) {
            throw new RuntimeException("Cannot delete " + lockFile);
         }
      }

   }

   public static void main(String[] args) throws IOException {
      File file = new File("tmp.lock");
      file.createNewFile();
      FileChannel channel = (new RandomAccessFile(file, "r")).getChannel();
      boolean shared = true;
      FileLock lock1 = channel.tryLock(0L, Long.MAX_VALUE, shared);
      System.out.println("locked " + lock1);
      System.in.read();
      System.out.println("release " + lock1);
      lock1.release();
   }

   static class NativeLock implements Lock {
      private RandomAccessFile tmpRaFile;
      private FileChannel tmpChannel;
      private FileLock tmpLock;
      private final String name;
      private final File lockDir;
      private final File lockFile;
      private final boolean writeLock;
      private Exception failedReason;

      public NativeLock(File lockDir, String fileName, boolean writeLock) {
         this.name = fileName;
         this.lockDir = lockDir;
         this.lockFile = new File(lockDir, fileName);
         this.writeLock = writeLock;
      }

      public synchronized boolean tryLock() {
         if(this.lockExists()) {
            return false;
         } else if(!this.lockDir.exists() && !this.lockDir.mkdirs()) {
            throw new RuntimeException("Directory " + this.lockDir + " does not exist and cannot created to place lock file there: " + this.lockFile);
         } else if(!this.lockDir.isDirectory()) {
            throw new IllegalArgumentException("lockDir has to be a directory: " + this.lockDir);
         } else {
            try {
               this.failedReason = null;
               this.tmpRaFile = new RandomAccessFile(this.lockFile, "rw");
            } catch (IOException var13) {
               this.failedReason = var13;
               return false;
            }

            try {
               this.tmpChannel = this.tmpRaFile.getChannel();

               try {
                  this.tmpLock = this.tmpChannel.tryLock(0L, Long.MAX_VALUE, !this.writeLock);
               } catch (Exception var12) {
                  this.failedReason = var12;
               } finally {
                  if(this.tmpLock == null) {
                     Helper.close(this.tmpChannel);
                     this.tmpChannel = null;
                  }

               }
            } finally {
               if(this.tmpChannel == null) {
                  Helper.close(this.tmpRaFile);
                  this.tmpRaFile = null;
               }

            }

            return this.lockExists();
         }
      }

      private synchronized boolean lockExists() {
         return this.tmpLock != null;
      }

      public synchronized boolean isLocked() {
         if(!this.lockFile.exists()) {
            return false;
         } else if(this.lockExists()) {
            return true;
         } else {
            try {
               boolean ex = this.tryLock();
               if(ex) {
                  this.release();
               }

               return !ex;
            } catch (Exception var2) {
               return false;
            }
         }
      }

      public synchronized void release() {
         if(this.lockExists()) {
            try {
               this.failedReason = null;
               this.tmpLock.release();
            } catch (Exception var104) {
               throw new RuntimeException(var104);
            } finally {
               this.tmpLock = null;

               try {
                  this.tmpChannel.close();
               } catch (Exception var102) {
                  throw new RuntimeException(var102);
               } finally {
                  this.tmpChannel = null;

                  try {
                     this.tmpRaFile.close();
                  } catch (Exception var100) {
                     throw new RuntimeException(var100);
                  } finally {
                     this.tmpRaFile = null;
                  }
               }
            }

            this.lockFile.delete();
         }

      }

      public String getName() {
         return this.name;
      }

      public Exception getObtainFailedReason() {
         return this.failedReason;
      }

      public String toString() {
         return this.lockFile.toString();
      }
   }
}
