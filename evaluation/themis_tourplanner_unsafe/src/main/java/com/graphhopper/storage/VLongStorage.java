package com.graphhopper.storage;

import java.util.Arrays;

public class VLongStorage {
   private byte[] bytes;
   private int pointer;

   public VLongStorage() {
      this(10);
   }

   public VLongStorage(int cap) {
      this(new byte[cap]);
   }

   public VLongStorage(byte[] bytes) {
      this.pointer = 0;
      this.bytes = bytes;
   }

   public void seek(long pos) {
      this.pointer = (int)pos;
   }

   public long getPosition() {
      return (long)this.pointer;
   }

   public long getLength() {
      return (long)this.bytes.length;
   }

   byte readByte() {
      byte b = this.bytes[this.pointer];
      ++this.pointer;
      return b;
   }

   void writeByte(byte b) {
      if(this.pointer >= this.bytes.length) {
         int cap = Math.max(10, (int)((float)this.pointer * 1.5F));
         this.bytes = Arrays.copyOf(this.bytes, cap);
      }

      this.bytes[this.pointer] = b;
      ++this.pointer;
   }

   public final void writeVLong(long i) {
      assert i >= 0L;

      while((i & -128L) != 0L) {
         this.writeByte((byte)((int)(i & 127L | 128L)));
         i >>>= 7;
      }

      this.writeByte((byte)((int)i));
   }

   public long readVLong() {
      byte b = this.readByte();
      if(b >= 0) {
         return (long)b;
      } else {
         long i = (long)b & 127L;
         b = this.readByte();
         i |= ((long)b & 127L) << 7;
         if(b >= 0) {
            return i;
         } else {
            b = this.readByte();
            i |= ((long)b & 127L) << 14;
            if(b >= 0) {
               return i;
            } else {
               b = this.readByte();
               i |= ((long)b & 127L) << 21;
               if(b >= 0) {
                  return i;
               } else {
                  b = this.readByte();
                  i |= ((long)b & 127L) << 28;
                  if(b >= 0) {
                     return i;
                  } else {
                     b = this.readByte();
                     i |= ((long)b & 127L) << 35;
                     if(b >= 0) {
                        return i;
                     } else {
                        b = this.readByte();
                        i |= ((long)b & 127L) << 42;
                        if(b >= 0) {
                           return i;
                        } else {
                           b = this.readByte();
                           i |= ((long)b & 127L) << 49;
                           if(b >= 0) {
                              return i;
                           } else {
                              b = this.readByte();
                              i |= ((long)b & 127L) << 56;
                              if(b >= 0) {
                                 return i;
                              } else {
                                 throw new RuntimeException("Invalid vLong detected (negative values disallowed)");
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public void trimToSize() {
      if(this.bytes.length > this.pointer) {
         byte[] tmp = new byte[this.pointer];
         System.arraycopy(this.bytes, 0, tmp, 0, this.pointer);
         this.bytes = tmp;
      }

   }

   public byte[] getBytes() {
      return this.bytes;
   }
}
