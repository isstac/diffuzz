package com.graphhopper.routing.util;

import com.graphhopper.reader.OSMRelation;
import com.graphhopper.reader.OSMWay;
import com.graphhopper.routing.util.AbstractFlagEncoder;
import com.graphhopper.routing.util.EncodedDoubleValue;
import com.graphhopper.routing.util.EncodedValue;
import com.graphhopper.routing.util.PriorityCode;
import com.graphhopper.routing.util.PriorityWeighting;
import com.graphhopper.util.Helper;
import com.graphhopper.util.InstructionAnnotation;
import com.graphhopper.util.Translation;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class BikeCommonFlagEncoder extends AbstractFlagEncoder {
   public static final int K_UNPAVED = 100;
   protected static final int PUSHING_SECTION_SPEED = 4;
   private long unpavedBit = 0L;
   protected final HashSet pushingSections = new HashSet();
   protected final HashSet oppositeLanes = new HashSet();
   protected final Set preferHighwayTags = new HashSet();
   protected final Set avoidHighwayTags = new HashSet();
   protected final Set unpavedSurfaceTags = new HashSet();
   private final Map trackTypeSpeeds = new HashMap();
   private final Map surfaceSpeeds = new HashMap();
   private final Set roadValues = new HashSet();
   private final Map highwaySpeeds = new HashMap();
   private final Map bikeNetworkToCode = new HashMap();
   protected EncodedValue relationCodeEncoder;
   private EncodedValue wayTypeEncoder;
   EncodedValue priorityWayEncoder;
   private int avoidSpeedLimit;
   private String specificBicycleClass;

   protected BikeCommonFlagEncoder(int speedBits, double speedFactor, int maxTurnCosts) {
      super(speedBits, speedFactor, maxTurnCosts);
      this.restrictions.addAll(Arrays.asList(new String[]{"bicycle", "access"}));
      this.restrictedValues.add("private");
      this.restrictedValues.add("no");
      this.restrictedValues.add("restricted");
      this.restrictedValues.add("military");
      this.intendedValues.add("yes");
      this.intendedValues.add("designated");
      this.intendedValues.add("official");
      this.intendedValues.add("permissive");
      this.oppositeLanes.add("opposite");
      this.oppositeLanes.add("opposite_lane");
      this.oppositeLanes.add("opposite_track");
      this.setBlockByDefault(false);
      this.potentialBarriers.add("gate");
      this.potentialBarriers.add("swing_gate");
      this.absoluteBarriers.add("stile");
      this.absoluteBarriers.add("turnstile");
      this.acceptedRailways.add("platform");
      this.unpavedSurfaceTags.add("unpaved");
      this.unpavedSurfaceTags.add("gravel");
      this.unpavedSurfaceTags.add("ground");
      this.unpavedSurfaceTags.add("dirt");
      this.unpavedSurfaceTags.add("grass");
      this.unpavedSurfaceTags.add("compacted");
      this.unpavedSurfaceTags.add("earth");
      this.unpavedSurfaceTags.add("fine_gravel");
      this.unpavedSurfaceTags.add("grass_paver");
      this.unpavedSurfaceTags.add("ice");
      this.unpavedSurfaceTags.add("mud");
      this.unpavedSurfaceTags.add("salt");
      this.unpavedSurfaceTags.add("sand");
      this.unpavedSurfaceTags.add("wood");
      this.roadValues.add("living_street");
      this.roadValues.add("road");
      this.roadValues.add("service");
      this.roadValues.add("unclassified");
      this.roadValues.add("residential");
      this.roadValues.add("trunk");
      this.roadValues.add("trunk_link");
      this.roadValues.add("primary");
      this.roadValues.add("primary_link");
      this.roadValues.add("secondary");
      this.roadValues.add("secondary_link");
      this.roadValues.add("tertiary");
      this.roadValues.add("tertiary_link");
      this.maxPossibleSpeed = 30;
      this.setTrackTypeSpeed("grade1", 18);
      this.setTrackTypeSpeed("grade2", 12);
      this.setTrackTypeSpeed("grade3", 8);
      this.setTrackTypeSpeed("grade4", 6);
      this.setTrackTypeSpeed("grade5", 4);
      this.setSurfaceSpeed("paved", 18);
      this.setSurfaceSpeed("asphalt", 18);
      this.setSurfaceSpeed("cobblestone", 8);
      this.setSurfaceSpeed("cobblestone:flattened", 10);
      this.setSurfaceSpeed("sett", 10);
      this.setSurfaceSpeed("concrete", 18);
      this.setSurfaceSpeed("concrete:lanes", 16);
      this.setSurfaceSpeed("concrete:plates", 16);
      this.setSurfaceSpeed("paving_stones", 12);
      this.setSurfaceSpeed("paving_stones:30", 12);
      this.setSurfaceSpeed("unpaved", 14);
      this.setSurfaceSpeed("compacted", 16);
      this.setSurfaceSpeed("dirt", 10);
      this.setSurfaceSpeed("earth", 12);
      this.setSurfaceSpeed("fine_gravel", 18);
      this.setSurfaceSpeed("grass", 8);
      this.setSurfaceSpeed("grass_paver", 8);
      this.setSurfaceSpeed("gravel", 12);
      this.setSurfaceSpeed("ground", 12);
      this.setSurfaceSpeed("ice", 2);
      this.setSurfaceSpeed("metal", 10);
      this.setSurfaceSpeed("mud", 10);
      this.setSurfaceSpeed("pebblestone", 16);
      this.setSurfaceSpeed("salt", 6);
      this.setSurfaceSpeed("sand", 6);
      this.setSurfaceSpeed("wood", 6);
      this.setHighwaySpeed("living_street", 6);
      this.setHighwaySpeed("steps", 2);
      this.setHighwaySpeed("cycleway", 18);
      this.setHighwaySpeed("path", 12);
      this.setHighwaySpeed("footway", 6);
      this.setHighwaySpeed("pedestrian", 6);
      this.setHighwaySpeed("track", 12);
      this.setHighwaySpeed("service", 14);
      this.setHighwaySpeed("residential", 18);
      this.setHighwaySpeed("unclassified", 16);
      this.setHighwaySpeed("road", 12);
      this.setHighwaySpeed("trunk", 18);
      this.setHighwaySpeed("trunk_link", 18);
      this.setHighwaySpeed("primary", 18);
      this.setHighwaySpeed("primary_link", 18);
      this.setHighwaySpeed("secondary", 18);
      this.setHighwaySpeed("secondary_link", 18);
      this.setHighwaySpeed("tertiary", 18);
      this.setHighwaySpeed("tertiary_link", 18);
      this.setHighwaySpeed("motorway", 18);
      this.setHighwaySpeed("motorway_link", 18);
      this.avoidHighwayTags.add("motorway");
      this.avoidHighwayTags.add("motorway_link");
      this.setCyclingNetworkPreference("icn", PriorityCode.BEST.getValue());
      this.setCyclingNetworkPreference("ncn", PriorityCode.BEST.getValue());
      this.setCyclingNetworkPreference("rcn", PriorityCode.VERY_NICE.getValue());
      this.setCyclingNetworkPreference("lcn", PriorityCode.PREFER.getValue());
      this.setCyclingNetworkPreference("mtb", PriorityCode.UNCHANGED.getValue());
      this.setCyclingNetworkPreference("deprecated", PriorityCode.AVOID_AT_ALL_COSTS.getValue());
      this.setAvoidSpeedLimit(71);
   }

   public int getVersion() {
      return 1;
   }

   public int defineWayBits(int index, int shift) {
      shift = super.defineWayBits(index, shift);
      this.speedEncoder = new EncodedDoubleValue("Speed", shift, this.speedBits, this.speedFactor, (long)((Integer)this.highwaySpeeds.get("cycleway")).intValue(), this.maxPossibleSpeed);
      shift += this.speedEncoder.getBits();
      this.unpavedBit = 1L << shift++;
      this.wayTypeEncoder = new EncodedValue("WayType", shift, 2, 1.0D, 0L, 3, true);
      shift += this.wayTypeEncoder.getBits();
      this.priorityWayEncoder = new EncodedValue("PreferWay", shift, 3, 1.0D, 0L, 7);
      shift += this.priorityWayEncoder.getBits();
      return shift;
   }

   public int defineRelationBits(int index, int shift) {
      this.relationCodeEncoder = new EncodedValue("RelationCode", shift, 3, 1.0D, 0L, 7);
      return shift + this.relationCodeEncoder.getBits();
   }

   public long acceptWay(OSMWay way) {
      String highwayValue = way.getTag("highway");
      String sacScale;
      if(highwayValue == null) {
         if(way.hasTag("route", this.ferries)) {
            sacScale = way.getTag("bicycle");
            if(sacScale == null && !way.hasTag("foot", new String[0]) || "yes".equals(sacScale)) {
               return this.acceptBit | this.ferryBit;
            }
         }

         if(way.hasTag("railway", "platform")) {
            return this.acceptBit;
         } else {
            return 0L;
         }
      } else if(!this.highwaySpeeds.containsKey(highwayValue)) {
         return 0L;
      } else if(way.hasTag("bicycle", this.intendedValues)) {
         return this.acceptBit;
      } else if(!"motorway".equals(highwayValue) && !"motorway_link".equals(highwayValue)) {
         if(way.hasTag("motorroad", "yes")) {
            return 0L;
         } else if(this.isBlockFords() && (way.hasTag("highway", "ford") || way.hasTag("ford", new String[0]))) {
            return 0L;
         } else if(way.hasTag(this.restrictions, this.restrictedValues)) {
            return 0L;
         } else if(way.hasTag("railway", new String[0]) && !way.hasTag("railway", this.acceptedRailways)) {
            return 0L;
         } else {
            sacScale = way.getTag("sac_scale");
            if(sacScale != null) {
               if(way.hasTag("highway", "cycleway") && way.hasTag("sac_scale", "hiking")) {
                  return this.acceptBit;
               }

               if(!this.allowedSacScale(sacScale)) {
                  return 0L;
               }
            }

            return this.acceptBit;
         }
      } else {
         return 0L;
      }
   }

   boolean allowedSacScale(String sacScale) {
      return "hiking".equals(sacScale);
   }

   public long handleRelationTags(OSMRelation relation, long oldRelationFlags) {
      int code = 0;
      if(relation.hasTag("route", "bicycle")) {
         Integer oldCode = (Integer)this.bikeNetworkToCode.get(relation.getTag("network"));
         if(oldCode != null) {
            code = oldCode.intValue();
         }
      } else if(relation.hasTag("route", "ferry")) {
         code = PriorityCode.AVOID_IF_POSSIBLE.getValue();
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
            double priorityFromRelation = (double)this.getSpeed(way);
            priorityFromRelation = this.applyMaxSpeed(way, priorityFromRelation, false);
            encoded = this.handleSpeed(way, priorityFromRelation, encoded);
            encoded = this.handleBikeRelated(way, encoded, relationFlags > (long)PriorityCode.UNCHANGED.getValue());
            boolean isRoundabout = way.hasTag("junction", "roundabout");
            if(isRoundabout) {
               encoded = this.setBool(encoded, 2, true);
            }
         } else {
            encoded = this.handleFerryTags(way, (double)((Integer)this.highwaySpeeds.get("living_street")).intValue(), (double)((Integer)this.highwaySpeeds.get("track")).intValue(), (double)((Integer)this.highwaySpeeds.get("primary")).intValue());
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

   int getSpeed(OSMWay way) {
      int speed = 4;
      String highwayTag = way.getTag("highway");
      Integer highwaySpeed = (Integer)this.highwaySpeeds.get(highwayTag);
      String s = way.getTag("surface");
      if(!Helper.isEmpty(s)) {
         Integer tt = (Integer)this.surfaceSpeeds.get(s);
         if(tt != null) {
            speed = tt.intValue();
            if(highwaySpeed != null && tt.intValue() > highwaySpeed.intValue()) {
               if(this.pushingSections.contains(highwayTag)) {
                  speed = highwaySpeed.intValue();
               } else {
                  speed = tt.intValue();
               }
            }
         }
      } else {
         String tt1 = way.getTag("tracktype");
         if(!Helper.isEmpty(tt1)) {
            Integer tInt = (Integer)this.trackTypeSpeeds.get(tt1);
            if(tInt != null) {
               speed = tInt.intValue();
            }
         } else if(highwaySpeed != null) {
            if(!way.hasTag("service", new String[0])) {
               speed = highwaySpeed.intValue();
            } else {
               speed = ((Integer)this.highwaySpeeds.get("living_street")).intValue();
            }
         }
      }

      if(speed > 4 && !way.hasTag("bicycle", this.intendedValues) && way.hasTag("highway", this.pushingSections)) {
         if(way.hasTag("highway", "steps")) {
            speed = 2;
         } else {
            speed = 4;
         }
      }

      return speed;
   }

   public InstructionAnnotation getAnnotation(long flags, Translation tr) {
      byte paveType = 0;
      if(this.isBool(flags, 100)) {
         paveType = 1;
      }

      int wayType = (int)this.wayTypeEncoder.getValue(flags);
      String wayName = this.getWayName(paveType, wayType, tr);
      return new InstructionAnnotation(0, wayName);
   }

   String getWayName(int pavementType, int wayType, Translation tr) {
      String pavementName = "";
      if(pavementType == 1) {
         pavementName = tr.tr("unpaved", new Object[0]);
      }

      String wayTypeName = "";
      switch(wayType) {
      case 0:
         wayTypeName = tr.tr("road", new Object[0]);
         break;
      case 1:
         wayTypeName = tr.tr("off_bike", new Object[0]);
         break;
      case 2:
         wayTypeName = tr.tr("cycleway", new Object[0]);
         break;
      case 3:
         wayTypeName = tr.tr("way", new Object[0]);
      }

      return pavementName.isEmpty()?(wayType != 0 && wayType != 3?wayTypeName:""):(wayTypeName.isEmpty()?pavementName:wayTypeName + ", " + pavementName);
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

   private PriorityCode convertCallValueToPriority(String tagvalue) {
      int classvalue;
      try {
         classvalue = Integer.parseInt(tagvalue);
      } catch (NumberFormatException var4) {
         return PriorityCode.UNCHANGED;
      }

      switch(classvalue) {
      case -3:
         return PriorityCode.AVOID_AT_ALL_COSTS;
      case -2:
         return PriorityCode.REACH_DEST;
      case -1:
         return PriorityCode.AVOID_IF_POSSIBLE;
      case 0:
         return PriorityCode.UNCHANGED;
      case 1:
         return PriorityCode.PREFER;
      case 2:
         return PriorityCode.VERY_NICE;
      case 3:
         return PriorityCode.BEST;
      default:
         return PriorityCode.UNCHANGED;
      }
   }

   void collect(OSMWay way, TreeMap weightToPrioMap) {
      String service = way.getTag("service");
      String highway = way.getTag("highway");
      if(way.hasTag("bicycle", "designated")) {
         weightToPrioMap.put(Double.valueOf(100.0D), Integer.valueOf(PriorityCode.PREFER.getValue()));
      }

      if("cycleway".equals(highway)) {
         weightToPrioMap.put(Double.valueOf(100.0D), Integer.valueOf(PriorityCode.VERY_NICE.getValue()));
      }

      double maxSpeed = this.getMaxSpeed(way);
      if(this.preferHighwayTags.contains(highway) || maxSpeed > 0.0D && maxSpeed <= 30.0D) {
         if(maxSpeed < (double)this.avoidSpeedLimit) {
            weightToPrioMap.put(Double.valueOf(40.0D), Integer.valueOf(PriorityCode.PREFER.getValue()));
            if(way.hasTag("tunnel", this.intendedValues)) {
               weightToPrioMap.put(Double.valueOf(40.0D), Integer.valueOf(PriorityCode.UNCHANGED.getValue()));
            }
         }
      } else if(this.avoidHighwayTags.contains(highway) || maxSpeed >= (double)this.avoidSpeedLimit && !"track".equals(highway)) {
         weightToPrioMap.put(Double.valueOf(50.0D), Integer.valueOf(PriorityCode.REACH_DEST.getValue()));
         if(way.hasTag("tunnel", this.intendedValues)) {
            weightToPrioMap.put(Double.valueOf(50.0D), Integer.valueOf(PriorityCode.AVOID_AT_ALL_COSTS.getValue()));
         }
      }

      if(this.pushingSections.contains(highway) || way.hasTag("bicycle", "use_sidepath") || "parking_aisle".equals(service)) {
         if(way.hasTag("bicycle", "yes")) {
            weightToPrioMap.put(Double.valueOf(100.0D), Integer.valueOf(PriorityCode.UNCHANGED.getValue()));
         } else {
            weightToPrioMap.put(Double.valueOf(50.0D), Integer.valueOf(PriorityCode.AVOID_IF_POSSIBLE.getValue()));
         }
      }

      if(way.hasTag("railway", "tram")) {
         weightToPrioMap.put(Double.valueOf(50.0D), Integer.valueOf(PriorityCode.AVOID_AT_ALL_COSTS.getValue()));
      }

      String classBicycleSpecific = way.getTag(this.specificBicycleClass);
      if(classBicycleSpecific != null) {
         weightToPrioMap.put(Double.valueOf(100.0D), Integer.valueOf(this.convertCallValueToPriority(classBicycleSpecific).getValue()));
      } else {
         String classBicycle = way.getTag("class:bicycle");
         if(classBicycle != null) {
            weightToPrioMap.put(Double.valueOf(100.0D), Integer.valueOf(this.convertCallValueToPriority(classBicycle).getValue()));
         }
      }

   }

   long handleBikeRelated(OSMWay way, long encoded, boolean partOfCycleRelation) {
      String surfaceTag = way.getTag("surface");
      String highway = way.getTag("highway");
      String trackType = way.getTag("tracktype");
      BikeCommonFlagEncoder.WayType wayType = BikeCommonFlagEncoder.WayType.OTHER_SMALL_WAY;
      boolean isPusingSection = this.isPushingSection(way);
      if(isPusingSection && !partOfCycleRelation || "steps".equals(highway)) {
         wayType = BikeCommonFlagEncoder.WayType.PUSHING_SECTION;
      }

      if("track".equals(highway) && (trackType == null || !"grade1".equals(trackType)) || "path".equals(highway) && surfaceTag == null || this.unpavedSurfaceTags.contains(surfaceTag)) {
         encoded = this.setBool(encoded, 100, true);
      }

      if(way.hasTag("bicycle", this.intendedValues)) {
         if(isPusingSection && !way.hasTag("bicycle", "designated")) {
            wayType = BikeCommonFlagEncoder.WayType.OTHER_SMALL_WAY;
         } else {
            wayType = BikeCommonFlagEncoder.WayType.CYCLEWAY;
         }
      } else if("cycleway".equals(highway)) {
         wayType = BikeCommonFlagEncoder.WayType.CYCLEWAY;
      } else if(this.roadValues.contains(highway)) {
         wayType = BikeCommonFlagEncoder.WayType.ROAD;
      }

      return this.wayTypeEncoder.setValue(encoded, (long)wayType.getValue());
   }

   public long setBool(long flags, int key, boolean value) {
      switch(key) {
      case 100:
         return value?flags | this.unpavedBit:flags & ~this.unpavedBit;
      default:
         return super.setBool(flags, key, value);
      }
   }

   public boolean isBool(long flags, int key) {
      switch(key) {
      case 100:
         return (flags & this.unpavedBit) != 0L;
      default:
         return super.isBool(flags, key);
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

   boolean isPushingSection(OSMWay way) {
      return way.hasTag("highway", this.pushingSections) || way.hasTag("railway", "platform");
   }

   protected long handleSpeed(OSMWay way, double speed, long encoded) {
      encoded = this.setSpeed(encoded, speed);
      boolean isOneway = way.hasTag("oneway", this.oneways) || way.hasTag("oneway:bicycle", this.oneways) || way.hasTag("vehicle:backward", new String[0]) || way.hasTag("vehicle:forward", new String[0]) || way.hasTag("bicycle:forward", new String[0]);
      if((isOneway || way.hasTag("junction", "roundabout")) && !way.hasTag("oneway:bicycle", "no") && !way.hasTag("bicycle:backward", new String[0]) && !way.hasTag("cycleway", this.oppositeLanes)) {
         boolean isBackward = way.hasTag("oneway", "-1") || way.hasTag("oneway:bicycle", "-1") || way.hasTag("vehicle:forward", "no") || way.hasTag("bicycle:forward", "no");
         if(isBackward) {
            encoded |= this.backwardBit;
         } else {
            encoded |= this.forwardBit;
         }
      } else {
         encoded |= this.directionBitMask;
      }

      return encoded;
   }

   protected void setHighwaySpeed(String highway, int speed) {
      this.highwaySpeeds.put(highway, Integer.valueOf(speed));
   }

   protected int getHighwaySpeed(String key) {
      return ((Integer)this.highwaySpeeds.get(key)).intValue();
   }

   void setTrackTypeSpeed(String tracktype, int speed) {
      this.trackTypeSpeeds.put(tracktype, Integer.valueOf(speed));
   }

   void setSurfaceSpeed(String surface, int speed) {
      this.surfaceSpeeds.put(surface, Integer.valueOf(speed));
   }

   void setCyclingNetworkPreference(String network, int code) {
      this.bikeNetworkToCode.put(network, Integer.valueOf(code));
   }

   void addPushingSection(String highway) {
      this.pushingSections.add(highway);
   }

   public boolean supports(Class feature) {
      return super.supports(feature)?true:PriorityWeighting.class.isAssignableFrom(feature);
   }

   public void setAvoidSpeedLimit(int limit) {
      this.avoidSpeedLimit = limit;
   }

   public void setSpecificBicycleClass(String subkey) {
      this.specificBicycleClass = "class:bicycle:" + subkey.toString();
   }

   private static enum WayType {
      ROAD(0),
      PUSHING_SECTION(1),
      CYCLEWAY(2),
      OTHER_SMALL_WAY(3);

      private final int value;

      private WayType(int value) {
         this.value = value;
      }

      public int getValue() {
         return this.value;
      }
   }
}
