package com.graphhopper.util;

import com.graphhopper.util.DistanceCalc;
import com.graphhopper.util.DistanceCalc3D;
import com.graphhopper.util.DistanceCalcEarth;
import com.graphhopper.util.DistancePlaneProjection;
import com.graphhopper.util.PointList;
import com.graphhopper.util.shapes.BBox;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.charset.Charset;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

public class Helper {
   public static final DistanceCalc DIST_EARTH = new DistanceCalcEarth();
   public static final DistanceCalc3D DIST_3D = new DistanceCalc3D();
   public static final DistancePlaneProjection DIST_PLANE = new DistancePlaneProjection();
//   private static final Logger logger = LoggerFactory.getLogger(Helper.class);
   public static Charset UTF_CS = Charset.forName("UTF-8");
   public static final long MB = 1048576L;
   private static final float DEGREE_FACTOR = 5368709.0F;
   private static final float ELE_FACTOR = 1000.0F;

   public static ArrayList tIntListToArrayList(TIntList from) {
      int len = from.size();
      ArrayList list = new ArrayList(len);

      for(int i = 0; i < len; ++i) {
         list.add(Integer.valueOf(from.get(i)));
      }

      return list;
   }

   public static Locale getLocale(String param) {
      int pointIndex = param.indexOf(46);
      if(pointIndex > 0) {
         param = param.substring(0, pointIndex);
      }

      param = param.replace("-", "_");
      int index = param.indexOf("_");
      return index < 0?new Locale(param):new Locale(param.substring(0, index), param.substring(index + 1));
   }

   static String packageToPath(Package pkg) {
      return pkg.getName().replaceAll("\\.", File.separator);
   }

   public static int countBitValue(int maxTurnCosts) {
      double val = Math.log((double)maxTurnCosts) / Math.log(2.0D);
      int intVal = (int)val;
      return val == (double)intVal?intVal:intVal + 1;
   }

   public static void loadProperties(Map map, Reader tmpReader) throws IOException {
      BufferedReader reader = new BufferedReader(tmpReader);

      String line;
      try {
         while((line = reader.readLine()) != null) {
            if(!line.startsWith("//") && !line.startsWith("#") && !isEmpty(line)) {
               int index = line.indexOf("=");
               if(index < 0) {
//                  logger.warn("Skipping configuration at line:" + line);
               } else {
                  String field = line.substring(0, index);
                  String value = line.substring(index + 1);
                  map.put(field, value);
               }
            }
         }
      } finally {
         reader.close();
      }

   }

   public static void saveProperties(Map map, Writer tmpWriter) throws IOException {
      BufferedWriter writer = new BufferedWriter(tmpWriter);

      try {
         Iterator i$ = map.entrySet().iterator();

         while(i$.hasNext()) {
            Entry e = (Entry)i$.next();
            writer.append((CharSequence)e.getKey());
            writer.append('=');
            writer.append((CharSequence)e.getValue());
            writer.append('\n');
         }
      } finally {
         writer.close();
      }

   }

   public static List readFile(String file) throws IOException {
      return readFile((Reader)(new InputStreamReader(new FileInputStream(file), UTF_CS)));
   }

   public static List readFile(Reader simpleReader) throws IOException {
      BufferedReader reader = new BufferedReader(simpleReader);

      try {
         ArrayList res = new ArrayList();

         String line;
         while((line = reader.readLine()) != null) {
            res.add(line);
         }

         ArrayList var4 = res;
         return var4;
      } finally {
         reader.close();
      }
   }

   public static String isToString(InputStream inputStream) throws IOException {
      short size = 8192;
      String encoding = "UTF-8";
      BufferedInputStream in = new BufferedInputStream(inputStream, size);

      try {
         byte[] buffer = new byte[size];
         ByteArrayOutputStream output = new ByteArrayOutputStream();

         int numRead;
         while((numRead = in.read(buffer)) != -1) {
            output.write(buffer, 0, numRead);
         }

         String var7 = output.toString(encoding);
         return var7;
      } finally {
         in.close();
      }
   }

   public static int idealIntArraySize(int need) {
      return idealByteArraySize(need * 4) / 4;
   }

   public static int idealByteArraySize(int need) {
      for(int i = 4; i < 32; ++i) {
         if(need <= (1 << i) - 12) {
            return (1 << i) - 12;
         }
      }

      return need;
   }

   public static boolean removeDir(File file) {
      if(!file.exists()) {
         return true;
      } else {
         if(file.isDirectory()) {
            File[] arr$ = file.listFiles();
            int len$ = arr$.length;

            for(int i$ = 0; i$ < len$; ++i$) {
               File f = arr$[i$];
               removeDir(f);
            }
         }

         return file.delete();
      }
   }

   public static long getTotalMB() {
      return Runtime.getRuntime().totalMemory() / 1048576L;
   }

