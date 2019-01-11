package com.graphhopper.routing.util;

import com.graphhopper.reader.OSMWay;
import com.graphhopper.routing.util.BikeCommonFlagEncoder;
import com.graphhopper.util.PMap;

public class BikeFlagEncoder extends BikeCommonFlagEncoder {
   public BikeFlagEncoder() {
      this(4, 2.0D, 0);
   }

   public BikeFlagEncoder(String propertiesString) {
      this(new PMap(propertiesString));
   }

   public BikeFlagEncoder(PMap properties) {
      this((int)properties.getLong("speedBits", 4L), (double)properties.getLong("speedFactor", 2L), properties.getBool("turnCosts", false)?1:0);
      this.properties = properties;
      this.setBlockFords(properties.getBool("blockFords", true));
   }

   public BikeFlagEncoder(int speedBits, double speedFactor, int maxTurnCosts) {
      super(speedBits, speedFactor, maxTurnCosts);
      this.addPushingSection("path");
      this.addPushingSection("footway");
      this.addPushingSection("pedestrian");
      this.addPushingSection("steps");
      this.avoidHighwayTags.add("trunk");
      this.avoidHighwayTags.add("trunk_link");
      this.avoidHighwayTags.add("primary");
      this.avoidHighwayTags.add("primary_link");
      this.avoidHighwayTags.add("secondary");
      this.avoidHighwayTags.add("secondary_link");
      this.preferHighwayTags.add("service");
      this.preferHighwayTags.add("tertiary");
      this.preferHighwayTags.add("tertiary_link");
      this.preferHighwayTags.add("residential");
      this.preferHighwayTags.add("unclassified");
      this.absoluteBarriers.add("kissing_gate");
      this.setSpecificBicycleClass("touring");
   }

   public int getVersion() {
      return 1;
   }

   boolean isPushingSection(OSMWay way) {
      String highway = way.getTag("highway");
      String trackType = way.getTag("tracktype");
      return way.hasTag("highway", this.pushingSections) || way.hasTag("railway", "platform") || "track".equals(highway) && trackType != null && !"grade1".equals(trackType);
   }

   public String toString() {
      return "bike";
   }
}
