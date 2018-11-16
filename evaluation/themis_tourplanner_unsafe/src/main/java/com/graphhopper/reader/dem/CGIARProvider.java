package com.graphhopper.reader.dem;

import com.graphhopper.reader.dem.ElevationProvider;
import com.graphhopper.reader.dem.HeightTile;
import com.graphhopper.storage.DAType;
import com.graphhopper.storage.DataAccess;
import com.graphhopper.storage.Directory;
import com.graphhopper.storage.GHDirectory;
import com.graphhopper.util.Downloader;
import com.graphhopper.util.Helper;
import java.awt.image.Raster;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.xmlgraphics.image.codec.tiff.TIFFDecodeParam;
import org.apache.xmlgraphics.image.codec.tiff.TIFFImageDecoder;
import org.apache.xmlgraphics.image.codec.util.SeekableStream;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

public class CGIARProvider implements ElevationProvider {
   private static final int WIDTH = 6000;
   private Downloader downloader = (new Downloader("GraphHopper CGIARReader")).setTimeout(10000);
//   private final Logger logger = LoggerFactory.getLogger(this.getClass());
   private final Map cacheData = new HashMap();
   private File cacheDir = new File("/tmp/cgiar");
   private String baseUrl = "http://srtm.csi.cgiar.org/SRT-ZIP/SRTM_V41/SRTM_Data_GeoTiff";
   private Directory dir;
   private DAType daType;
   final double precision;
   private final double invPrecision;
   private final int degree;
   private boolean calcMean;
   private boolean autoRemoveTemporary;

   public CGIARProvider() {
      this.daType = DAType.MMAP;
      this.precision = 1.0E7D;
      this.invPrecision = 1.0E-7D;
      this.degree = 5;
      this.calcMean = false;
      this.autoRemoveTemporary = true;
   }

   public void setCalcMean(boolean eleCalcMean) {
      this.calcMean = eleCalcMean;
   }

