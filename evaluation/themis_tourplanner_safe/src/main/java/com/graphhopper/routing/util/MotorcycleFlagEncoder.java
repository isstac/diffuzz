package com.graphhopper.routing.util;

import com.graphhopper.reader.OSMWay;
import com.graphhopper.routing.util.CarFlagEncoder;
import com.graphhopper.routing.util.EncodedDoubleValue;
import com.graphhopper.routing.util.EncodedValue;
import com.graphhopper.routing.util.PriorityCode;
import com.graphhopper.routing.util.PriorityWeighting;
import com.graphhopper.util.BitUtil;
import com.graphhopper.util.PMap;
import java.util.HashSet;

public class MotorcycleFlagEncoder extends CarFlagEncoder {
   private EncodedDoubleValue reverseSpeedEncoder;
   private EncodedValue priorityWayEncoder;
   private final HashSet avoidSet;
   private final HashSet preferSet;

   public MotorcycleFlagEncoder(PMap properties) {
      this((int)properties.getLong("speedBits", 5L), properties.getDouble("speedFactor", 5.0D), properties.getBool("turnCosts", false)?1:0);
      this.properties = properties;
      this.setBlockFords(properties.getBool("blockFords", true));
   }

   public MotorcycleFlagEncoder(String propertiesStr) {
      this(new PMap(propertiesStr));
   }

   public MotorcycleFlagEncoder(int speedBits, double speedFactor, int maxTurnCosts) {
      super(speedBits, speedFactor, maxTurnCosts);
      this.avoidSet = new HashSet();
      this.preferSet = new HashSet();
      this.restrictions.remove("motorcar");
      this.restrictions.add("motorcycle");
      this.trackTypeSpeedMap.clear();
      this.defaultSpeedMap.clear();
      this.trackTypeSpeedMap.put("grade1", Integer.valueOf(20));
      this.trackTypeSpeedMap.put("grade2", Integer.valueOf(15));
      this.trackTypeSpeedMap.put("grade3", Integer.valueOf(10));
      this.trackTypeSpeedMap.put("grade4", Integer.valueOf(5));
      this.trackTypeSpeedMap.put("grade5", Integer.valueOf(5));
      this.avoidSet.add("motorway");
      this.avoidSet.add("trunk");
      this.avoidSet.add("motorroad");
      this.preferSet.add("primary");
      this.preferSet.add("secondary");
      this.maxPossibleSpeed = 120;
      this.defaultSpeedMap.put("motorway", Integer.valueOf(100));
      this.defaultSpeedMap.put("motorway_link", Integer.valueOf(70));
      this.defaultSpeedMap.put("motorroad", Integer.valueOf(90));
      this.defaultSpeedMap.put("trunk", Integer.valueOf(80));
      this.defaultSpeedMap.put("trunk_link", Integer.valueOf(75));
      this.defaultSpeedMap.put("primary", Integer.valueOf(65));
      this.defaultSpeedMap.put("primary_link", Integer.valueOf(60));
      this.defaultSpeedMap.put("secondary", Integer.valueOf(60));
      this.defaultSpeedMap.put("secondary_link", Integer.valueOf(50));
      this.defaultSpeedMap.put("tertiary", Integer.valueOf(50));
      this.defaultSpeedMap.put("tertiary_link", Integer.valueOf(40));
      this.defaultSpeedMap.put("unclassified", Integer.valueOf(30));
      this.defaultSpeedMap.put("residential", Integer.valueOf(30));
      this.defaultSpeedMap.put("living_street", Integer.valueOf(5));
      this.defaultSpeedMap.put("service", Integer.valueOf(20));
      this.defaultSpeedMap.put("road", Integer.valueOf(20));
      this.defaultSpeedMap.put("track", Integer.valueOf(15));
   }

   public int getVersion() {
      return 1;
   }

   public int defineWayBits(int index, int shift) {
      shift = super.defineWayBits(index, shift);
      this.reverseSpeedEncoder = new EncodedDoubleValue("Reverse Speed", shift, this.speedBits, this.speedFactor, (long)((Integer)this.defaultSpeedMap.get("secondary")).intValue(), this.maxPossibleSpeed);
      shift += this.reverseSpeedEncoder.getBits();
      this.priorityWayEncoder = new EncodedValue("PreferWay", shift, 3, 1.0D, 3L, 7);
      shift += this.reverseSpeedEncoder.getBits();
      return shift;
   }

