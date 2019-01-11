package com.graphhopper.util;

import com.graphhopper.util.AngleCalc;
import com.graphhopper.util.GPXEntry;
import com.graphhopper.util.Helper;
import com.graphhopper.util.InstructionAnnotation;
import com.graphhopper.util.PointList;
import com.graphhopper.util.Translation;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Instruction {
   private static final AngleCalc ac = new AngleCalc();
   public static final int LEAVE_ROUNDABOUT = -6;
   public static final int TURN_SHARP_LEFT = -3;
   public static final int TURN_LEFT = -2;
   public static final int TURN_SLIGHT_LEFT = -1;
   public static final int CONTINUE_ON_STREET = 0;
   public static final int TURN_SLIGHT_RIGHT = 1;
   public static final int TURN_RIGHT = 2;
   public static final int TURN_SHARP_RIGHT = 3;
   public static final int FINISH = 4;
   public static final int REACHED_VIA = 5;
   public static final int USE_ROUNDABOUT = 6;
   protected boolean rawName;
   protected int sign;
   protected String name;
   protected double distance;
   protected long time;
   protected final PointList points;
   protected final InstructionAnnotation annotation;

   public Instruction(int sign, String name, InstructionAnnotation ia, PointList pl) {
      this.sign = sign;
      this.name = name;
      this.points = pl;
      this.annotation = ia;
   }

   public void setUseRawName() {
      this.rawName = true;
   }

   public InstructionAnnotation getAnnotation() {
      return this.annotation;
   }

   public int getSign() {
      return this.sign;
   }

   public String getName() {
      return this.name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public Map getExtraInfoJSON() {
      return Collections.emptyMap();
   }

   public void setExtraInfo(String key, Object value) {
      throw new IllegalArgumentException("Key" + key + " is not a valid option");
   }

   public Instruction setDistance(double distance) {
      this.distance = distance;
      return this;
   }

   public double getDistance() {
      return this.distance;
   }

   public Instruction setTime(long time) {
      this.time = time;
      return this;
   }

   public long getTime() {
      return this.time;
   }

   double getFirstLat() {
      return this.points.getLatitude(0);
   }

   double getFirstLon() {
      return this.points.getLongitude(0);
   }

   double getFirstEle() {
      return this.points.getElevation(0);
   }

   public PointList getPoints() {
      return this.points;
   }

   long fillGPXList(List list, long time, Instruction prevInstr, Instruction nextInstr, boolean firstInstr) {
      this.checkOne();
      int len = this.points.size();
      long prevTime = time;
      double lat = this.points.getLatitude(0);
      double lon = this.points.getLongitude(0);
      double ele = Double.NaN;
      boolean is3D = this.points.is3D();
      if(is3D) {
         ele = this.points.getElevation(0);
      }

      for(int i = 0; i < len; ++i) {
         list.add(new GPXEntry(lat, lon, ele, prevTime));
         boolean last = i + 1 == len;
         double nextLat = last?nextInstr.getFirstLat():this.points.getLatitude(i + 1);
         double nextLon = last?nextInstr.getFirstLon():this.points.getLongitude(i + 1);
         double nextEle = is3D?(last?nextInstr.getFirstEle():this.points.getElevation(i + 1)):Double.NaN;
         if(is3D) {
            prevTime = Math.round((double)prevTime + (double)this.time * Helper.DIST_3D.calcDist(nextLat, nextLon, nextEle, lat, lon, ele) / this.distance);
         } else {
            prevTime = Math.round((double)prevTime + (double)this.time * Helper.DIST_3D.calcDist(nextLat, nextLon, lat, lon) / this.distance);
         }

         lat = nextLat;
         lon = nextLon;
         ele = nextEle;
      }

      return time + this.time;
   }

   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append('(');
      sb.append(this.sign).append(',');
      sb.append(this.name).append(',');
      sb.append(this.distance).append(',');
      sb.append(this.time);
      sb.append(')');
      return sb.toString();
   }

   String calcDirection(Instruction nextI) {
      double azimuth = this.calcAzimuth(nextI);
      return Double.isNaN(azimuth)?"":ac.azimuth2compassPoint(azimuth);
   }

   public double calcAzimuth(Instruction nextI) {
      double nextLat;
      double nextLon;
      if(this.points.getSize() >= 2) {
         nextLat = this.points.getLatitude(1);
         nextLon = this.points.getLongitude(1);
      } else {
         if(nextI == null || this.points.getSize() != 1) {
            return Double.NaN;
         }

         nextLat = nextI.points.getLatitude(0);
         nextLon = nextI.points.getLongitude(0);
      }

      double lat = this.points.getLatitude(0);
      double lon = this.points.getLongitude(0);
      return ac.calcAzimuth(lat, lon, nextLat, nextLon);
   }

   void checkOne() {
      if(this.points.size() < 1) {
         throw new IllegalStateException("Instruction must contain at least one point " + this.toString());
      }
   }

   public String getTurnDescription(Translation tr) {
      if(this.rawName) {
         return this.getName();
      } else {
         String streetName = this.getName();
         int indi = this.getSign();
         String str;
         if(indi == 0) {
            str = Helper.isEmpty(streetName)?tr.tr("continue", new Object[0]):tr.tr("continue_onto", new Object[]{streetName});
         } else {
            String dir = null;
            switch(indi) {
            case -3:
               dir = tr.tr("turn_sharp_left", new Object[0]);
               break;
            case -2:
               dir = tr.tr("turn_left", new Object[0]);
               break;
            case -1:
               dir = tr.tr("turn_slight_left", new Object[0]);
            case 0:
            default:
               break;
            case 1:
               dir = tr.tr("turn_slight_right", new Object[0]);
               break;
            case 2:
               dir = tr.tr("turn_right", new Object[0]);
               break;
            case 3:
               dir = tr.tr("turn_sharp_right", new Object[0]);
            }

            if(dir == null) {
               throw new IllegalStateException("Turn indication not found " + indi);
            }

            str = Helper.isEmpty(streetName)?dir:tr.tr("turn_onto", new Object[]{dir, streetName});
         }

         return str;
      }
   }
}
