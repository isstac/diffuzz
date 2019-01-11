package com.graphhopper.util.shapes;

import com.graphhopper.util.NumHelper;
import com.graphhopper.util.shapes.GHPoint;

public class GHPoint3D extends GHPoint {
   public double ele;

   public GHPoint3D(double lat, double lon, double elevation) {
      super(lat, lon);
      this.ele = elevation;
   }

   public double getElevation() {
      return this.ele;
   }

   public double getEle() {
      return this.ele;
   }

   public int hashCode() {
      int hash = 59 * super.hashCode() + (int)(Double.doubleToLongBits(this.ele) ^ Double.doubleToLongBits(this.ele) >>> 32);
      return hash;
   }

   public boolean equals(Object obj) {
      if(obj == null) {
         return false;
      } else {
         GHPoint3D other = (GHPoint3D)obj;
         return Double.isNaN(this.ele)?NumHelper.equalsEps(this.lat, other.lat) && NumHelper.equalsEps(this.lon, other.lon):NumHelper.equalsEps(this.lat, other.lat) && NumHelper.equalsEps(this.lon, other.lon) && NumHelper.equalsEps(this.ele, other.ele);
      }
   }

   public String toString() {
      return super.toString() + "," + this.ele;
   }

   public Double[] toGeoJson() {
      return new Double[]{Double.valueOf(this.lon), Double.valueOf(this.lat), Double.valueOf(this.ele)};
   }
}