   public long acceptWay(OSMWay way) {
      String highwayValue = way.getTag("highway");
      String firstValue;
      if(highwayValue == null) {
         if(way.hasTag("route", this.ferries)) {
            firstValue = way.getTag("motorcycle");
            if(firstValue == null) {
               firstValue = way.getTag("motor_vehicle");
            }

            if(firstValue == null && !way.hasTag("foot", new String[0]) && !way.hasTag("bicycle", new String[0]) || "yes".equals(firstValue)) {
               return this.acceptBit | this.ferryBit;
            }
         }

         return 0L;
      } else {
         if("track".equals(highwayValue)) {
            firstValue = way.getTag("tracktype");
            if(firstValue != null && !firstValue.equals("grade1")) {
               return 0L;
            }
         }

         if(!this.defaultSpeedMap.containsKey(highwayValue)) {
            return 0L;
         } else if(!way.hasTag("impassable", "yes") && !way.hasTag("status", "impassable")) {
            firstValue = way.getFirstPriorityTag(this.restrictions);
            if(!firstValue.isEmpty()) {
               if(this.restrictedValues.contains(firstValue)) {
                  return 0L;
               }

               if(this.intendedValues.contains(firstValue)) {
                  return this.acceptBit;
               }
            }

            return this.isBlockFords() && ("ford".equals(highwayValue) || way.hasTag("ford", new String[0]))?0L:(way.hasTag("railway", new String[0]) && !way.hasTag("railway", this.acceptedRailways)?0L:this.acceptBit);
         } else {
            return 0L;
         }
      }
   }

   public long handleWayTags(OSMWay way, long allowed, long priorityFromRelation) {
      if(!this.isAccept(allowed)) {
         return 0L;
      } else {
         long encoded = 0L;
         if(!this.isFerry(allowed)) {
            double speed = this.getSpeed(way);
            speed = this.applyMaxSpeed(way, speed, true);
            double maxMCSpeed = this.parseSpeed(way.getTag("maxspeed:motorcycle"));
            if(maxMCSpeed > 0.0D && maxMCSpeed < speed) {
               speed = maxMCSpeed * 0.9D;
            }

            if(speed > 30.0D && way.hasTag("surface", this.badSurfaceSpeedMap)) {
               speed = 30.0D;
            }

            boolean isRoundabout = way.hasTag("junction", "roundabout");
            if(isRoundabout) {
               encoded = this.setBool(0L, 2, true);
            }

            if(!way.hasTag("oneway", this.oneways) && !isRoundabout) {
               encoded = this.setSpeed(encoded, speed);
               encoded = this.setReverseSpeed(encoded, speed);
               encoded |= this.directionBitMask;
            } else if(way.hasTag("oneway", "-1")) {
               encoded = this.setReverseSpeed(encoded, speed);
               encoded |= this.backwardBit;
            } else {
               encoded = this.setSpeed(encoded, speed);
               encoded |= this.forwardBit;
            }
         } else {
            encoded = this.handleFerryTags(way, (double)((Integer)this.defaultSpeedMap.get("living_street")).intValue(), (double)((Integer)this.defaultSpeedMap.get("service")).intValue(), (double)((Integer)this.defaultSpeedMap.get("residential")).intValue());
            encoded |= this.directionBitMask;
         }

         encoded = this.priorityWayEncoder.setValue(encoded, (long)this.handlePriority(way, priorityFromRelation));
         return encoded;
      }
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

   public double getDouble(long flags, int key) {
      switch(key) {
      case 101:
         return (double)this.priorityWayEncoder.getValue(flags) / (double)PriorityCode.BEST.getValue();
      default:
         return super.getDouble(flags, key);
      }
   }

   private int handlePriority(OSMWay way, long relationFlags) {
      String highway = (String)way.getTag("highway", "");
      return this.avoidSet.contains(highway)?PriorityCode.AVOID_AT_ALL_COSTS.getValue():(this.preferSet.contains(highway)?PriorityCode.VERY_NICE.getValue():PriorityCode.UNCHANGED.getValue());
   }

   public boolean supports(Class feature) {
      return super.supports(feature)?true:PriorityWeighting.class.isAssignableFrom(feature);
   }

   public String toString() {
      return "motorcycle";
   }
}
