package com.graphhopper.util;

import com.graphhopper.GraphHopper;
import com.graphhopper.util.Helper;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

public class Constants {
   public static final String JAVA_VERSION = System.getProperty("java.version");
   public static final String OS_NAME = System.getProperty("os.name");
   public static final boolean LINUX;
   public static final boolean WINDOWS;
   public static final boolean SUN_OS;
   public static final boolean MAC_OS_X;
   public static final String OS_ARCH;
   public static final String OS_VERSION;
   public static final String JAVA_VENDOR;
   public static final int VERSION_NODE = 4;
   public static final int VERSION_EDGE = 12;
   public static final int VERSION_SHORTCUT = 1;
   public static final int VERSION_GEOMETRY = 3;
   public static final int VERSION_LOCATION_IDX = 2;
   public static final int VERSION_NAME_IDX = 2;
   public static final String VERSION;
   public static final String BUILD_DATE;
   public static final boolean SNAPSHOT;

   public static String getVersions() {
      return "4,12,3,2,2,1";
   }

   static {
      LINUX = OS_NAME.startsWith("Linux");
      WINDOWS = OS_NAME.startsWith("Windows");
      SUN_OS = OS_NAME.startsWith("SunOS");
      MAC_OS_X = OS_NAME.startsWith("Mac OS X");
      OS_ARCH = System.getProperty("os.arch");
      OS_VERSION = System.getProperty("os.version");
      JAVA_VENDOR = System.getProperty("java.vendor");
      String version = "0.0";

      try {
         List indexM = Helper.readFile((Reader)(new InputStreamReader(GraphHopper.class.getResourceAsStream("version"), Helper.UTF_CS)));
         version = (String)indexM.get(0);
      } catch (Exception var5) {
         System.err.println("GraphHopper Initialization ERROR: cannot read version!? " + var5.getMessage());
      }

      int indexM1 = version.indexOf("-");
      String buildDate;
      if("${project.version}".equals(version)) {
         VERSION = "0.0";
         SNAPSHOT = true;
         System.err.println("GraphHopper Initialization WARNING: maven did not preprocess the version file! Do not use the jar for a release!");
      } else if("0.0".equals(version)) {
         VERSION = "0.0";
         SNAPSHOT = true;
         System.err.println("GraphHopper Initialization WARNING: cannot get version!?");
      } else {
         buildDate = version;
         if(indexM1 >= 0) {
            buildDate = version.substring(0, indexM1);
         }

         SNAPSHOT = version.toLowerCase().contains("-snapshot");
         VERSION = buildDate;
      }

      buildDate = "";

      try {
         List v = Helper.readFile((Reader)(new InputStreamReader(GraphHopper.class.getResourceAsStream("builddate"), Helper.UTF_CS)));
         buildDate = (String)v.get(0);
      } catch (Exception var4) {
         ;
      }

      BUILD_DATE = buildDate;
   }
}
