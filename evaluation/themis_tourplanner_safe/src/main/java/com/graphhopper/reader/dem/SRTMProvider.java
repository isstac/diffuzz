package com.graphhopper.reader.dem;

import com.graphhopper.reader.dem.ElevationProvider;
import com.graphhopper.reader.dem.HeightTile;
import com.graphhopper.storage.DAType;
import com.graphhopper.storage.DataAccess;
import com.graphhopper.storage.Directory;
import com.graphhopper.storage.GHDirectory;
import com.graphhopper.util.BitUtil;
import com.graphhopper.util.Downloader;
import com.graphhopper.util.Helper;
import gnu.trove.map.hash.TIntObjectHashMap;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.SocketTimeoutException;
import java.util.Iterator;
import java.util.zip.ZipInputStream;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

public class SRTMProvider implements ElevationProvider {
   private static final BitUtil BIT_UTIL;
//   private final Logger logger = LoggerFactory.getLogger(this.getClass());
   private final int WIDTH = 1201;
   private Directory dir;
   private DAType daType;
   private Downloader downloader;
   private File cacheDir;
   private final TIntObjectHashMap cacheData;
   private final TIntObjectHashMap areas;
   private final double precision;
   private final double invPrecision;
   private String baseUrl;
   private boolean calcMean;

   public static void main(String[] args) throws IOException {
      SRTMProvider provider = new SRTMProvider();
      System.out.println(provider.getEle(47.468668D, 14.575127D));
      System.out.println(provider.getEle(47.467753D, 14.573911D));
      System.out.println(provider.getEle(46.468835D, 12.578777D));
      System.out.println(provider.getEle(48.469123D, 9.576393D));
      provider.setCalcMean(true);
      System.out.println(provider.getEle(47.467753D, 14.573911D));
   }

   public SRTMProvider() {
      this.daType = DAType.MMAP;
      this.downloader = (new Downloader("GraphHopper SRTMReader")).setTimeout(10000);
      this.cacheDir = new File("/tmp/srtm");
      this.cacheData = new TIntObjectHashMap();
      this.areas = new TIntObjectHashMap();
      this.precision = 1.0E7D;
      this.invPrecision = 1.0E-7D;
      this.baseUrl = "http://dds.cr.usgs.gov/srtm/version2_1/SRTM3/";
      this.calcMean = false;
      this.init();
   }

   public void setCalcMean(boolean calcMean) {
      this.calcMean = calcMean;
   }

   private SRTMProvider init() {
      try {
         String[] ex = new String[]{"Africa", "Australia", "Eurasia", "Islands", "North_America", "South_America"};
         String[] arr$ = ex;
         int len$ = ex.length;

         for(int i$ = 0; i$ < len$; ++i$) {
            String str = arr$[i$];
            InputStream is = this.getClass().getResourceAsStream(str + "_names.txt");
            Iterator i$1 = Helper.readFile((Reader)(new InputStreamReader(is, Helper.UTF_CS))).iterator();

            while(i$1.hasNext()) {
               String line = (String)i$1.next();
               int lat = Integer.parseInt(line.substring(1, 3));
               if(line.substring(0, 1).charAt(0) == 83) {
                  lat = -lat;
               }

               int lon = Integer.parseInt(line.substring(4, 7));
               if(line.substring(3, 4).charAt(0) == 87) {
                  lon = -lon;
               }

               int intKey = this.calcIntKey((double)lat, (double)lon);
               String key = (String)this.areas.put(intKey, str);
               if(key != null) {
                  throw new IllegalStateException("do not overwrite existing! key " + intKey + " " + key + " vs. " + str);
               }
            }
         }

         return this;
      } catch (Exception var13) {
         throw new IllegalStateException("Cannot load area names from classpath", var13);
      }
   }

   private int calcIntKey(double lat, double lon) {
      return (this.down(lat) + 90) * 1000 + this.down(lon) + 180;
   }

   public void setDownloader(Downloader downloader) {
      this.downloader = downloader;
   }

   public ElevationProvider setCacheDir(File cacheDir) {
      if(cacheDir.exists() && !cacheDir.isDirectory()) {
         throw new IllegalArgumentException("Cache path has to be a directory");
      } else {
         try {
            this.cacheDir = cacheDir.getCanonicalFile();
            return this;
         } catch (IOException var3) {
            throw new RuntimeException(var3);
         }
      }
   }

