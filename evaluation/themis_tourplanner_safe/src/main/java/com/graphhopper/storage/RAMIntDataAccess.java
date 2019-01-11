package com.graphhopper.storage;

import com.graphhopper.storage.AbstractDataAccess;
import com.graphhopper.storage.DAType;
import com.graphhopper.storage.DataAccess;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.util.Arrays;

class RAMIntDataAccess extends AbstractDataAccess {
   private int[][] segments = new int[0][];
   private boolean closed = false;
   private boolean store;
   private transient int segmentSizeIntsPower;

   RAMIntDataAccess(String name, String location, boolean store, ByteOrder order) {
      super(name, location, order);
      this.store = store;
   }

   public RAMIntDataAccess setStore(boolean store) {
      this.store = store;
      return this;
   }

   public boolean isStoring() {
      return this.store;
   }

   public DataAccess copyTo(DataAccess da) {
      if(!(da instanceof RAMIntDataAccess)) {
         return super.copyTo(da);
      } else {
         this.copyHeader(da);
         RAMIntDataAccess rda = (RAMIntDataAccess)da;
         rda.segments = new int[this.segments.length][];

         for(int i = 0; i < this.segments.length; ++i) {
            int[] area = this.segments[i];
            rda.segments[i] = Arrays.copyOf(area, area.length);
         }

         rda.setSegmentSize(this.segmentSizeInBytes);
         return da;
      }
   }

   public RAMIntDataAccess create(long bytes) {
      if(this.segments.length > 0) {
         throw new IllegalThreadStateException("already created");
      } else {
         this.setSegmentSize(this.segmentSizeInBytes);
         this.ensureCapacity(Math.max(40L, bytes));
         return this;
      }
   }

   public boolean ensureCapacity(long bytes) {
      if(bytes < 0L) {
         throw new IllegalArgumentException("new capacity has to be strictly positive");
      } else {
         long cap = this.getCapacity();
         long newBytes = bytes - cap;
         if(newBytes <= 0L) {
            return false;
         } else {
            int segmentsToCreate = (int)(newBytes / (long)this.segmentSizeInBytes);
            if(newBytes % (long)this.segmentSizeInBytes != 0L) {
               ++segmentsToCreate;
            }

            try {
               int[][] err = (int[][])Arrays.copyOf(this.segments, this.segments.length + segmentsToCreate);

               for(int i = this.segments.length; i < err.length; ++i) {
                  err[i] = new int[1 << this.segmentSizeIntsPower];
               }

               this.segments = err;
               return true;
            } catch (OutOfMemoryError var10) {
               throw new OutOfMemoryError(var10.getMessage() + " - problem when allocating new memory. Old capacity: " + cap + ", new bytes:" + newBytes + ", segmentSizeIntsPower:" + this.segmentSizeIntsPower + ", new segments:" + segmentsToCreate + ", existing:" + this.segments.length);
            }
         }
      }
   }

   public boolean loadExisting() {
      if(this.segments.length > 0) {
    	  System.out.println("already initialized");
         throw new IllegalStateException("already initialized");
      } else if(this.isClosed()) {
    	  System.out.println("already closed");
         throw new IllegalStateException("already closed");
      } else if(!this.store) {
         return false;
      } else {
         File file = new File(this.getFullName());
         if(file.exists() && file.length() != 0L) {
            try {
//               RandomAccessFile ex = new RandomAccessFile(this.getFullName(), "r");
            	DataInputStream ex = new DataInputStream(new FileInputStream(file));

               boolean bytes;
               try {
                  long byteCount = this.readHeader(ex) - 100L;
                  if(byteCount >= 0L) {
                     byte[] var16 = new byte[this.segmentSizeInBytes];
               	  ex = new DataInputStream(new FileInputStream(file));
//                ex.seek(100L);
           	  ex.read(new byte[100]);
                     int segmentCount = (int)(byteCount / (long)this.segmentSizeInBytes);
                     if(byteCount % (long)this.segmentSizeInBytes != 0L) {
                        ++segmentCount;
                     }

                     this.segments = new int[segmentCount][];

                     for(int s = 0; s < segmentCount; ++s) {
                        int read = ex.read(var16) / 4;
                        int[] area = new int[read];

                        for(int j = 0; j < read; ++j) {
                           area[j] = this.bitUtil.toInt(var16, j * 4);
                        }

                        this.segments[s] = area;
                     }

                     boolean var17 = true;
                     return var17;
                  }

                  bytes = false;
               } finally {
                  ex.close();
               }

               return bytes;
            } catch (IOException var15) {
               throw new RuntimeException("Problem while loading " + this.getFullName(), var15);
            }
         } else {
            return false;
         }
      }
   }

