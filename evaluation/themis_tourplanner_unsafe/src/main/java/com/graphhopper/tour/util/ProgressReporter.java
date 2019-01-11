package com.graphhopper.tour.util;

import java.io.IOException;

public interface ProgressReporter {
   ProgressReporter SILENT = new ProgressReporter() {
      public void reportProgress(int total, int complete) {
      }
   };
   ProgressReporter STDERR = new ProgressReporter() {
      public void reportProgress(int complete, int total) {
         System.err.format("%d/%d complete\n", new Object[]{Integer.valueOf(complete), Integer.valueOf(total)});
      }
   };

   void reportProgress(int var1, int var2) throws IOException;
}
