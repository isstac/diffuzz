package com.graphhopper.routing.util;

import com.graphhopper.reader.OSMRelation;
import com.graphhopper.reader.OSMWay;
import com.graphhopper.routing.util.AbstractFlagEncoder;
import com.graphhopper.routing.util.EncodedDoubleValue;
import com.graphhopper.routing.util.EncodedValue;
import com.graphhopper.routing.util.PriorityCode;
import com.graphhopper.routing.util.PriorityWeighting;
import com.graphhopper.util.PMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class FootFlagEncoder extends AbstractFlagEncoder {
   static final int SLOW_SPEED = 2;
   static final int MEAN_SPEED = 5;
   static final int FERRY_SPEED = 10;
   private EncodedValue priorityWayEncoder;
   private EncodedValue relationCodeEncoder;
   protected HashSet sidewalks;
   private final Set safeHighwayTags;
   private final Set allowedHighwayTags;
   private final Set avoidHighwayTags;
   private final Map hikingNetworkToCode;

   public FootFlagEncoder() {
      this(4, 1.0D);
   }

   public FootFlagEncoder(PMap properties) {
      this((int)properties.getLong("speedBits", 4L), properties.getDouble("speedFactor", 1.0D));
      this.properties = properties;
      this.setBlockFords(properties.getBool("blockFords", true));
   }

   public FootFlagEncoder(String propertiesStr) {
      this(new PMap(propertiesStr));
   }

   public FootFlagEncoder(int speedBits, double speedFactor) {
      super(speedBits, speedFactor, 0);
      this.sidewalks = new HashSet();
      this.safeHighwayTags = new HashSet();
      this.allowedHighwayTags = new HashSet();
      this.avoidHighwayTags = new HashSet();
      this.hikingNetworkToCode = new HashMap();
      this.restrictions.addAll(Arrays.asList(new String[]{"foot", "access"}));
      this.restrictedValues.add("private");
      this.restrictedValues.add("no");
      this.restrictedValues.add("restricted");
      this.restrictedValues.add("military");
      this.intendedValues.add("yes");
      this.intendedValues.add("designated");
      this.intendedValues.add("official");
      this.intendedValues.add("permissive");
      this.sidewalks.add("yes");
      this.sidewalks.add("both");
      this.sidewalks.add("left");
      this.sidewalks.add("right");
      this.setBlockByDefault(false);
      this.potentialBarriers.add("gate");
      this.acceptedRailways.add("platform");
      this.safeHighwayTags.add("footway");
      this.safeHighwayTags.add("path");
      this.safeHighwayTags.add("steps");
      this.safeHighwayTags.add("pedestrian");
      this.safeHighwayTags.add("living_street");
      this.safeHighwayTags.add("track");
      this.safeHighwayTags.add("residential");
      this.safeHighwayTags.add("service");
      this.avoidHighwayTags.add("trunk");
      this.avoidHighwayTags.add("trunk_link");
      this.avoidHighwayTags.add("primary");
      this.avoidHighwayTags.add("primary_link");
      this.avoidHighwayTags.add("secondary");
      this.avoidHighwayTags.add("secondary_link");
      this.avoidHighwayTags.add("tertiary");
      this.avoidHighwayTags.add("tertiary_link");
      this.allowedHighwayTags.addAll(this.safeHighwayTags);
      this.allowedHighwayTags.addAll(this.avoidHighwayTags);
      this.allowedHighwayTags.add("cycleway");
      this.allowedHighwayTags.add("unclassified");
      this.allowedHighwayTags.add("road");
      this.hikingNetworkToCode.put("iwn", Integer.valueOf(PriorityCode.BEST.getValue()));
      this.hikingNetworkToCode.put("nwn", Integer.valueOf(PriorityCode.BEST.getValue()));
      this.hikingNetworkToCode.put("rwn", Integer.valueOf(PriorityCode.VERY_NICE.getValue()));
      this.hikingNetworkToCode.put("lwn", Integer.valueOf(PriorityCode.VERY_NICE.getValue()));
      this.maxPossibleSpeed = 10;
   }

   public int getVersion() {
      return 1;
   }

   public int defineWayBits(int index, int shift) {
      shift = super.defineWayBits(index, shift);
      this.speedEncoder = new EncodedDoubleValue("Speed", shift, this.speedBits, this.speedFactor, 5L, this.maxPossibleSpeed);
      shift += this.speedEncoder.getBits();
      this.priorityWayEncoder = new EncodedValue("PreferWay", shift, 3, 1.0D, 0L, 7);
      shift += this.priorityWayEncoder.getBits();
      return shift;
   }

   public int defineRelationBits(int index, int shift) {
      this.relationCodeEncoder = new EncodedValue("RelationCode", shift, 3, 1.0D, 0L, 7);
      return shift + this.relationCodeEncoder.getBits();
   }

   public int defineTurnBits(int index, int shift) {
      return shift;
   }

   public boolean isTurnRestricted(long flag) {
      return false;
   }

   public double getTurnCost(long flag) {
      return 0.0D;
   }

   public long getTurnFlags(boolean restricted, double costs) {
      return 0L;
   }

   public long acceptWay(OSMWay way) {
      String highwayValue = way.getTag("highway");
      String sacScale;
      if(highwayValue == null) {
         if(way.hasTag("route", this.ferries)) {
            sacScale = way.getTag("foot");
            if(sacScale == null || "yes".equals(sacScale)) {
               return this.acceptBit | this.ferryBit;
            }
         }

         return way.hasTag("railway", "platform")?this.acceptBit:0L;
      } else {
         sacScale = way.getTag("sac_scale");
         return sacScale != null && !"hiking".equals(sacScale) && !"mountain_hiking".equals(sacScale) && !"demanding_mountain_hiking".equals(sacScale) && !"alpine_hiking".equals(sacScale)?0L:(way.hasTag("sidewalk", this.sidewalks)?this.acceptBit:(way.hasTag("foot", this.intendedValues)?this.acceptBit:(!this.allowedHighwayTags.contains(highwayValue)?0L:(way.hasTag("motorroad", "yes")?0L:(this.isBlockFords() && (way.hasTag("highway", "ford") || way.hasTag("ford", new String[0]))?0L:(way.hasTag(this.restrictions, this.restrictedValues)?0L:(way.hasTag("railway", new String[0]) && !way.hasTag("railway", this.acceptedRailways)?0L:this.acceptBit)))))));
      }
   }

   public long handleRelationTags(OSMRelation relation, long oldRelationFlags) {
      int code = 0;
      if(!relation.hasTag("route", "hiking") && !relation.hasTag("route", "foot")) {
         if(relation.hasTag("route", "ferry")) {
            code = PriorityCode.AVOID_IF_POSSIBLE.getValue();
         }
      } else {
         Integer oldCode = (Integer)this.hikingNetworkToCode.get(relation.getTag("network"));
         if(oldCode != null) {
            code = oldCode.intValue();
         }
      }

      int oldCode1 = (int)this.relationCodeEncoder.getValue(oldRelationFlags);
      return oldCode1 < code?this.relationCodeEncoder.setValue(0L, (long)code):oldRelationFlags;
   }

   public long handleWayTags(OSMWay way, long allowed, long relationFlags) {
      if(!this.isAccept(allowed)) {
         return 0L;
      } else {
         long encoded = 0L;
         if(!this.isFerry(allowed)) {
            String priorityFromRelation = way.getTag("sac_scale");
            if(priorityFromRelation != null) {
               if("hiking".equals(priorityFromRelation)) {
                  encoded = this.speedEncoder.setDoubleValue(encoded, 5.0D);
               } else {
                  encoded = this.speedEncoder.setDoubleValue(encoded, 2.0D);
               }
            } else {
               encoded = this.speedEncoder.setDoubleValue(encoded, 5.0D);
            }

            encoded |= this.directionBitMask;
            boolean isRoundabout = way.hasTag("junction", "roundabout");
            if(isRoundabout) {
               encoded = this.setBool(encoded, 2, true);
            }
         } else {
            encoded |= this.handleFerryTags(way, 2.0D, 5.0D, 10.0D);
            encoded |= this.directionBitMask;
         }

         int priorityFromRelation1 = 0;
         if(relationFlags != 0L) {
            priorityFromRelation1 = (int)this.relationCodeEncoder.getValue(relationFlags);
         }

         encoded = this.priorityWayEncoder.setValue(encoded, (long)this.handlePriority(way, priorityFromRelation1));
         return encoded;
      }
   }

   public double getDouble(long flags, int key) {
      switch(key) {
      case 101:
         return (double)this.priorityWayEncoder.getValue(flags) / (double)PriorityCode.BEST.getValue();
      default:
         return super.getDouble(flags, key);
      }
   }

   protected int handlePriority(OSMWay way, int priorityFromRelation) {
      TreeMap weightToPrioMap = new TreeMap();
      if(priorityFromRelation == 0) {
         weightToPrioMap.put(Double.valueOf(0.0D), Integer.valueOf(PriorityCode.UNCHANGED.getValue()));
      } else {
         weightToPrioMap.put(Double.valueOf(110.0D), Integer.valueOf(priorityFromRelation));
      }

      this.collect(way, weightToPrioMap);
      return ((Integer)weightToPrioMap.lastEntry().getValue()).intValue();
   }

   void collect(OSMWay way, TreeMap weightToPrioMap) {
      String highway = way.getTag("highway");
      if(way.hasTag("foot", "designated")) {
         weightToPrioMap.put(Double.valueOf(100.0D), Integer.valueOf(PriorityCode.PREFER.getValue()));
      }

      double maxSpeed = this.getMaxSpeed(way);
      if(this.safeHighwayTags.contains(highway) || maxSpeed > 0.0D && maxSpeed <= 20.0D) {
         weightToPrioMap.put(Double.valueOf(40.0D), Integer.valueOf(PriorityCode.PREFER.getValue()));
         if(way.hasTag("tunnel", this.intendedValues)) {
            if(way.hasTag("sidewalk", "no")) {
               weightToPrioMap.put(Double.valueOf(40.0D), Integer.valueOf(PriorityCode.REACH_DEST.getValue()));
            } else {
               weightToPrioMap.put(Double.valueOf(40.0D), Integer.valueOf(PriorityCode.UNCHANGED.getValue()));
            }
         }
      } else if(maxSpeed > 50.0D || this.avoidHighwayTags.contains(highway)) {
         if(way.hasTag("sidewalk", "no")) {
            weightToPrioMap.put(Double.valueOf(45.0D), Integer.valueOf(PriorityCode.WORST.getValue()));
         } else {
            weightToPrioMap.put(Double.valueOf(45.0D), Integer.valueOf(PriorityCode.REACH_DEST.getValue()));
         }
      }

      if(way.hasTag("bicycle", "official") || way.hasTag("bicycle", "designated")) {
         weightToPrioMap.put(Double.valueOf(44.0D), Integer.valueOf(PriorityCode.AVOID_IF_POSSIBLE.getValue()));
      }

   }

   public boolean supports(Class feature) {
      return super.supports(feature)?true:PriorityWeighting.class.isAssignableFrom(feature);
   }

   public String toString() {
      return "foot";
   }
}