   public static long getUsedMB() {
      return (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1048576L;
   }

   public static String getMemInfo() {
      return "totalMB:" + getTotalMB() + ", usedMB:" + getUsedMB();
   }

   public static int getSizeOfObjectRef(int factor) {
      return factor * 12;
   }

   public static int getSizeOfLongArray(int length, int factor) {
      return factor * 16 + 8 * length;
   }

   public static int getSizeOfObjectArray(int length, int factor) {
      return factor * 16 + 4 * length;
   }

   public static void close(Closeable cl) {
      try {
         if(cl != null) {
            cl.close();
         }

      } catch (IOException var2) {
         throw new RuntimeException("Couldn\'t close resource", var2);
      }
   }

   public static boolean isEmpty(String str) {
      return str == null || str.trim().length() == 0;
   }

   public static boolean isFileMapped(ByteBuffer bb) {
      if(bb instanceof MappedByteBuffer) {
         try {
            ((MappedByteBuffer)bb).isLoaded();
            return true;
         } catch (UnsupportedOperationException var2) {
            ;
         }
      }

      return false;
   }

   public static int calcIndexSize(BBox graphBounds) {
      if(!graphBounds.isValid()) {
         throw new IllegalArgumentException("Bounding box is not valid to calculate index size: " + graphBounds);
      } else {
         double dist = DIST_EARTH.calcDist(graphBounds.maxLat, graphBounds.minLon, graphBounds.minLat, graphBounds.maxLon);
         dist = Math.min(dist / 1000.0D, 50000.0D);
         return Math.max(2000, (int)(dist * dist));
      }
   }

   public static String pruneFileEnd(String file) {
      int index = file.lastIndexOf(".");
      return index < 0?file:file.substring(0, index);
   }

   public static TIntList createTList(int... list) {
      TIntArrayList res = new TIntArrayList(list.length);
      int[] arr$ = list;
      int len$ = list.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         int val = arr$[i$];
         res.add(val);
      }

      return res;
   }

   public static PointList createPointList(double... list) {
      if(list.length % 2 != 0) {
         throw new IllegalArgumentException("list should consist of lat,lon pairs!");
      } else {
         int max = list.length / 2;
         PointList res = new PointList(max, false);

         for(int i = 0; i < max; ++i) {
            res.add(list[2 * i], list[2 * i + 1], Double.NaN);
         }

         return res;
      }
   }

   public static PointList createPointList3D(double... list) {
      if(list.length % 3 != 0) {
         throw new IllegalArgumentException("list should consist of lat,lon,ele tuples!");
      } else {
         int max = list.length / 3;
         PointList res = new PointList(max, true);

         for(int i = 0; i < max; ++i) {
            res.add(list[3 * i], list[3 * i + 1], list[3 * i + 2]);
         }

         return res;
      }
   }

   public static final int degreeToInt(double deg) {
      return deg >= Double.MAX_VALUE?Integer.MAX_VALUE:(deg <= -1.7976931348623157E308D?-2147483647:(int)(deg * 5368709.0D));
   }

   public static final double intToDegree(int storedInt) {
      return storedInt == Integer.MAX_VALUE?Double.MAX_VALUE:(storedInt == -2147483647?-1.7976931348623157E308D:(double)storedInt / 5368709.0D);
   }

   public static final int eleToInt(double ele) {
      return ele >= 2.147483647E9D?Integer.MAX_VALUE:(int)(ele * 1000.0D);
   }

   public static final double intToEle(int integEle) {
      return integEle == Integer.MAX_VALUE?Double.MAX_VALUE:(double)((float)integEle / 1000.0F);
   }

   public static void cleanMappedByteBuffer(final ByteBuffer buffer) {
      try {
         AccessController.doPrivileged(new PrivilegedExceptionAction() {
            public Object run() throws Exception {
               try {
                  Method getCleanerMethod = buffer.getClass().getMethod("cleaner", new Class[0]);
                  getCleanerMethod.setAccessible(true);
                  Object cleaner = getCleanerMethod.invoke(buffer, new Object[0]);
                  if(cleaner != null) {
                     cleaner.getClass().getMethod("clean", new Class[0]).invoke(cleaner, new Object[0]);
                  }
               } catch (NoSuchMethodException var3) {
                  ;
               }

               return null;
            }
         });
      } catch (PrivilegedActionException var2) {
         throw new RuntimeException("unable to unmap the mapped buffer", var2);
      }
   }

   public static void cleanHack() {
      System.gc();
   }

   public static String nf(long no) {
      return NumberFormat.getInstance(Locale.FRANCE).format(no);
   }

   public static String firstBig(String sayText) {
      return sayText != null && sayText.length() > 0?Character.toUpperCase(sayText.charAt(0)) + sayText.substring(1):sayText;
   }

   public static final double keepIn(double value, double min, double max) {
      return Math.max(min, Math.min(value, max));
   }

   public static double round(double value, int exponent) {
      double factor = Math.pow(10.0D, (double)exponent);
      return (double)Math.round(value * factor) / factor;
   }

   public static final double round6(double value) {
      return (double)Math.round(value * 1000000.0D) / 1000000.0D;
   }

   public static final double round4(double value) {
      return (double)Math.round(value * 10000.0D) / 10000.0D;
   }

   public static final double round2(double value) {
      return (double)Math.round(value * 100.0D) / 100.0D;
   }
}
