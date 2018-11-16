package com.graphhopper.storage.index;

import com.graphhopper.storage.index.PointEmitter;

public class BresenhamLine {
   public static void calcPoints(int y1, int x1, int y2, int x2, PointEmitter emitter) {
      bresenham(y1, x1, y2, x2, emitter);
   }

   public static void voxelTraversal(double y1, double x1, double y2, double x2, PointEmitter emitter) {
      x1 = fix(x1);
      y1 = fix(y1);
      x2 = fix(x2);
      y2 = fix(y2);
      int x = (int)x1;
      int y = (int)y1;
      int endX = (int)x2;
      int endY = (int)y2;
      double gridCellWidth = 1.0D;
      double gridCellHeight = 1.0D;
      double deltaX = 1.0D / Math.abs(x2 - x1);
      int stepX = (int)Math.signum(x2 - x1);
      double tmp = frac(x1 / 1.0D);
      double maxX = deltaX * (1.0D - tmp);
      double deltaY = 1.0D / Math.abs(y2 - y1);
      int stepY = (int)Math.signum(y2 - y1);
      tmp = frac(y1 / 1.0D);
      double maxY = deltaY * (1.0D - tmp);
      boolean reachedY = false;
      boolean reachedX = false;
      emitter.set((double)y, (double)x);

      while(!reachedX || !reachedY) {
         if(maxX < maxY) {
            maxX += deltaX;
            x += stepX;
         } else {
            maxY += deltaY;
            y += stepY;
         }

         emitter.set((double)y, (double)x);
         if((double)stepX > 0.0D) {
            if(x >= endX) {
               reachedX = true;
            }
         } else if(x <= endX) {
            reachedX = true;
         }

         if((double)stepY > 0.0D) {
            if(y >= endY) {
               reachedY = true;
            }
         } else if(y <= endY) {
            reachedY = true;
         }
      }

   }

   static final double fix(double val) {
      return frac(val) == 0.0D?val + 0.1D:val;
   }

   static final double frac(double val) {
      return val - (double)((int)val);
   }

   public static void bresenham(int y1, int x1, int y2, int x2, PointEmitter emitter) {
      boolean latIncreasing = y1 < y2;
      boolean lonIncreasing = x1 < x2;
      int dLat = Math.abs(y2 - y1);
      int sLat = latIncreasing?1:-1;
      int dLon = Math.abs(x2 - x1);
      int sLon = lonIncreasing?1:-1;
      int err = dLon - dLat;

      while(true) {
         emitter.set((double)y1, (double)x1);
         if(y1 == y2 && x1 == x2) {
            return;
         }

         int tmpErr = 2 * err;
         if(tmpErr > -dLat) {
            err -= dLat;
            x1 += sLon;
         }

         if(tmpErr < dLon) {
            err += dLon;
            y1 += sLat;
         }
      }
   }

   public static void xiaolinWu(double y1, double x1, double y2, double x2, PointEmitter emitter) {
      double dx = x2 - x1;
      double dy = y2 - y1;
      double gradient;
      int yend;
      double xend;
      int ypxl1;
      int xpxl1;
      double interx;
      int ypxl2;
      int xpxl2;
      int y;
      if(Math.abs(dx) > Math.abs(dy)) {
         if(x2 < x1) {
            gradient = x1;
            x1 = x2;
            x2 = gradient;
            gradient = y1;
            y1 = y2;
            y2 = gradient;
         }

         gradient = dy / dx;
         yend = (int)x1;
         xend = y1 + gradient * ((double)yend - x1);
         ypxl1 = yend;
         xpxl1 = (int)xend;
         emitter.set((double)xpxl1, (double)yend);
         emitter.set((double)(xpxl1 + 1), (double)yend);
         interx = xend + gradient;
         yend = (int)x2;
         xend = y2 + gradient * ((double)yend - x2);
         ypxl2 = yend;
         xpxl2 = (int)xend;
         emitter.set((double)xpxl2, (double)yend);
         emitter.set((double)(xpxl2 + 1), (double)yend);

         for(y = ypxl1 + 1; y <= ypxl2 - 1; ++y) {
            emitter.set((double)((int)interx), (double)y);
            emitter.set((double)((int)interx + 1), (double)y);
            interx += gradient;
         }
      } else {
         if(y2 < y1) {
            gradient = x1;
            x1 = x2;
            x2 = gradient;
            gradient = y1;
            y1 = y2;
            y2 = gradient;
         }

         gradient = dx / dy;
         yend = (int)y1;
         xend = x1 + gradient * ((double)yend - y1);
         ypxl1 = yend;
         xpxl1 = (int)xend;
         emitter.set((double)yend, (double)xpxl1);
         emitter.set((double)(yend + 1), (double)xpxl1);
         interx = xend + gradient;
         yend = (int)y2;
         xend = x2 + gradient * ((double)yend - y2);
         ypxl2 = yend;
         xpxl2 = (int)xend;
         emitter.set((double)yend, (double)xpxl2);
         emitter.set((double)(yend + 1), (double)xpxl2);

         for(y = ypxl1 + 1; y <= ypxl2 - 1; ++y) {
            emitter.set((double)y, (double)((int)interx));
            emitter.set((double)y, (double)((int)interx + 1));
            interx += gradient;
         }
      }

   }

   public static void calcPoints(double lat1, double lon1, double lat2, double lon2, final PointEmitter emitter, final double offsetLat, final double offsetLon, final double deltaLat, final double deltaLon) {
      int y1 = (int)((lat1 - offsetLat) / deltaLat);
      int x1 = (int)((lon1 - offsetLon) / deltaLon);
      int y2 = (int)((lat2 - offsetLat) / deltaLat);
      int x2 = (int)((lon2 - offsetLon) / deltaLon);
      bresenham(y1, x1, y2, x2, new PointEmitter() {
         public void set(double lat, double lon) {
            emitter.set((lat + 0.1D) * deltaLat + offsetLat, (lon + 0.1D) * deltaLon + offsetLon);
         }
      });
   }
}
