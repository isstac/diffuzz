package com.graphhopper.reader.pbf;

import com.graphhopper.reader.pbf.PbfRawBlob;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openstreetmap.osmosis.osmbinary.Fileformat.BlobHeader;

public class PbfStreamSplitter implements Iterator {
   private static Logger log = Logger.getLogger(PbfStreamSplitter.class.getName());
   private DataInputStream dis;
   private int dataBlockCount;
   private boolean eof;
   private PbfRawBlob nextBlob;

   public PbfStreamSplitter(DataInputStream pbfStream) {
      this.dis = pbfStream;
      this.dataBlockCount = 0;
      this.eof = false;
   }

   private BlobHeader readHeader(int headerLength) throws IOException {
      byte[] headerBuffer = new byte[headerLength];
      this.dis.readFully(headerBuffer);
      BlobHeader blobHeader = BlobHeader.parseFrom(headerBuffer);
      return blobHeader;
   }

   private byte[] readRawBlob(BlobHeader blobHeader) throws IOException {
      byte[] rawBlob = new byte[blobHeader.getDatasize()];
      this.dis.readFully(rawBlob);
      return rawBlob;
   }

   private void getNextBlob() {
      try {
         int e;
         try {
            e = this.dis.readInt();
         } catch (EOFException var4) {
            this.eof = true;
            return;
         }

         if(log.isLoggable(Level.FINER)) {
            log.finer("Reading header for blob " + this.dataBlockCount++);
         }

         BlobHeader blobHeader = this.readHeader(e);
         if(log.isLoggable(Level.FINER)) {
            log.finer("Processing blob of type " + blobHeader.getType() + ".");
         }

         byte[] blobData = this.readRawBlob(blobHeader);
         this.nextBlob = new PbfRawBlob(blobHeader.getType(), blobData);
      } catch (IOException var5) {
         throw new RuntimeException("Unable to get next blob from PBF stream.", var5);
      }
   }

   public boolean hasNext() {
      if(this.nextBlob == null && !this.eof) {
         this.getNextBlob();
      }

      return this.nextBlob != null;
   }

   public PbfRawBlob next() {
      PbfRawBlob result = this.nextBlob;
      this.nextBlob = null;
      return result;
   }

   public void remove() {
      throw new UnsupportedOperationException();
   }

   public void release() {
      if(this.dis != null) {
         try {
            this.dis.close();
         } catch (IOException var2) {
            log.log(Level.SEVERE, "Unable to close PBF stream.", var2);
         }
      }

      this.dis = null;
   }
}
