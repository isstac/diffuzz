package com.graphhopper.routing.util;

import com.graphhopper.reader.OSMRelation;
import com.graphhopper.reader.OSMWay;
import com.graphhopper.routing.util.AbstractFlagEncoder;
import com.graphhopper.routing.util.EncodedDoubleValue;
import com.graphhopper.util.Helper;
import com.graphhopper.util.PMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CarFlagEncoder extends AbstractFlagEncoder {
   protected final Map trackTypeSpeedMap;
   protected final Set badSurfaceSpeedMap;
   protected final Map defaultSpeedMap;

   public CarFlagEncoder() {
      this(5, 5.0D, 0);
   }

   public CarFlagEncoder(PMap properties) {
      this((int)properties.getLong("speedBits", 5L), properties.getDouble("speedFactor", 5.0D), properties.getBool("turnCosts", false)?1:0);
      this.properties = properties;
      this.setBlockFords(properties.getBool("blockFords", true));
   }

   public CarFlagEncoder(String propertiesStr) {
      this(new PMap(propertiesStr));
   }

   public CarFlagEncoder(int speedBits, double speedFactor, int maxTurnCosts) {
      super(speedBits, speedFactor, maxTurnCosts);
      this.trackTypeSpeedMap = new HashMap();
      this.badSurfaceSpeedMap = new HashSet();
      this.defaultSpeedMap = new HashMap();
      this.restrictions.addAll(Arrays.asList(new String[]{"motorcar", "motor_vehicle", "vehicle", "access"}));
      this.restrictedValues.add("private");
      this.restrictedValues.add("agricultural");
      this.restrictedValues.add("forestry");
      this.restrictedValues.add("no");
      this.restrictedValues.add("restricted");
      this.restrictedValues.add("delivery");
      this.restrictedValues.add("military");
      this.intendedValues.add("yes");
      this.intendedValues.add("permissive");
      this.potentialBarriers.add("gate");
      this.potentialBarriers.add("lift_gate");
      this.potentialBarriers.add("kissing_gate");
      this.potentialBarriers.add("swing_gate");
      this.absoluteBarriers.add("bollard");
      this.absoluteBarriers.add("stile");
      this.absoluteBarriers.add("turnstile");
      this.absoluteBarriers.add("cycle_barrier");
      this.absoluteBarriers.add("motorcycle_barrier");
      this.absoluteBarriers.add("block");
      this.trackTypeSpeedMap.put("grade1", Integer.valueOf(20));
      this.trackTypeSpeedMap.put("grade2", Integer.valueOf(15));
      this.trackTypeSpeedMap.put("grade3", Integer.valueOf(10));
      this.trackTypeSpeedMap.put("grade4", Integer.valueOf(5));
      this.trackTypeSpeedMap.put("grade5", Integer.valueOf(5));
      this.badSurfaceSpeedMap.add("cobblestone");
      this.badSurfaceSpeedMap.add("grass_paver");
      this.badSurfaceSpeedMap.add("gravel");
      this.badSurfaceSpeedMap.add("sand");
      this.badSurfaceSpeedMap.add("paving_stones");
      this.badSurfaceSpeedMap.add("dirt");
      this.badSurfaceSpeedMap.add("ground");
      this.badSurfaceSpeedMap.add("grass");
      this.maxPossibleSpeed = 140;
      this.defaultSpeedMap.put("motorway", Integer.valueOf(100));
      this.defaultSpeedMap.put("motorway_link", Integer.valueOf(70));
      this.defaultSpeedMap.put("motorroad", Integer.valueOf(90));
      this.defaultSpeedMap.put("trunk", Integer.valueOf(70));
      this.defaultSpeedMap.put("trunk_link", Integer.valueOf(65));
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
      this.speedEncoder = new EncodedDoubleValue("Speed", shift, this.speedBits, this.speedFactor, (long)((Integer)this.defaultSpeedMap.get("secondary")).intValue(), this.maxPossibleSpeed);
      return shift + this.speedEncoder.getBits();
   }

   protected double getSpeed(OSMWay way) {
      String highwayValue = way.getTag("highway");
      Integer speed = (Integer)this.defaultSpeedMap.get(highwayValue);
      if(speed == null) {
         throw new IllegalStateException(this.toString() + ", no speed found for: " + highwayValue + ", tags: " + way);
      } else {
         if(highwayValue.equals("track")) {
            String tt = way.getTag("tracktype");
            if(!Helper.isEmpty(tt)) {
               Integer tInt = (Integer)this.trackTypeSpeedMap.get(tt);
               if(tInt != null) {
                  speed = tInt;
               }
            }
         }

         return (double)speed.intValue();
      }
   }

   public long acceptWay(OSMWay way) {
      String highwayValue = way.getTag("highway");
      String firstValue;
      if(highwayValue == null) {
         if(way.hasTag("route", this.ferries)) {
            firstValue = way.getTag("motorcar");
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
            if(firstValue != null && !firstValue.equals("grade1") && !firstValue.equals("grade2") && !firstValue.equals("grade3")) {
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

   public long handleRelationTags(OSMRelation relation, long oldRelationFlags) {
      return oldRelationFlags;
   }

   public long handleWayTags(OSMWay way, long allowed, long relationFlags) {
      if(!this.isAccept(allowed)) {
         return 0L;
      } else {
         long encoded;
         if(!this.isFerry(allowed)) {
            double speed = this.getSpeed(way);
            speed = this.applyMaxSpeed(way, speed, true);
            if(speed > 30.0D && way.hasTag("surface", this.badSurfaceSpeedMap)) {
               speed = 30.0D;
            }

            encoded = this.setSpeed(0L, speed);
            boolean isRoundabout = way.hasTag("junction", "roundabout");
            if(isRoundabout) {
               encoded = this.setBool(encoded, 2, true);
            }

            boolean isOneway = way.hasTag("oneway", this.oneways) || way.hasTag("vehicle:backward", new String[0]) || way.hasTag("vehicle:forward", new String[0]) || way.hasTag("motor_vehicle:backward", new String[0]) || way.hasTag("motor_vehicle:forward", new String[0]);
            if(!isOneway && !isRoundabout) {
               encoded |= this.directionBitMask;
            } else {
               boolean isBackward = way.hasTag("oneway", "-1") || way.hasTag("vehicle:forward", "no") || way.hasTag("motor_vehicle:forward", "no");
               if(isBackward) {
                  encoded |= this.backwardBit;
               } else {
                  encoded |= this.forwardBit;
               }
            }
         } else {
            encoded = this.handleFerryTags(way, (double)((Integer)this.defaultSpeedMap.get("living_street")).intValue(), (double)((Integer)this.defaultSpeedMap.get("service")).intValue(), (double)((Integer)this.defaultSpeedMap.get("residential")).intValue());
            encoded |= this.directionBitMask;
         }

         return encoded;
      }
   }

   public String getWayInfo(OSMWay way) {
      String str = "";
      String highwayValue = way.getTag("highway");
      if("motorway_link".equals(highwayValue)) {
         String destination = way.getTag("destination");
         if(!Helper.isEmpty(destination)) {
            int counter = 0;
            String[] arr$ = destination.split(";");
            int len$ = arr$.length;

            for(int i$ = 0; i$ < len$; ++i$) {
               String d = arr$[i$];
               if(!d.trim().isEmpty()) {
                  if(counter > 0) {
                     str = str + ", ";
                  }

                  str = str + d.trim();
                  ++counter;
               }
            }
         }
      }

      return str.isEmpty()?str:(str.contains(",")?"destinations: " + str:"destination: " + str);
   }

   public String toString() {
      return "car";
   }
}