   public void setAutoRemoveTemporaryFiles(boolean autoRemoveTemporary) {
      this.autoRemoveTemporary = autoRemoveTemporary;
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

   protected File getCacheDir() {
      return this.cacheDir;
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

   public double getEle(double lat, double lon) {
      if(lat <= 60.0D && lat >= -60.0D) {
         lat = (double)((int)(lat * 1.0E7D)) / 1.0E7D;
         lon = (double)((int)(lon * 1.0E7D)) / 1.0E7D;
         String name = this.getFileName(lat, lon);
         HeightTile demProvider = (HeightTile)this.cacheData.get(name);
         if(demProvider == null) {
            if(!this.cacheDir.exists()) {
               this.cacheDir.mkdirs();
            }

            int minLat = this.down(lat);
            int minLon = this.down(lon);
            demProvider = new HeightTile(minLat, minLon, 6000, 5.0E7D, 5);
            demProvider.setCalcMean(this.calcMean);
            this.cacheData.put(name, demProvider);
            DataAccess heights = this.getDirectory().find(name + ".gh");
            demProvider.setHeights(heights);
            boolean loadExisting = false;

            try {
               loadExisting = heights.loadExisting();
            } catch (Exception var28) {
//               this.logger.warn("cannot load " + name + ", error:" + var28.getMessage());
            }

            if(!loadExisting) {
               String tifName = name + ".tif";
               String zippedURL = this.baseUrl + "/" + name + ".zip";
               File file = new File(this.cacheDir, (new File(zippedURL)).getName());
               if(!file.exists()) {
                  try {
                     int raster = 0;

                     while(raster < 3) {
                        try {
                           this.downloader.downloadFile(zippedURL, file.getAbsolutePath());
                           break;
                        } catch (SocketTimeoutException var32) {
                           Thread.sleep(2000L);
                           ++raster;
                        } catch (IOException var33) {
                           demProvider.setSeaLevel(true);
                           heights.setSegmentSize(100).create(10L).flush();
                           return 0.0D;
                        }
                     }
                  } catch (Exception var34) {
                     throw new RuntimeException(var34);
                  }
               }

               heights.create(72000000L);
               SeekableStream ss = null;

               Raster var35;
               try {
                  FileInputStream height = new FileInputStream(file);
                  ZipInputStream width = new ZipInputStream(height);
                  ZipEntry x = width.getNextEntry();

                  while(true) {
                     if(x == null || x.getName().equals(tifName)) {
                        ss = SeekableStream.wrapInputStream(width, true);
                        TIFFImageDecoder y = new TIFFImageDecoder(ss, new TIFFDecodeParam());
                        var35 = y.decodeAsRaster();
                        break;
                     }

                     x = width.getNextEntry();
                  }
               } catch (Exception var30) {
                  throw new RuntimeException("Can\'t decode " + tifName, var30);
               } finally {
                  if(ss != null) {
                     Helper.close(ss);
                  }

               }

               int var36 = var35.getHeight();
               int var37 = var35.getWidth();
               byte var38 = 0;
               byte var40 = 0;

               try {
                  for(int var41 = 0; var41 < var36; ++var41) {
                     for(int var39 = 0; var39 < var37; ++var39) {
                        short ex = (short)var35.getPixel(var39, var41, (int[])null)[0];
                        if(ex < -1000 || ex > 12000) {
                           ex = -32768;
                        }

                        heights.setShort((long)(2 * (var41 * 6000 + var39)), ex);
                     }
                  }

                  heights.flush();
               } catch (Exception var29) {
                  throw new RuntimeException("Problem at x:" + var38 + ", y:" + var40, var29);
               }
            }
         }

         return demProvider.isSeaLevel()?0.0D:demProvider.getHeight(lat, lon);
      } else {
         return 0.0D;
      }
   }

   int down(double val) {
      int intVal = (int)(val / 5.0D) * 5;
      if(val < 0.0D && (double)intVal - val >= 1.0E-7D) {
         intVal -= 5;
      }

      return intVal;
   }

   protected String getFileName(double lat, double lon) {
      lon = 1.0D + (180.0D + lon) / 5.0D;
      int lonInt = (int)lon;
      lat = 1.0D + (60.0D - lat) / 5.0D;
      int latInt = (int)lat;
      if(Math.abs((double)latInt - lat) < 2.0E-8D) {
         --latInt;
      }

      String str = "srtm_";
      str = str + (lonInt < 10?"0":"");
      str = str + lonInt;
      str = str + (latInt < 10?"_0":"_");
      str = str + latInt;
      return str;
   }

   public void release() {
      this.cacheData.clear();
      if(this.autoRemoveTemporary && this.dir != null) {
         this.dir.clear();
      }

   }

   public String toString() {
      return "CGIAR";
   }

   private Directory getDirectory() {
      if(this.dir != null) {
         return this.dir;
      } else {
//         this.logger.info(this.toString() + " Elevation Provider, from: " + this.baseUrl + ", to: " + this.cacheDir + ", as: " + this.daType);
         return this.dir = new GHDirectory(this.cacheDir.getAbsolutePath(), this.daType);
      }
   }

   public static void main(String[] args) {
      CGIARProvider provider = new CGIARProvider();
      System.out.println(provider.getEle(46.0D, -20.0D));
      System.out.println(provider.getEle(49.949784D, 11.57517D));
      System.out.println(provider.getEle(49.968668D, 11.575127D));
      System.out.println(provider.getEle(49.968682D, 11.574842D));
      System.out.println(provider.getEle(-22.532854D, -65.110474D));
      System.out.println(provider.getEle(38.065392D, -87.099609D));
      System.out.println(provider.getEle(40.0D, -105.2277023D));
      System.out.println(provider.getEle(39.99999999D, -105.2277023D));
      System.out.println(provider.getEle(39.9999999D, -105.2277023D));
      System.out.println(provider.getEle(39.999999D, -105.2277023D));
      System.out.println(provider.getEle(29.840644D, -42.890625D));
   }
}