   public void flush() {
      if(this.closed) {
         throw new IllegalStateException("already closed");
      } else if(this.store) {
         try {
            RandomAccessFile ex = new RandomAccessFile(this.getFullName(), "rw");

            try {
               long len = this.getCapacity();
               this.writeHeader(ex, len, this.segmentSizeInBytes);
               ex.seek(100L);

               for(int s = 0; s < this.segments.length; ++s) {
                  int[] area = this.segments[s];
                  int intLen = area.length;
                  byte[] byteArea = new byte[intLen * 4];

                  for(int i = 0; i < intLen; ++i) {
                     this.bitUtil.fromInt(byteArea, area[i], i * 4);
                  }

                  ex.write(byteArea);
               }
            } finally {
               ex.close();
            }

         } catch (Exception var13) {
            throw new RuntimeException("Couldn\'t store integers to " + this.toString(), var13);
         }
      }
   }

   public final void setInt(long bytePos, int value) {
      assert this.segmentSizeIntsPower > 0 : "call create or loadExisting before usage!";

      bytePos >>>= 2;
      int bufferIndex = (int)(bytePos >>> this.segmentSizeIntsPower);
      int index = (int)(bytePos & (long)this.indexDivisor);
      this.segments[bufferIndex][index] = value;
   }

   public final int getInt(long bytePos) {
      assert this.segmentSizeIntsPower > 0 : "call create or loadExisting before usage!";

      bytePos >>>= 2;
      int bufferIndex = (int)(bytePos >>> this.segmentSizeIntsPower);
      int index = (int)(bytePos & (long)this.indexDivisor);
      return this.segments[bufferIndex][index];
   }

   public final void setShort(long bytePos, short value) {
      assert this.segmentSizeIntsPower > 0 : "call create or loadExisting before usage!";

      if(bytePos % 4L != 0L && bytePos % 4L != 2L) {
         throw new IllegalMonitorStateException("bytePos of wrong multiple for RAMInt " + bytePos);
      } else {
         long tmpIndex = bytePos >>> 1;
         int bufferIndex = (int)(tmpIndex >>> this.segmentSizeIntsPower);
         int index = (int)(tmpIndex & (long)this.indexDivisor);
         if(tmpIndex * 2L == bytePos) {
            this.segments[bufferIndex][index] = value;
         } else {
            this.segments[bufferIndex][index] = value << 16;
         }

      }
   }

   public final short getShort(long bytePos) {
      assert this.segmentSizeIntsPower > 0 : "call create or loadExisting before usage!";

      if(bytePos % 4L != 0L && bytePos % 4L != 2L) {
         throw new IllegalMonitorStateException("bytePos of wrong multiple for RAMInt " + bytePos);
      } else {
         long tmpIndex = bytePos >> 1;
         int bufferIndex = (int)(tmpIndex >> this.segmentSizeIntsPower);
         int index = (int)(tmpIndex & (long)this.indexDivisor);
         return tmpIndex * 2L == bytePos?(short)this.segments[bufferIndex][index]:(short)(this.segments[bufferIndex][index] >> 16);
      }
   }

   public void getBytes(long bytePos, byte[] values, int length) {
      throw new UnsupportedOperationException(this.toString() + " does not support byte based acccess. Use RAMDataAccess instead");
   }

   public void setBytes(long bytePos, byte[] values, int length) {
      throw new UnsupportedOperationException(this.toString() + " does not support byte based acccess. Use RAMDataAccess instead");
   }

   public void close() {
      super.close();
      this.segments = new int[0][];
      this.closed = true;
   }

   public long getCapacity() {
      return (long)this.getSegments() * (long)this.segmentSizeInBytes;
   }

   public int getSegments() {
      return this.segments.length;
   }

   public DataAccess setSegmentSize(int bytes) {
      super.setSegmentSize(bytes);
      this.segmentSizeIntsPower = (int)(Math.log((double)(this.segmentSizeInBytes / 4)) / Math.log(2.0D));
      this.indexDivisor = this.segmentSizeInBytes / 4 - 1;
      return this;
   }

   public void trimTo(long capacity) {
      if(capacity < (long)this.segmentSizeInBytes) {
         capacity = (long)this.segmentSizeInBytes;
      }

      int remainingSegments = (int)(capacity / (long)this.segmentSizeInBytes);
      if(capacity % (long)this.segmentSizeInBytes != 0L) {
         ++remainingSegments;
      }

      this.segments = (int[][])Arrays.copyOf(this.segments, remainingSegments);
   }

   boolean releaseSegment(int segNumber) {
      this.segments[segNumber] = null;
      return true;
   }

   public void rename(String newName) {
      if(this.checkBeforeRename(newName)) {
         if(this.store) {
            super.rename(newName);
         }

         this.name = newName;
      }
   }

   protected boolean isIntBased() {
      return true;
   }

   public DAType getType() {
      return this.isStoring()?DAType.RAM_INT_STORE:DAType.RAM_INT;
   }
}
