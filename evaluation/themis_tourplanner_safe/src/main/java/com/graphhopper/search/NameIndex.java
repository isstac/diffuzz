package com.graphhopper.search;

import com.graphhopper.storage.DataAccess;
import com.graphhopper.storage.Directory;
import com.graphhopper.storage.Storable;
import com.graphhopper.util.BitUtil;
import com.graphhopper.util.Helper;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

public class NameIndex implements Storable {
//   private static final Logger logger = LoggerFactory.getLogger(NameIndex.class);
   private static final long START_POINTER = 1L;
   private final DataAccess names;
   private long bytePointer = 1L;
   private String lastName;
   private long lastIndex;

   public NameIndex(Directory dir) {
      this.names = dir.find("names");
   }

   public NameIndex create(long cap) {
      this.names.create(cap);
      return this;
   }

   public boolean loadExisting() {
      if(this.names.loadExisting()) {
         this.bytePointer = BitUtil.LITTLE.combineIntsToLong(this.names.getHeader(0), this.names.getHeader(4));
         return true;
      } else {
         return false;
      }
   }

   public long put(String name) {
      if(name != null && !name.isEmpty()) {
         if(name.equals(this.lastName)) {
            return this.lastIndex;
         } else {
            byte[] bytes = this.getBytes(name);
            long oldPointer = this.bytePointer;
            this.names.ensureCapacity(this.bytePointer + 1L + (long)bytes.length);
            byte[] sizeBytes = new byte[]{(byte)bytes.length};
            this.names.setBytes(this.bytePointer, sizeBytes, sizeBytes.length);
            ++this.bytePointer;
            this.names.setBytes(this.bytePointer, bytes, bytes.length);
            this.bytePointer += (long)bytes.length;
            this.lastName = name;
            this.lastIndex = oldPointer;
            return oldPointer;
         }
      } else {
         return 0L;
      }
   }

   private byte[] getBytes(String name) {
      byte[] bytes = null;

      for(int i = 0; i < 2; ++i) {
         bytes = name.getBytes(Helper.UTF_CS);
         if(bytes.length <= 255) {
            break;
         }

         String newName = name.substring(0, 64);
//         logger.info("Way name is too long: " + name + " truncated to " + newName);
         name = newName;
      }

      if(bytes.length > 255) {
         throw new IllegalStateException("Way name is too long: " + name);
      } else {
         return bytes;
      }
   }

   public String get(long pointer) {
      if(pointer < 0L) {
         throw new IllegalStateException("Pointer to access NameIndex cannot be negative:" + pointer);
      } else if(pointer == 0L) {
         return "";
      } else {
         byte[] sizeBytes = new byte[1];
         this.names.getBytes(pointer, sizeBytes, 1);
         int size = sizeBytes[0] & 255;
         byte[] bytes = new byte[size];
         this.names.getBytes(pointer + (long)sizeBytes.length, bytes, size);
         return new String(bytes, Helper.UTF_CS);
      }
   }

   public void flush() {
      this.names.setHeader(0, BitUtil.LITTLE.getIntLow(this.bytePointer));
      this.names.setHeader(4, BitUtil.LITTLE.getIntHigh(this.bytePointer));
      this.names.flush();
   }

   public void close() {
      this.names.close();
   }

   public boolean isClosed() {
      return this.names.isClosed();
   }

   public void setSegmentSize(int segments) {
      this.names.setSegmentSize(segments);
   }

   public long getCapacity() {
      return this.names.getCapacity();
   }

   public void copyTo(NameIndex nameIndex) {
      this.names.copyTo(nameIndex.names);
   }
}
