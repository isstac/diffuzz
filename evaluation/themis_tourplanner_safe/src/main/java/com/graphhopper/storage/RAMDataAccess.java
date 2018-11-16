package com.graphhopper.storage;

import com.graphhopper.storage.AbstractDataAccess;
import com.graphhopper.storage.DAType;
import com.graphhopper.storage.DataAccess;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.util.Arrays;
//import org.slf4j.LoggerFactory;

public class RAMDataAccess extends AbstractDataAccess {
   private byte[][] segments = new byte[0][];
   private boolean store;

   RAMDataAccess(String name, String location, boolean store, ByteOrder order) {
      super(name, location, order);
      this.store = store;
   }

   public RAMDataAccess store(boolean store) {
      this.store = store;
      return this;
   }

   public boolean isStoring() {
      return this.store;
   }

   public DataAccess copyTo(DataAccess da) {
      if(!(da instanceof RAMDataAccess)) {
         return super.copyTo(da);
      } else {
         this.copyHeader(da);
         RAMDataAccess rda = (RAMDataAccess)da;
         rda.segments = new byte[this.segments.length][];

         for(int i = 0; i < this.segments.length; ++i) {
            byte[] area = this.segments[i];
            rda.segments[i] = Arrays.copyOf(area, area.length);
         }

         rda.setSegmentSize(this.segmentSizeInBytes);
         return da;
      }
   }

