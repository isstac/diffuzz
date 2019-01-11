package com.graphhopper.reader.pbf;

public class PbfRawBlob {
   private String type;
   private byte[] data;

   public PbfRawBlob(String type, byte[] data) {
      this.type = type;
      this.data = data;
   }

   public String getType() {
      return this.type;
   }

   public byte[] getData() {
      return this.data;
   }
}
