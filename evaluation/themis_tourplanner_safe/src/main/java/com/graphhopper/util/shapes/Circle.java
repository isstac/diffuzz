package com.graphhopper.util.shapes;

import com.graphhopper.util.DistanceCalc;
import com.graphhopper.util.Helper;
import com.graphhopper.util.shapes.BBox;
import com.graphhopper.util.shapes.Shape;

public class Circle implements Shape {
   private DistanceCalc calc;
   private final double radiusInKm;
   private final double lat;
   private final double lon;
   private final double normedDist;
   private final BBox bbox;

   public Circle(double lat, double lon, double radiusInMeter) {
      this(lat, lon, radiusInMeter, Helper.DIST_EARTH);
   }

   public Circle(double lat, double lon, double radiusInMeter, DistanceCalc calc) {
      this.calc = Helper.DIST_EARTH;
      this.calc = calc;
      this.lat = lat;
      this.lon = lon;
      this.radiusInKm = radiusInMeter;
      this.normedDist = calc.calcNormalizedDist(radiusInMeter);
      this.bbox = calc.createBBox(lat, lon, radiusInMeter);
   }

   public double getLat() {
      return this.lat;
   }

   public double getLon() {
      return this.lon;
   }

   public boolean contains(double lat1, double lon1) {
      return this.normDist(lat1, lon1) <= this.normedDist;
   }

   public BBox getBounds() {
      return this.bbox;
   }

   private double normDist(double lat1, double lon1) {
      return this.calc.calcNormalizedDist(this.lat, this.lon, lat1, lon1);
   }

   public boolean intersect(Shape o) {
      return o instanceof Circle?this.intersect((Circle)o):(o instanceof BBox?this.intersect((BBox)o):o.intersect(this));
   }

   public boolean contains(Shape o) {
      if(o instanceof Circle) {
         return this.contains((Circle)o);
      } else if(o instanceof BBox) {
         return this.contains((BBox)o);
      } else {
         throw new UnsupportedOperationException("unsupported shape");
      }
   }

   public boolean intersect(BBox b) {
      return this.lat > b.maxLat?(this.lon < b.minLon?this.normDist(b.maxLat, b.minLon) <= this.normedDist:(this.lon > b.maxLon?this.normDist(b.maxLat, b.maxLon) <= this.normedDist:b.maxLat - this.bbox.minLat > 0.0D)):(this.lat < b.minLat?(this.lon < b.minLon?this.normDist(b.minLat, b.minLon) <= this.normedDist:(this.lon > b.maxLon?this.normDist(b.minLat, b.maxLon) <= this.normedDist:this.bbox.maxLat - b.minLat > 0.0D)):(this.lon < b.minLon?this.bbox.maxLon - b.minLon > 0.0D:(this.lon > b.maxLon?b.maxLon - this.bbox.minLon > 0.0D:true)));
   }

   public boolean intersect(Circle c) {
      return !this.getBounds().intersect(c.getBounds())?false:this.normDist(c.lat, c.lon) <= this.calc.calcNormalizedDist(this.radiusInKm + c.radiusInKm);
   }

   public boolean contains(BBox b) {
      return !this.bbox.contains(b)?false:this.contains(b.maxLat, b.minLon) && this.contains(b.minLat, b.minLon) && this.contains(b.maxLat, b.maxLon) && this.contains(b.minLat, b.maxLon);
   }

   public boolean contains(Circle c) {
      double res = this.radiusInKm - c.radiusInKm;
      return res < 0.0D?false:this.calc.calcDist(this.lat, this.lon, c.lat, c.lon) <= res;
   }

   public String toString() {
      return this.lat + "," + this.lon + ", radius:" + this.radiusInKm;
   }
}
