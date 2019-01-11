package com.graphhopper.util.shapes;

import com.graphhopper.util.NumHelper;

public class GHPoint {
   public double lat = Double.NaN;
   public double lon = Double.NaN;

   public GHPoint() {
   }

   public GHPoint(double lat, double lon) {
      this.lat = lat;
      this.lon = lon;
   }

   public double getLon() {
      return this.lon;
   }

   public double getLat() {
      return this.lat;
   }

   public boolean isValid() {
      return !Double.isNaN(this.lat) && !Double.isNaN(this.lon);
   }

   public int hashCode() {
      byte hash = 7;
      int hash1 = 83 * hash + (int)(Double.doubleToLongBits(this.lat) ^ Double.doubleToLongBits(this.lat) >>> 32);
      hash1 = 83 * hash1 + (int)(Double.doubleToLongBits(this.lon) ^ Double.doubleToLongBits(this.lon) >>> 32);
      return hash1;
   }

   public boolean equals(Object obj) {
      if(obj == null) {
         return false;
      } else {
         GHPoint other = (GHPoint)obj;
         return NumHelper.equalsEps(this.lat, other.lat) && NumHelper.equalsEps(this.lon, other.lon);
      }
   }

   public String toString() {
      return "<" + this.lat + ", " + this.lon + ">";
   }

   public Double[] toGeoJson() {
      return new Double[]{Double.valueOf(this.lon), Double.valueOf(this.lat)};
   }

   public static GHPoint parse(String str) {
      if(str.startsWith("<") && str.endsWith(">")) {
//         str = str.substring(1, str.length());
         str = str.substring(1, str.length()-1); // YN: fixed
      }

      String[] fromStrs = str.split(",");
      if(fromStrs.length == 2) {
         try {
            double fromLat = Double.parseDouble(fromStrs[0]);
            double fromLon = Double.parseDouble(fromStrs[1]);
            return new GHPoint(fromLat, fromLon);
         } catch (Exception var6) {
            ;
         }
      }

      return null;
   }
}
