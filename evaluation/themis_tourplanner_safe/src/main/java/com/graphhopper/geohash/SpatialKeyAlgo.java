package com.graphhopper.geohash;

import com.graphhopper.geohash.KeyAlgo;
import com.graphhopper.util.shapes.BBox;
import com.graphhopper.util.shapes.GHPoint;

public class SpatialKeyAlgo implements KeyAlgo {
   private BBox bbox;
   private int allBits;
   private long initialBits;

   public SpatialKeyAlgo(int allBits) {
      this.myinit(allBits);
   }

   private void myinit(int allBits) {
      if(allBits > 64) {
         throw new IllegalStateException("allBits is too big and does not fit into 8 bytes");
      } else if(allBits <= 0) {
         throw new IllegalStateException("allBits must be positive");
      } else {
         this.allBits = allBits;
         this.initialBits = 1L << allBits - 1;
         this.setWorldBounds();
      }
   }

   public int getBits() {
      return this.allBits;
   }

   public int getExactPrecision() {
      int p = (int)(Math.pow(2.0D, (double)this.allBits) / 360.0D);
      ++p;
      return (int)Math.log10((double)p);
   }

   public SpatialKeyAlgo bounds(BBox box) {
      this.bbox = box.clone();
      return this;
   }

   public SpatialKeyAlgo setBounds(double minLonInit, double maxLonInit, double minLatInit, double maxLatInit) {
      this.bounds(new BBox(minLonInit, maxLonInit, minLatInit, maxLatInit));
      return this;
   }

   protected void setWorldBounds() {
      this.setBounds(-180.0D, 180.0D, -90.0D, 90.0D);
   }

   public long encode(GHPoint coord) {
      return this.encode(coord.lat, coord.lon);
   }

   public final long encode(double lat, double lon) {
      long hash = 0L;
      double minLatTmp = this.bbox.minLat;
      double maxLatTmp = this.bbox.maxLat;
      double minLonTmp = this.bbox.minLon;
      double maxLonTmp = this.bbox.maxLon;
      int i = 0;

      while(true) {
         double midLon;
         if(minLatTmp < maxLatTmp) {
            midLon = (minLatTmp + maxLatTmp) / 2.0D;
            if(lat < midLon) {
               maxLatTmp = midLon;
            } else {
               hash |= 1L;
               minLatTmp = midLon;
            }
         }

         ++i;
         if(i >= this.allBits) {
            break;
         }

         hash <<= 1;
         if(minLonTmp < maxLonTmp) {
            midLon = (minLonTmp + maxLonTmp) / 2.0D;
            if(lon < midLon) {
               maxLonTmp = midLon;
            } else {
               hash |= 1L;
               minLonTmp = midLon;
            }
         }

         ++i;
         if(i >= this.allBits) {
            break;
         }

         hash <<= 1;
      }

      return hash;
   }

   public final void decode(long spatialKey, GHPoint latLon) {
      double midLat = (this.bbox.maxLat - this.bbox.minLat) / 2.0D;
      double midLon = (this.bbox.maxLon - this.bbox.minLon) / 2.0D;
      double lat = this.bbox.minLat;
      double lon = this.bbox.minLon;
      long bits = this.initialBits;

      while(true) {
         if((spatialKey & bits) != 0L) {
            lat += midLat;
         }

         midLat /= 2.0D;
         bits >>>= 1;
         if((spatialKey & bits) != 0L) {
            lon += midLon;
         }

         midLon /= 2.0D;
         if(bits <= 1L) {
            lat += midLat;
            lon += midLon;
            latLon.lat = lat;
            latLon.lon = lon;
            return;
         }

         bits >>>= 1;
      }
   }

   public String toString() {
      return "bits:" + this.allBits + ", bounds:" + this.bbox;
   }
}
