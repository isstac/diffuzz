package com.graphhopper.util;

import com.graphhopper.util.DistanceCalc;
import com.graphhopper.util.shapes.BBox;
import com.graphhopper.util.shapes.GHPoint;

public class DistanceCalcEarth implements DistanceCalc {
   public static final double R = 6371000.0D;
   public static final double R_EQ = 6378137.0D;
   public static final double C = 4.003017359204114E7D;
   public static final double KM_MILE = 1.609344D;

   public double calcDist(double fromLat, double fromLon, double toLat, double toLon) {
      double sinDeltaLat = Math.sin(Math.toRadians(toLat - fromLat) / 2.0D);
      double sinDeltaLon = Math.sin(Math.toRadians(toLon - fromLon) / 2.0D);
      double normedDist = sinDeltaLat * sinDeltaLat + sinDeltaLon * sinDeltaLon * Math.cos(Math.toRadians(fromLat)) * Math.cos(Math.toRadians(toLat));
      return 1.2742E7D * Math.asin(Math.sqrt(normedDist));
   }

   public double calcDenormalizedDist(double normedDist) {
      return 1.2742E7D * Math.asin(Math.sqrt(normedDist));
   }

   public double calcNormalizedDist(double dist) {
      double tmp = Math.sin(dist / 2.0D / 6371000.0D);
      return tmp * tmp;
   }

   public double calcNormalizedDist(double fromLat, double fromLon, double toLat, double toLon) {
      double sinDeltaLat = Math.sin(Math.toRadians(toLat - fromLat) / 2.0D);
      double sinDeltaLon = Math.sin(Math.toRadians(toLon - fromLon) / 2.0D);
      return sinDeltaLat * sinDeltaLat + sinDeltaLon * sinDeltaLon * Math.cos(Math.toRadians(fromLat)) * Math.cos(Math.toRadians(toLat));
   }

   public double calcCircumference(double lat) {
//	   System.out.println("calcCircumference()");
      return 4.003017359204114E7D * Math.cos(Math.toRadians(lat));
   }

   public boolean isDateLineCrossOver(double lon1, double lon2) {
      return Math.abs(lon1 - lon2) > 180.0D;
   }

   public BBox createBBox(double lat, double lon, double radiusInMeter) {
      if(radiusInMeter <= 0.0D) {
         throw new IllegalArgumentException("Distance must not be zero or negative! " + radiusInMeter + " lat,lon:" + lat + "," + lon);
      } else {
         double dLon = 360.0D / (this.calcCircumference(lat) / radiusInMeter);
         double dLat = 360.0D / (4.003017359204114E7D / radiusInMeter);
         return new BBox(lon - dLon, lon + dLon, lat - dLat, lat + dLat);
      }
   }

   public double calcNormalizedEdgeDistance(double r_lat_deg, double r_lon_deg, double a_lat_deg, double a_lon_deg, double b_lat_deg, double b_lon_deg) {
      return this.calcNormalizedEdgeDistanceNew(r_lat_deg, r_lon_deg, a_lat_deg, a_lon_deg, b_lat_deg, b_lon_deg, false);
   }

   public double calcNormalizedEdgeDistanceNew(double r_lat_deg, double r_lon_deg, double a_lat_deg, double a_lon_deg, double b_lat_deg, double b_lon_deg, boolean reduceToSegment) {
      double shrinkFactor = Math.cos(Math.toRadians((a_lat_deg + b_lat_deg) / 2.0D));
      double a_lon = a_lon_deg * shrinkFactor;
      double b_lon = b_lon_deg * shrinkFactor;
      double r_lon = r_lon_deg * shrinkFactor;
      double delta_lon = b_lon - a_lon;
      double delta_lat = b_lat_deg - a_lat_deg;
      if(delta_lat == 0.0D) {
         return this.calcNormalizedDist(a_lat_deg, r_lon_deg, r_lat_deg, r_lon_deg);
      } else if(delta_lon == 0.0D) {
         return this.calcNormalizedDist(r_lat_deg, a_lon_deg, r_lat_deg, r_lon_deg);
      } else {
         double norm = delta_lon * delta_lon + delta_lat * delta_lat;
         double factor = ((r_lon - a_lon) * delta_lon + (r_lat_deg - a_lat_deg) * delta_lat) / norm;
         if(reduceToSegment) {
            if(factor > 1.0D) {
               factor = 1.0D;
            } else if(factor < 0.0D) {
               factor = 0.0D;
            }
         }

         double c_lon = a_lon + factor * delta_lon;
         double c_lat = a_lat_deg + factor * delta_lat;
         return this.calcNormalizedDist(c_lat, c_lon / shrinkFactor, r_lat_deg, r_lon_deg);
      }
   }

   public GHPoint calcCrossingPointToEdge(double r_lat_deg, double r_lon_deg, double a_lat_deg, double a_lon_deg, double b_lat_deg, double b_lon_deg) {
      double shrinkFactor = Math.cos(Math.toRadians((a_lat_deg + b_lat_deg) / 2.0D));
      double a_lon = a_lon_deg * shrinkFactor;
      double b_lon = b_lon_deg * shrinkFactor;
      double r_lon = r_lon_deg * shrinkFactor;
      double delta_lon = b_lon - a_lon;
      double delta_lat = b_lat_deg - a_lat_deg;
      if(delta_lat == 0.0D) {
         return new GHPoint(a_lat_deg, r_lon_deg);
      } else if(delta_lon == 0.0D) {
         return new GHPoint(r_lat_deg, a_lon_deg);
      } else {
         double norm = delta_lon * delta_lon + delta_lat * delta_lat;
         double factor = ((r_lon - a_lon) * delta_lon + (r_lat_deg - a_lat_deg) * delta_lat) / norm;
         double c_lon = a_lon + factor * delta_lon;
         double c_lat = a_lat_deg + factor * delta_lat;
         return new GHPoint(c_lat, c_lon / shrinkFactor);
      }
   }

   public boolean validEdgeDistance(double r_lat_deg, double r_lon_deg, double a_lat_deg, double a_lon_deg, double b_lat_deg, double b_lon_deg) {
      double shrinkFactor = Math.cos(Math.toRadians((a_lat_deg + b_lat_deg) / 2.0D));
      double a_lon = a_lon_deg * shrinkFactor;
      double b_lon = b_lon_deg * shrinkFactor;
      double r_lon = r_lon_deg * shrinkFactor;
      double ar_x = r_lon - a_lon;
      double ar_y = r_lat_deg - a_lat_deg;
      double ab_x = b_lon - a_lon;
      double ab_y = b_lat_deg - a_lat_deg;
      double ab_ar = ar_x * ab_x + ar_y * ab_y;
      double rb_x = b_lon - r_lon;
      double rb_y = b_lat_deg - r_lat_deg;
      double ab_rb = rb_x * ab_x + rb_y * ab_y;
      return ab_ar > 0.0D && ab_rb > 0.0D;
   }

   public String toString() {
      return "EXACT";
   }
}