   public ElevationProvider setBaseURL(String baseUrl) {
      if(baseUrl != null && !baseUrl.isEmpty()) {
         this.baseUrl = baseUrl;
         return this;
      } else {
         throw new IllegalArgumentException("baseUrl cannot be empty");
      }
   }

   public ElevationProvider setDAType(DAType daType) {
      this.daType = daType;
      return this;
   }

   int down(double val) {
      int intVal = (int)val;
      return val < 0.0D && (double)intVal - val >= 1.0E-7D?intVal - 1:intVal;
   }

   String getFileString(double lat, double lon) {
      int intKey = this.calcIntKey(lat, lon);
      String str = (String)this.areas.get(intKey);
      if(str == null) {
         return null;
      } else {
         int minLat = Math.abs(this.down(lat));
         int minLon = Math.abs(this.down(lon));
         str = str + "/";
         if(lat >= 0.0D) {
            str = str + "N";
         } else {
            str = str + "S";
         }

         if(minLat < 10) {
            str = str + "0";
         }

         str = str + minLat;
         if(lon >= 0.0D) {
            str = str + "E";
         } else {
            str = str + "W";
         }

         if(minLon < 10) {
            str = str + "0";
         }

         if(minLon < 100) {
            str = str + "0";
         }

         str = str + minLon;
         return str;
      }
   }

   public double getEle(double lat, double lon) {
      lat = (double)((int)(lat * 1.0E7D)) / 1.0E7D;
      lon = (double)((int)(lon * 1.0E7D)) / 1.0E7D;
      int intKey = this.calcIntKey(lat, lon);
      HeightTile demProvider = (HeightTile)this.cacheData.get(intKey);
      if(demProvider == null) {
         if(!this.cacheDir.exists()) {
            this.cacheDir.mkdirs();
         }

         String fileDetails = this.getFileString(lat, lon);
         if(fileDetails == null) {
            return 0.0D;
         }

         int minLat = this.down(lat);
         int minLon = this.down(lon);
         demProvider = new HeightTile(minLat, minLon, 1201, 1.0E7D, 1);
         demProvider.setCalcMean(this.calcMean);
         this.cacheData.put(intKey, demProvider);
         DataAccess heights = this.getDirectory().find("dem" + intKey);
         demProvider.setHeights(heights);
         boolean loadExisting = false;

         try {
            loadExisting = heights.loadExisting();
         } catch (Exception var21) {
//            this.logger.warn("cannot load dem" + intKey + ", error:" + var21.getMessage());
         }

         if(!loadExisting) {
            byte[] bytes = new byte[2884802];
            heights.create((long)bytes.length);

            try {
               String ex = this.baseUrl + "/" + fileDetails + ".hgt.zip";
               File file = new File(this.cacheDir, (new File(ex)).getName());
               if(!file.exists()) {
                  for(int zis = 0; zis < 3; ++zis) {
                     try {
                        this.downloader.downloadFile(ex, file.getAbsolutePath());
                        break;
                     } catch (SocketTimeoutException var22) {
                        Thread.sleep(2000L);
                     } catch (FileNotFoundException var23) {
                        ex = this.baseUrl + "/" + fileDetails + "hgt.zip";
                     }
                  }
               }

               FileInputStream is = new FileInputStream(file);
               ZipInputStream var25 = new ZipInputStream(is);
               var25.getNextEntry();
               BufferedInputStream buff = new BufferedInputStream(var25);

               int len;
               while((len = buff.read(bytes)) > 0) {
                  for(int bytePos = 0; bytePos < len; bytePos += 2) {
                     short val = BIT_UTIL.toShort(bytes, bytePos);
                     if(val < -1000 || val > 12000) {
                        val = -32768;
                     }

                     heights.setShort((long)bytePos, val);
                  }
               }

               heights.flush();
            } catch (Exception var24) {
               throw new RuntimeException(var24);
            }
         }
      }

      return demProvider.getHeight(lat, lon);
   }

   public void release() {
      this.cacheData.clear();
      if(this.dir != null) {
         this.dir.clear();
      }

   }

   public String toString() {
      return "SRTM";
   }

   private Directory getDirectory() {
      if(this.dir != null) {
         return this.dir;
      } else {
//         this.logger.info(this.toString() + " Elevation Provider, from: " + this.baseUrl + ", to: " + this.cacheDir + ", as: " + this.daType);
         return this.dir = new GHDirectory(this.cacheDir.getAbsolutePath(), this.daType);
      }
   }

   static {
      BIT_UTIL = BitUtil.BIG;
   }
}
