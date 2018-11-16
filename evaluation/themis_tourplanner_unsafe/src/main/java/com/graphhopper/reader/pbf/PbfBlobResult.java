package com.graphhopper.reader.pbf;

import java.util.List;

public class PbfBlobResult {
   private List entities;
   private boolean complete = false;
   private boolean success = false;
   private Exception ex = new RuntimeException("no success result stored");

   public void storeSuccessResult(List decodedEntities) {
      this.entities = decodedEntities;
      this.complete = true;
      this.success = true;
   }

   public void storeFailureResult(Exception ex) {
      this.complete = true;
      this.success = false;
      this.ex = ex;
   }

   public boolean isComplete() {
      return this.complete;
   }

   public boolean isSuccess() {
      return this.success;
   }

   public Exception getException() {
      return this.ex;
   }

   public List getEntities() {
      return this.entities;
   }
}
