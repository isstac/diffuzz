package com.graphhopper.util;

import com.graphhopper.util.Helper;

public class AngleCalc {
   private static final double PI_4 = 0.7853981633974483D;
   private static final double PI_2 = 1.5707963267948966D;
   private static final double PI3_4 = 2.356194490192345D;

   static final double atan2(double y, double x) {
      double absY = Math.abs(y) + 1.0E-10D;
      double r;
      double angle;
      if(x < 0.0D) {
         r = (x + absY) / (absY - x);
         angle = 2.356194490192345D;
      } else {
         r = (x - absY) / (x + absY);
         angle = 0.7853981633974483D;
      }

      angle += (0.1963D * r * r - 0.9817D) * r;
      return y < 0.0D?-angle:angle;
   }

   public double calcOrientation(double lat1, double lon1, double lat2, double lon2) {
      double shrinkFactor = Math.cos(Math.toRadians((lat1 + lat2) / 2.0D));
      return Math.atan2(lat2 - lat1, shrinkFactor * (lon2 - lon1));
   }

   public double convertAzimuth2xaxisAngle(double azimuth) {
      if(Double.compare(azimuth, 360.0D) <= 0 && Double.compare(azimuth, 0.0D) >= 0) {
         double angleXY = 1.5707963267948966D - azimuth / 180.0D * 3.141592653589793D;
         if(angleXY < -3.141592653589793D) {
            angleXY += 6.283185307179586D;
         }

         if(angleXY > 3.141592653589793D) {
            angleXY -= 6.283185307179586D;
         }

         return angleXY;
      } else {
         throw new IllegalArgumentException("Azimuth " + azimuth + " must be in (0, 360)");
      }
   }

   public double alignOrientation(double baseOrientation, double orientation) {
      double resultOrientation;
      if(baseOrientation >= 0.0D) {
         if(orientation < -3.141592653589793D + baseOrientation) {
            resultOrientation = orientation + 6.283185307179586D;
         } else {
            resultOrientation = orientation;
         }
      } else if(orientation > 3.141592653589793D + baseOrientation) {
         resultOrientation = orientation - 6.283185307179586D;
      } else {
         resultOrientation = orientation;
      }

      return resultOrientation;
   }

   double calcAzimuth(double lat1, double lon1, double lat2, double lon2) {
      double orientation = -this.calcOrientation(lat1, lon1, lat2, lon2);
      orientation = Helper.round4(orientation + 1.5707963267948966D);
      if(orientation < 0.0D) {
         orientation += 6.283185307179586D;
      }

      return Math.toDegrees(orientation);
   }

   String azimuth2compassPoint(double azimuth) {
      double slice = 22.5D;
      String cp;
      if(azimuth < slice) {
         cp = "N";
      } else if(azimuth < slice * 3.0D) {
         cp = "NE";
      } else if(azimuth < slice * 5.0D) {
         cp = "E";
      } else if(azimuth < slice * 7.0D) {
         cp = "SE";
      } else if(azimuth < slice * 9.0D) {
         cp = "S";
      } else if(azimuth < slice * 11.0D) {
         cp = "SW";
      } else if(azimuth < slice * 13.0D) {
         cp = "W";
      } else if(azimuth < slice * 15.0D) {
         cp = "NW";
      } else {
         cp = "N";
      }

      return cp;
   }
}
