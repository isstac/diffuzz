package com.graphhopper.coll;

import com.graphhopper.geohash.SpatialKeyAlgo;
import com.graphhopper.storage.VLongStorage;
import com.graphhopper.util.shapes.GHPoint;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class CompressedArray {
   private int compressionLevel;
   private VLongStorage currentWriter;
   private int currentEntry;
   private List segments;
   private int entriesPerSegment;
   private int approxBytesPerEntry;
   private SpatialKeyAlgo algo;

   public CompressedArray() {
      this(100, 200, 4);
   }

   public CompressedArray(int _segments, int entriesPerSeg, int approxBytesPerEntry) {
      this.compressionLevel = 5;
      this.currentEntry = 0;
      if(entriesPerSeg < 1) {
         throw new IllegalArgumentException("at least one entry should be per segment");
      } else {
         this.entriesPerSegment = entriesPerSeg;
         this.approxBytesPerEntry = approxBytesPerEntry;
         this.segments = new ArrayList(_segments);
         this.algo = new SpatialKeyAlgo(63);
      }
   }

   public CompressedArray setCompressionLevel(int compressionLevel) {
      this.compressionLevel = compressionLevel;
      return this;
   }

   public void write(double lat, double lon) {
      try {
         if(this.currentWriter == null) {
            this.currentWriter = new VLongStorage(this.entriesPerSegment * this.approxBytesPerEntry);
         }

         long ex = this.algo.encode(new GHPoint(lat, lon));
         this.currentWriter.writeVLong(ex);
         ++this.currentEntry;
         if(this.currentEntry >= this.entriesPerSegment) {
            this.flush();
         }

      } catch (Exception var7) {
         throw new RuntimeException(var7);
      }
   }

   public GHPoint get(long index) {
      int segmentNo = (int)(index / (long)this.entriesPerSegment);
      int entry = (int)(index % (long)this.entriesPerSegment);

      try {
         if(segmentNo >= this.segments.size()) {
            return null;
         } else {
            byte[] ex = (byte[])this.segments.get(segmentNo);
            VLongStorage store = new VLongStorage(decompress(ex));
            long len = store.getLength();

            for(int i = 0; store.getPosition() < len; ++i) {
               long latlon = store.readVLong();
               if(i == entry) {
                  GHPoint point = new GHPoint();
                  this.algo.decode(latlon, point);
                  return point;
               }
            }

            return null;
         }
      } catch (ArrayIndexOutOfBoundsException var13) {
         throw new RuntimeException("index " + index + "=> segNo:" + segmentNo + ", entry=" + entry + ", segments:" + this.segments.size(), var13);
      } catch (Exception var14) {
         throw new RuntimeException(var14);
      }
   }

   public void flush() {
      if(this.currentWriter != null) {
         try {
            this.currentWriter.trimToSize();
            byte[] ex = this.currentWriter.getBytes();
            this.segments.add(compress(ex, 0, ex.length, this.compressionLevel));
            this.currentWriter = null;
            this.currentEntry = 0;
         } catch (Exception var2) {
            throw new RuntimeException(var2);
         }
      }
   }

   public float calcMemInMB() {
      long bytes = 0L;

      for(int i = 0; i < this.segments.size(); ++i) {
         bytes += (long)((byte[])this.segments.get(i)).length;
      }

      return (float)((long)(this.segments.size() * 4) + bytes) / 1048576.0F;
   }

   public static byte[] compress(byte[] value, int offset, int length, int compressionLevel) {
      ByteArrayOutputStream bos = new ByteArrayOutputStream(length);
      Deflater compressor = new Deflater();

      try {
         compressor.setLevel(compressionLevel);
         compressor.setInput(value, offset, length);
         compressor.finish();
         byte[] buf = new byte[1024];

         while(!compressor.finished()) {
            int count = compressor.deflate(buf);
            bos.write(buf, 0, count);
         }
      } finally {
         compressor.end();
      }

      return bos.toByteArray();
   }

   public static byte[] decompress(byte[] value) throws DataFormatException {
      ByteArrayOutputStream bos = new ByteArrayOutputStream(value.length);
      Inflater decompressor = new Inflater();

      try {
         decompressor.setInput(value);
         byte[] buf = new byte[1024];

         while(!decompressor.finished()) {
            int count = decompressor.inflate(buf);
            bos.write(buf, 0, count);
         }
      } finally {
         decompressor.end();
      }

      return bos.toByteArray();
   }
}
