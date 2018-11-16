package com.graphhopper.util;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

public class Helper7 {
   public static final boolean UNMAP_SUPPORTED;

   public static String getBeanMemInfo() {
      OperatingSystemMXBean mxbean = ManagementFactory.getOperatingSystemMXBean();
      com.sun.management.OperatingSystemMXBean sunmxbean = (com.sun.management.OperatingSystemMXBean)mxbean;
      long freeMemory = sunmxbean.getFreePhysicalMemorySize();
      long availableMemory = sunmxbean.getTotalPhysicalMemorySize();
      return "free:" + freeMemory / 1048576L + ", available:" + availableMemory / 1048576L + ", rfree:" + Runtime.getRuntime().freeMemory() / 1048576L;
   }

   public static void close(XMLStreamReader r) {
      try {
         if(r != null) {
            r.close();
         }

      } catch (XMLStreamException var2) {
         throw new RuntimeException("Couldn\'t close xml reader", var2);
      }
   }

   static {
      boolean v;
      try {
         Class.forName("sun.misc.Cleaner");
         Class.forName("java.nio.DirectByteBuffer").getMethod("cleaner", new Class[0]);
         v = true;
      } catch (Exception var2) {
         v = false;
      }

      UNMAP_SUPPORTED = v;
   }
}
