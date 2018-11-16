package com.graphhopper.storage;

import com.graphhopper.storage.AbstractDataAccess;
import com.graphhopper.storage.DAType;
import com.graphhopper.storage.DataAccess;
import com.graphhopper.util.Constants;
import com.graphhopper.util.Helper;
import com.graphhopper.util.NotThreadSafe;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@NotThreadSafe
public class MMapDataAccess extends AbstractDataAccess {
   private RandomAccessFile raFile;
   private List segments = new ArrayList();
   private boolean cleanAndRemap = false;
   private final boolean allowWrites;

   MMapDataAccess(String name, String location, ByteOrder order, boolean allowWrites) {
      super(name, location, order);
      this.allowWrites = allowWrites;
   }

   MMapDataAccess cleanAndRemap(boolean cleanAndRemap) {
      this.cleanAndRemap = cleanAndRemap;
      return this;
   }

   private void initRandomAccessFile() {
      if(this.raFile == null) {
         try {
            this.raFile = new RandomAccessFile(this.getFullName(), this.allowWrites?"rw":"r");
         } catch (IOException var2) {
            throw new RuntimeException(var2);
         }
      }
   }

   public MMapDataAccess create(long bytes) {
      if(!this.segments.isEmpty()) {
         throw new IllegalThreadStateException("already created");
      } else {
         this.initRandomAccessFile();
         bytes = Math.max(40L, bytes);
         this.setSegmentSize(this.segmentSizeInBytes);
         this.ensureCapacity(bytes);
         return this;
      }
   }

   public DataAccess copyTo(DataAccess da) {
      return super.copyTo(da);
   }

   public boolean ensureCapacity(long bytes) {
      return this.mapIt(100L, bytes, true);
   }

   protected boolean mapIt(long offset, long byteCount, boolean clearNew) {
      if(byteCount < 0L) {
         throw new IllegalArgumentException("new capacity has to be strictly positive");
      } else if(byteCount <= this.getCapacity()) {
         return false;
      } else {
         long longSegmentSize = (long)this.segmentSizeInBytes;
         int segmentsToMap = (int)(byteCount / longSegmentSize);
         if(segmentsToMap < 0) {
            throw new IllegalStateException("Too many segments needs to be allocated. Increase segmentSize.");
         } else {
            if(byteCount % longSegmentSize != 0L) {
               ++segmentsToMap;
            }

            if(segmentsToMap == 0) {
               throw new IllegalStateException("0 segments are not allowed.");
            } else {
               long bufferStart = offset;
               int i = 0;
               long newFileLength = offset + (long)segmentsToMap * longSegmentSize;

               try {
                  int newSegments;
                  if(this.cleanAndRemap) {
                     newSegments = segmentsToMap;
                     this.clean(0, this.segments.size());
                     Helper.cleanHack();
                     this.segments.clear();
                  } else {
                     bufferStart += (long)this.segments.size() * longSegmentSize;
                     newSegments = segmentsToMap - this.segments.size();
                  }

                  while(i < newSegments) {
                     this.segments.add(this.newByteBuffer(bufferStart, longSegmentSize));
                     bufferStart += longSegmentSize;
                     ++i;
                  }

                  return true;
               } catch (IOException var16) {
                  throw new RuntimeException("Couldn\'t map buffer " + i + " of " + segmentsToMap + " at position " + offset + " for " + byteCount + " bytes with offset " + offset + ", new fileLength:" + newFileLength, var16);
               }
            }
         }
      }
   }

   private ByteBuffer newByteBuffer(long offset, long byteCount) throws IOException {
      MappedByteBuffer buf = null;
      IOException ioex = null;
      int tmp = 0;

      while(tmp < 1) {
         try {
            buf = this.raFile.getChannel().map(this.allowWrites?MapMode.READ_WRITE:MapMode.READ_ONLY, offset, byteCount);
            break;
         } catch (IOException var11) {
            ioex = var11;
            ++tmp;
            Helper.cleanHack();

            try {
               Thread.sleep(5L);
            } catch (InterruptedException var10) {
               ;
            }
         }
      }

      if(buf == null) {
         if(ioex == null) {
            throw new AssertionError("internal problem as the exception \'ioex\' shouldn\'t be null");
         } else {
            throw ioex;
         }
      } else {
         buf.order(this.byteOrder);
         boolean var12 = false;
         if(var12) {
            int count = (int)(byteCount / (long)EMPTY.length);

            int len;
            for(len = 0; len < count; ++len) {
               buf.put(EMPTY);
            }

            len = (int)(byteCount % (long)EMPTY.length);
            if(len > 0) {
               buf.put(EMPTY, count * EMPTY.length, len);
            }
         }

         return buf;
      }
   }

   public boolean loadExisting() {
      if(this.segments.size() > 0) {
    	  System.out.println("already initialized");
         throw new IllegalStateException("already initialized");
      } else if(this.isClosed()) {
    	  System.out.println("already closed");
         throw new IllegalStateException("already closed");
      } else {
         File file = new File(this.getFullName());
         if(file.exists() && file.length() != 0L) {
            this.initRandomAccessFile();

            try {
               long ex = this.readHeader(this.raFile);
               if(ex < 0L) {
                  return false;
               } else {
                  this.mapIt(100L, ex - 100L, false);
                  return true;
               }
            } catch (IOException var4) {
               throw new RuntimeException("Problem while loading " + this.getFullName(), var4);
            }
         } else {
            return false;
         }
      }
   }

