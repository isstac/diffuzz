package com.graphhopper.routing.util;

import com.graphhopper.reader.OSMWay;
import com.graphhopper.routing.util.BikeCommonFlagEncoder;
import com.graphhopper.routing.util.PriorityCode;
import com.graphhopper.util.PMap;
import java.util.TreeMap;

public class RacingBikeFlagEncoder extends BikeCommonFlagEncoder {
   public RacingBikeFlagEncoder() {
      this(4, 2.0D, 0);
   }

   public RacingBikeFlagEncoder(PMap properties) {
      this((int)properties.getLong("speedBits", 4L), properties.getDouble("speedFactor", 2.0D), properties.getBool("turnCosts", false)?1:0);
      this.properties = properties;
      this.setBlockFords(properties.getBool("blockFords", true));
   }

   public RacingBikeFlagEncoder(String propertiesStr) {
      this(new PMap(propertiesStr));
   }

   public RacingBikeFlagEncoder(int speedBits, double speedFactor, int maxTurnCosts) {
      super(speedBits, speedFactor, maxTurnCosts);
      this.preferHighwayTags.add("road");
      this.preferHighwayTags.add("secondary");
      this.preferHighwayTags.add("secondary_link");
      this.preferHighwayTags.add("tertiary");
      this.preferHighwayTags.add("tertiary_link");
      this.preferHighwayTags.add("residential");
      this.setTrackTypeSpeed("grade1", 20);
      this.setTrackTypeSpeed("grade2", 10);
      this.setTrackTypeSpeed("grade3", 4);
      this.setTrackTypeSpeed("grade4", 4);
      this.setTrackTypeSpeed("grade5", 4);
      this.setSurfaceSpeed("paved", 20);
      this.setSurfaceSpeed("asphalt", 20);
      this.setSurfaceSpeed("cobblestone", 10);
      this.setSurfaceSpeed("cobblestone:flattened", 10);
      this.setSurfaceSpeed("sett", 10);
      this.setSurfaceSpeed("concrete", 20);
      this.setSurfaceSpeed("concrete:lanes", 16);
      this.setSurfaceSpeed("concrete:plates", 16);
      this.setSurfaceSpeed("paving_stones", 10);
      this.setSurfaceSpeed("paving_stones:30", 10);
      this.setSurfaceSpeed("unpaved", 2);
      this.setSurfaceSpeed("compacted", 2);
      this.setSurfaceSpeed("dirt", 2);
      this.setSurfaceSpeed("earth", 2);
      this.setSurfaceSpeed("fine_gravel", 4);
      this.setSurfaceSpeed("grass", 2);
      this.setSurfaceSpeed("grass_paver", 2);
      this.setSurfaceSpeed("gravel", 2);
      this.setSurfaceSpeed("ground", 2);
      this.setSurfaceSpeed("ice", 2);
      this.setSurfaceSpeed("metal", 2);
      this.setSurfaceSpeed("mud", 2);
      this.setSurfaceSpeed("pebblestone", 4);
      this.setSurfaceSpeed("salt", 2);
      this.setSurfaceSpeed("sand", 2);
      this.setSurfaceSpeed("wood", 2);
      this.setHighwaySpeed("cycleway", 18);
      this.setHighwaySpeed("path", 8);
      this.setHighwaySpeed("footway", 6);
      this.setHighwaySpeed("pedestrian", 6);
      this.setHighwaySpeed("road", 12);
      this.setHighwaySpeed("track", 2);
      this.setHighwaySpeed("service", 12);
      this.setHighwaySpeed("unclassified", 16);
      this.setHighwaySpeed("residential", 16);
      this.setHighwaySpeed("trunk", 20);
      this.setHighwaySpeed("trunk_link", 20);
      this.setHighwaySpeed("primary", 20);
      this.setHighwaySpeed("primary_link", 20);
      this.setHighwaySpeed("secondary", 20);
      this.setHighwaySpeed("secondary_link", 20);
      this.setHighwaySpeed("tertiary", 20);
      this.setHighwaySpeed("tertiary_link", 20);
      this.addPushingSection("path");
      this.addPushingSection("track");
      this.addPushingSection("footway");
      this.addPushingSection("pedestrian");
      this.addPushingSection("steps");
      this.setCyclingNetworkPreference("icn", PriorityCode.BEST.getValue());
      this.setCyclingNetworkPreference("ncn", PriorityCode.BEST.getValue());
      this.setCyclingNetworkPreference("rcn", PriorityCode.VERY_NICE.getValue());
      this.setCyclingNetworkPreference("lcn", PriorityCode.UNCHANGED.getValue());
      this.setCyclingNetworkPreference("mtb", PriorityCode.UNCHANGED.getValue());
      this.absoluteBarriers.add("kissing_gate");
      this.setAvoidSpeedLimit(81);
      this.setSpecificBicycleClass("roadcycling");
   }

   public int getVersion() {
      return 1;
   }

   void collect(OSMWay way, TreeMap weightToPrioMap) {
      super.collect(way, weightToPrioMap);
      String highway = way.getTag("highway");
      if("service".equals(highway)) {
         weightToPrioMap.put(Double.valueOf(40.0D), Integer.valueOf(PriorityCode.UNCHANGED.getValue()));
      } else if("track".equals(highway)) {
         String trackType = way.getTag("tracktype");
         if("grade1".equals(trackType)) {
            weightToPrioMap.put(Double.valueOf(110.0D), Integer.valueOf(PriorityCode.PREFER.getValue()));
         } else if(trackType == null || trackType.startsWith("grade")) {
            weightToPrioMap.put(Double.valueOf(110.0D), Integer.valueOf(PriorityCode.AVOID_AT_ALL_COSTS.getValue()));
         }
      }

   }

   boolean isPushingSection(OSMWay way) {
      String highway = way.getTag("highway");
      String trackType = way.getTag("tracktype");
      return way.hasTag("highway", this.pushingSections) || way.hasTag("railway", "platform") || "track".equals(highway) && trackType != null && !"grade1".equals(trackType);
   }

   boolean allowedSacScale(String sacScale) {
      return false;
   }

   public String toString() {
      return "racingbike";
   }
}
