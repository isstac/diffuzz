package com.graphhopper.util;

import com.graphhopper.util.shapes.GHPoint;
import com.graphhopper.util.shapes.GHPoint3D;

public class GPXEntry extends GHPoint3D {
   private long time;

   public GPXEntry(GHPoint p, long millis) {
      this(p.lat, p.lon, millis);
   }

   public GPXEntry(double lat, double lon, long millis) {
      super(lat, lon, Double.NaN);
      this.time = millis;
   }

   public GPXEntry(double lat, double lon, double ele, long millis) {
      super(lat, lon, ele);
      this.time = millis;
   }

   boolean is3D() {
      return !Double.isNaN(this.ele);
   }

   public long getTime() {
      return this.time;
   }

   public void setTime(long time) {
      this.time = time;
   }

   /** @deprecated */
   public long getMillis() {
      return this.time;
   }

   /** @deprecated */
   public void setMillis(long time) {
      this.time = time;
   }

   public boolean equals(Object obj) {
      if(obj == null) {
         return false;
      } else {
         GPXEntry other = (GPXEntry)obj;
         return this.time == other.time && super.equals(obj);
      }
   }

   public String toString() {
      return super.toString() + ", " + this.time;
   }
}
