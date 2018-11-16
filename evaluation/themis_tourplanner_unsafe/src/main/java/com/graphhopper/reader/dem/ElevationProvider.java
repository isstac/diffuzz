package com.graphhopper.reader.dem;

import com.graphhopper.storage.DAType;
import java.io.File;

public interface ElevationProvider {
   ElevationProvider NOOP = new ElevationProvider() {
      public double getEle(double lat, double lon) {
         return Double.NaN;
      }

      public ElevationProvider setCacheDir(File cacheDir) {
         return this;
      }

      public ElevationProvider setBaseURL(String baseURL) {
         return this;
      }

      public ElevationProvider setDAType(DAType daType) {
         return this;
      }

      public void release() {
      }

      public void setCalcMean(boolean eleCalcMean) {
      }
   };

   double getEle(double var1, double var3);

   ElevationProvider setBaseURL(String var1);

   ElevationProvider setCacheDir(File var1);

   ElevationProvider setDAType(DAType var1);

   void setCalcMean(boolean var1);

   void release();
}
