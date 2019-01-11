package com.graphhopper.routing.util;

import com.graphhopper.reader.OSMWay;
import com.graphhopper.routing.util.BikeFlagEncoder;
import com.graphhopper.routing.util.EncodedDoubleValue;
import com.graphhopper.util.BitUtil;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.Helper;
import com.graphhopper.util.PMap;
import com.graphhopper.util.PointList;

public class Bike2WeightFlagEncoder extends BikeFlagEncoder {
   private EncodedDoubleValue reverseSpeedEncoder;

   public Bike2WeightFlagEncoder() {
   }

   public Bike2WeightFlagEncoder(String propertiesStr) {
      super(new PMap(propertiesStr));
   }

   public Bike2WeightFlagEncoder(PMap properties) {
      super(properties);
   }

   public Bike2WeightFlagEncoder(int speedBits, double speedFactor, int maxTurnCosts) {
      super(speedBits, speedFactor, maxTurnCosts);
   }

   public int getVersion() {
      return 1;
   }

   public int defineWayBits(int index, int shift) {
      shift = super.defineWayBits(index, shift);
      this.reverseSpeedEncoder = new EncodedDoubleValue("Reverse Speed", shift, this.speedBits, this.speedFactor, (long)this.getHighwaySpeed("cycleway"), this.maxPossibleSpeed);
      shift += this.reverseSpeedEncoder.getBits();
      return shift;
   }

   public double getReverseSpeed(long flags) {
      return this.reverseSpeedEncoder.getDoubleValue(flags);
   }

   public long setReverseSpeed(long flags, double speed) {
      if(speed < 0.0D) {
         throw new IllegalArgumentException("Speed cannot be negative: " + speed + ", flags:" + BitUtil.LITTLE.toBitString(flags));
      } else if(speed < this.speedEncoder.factor / 2.0D) {
         return this.setLowSpeed(flags, speed, true);
      } else {
         if(speed > this.getMaxSpeed()) {
            speed = this.getMaxSpeed();
         }

         return this.reverseSpeedEncoder.setDoubleValue(flags, speed);
      }
   }

   public long handleSpeed(OSMWay way, double speed, long flags) {
      flags = super.handleSpeed(way, speed, flags);
      if(this.isBackward(flags)) {
         flags = this.setReverseSpeed(flags, speed);
      }

      if(this.isForward(flags)) {
         flags = this.setSpeed(flags, speed);
      }

      return flags;
   }

   protected long setLowSpeed(long flags, double speed, boolean reverse) {
      return reverse?this.setBool(this.reverseSpeedEncoder.setDoubleValue(flags, 0.0D), 1, false):this.setBool(this.speedEncoder.setDoubleValue(flags, 0.0D), 0, false);
   }

   public long flagsDefault(boolean forward, boolean backward) {
      long flags = super.flagsDefault(forward, backward);
      return backward?this.reverseSpeedEncoder.setDefaultValue(flags):flags;
   }

   public long setProperties(double speed, boolean forward, boolean backward) {
      long flags = super.setProperties(speed, forward, backward);
      return backward?this.setReverseSpeed(flags, speed):flags;
   }

   public long reverseFlags(long flags) {
      flags = super.reverseFlags(flags);
      double otherValue = this.reverseSpeedEncoder.getDoubleValue(flags);
      flags = this.setReverseSpeed(flags, this.speedEncoder.getDoubleValue(flags));
      return this.setSpeed(flags, otherValue);
   }

   public void applyWayTags(OSMWay way, EdgeIteratorState edge) {
      PointList pl = edge.fetchWayGeometry(3);
      if(!pl.is3D()) {
         throw new IllegalStateException("To support speed calculation based on elevation data it is necessary to enable import of it.");
      } else {
         long flags = edge.getFlags();
         if(!way.hasTag("tunnel", "yes") && !way.hasTag("bridge", "yes") && !way.hasTag("highway", "steps")) {
            double incEleSum = 0.0D;
            double incDist2DSum = 0.0D;
            double decEleSum = 0.0D;
            double decDist2DSum = 0.0D;
            double prevEle = pl.getElevation(0);
            double fullDist2D = edge.getDistance();
            if(Double.isInfinite(fullDist2D)) {
               System.err.println("infinity distance? for way:" + way.getId());
               return;
            }

            if(fullDist2D < 1.0D) {
               return;
            }

            double eleDelta = pl.getElevation(pl.size() - 1) - prevEle;
            if(eleDelta > 0.1D) {
               incEleSum = eleDelta;
               incDist2DSum = fullDist2D;
            } else if(eleDelta < -0.1D) {
               decEleSum = -eleDelta;
               decDist2DSum = fullDist2D;
            }

            double fwdIncline = incDist2DSum > 1.0D?incEleSum / incDist2DSum:0.0D;
            double fwdDecline = decDist2DSum > 1.0D?decEleSum / decDist2DSum:0.0D;
            double restDist2D = fullDist2D - incDist2DSum - decDist2DSum;
            double maxSpeed = (double)this.getHighwaySpeed("cycleway");
            double bwSlower;
            double speedReverse;
            double bwFaster;
            if(this.isForward(flags)) {
               speedReverse = this.getSpeed(flags);
               bwFaster = 1.0D + 2.0D * Helper.keepIn(fwdDecline, 0.0D, 0.2D);
               bwFaster *= bwFaster;
               bwSlower = 1.0D - 5.0D * Helper.keepIn(fwdIncline, 0.0D, 0.2D);
               bwSlower *= bwSlower;
               speedReverse = speedReverse * (bwSlower * incDist2DSum + bwFaster * decDist2DSum + 1.0D * restDist2D) / fullDist2D;
               flags = this.setSpeed(flags, Helper.keepIn(speedReverse, 2.0D, maxSpeed));
            }

            if(this.isBackward(flags)) {
               speedReverse = this.getReverseSpeed(flags);
               bwFaster = 1.0D + 2.0D * Helper.keepIn(fwdIncline, 0.0D, 0.2D);
               bwFaster *= bwFaster;
               bwSlower = 1.0D - 5.0D * Helper.keepIn(fwdDecline, 0.0D, 0.2D);
               bwSlower *= bwSlower;
               speedReverse = speedReverse * (bwFaster * incDist2DSum + bwSlower * decDist2DSum + 1.0D * restDist2D) / fullDist2D;
               flags = this.setReverseSpeed(flags, Helper.keepIn(speedReverse, 2.0D, maxSpeed));
            }
         }

         edge.setFlags(flags);
      }
   }

   public String toString() {
      return "bike2";
   }
}