   public RAMDataAccess create(long bytes) {
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
               byte[][] err = (byte[][])Arrays.copyOf(this.segments, this.segments.length + segmentsToCreate);

               for(int i = this.segments.length; i < err.length; ++i) {
                  err[i] = new byte[1 << this.segmentSizePower];
               }

               this.segments = err;
               return true;
            } catch (OutOfMemoryError var10) {
               throw new OutOfMemoryError(var10.getMessage() + " - problem when allocating new memory. Old capacity: " + cap + ", new bytes:" + newBytes + ", segmentSizeIntsPower:" + this.segmentSizePower + ", new segments:" + segmentsToCreate + ", existing:" + this.segments.length);
            }
         }
      }
   }

   public boolean loadExisting() {
      if(this.segments.length > 0) {
         throw new IllegalStateException("already initialized");
      } else if(this.isClosed()) {
         throw new IllegalStateException("already closed");
      } else if(!this.store) {
         return false;
      } else {
         File file = new File(this.getFullName());
         if(file.exists() && file.length() != 0L) {
            try {
//               RandomAccessFile ex = new RandomAccessFile(this.getFullName(), "r");
            	DataInputStream ex = new DataInputStream(new FileInputStream(file));

               boolean segmentCount;
               try {
                  long byteCount = this.readHeader(ex) - 100L;
                  if(byteCount >= 0L) {
                	  ex = new DataInputStream(new FileInputStream(file));
//                     ex.seek(100L);
                	  ex.read(new byte[100]);
                	  
                     int var14 = (int)(byteCount / (long)this.segmentSizeInBytes);
                     if(byteCount % (long)this.segmentSizeInBytes != 0L) {
                        ++var14;
                     }

                     this.segments = new byte[var14][];

                     for(int s = 0; s < var14; ++s) {
                        byte[] bytes = new byte[this.segmentSizeInBytes];
                        int read = ex.read(bytes);
                        if(read <= 0) {
                           throw new IllegalStateException("segment " + s + " is empty? " + this.toString());
                        }

                        this.segments[s] = bytes;
                     }
                     
                     boolean var15 = true;
                     return var15;
                  }

                  segmentCount = false;
               } finally {
                  ex.close();
               }

               return segmentCount;
            } catch (IOException var13) {
               throw new RuntimeException("Problem while loading " + this.getFullName(), var13);
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
                  byte[] area = this.segments[s];
                  ex.write(area);
               }
            } finally {
               ex.close();
            }

         } catch (Exception var10) {
            throw new RuntimeException("Couldn\'t store bytes to " + this.toString(), var10);
         }
      }
   }

   public final void setInt(long bytePos, int value) {
      assert this.segmentSizePower > 0 : "call create or loadExisting before usage!";

      int bufferIndex = (int)(bytePos >>> this.segmentSizePower);
      int index = (int)(bytePos & (long)this.indexDivisor);

      assert index + 4 <= this.segmentSizeInBytes : "integer cannot be distributed over two segments";

      this.bitUtil.fromInt(this.segments[bufferIndex], value, index);
   }

   public final int getInt(long bytePos) {
      assert this.segmentSizePower > 0 : "call create or loadExisting before usage!";

      int bufferIndex = (int)(bytePos >>> this.segmentSizePower);
      int index = (int)(bytePos & (long)this.indexDivisor);

      assert index + 4 <= this.segmentSizeInBytes : "integer cannot be distributed over two segments";

      if(bufferIndex > this.segments.length) {
//         LoggerFactory.getLogger(this.getClass()).error(this.getName() + ", segments:" + this.segments.length + ", bufIndex:" + bufferIndex + ", bytePos:" + bytePos + ", segPower:" + this.segmentSizePower);
      }

      return this.bitUtil.toInt(this.segments[bufferIndex], index);
   }

   public final void setShort(long bytePos, short value) {
      assert this.segmentSizePower > 0 : "call create or loadExisting before usage!";

      int bufferIndex = (int)(bytePos >>> this.segmentSizePower);
      int index = (int)(bytePos & (long)this.indexDivisor);

      assert index + 2 <= this.segmentSizeInBytes : "integer cannot be distributed over two segments";

      this.bitUtil.fromShort(this.segments[bufferIndex], value, index);
   }

   public final short getShort(long bytePos) {
      assert this.segmentSizePower > 0 : "call create or loadExisting before usage!";

      int bufferIndex = (int)(bytePos >>> this.segmentSizePower);
      int index = (int)(bytePos & (long)this.indexDivisor);

      assert index + 2 <= this.segmentSizeInBytes : "integer cannot be distributed over two segments";

      return this.bitUtil.toShort(this.segments[bufferIndex], index);
   }

   public void setBytes(long bytePos, byte[] values, int length) {
      assert length <= this.segmentSizeInBytes : "the length has to be smaller or equal to the segment size: " + length + " vs. " + this.segmentSizeInBytes;

      assert this.segmentSizePower > 0 : "call create or loadExisting before usage!";

      int bufferIndex = (int)(bytePos >>> this.segmentSizePower);
      int index = (int)(bytePos & (long)this.indexDivisor);
      byte[] seg = this.segments[bufferIndex];
      int delta = index + length - this.segmentSizeInBytes;
      if(delta > 0) {
         length -= delta;
         System.arraycopy(values, 0, seg, index, length);
         seg = this.segments[bufferIndex + 1];
         System.arraycopy(values, length, seg, 0, delta);
      } else {
         System.arraycopy(values, 0, seg, index, length);
      }

   }

   public void getBytes(long bytePos, byte[] values, int length) {
      assert length <= this.segmentSizeInBytes : "the length has to be smaller or equal to the segment size: " + length + " vs. " + this.segmentSizeInBytes;

      assert this.segmentSizePower > 0 : "call create or loadExisting before usage!";

      int bufferIndex = (int)(bytePos >>> this.segmentSizePower);
      int index = (int)(bytePos & (long)this.indexDivisor);
      byte[] seg = this.segments[bufferIndex];
      int delta = index + length - this.segmentSizeInBytes;
      if(delta > 0) {
         length -= delta;
         System.arraycopy(seg, index, values, 0, length);
         seg = this.segments[bufferIndex + 1];
         System.arraycopy(seg, 0, values, length, delta);
      } else {
         System.arraycopy(seg, index, values, 0, length);
      }

   }

   public void close() {
      super.close();
      this.segments = new byte[0][];
      this.closed = true;
   }

   public long getCapacity() {
      return (long)this.getSegments() * (long)this.segmentSizeInBytes;
   }

   public int getSegments() {
      return this.segments.length;
   }

   public void trimTo(long capacity) {
      if(capacity > this.getCapacity()) {
         throw new IllegalStateException("Cannot increase capacity (" + this.getCapacity() + ") to " + capacity + " via trimTo. Use ensureCapacity instead. ");
      } else {
         if(capacity < (long)this.segmentSizeInBytes) {
            capacity = (long)this.segmentSizeInBytes;
         }

         int remainingSegments = (int)(capacity / (long)this.segmentSizeInBytes);
         if(capacity % (long)this.segmentSizeInBytes != 0L) {
            ++remainingSegments;
         }

         this.segments = (byte[][])Arrays.copyOf(this.segments, remainingSegments);
      }
   }

   public void rename(String newName) {
      if(this.checkBeforeRename(newName)) {
         if(this.store) {
            super.rename(newName);
         }

         this.name = newName;
      }
   }

   public DAType getType() {
      return this.isStoring()?DAType.RAM_STORE:DAType.RAM;
   }
}
