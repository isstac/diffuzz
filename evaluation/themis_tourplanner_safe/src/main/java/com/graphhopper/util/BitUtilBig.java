package com.graphhopper.util;

import com.graphhopper.util.BitUtil;

public class BitUtilBig extends BitUtil {
   public final short toShort(byte[] b, int offset) {
      return (short)((b[offset] & 255) << 8 | b[offset + 1] & 255);
   }

   public final int toInt(byte[] b, int offset) {
      int var10000 = (b[offset] & 255) << 24;
      ++offset;
      var10000 |= (b[offset] & 255) << 16;
      ++offset;
      var10000 |= (b[offset] & 255) << 8;
      ++offset;
      return var10000 | b[offset] & 255;
   }

   public void fromShort(byte[] bytes, short value, int offset) {
      bytes[offset] = (byte)(value >> 8);
      bytes[offset + 1] = (byte)value;
   }

   public final void fromInt(byte[] bytes, int value, int offset) {
      bytes[offset] = (byte)(value >> 24);
      ++offset;
      bytes[offset] = (byte)(value >> 16);
      ++offset;
      bytes[offset] = (byte)(value >> 8);
      ++offset;
      bytes[offset] = (byte)value;
   }

   public final long toLong(int int0, int int1) {
      return (long)int0 << 32 | (long)int1 & 4294967295L;
   }

   public final long toLong(byte[] b, int offset) {
      return (long)this.toInt(b, offset) << 32 | (long)this.toInt(b, offset + 4) & 4294967295L;
   }

   public final void fromLong(byte[] bytes, long value, int offset) {
      bytes[offset] = (byte)((int)(value >> 56));
      ++offset;
      bytes[offset] = (byte)((int)(value >> 48));
      ++offset;
      bytes[offset] = (byte)((int)(value >> 40));
      ++offset;
      bytes[offset] = (byte)((int)(value >> 32));
      ++offset;
      bytes[offset] = (byte)((int)(value >> 24));
      ++offset;
      bytes[offset] = (byte)((int)(value >> 16));
      ++offset;
      bytes[offset] = (byte)((int)(value >> 8));
      ++offset;
      bytes[offset] = (byte)((int)value);
   }

   public byte[] fromBitString(String str) {
      int strLen = str.length();
      int bLen = str.length() / 8;
      if(strLen % 8 != 0) {
         ++bLen;
      }

      byte[] bytes = new byte[bLen];
      int charI = 0;

      for(int b = 0; b < bLen; ++b) {
         byte res = 0;

         for(int i = 0; i < 8; ++i) {
            res = (byte)(res << 1);
            if(charI < strLen && str.charAt(charI) != 48) {
               res = (byte)(res | 1);
            }

            ++charI;
         }

         bytes[b] = res;
      }

      return bytes;
   }

   public String toBitString(byte[] bytes) {
      StringBuilder sb = new StringBuilder(bytes.length * 8);
      byte lastBit = -128;
      byte[] arr$ = bytes;
      int len$ = bytes.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         byte b = arr$[i$];

         for(int i = 0; i < 8; ++i) {
            if((b & lastBit) == 0) {
               sb.append('0');
            } else {
               sb.append('1');
            }

            b = (byte)(b << 1);
         }
      }

      return sb.toString();
   }

   final long reversePart(long v, int maxBits) {
      long rest = v & ~((1L << maxBits) - 1L);
      return rest | this.reverse(v, maxBits);
   }

   public String toString() {
      return "big";
   }
}
