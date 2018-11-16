package com.graphhopper.storage;

import com.graphhopper.storage.Directory;
import com.graphhopper.storage.Graph;
import com.graphhopper.storage.Storable;

public interface GraphExtension extends Storable {
   boolean isRequireNodeField();

   boolean isRequireEdgeField();

   int getDefaultNodeFieldValue();

   int getDefaultEdgeFieldValue();

   void init(Graph var1, Directory var2);

   void setSegmentSize(int var1);

   GraphExtension copyTo(GraphExtension var1);

   public static class NoOpExtension implements GraphExtension {
      public boolean isRequireNodeField() {
         return false;
      }

      public boolean isRequireEdgeField() {
         return false;
      }

      public int getDefaultNodeFieldValue() {
         return 0;
      }

      public int getDefaultEdgeFieldValue() {
         return 0;
      }

      public void init(Graph grap, Directory dir) {
      }

      public GraphExtension create(long byteCount) {
         return this;
      }

      public boolean loadExisting() {
         return true;
      }

      public void setSegmentSize(int bytes) {
      }

      public void flush() {
      }

      public void close() {
      }

      public long getCapacity() {
         return 0L;
      }

      public GraphExtension copyTo(GraphExtension extStorage) {
         return extStorage;
      }

      public String toString() {
         return "NoExt";
      }

      public boolean isClosed() {
         return false;
      }
   }
}