   public void flush() {
      if(this.isClosed()) {
         throw new IllegalStateException("already closed");
      } else {
         try {
            if(!this.segments.isEmpty() && this.segments.get(0) instanceof MappedByteBuffer) {
               Iterator ex = this.segments.iterator();

               while(ex.hasNext()) {
                  ByteBuffer bb = (ByteBuffer)ex.next();
                  ((MappedByteBuffer)bb).force();
               }
            }

            this.writeHeader(this.raFile, this.raFile.length(), this.segmentSizeInBytes);
            this.raFile.getFD().sync();
         } catch (Exception var3) {
            throw new RuntimeException(var3);
         }
      }
   }

   public void close() {
      super.close();
      this.close(true);
   }

   void close(boolean forceClean) {
      this.clean(0, this.segments.size());
      this.segments.clear();
      Helper.close(this.raFile);
      if(forceClean) {
         Helper.cleanHack();
      }

   }

   public final void setInt(long bytePos, int value) {
      int bufferIndex = (int)(bytePos >> this.segmentSizePower);
      int index = (int)(bytePos & (long)this.indexDivisor);
      ((ByteBuffer)this.segments.get(bufferIndex)).putInt(index, value);
   }

   public final int getInt(long bytePos) {
      int bufferIndex = (int)(bytePos >> this.segmentSizePower);
      int index = (int)(bytePos & (long)this.indexDivisor);
      return ((ByteBuffer)this.segments.get(bufferIndex)).getInt(index);
   }

   public final void setShort(long bytePos, short value) {
      int bufferIndex = (int)(bytePos >>> this.segmentSizePower);
      int index = (int)(bytePos & (long)this.indexDivisor);
      ((ByteBuffer)this.segments.get(bufferIndex)).putShort(index, value);
   }

   public final short getShort(long bytePos) {
      int bufferIndex = (int)(bytePos >>> this.segmentSizePower);
      int index = (int)(bytePos & (long)this.indexDivisor);
      return ((ByteBuffer)this.segments.get(bufferIndex)).getShort(index);
   }

   public void setBytes(long bytePos, byte[] values, int length) {
      assert length <= this.segmentSizeInBytes : "the length has to be smaller or equal to the segment size: " + length + " vs. " + this.segmentSizeInBytes;

      int bufferIndex = (int)(bytePos >>> this.segmentSizePower);
      int index = (int)(bytePos & (long)this.indexDivisor);
      ByteBuffer bb = (ByteBuffer)this.segments.get(bufferIndex);
      bb.position(index);
      int delta = index + length - this.segmentSizeInBytes;
      if(delta > 0) {
         length -= delta;
         bb.put(values, 0, length);
         bb = (ByteBuffer)this.segments.get(bufferIndex + 1);
         bb.position(0);
         bb.put(values, length, delta);
      } else {
         bb.put(values, 0, length);
      }

   }

   public void getBytes(long bytePos, byte[] values, int length) {
      assert length <= this.segmentSizeInBytes : "the length has to be smaller or equal to the segment size: " + length + " vs. " + this.segmentSizeInBytes;

      int bufferIndex = (int)(bytePos >>> this.segmentSizePower);
      int index = (int)(bytePos & (long)this.indexDivisor);
      ByteBuffer bb = (ByteBuffer)this.segments.get(bufferIndex);
      bb.position(index);
      int delta = index + length - this.segmentSizeInBytes;
      if(delta > 0) {
         length -= delta;
         bb.get(values, 0, length);
         bb = (ByteBuffer)this.segments.get(bufferIndex + 1);
         bb.position(0);
         bb.get(values, length, delta);
      } else {
         bb.get(values, 0, length);
      }

   }

   public long getCapacity() {
      long cap = 0L;

      ByteBuffer bb;
      for(Iterator i$ = this.segments.iterator(); i$.hasNext(); cap += (long)bb.capacity()) {
         bb = (ByteBuffer)i$.next();
      }

      return cap;
   }

   public int getSegments() {
      return this.segments.size();
   }

   private void clean(int from, int to) {
      for(int i = from; i < to; ++i) {
         ByteBuffer bb = (ByteBuffer)this.segments.get(i);
         Helper.cleanMappedByteBuffer(bb);
         this.segments.set(i, (Object)null);
      }

   }

   public void trimTo(long capacity) {
      if(capacity < (long)this.segmentSizeInBytes) {
         capacity = (long)this.segmentSizeInBytes;
      }

      int remainingSegNo = (int)(capacity / (long)this.segmentSizeInBytes);
      if(capacity % (long)this.segmentSizeInBytes != 0L) {
         ++remainingSegNo;
      }

      this.clean(remainingSegNo, this.segments.size());
      Helper.cleanHack();
      this.segments = new ArrayList(this.segments.subList(0, remainingSegNo));

      try {
         if(!Constants.WINDOWS) {
            this.raFile.setLength((long)(100 + remainingSegNo * this.segmentSizeInBytes));
         }

      } catch (Exception var5) {
         throw new RuntimeException(var5);
      }
   }

   boolean releaseSegment(int segNumber) {
      ByteBuffer segment = (ByteBuffer)this.segments.get(segNumber);
      if(segment instanceof MappedByteBuffer) {
         ((MappedByteBuffer)segment).force();
      }

      Helper.cleanMappedByteBuffer(segment);
      this.segments.set(segNumber, (Object)null);
      Helper.cleanHack();
      return true;
   }

   public void rename(String newName) {
      if(this.checkBeforeRename(newName)) {
         this.close();
         super.rename(newName);
         this.raFile = null;
         this.closed = false;
         this.loadExisting();
      }
   }

   public DAType getType() {
      return DAType.MMAP;
   }
}
