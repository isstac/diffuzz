package com.graphhopper.routing.util;

import com.graphhopper.reader.OSMNode;
import com.graphhopper.reader.OSMRelation;
import com.graphhopper.reader.OSMWay;
import com.graphhopper.routing.util.EncodedDoubleValue;
import com.graphhopper.routing.util.EncodedValue;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.TurnCostEncoder;
import com.graphhopper.routing.util.TurnWeighting;
import com.graphhopper.util.BitUtil;
import com.graphhopper.util.EdgeExplorer;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.Helper;
import com.graphhopper.util.InstructionAnnotation;
import com.graphhopper.util.PMap;
import com.graphhopper.util.Translation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

public abstract class AbstractFlagEncoder implements FlagEncoder, TurnCostEncoder {
//   private static final Logger logger = LoggerFactory.getLogger(AbstractFlagEncoder.class);
   protected static final int K_FORWARD = 0;
   protected static final int K_BACKWARD = 1;
   private long nodeBitMask;
   private long wayBitMask;
   private long relBitMask;
   protected long forwardBit;
   protected long backwardBit;
   protected long directionBitMask;
   protected long roundaboutBit;
   protected EncodedDoubleValue speedEncoder;
   protected long acceptBit;
   protected long ferryBit;
   protected PMap properties;
   protected int maxPossibleSpeed;
   private EncodedValue turnCostEncoder;
   private long turnRestrictionBit;
   private final int maxTurnCosts;
   protected EdgeExplorer edgeOutExplorer;
   protected EdgeExplorer edgeInExplorer;
   protected final List restrictions;
   protected final Set intendedValues;
   protected final Set restrictedValues;
   protected final Set ferries;
   protected final Set oneways;
   protected final Set acceptedRailways;
   protected final Set absoluteBarriers;
   protected final Set potentialBarriers;
   private boolean blockByDefault;
   private boolean blockFords;
   protected final int speedBits;
   protected final double speedFactor;
   private boolean registered;

   public AbstractFlagEncoder(PMap properties) {
      this.restrictions = new ArrayList(5);
      this.intendedValues = new HashSet(5);
      this.restrictedValues = new HashSet(5);
      this.ferries = new HashSet(5);
      this.oneways = new HashSet(5);
      this.acceptedRailways = new HashSet(5);
      this.absoluteBarriers = new HashSet(5);
      this.potentialBarriers = new HashSet(5);
      this.blockByDefault = true;
      this.blockFords = true;
      throw new RuntimeException("This method must be overridden in derived classes");
   }

   public AbstractFlagEncoder(String propertiesStr) {
      this(new PMap(propertiesStr));
   }

   protected AbstractFlagEncoder(int speedBits, double speedFactor, int maxTurnCosts) {
      this.restrictions = new ArrayList(5);
      this.intendedValues = new HashSet(5);
      this.restrictedValues = new HashSet(5);
      this.ferries = new HashSet(5);
      this.oneways = new HashSet(5);
      this.acceptedRailways = new HashSet(5);
      this.absoluteBarriers = new HashSet(5);
      this.potentialBarriers = new HashSet(5);
      this.blockByDefault = true;
      this.blockFords = true;
      this.maxTurnCosts = maxTurnCosts <= 0?0:maxTurnCosts;
      this.speedBits = speedBits;
      this.speedFactor = speedFactor;
      this.oneways.add("yes");
      this.oneways.add("true");
      this.oneways.add("1");
      this.oneways.add("-1");
      this.ferries.add("shuttle_train");
      this.ferries.add("ferry");
      this.acceptedRailways.add("tram");
      this.acceptedRailways.add("abandoned");
      this.acceptedRailways.add("abandoned_tram");
      this.acceptedRailways.add("disused");
      this.acceptedRailways.add("dismantled");
      this.acceptedRailways.add("razed");
      this.acceptedRailways.add("historic");
      this.acceptedRailways.add("obliterated");
   }

   public void setRegistered(boolean registered) {
      this.registered = registered;
   }

   public boolean isRegistered() {
      return this.registered;
   }

   public void setBlockByDefault(boolean blockByDefault) {
      this.blockByDefault = blockByDefault;
   }

   public void setBlockFords(boolean blockFords) {
      this.blockFords = blockFords;
   }

   public boolean isBlockFords() {
      return this.blockFords;
   }

   public int defineNodeBits(int index, int shift) {
      return shift;
   }

   public int defineWayBits(int index, int shift) {
      if(this.isRegistered()) {
         throw new IllegalStateException("You must not register a FlagEncoder (" + this.toString() + ") twice!");
      } else {
         this.setRegistered(true);
         this.forwardBit = 1L << shift;
         this.backwardBit = 2L << shift;
         this.directionBitMask = 3L << shift;
         shift += 2;
         this.roundaboutBit = 1L << shift;
         ++shift;
         index *= 2;
         this.acceptBit = 1L << index;
         this.ferryBit = 2L << index;
         return shift;
      }
   }

   public int defineRelationBits(int index, int shift) {
      return shift;
   }

   public abstract long handleRelationTags(OSMRelation var1, long var2);

   public abstract long acceptWay(OSMWay var1);

   public abstract long handleWayTags(OSMWay var1, long var2, long var4);

   public long handleNodeTags(OSMNode node) {
      if(node.hasTag("barrier", this.absoluteBarriers)) {
         return this.directionBitMask;
      } else {
         if(node.hasTag("barrier", this.potentialBarriers)) {
            boolean locked = false;
            if(node.hasTag("locked", "yes")) {
               locked = true;
            }

            Iterator i$ = this.restrictions.iterator();

            while(i$.hasNext()) {
               String res = (String)i$.next();
               if(!locked && node.hasTag(res, this.intendedValues)) {
                  return 0L;
               }

               if(node.hasTag(res, this.restrictedValues)) {
                  return this.directionBitMask;
               }
            }

            if(this.blockByDefault) {
               return this.directionBitMask;
            }
         }

         return this.blockFords && (node.hasTag("highway", "ford") || node.hasTag("ford", new String[0])) && !node.hasTag(this.restrictions, this.intendedValues) && !node.hasTag("ford", "no")?this.directionBitMask:0L;
      }
   }

   public InstructionAnnotation getAnnotation(long flags, Translation tr) {
      return InstructionAnnotation.EMPTY;
   }

   public long reverseFlags(long flags) {
      long dir = flags & this.directionBitMask;
      return dir != this.directionBitMask && dir != 0L?flags ^ this.directionBitMask:flags;
   }

   public long flagsDefault(boolean forward, boolean backward) {
      long flags = this.speedEncoder.setDefaultValue(0L);
      return this.setAccess(flags, forward, backward);
   }

   public long setAccess(long flags, boolean forward, boolean backward) {
      return this.setBool(this.setBool(flags, 1, backward), 0, forward);
   }

   public long setSpeed(long flags, double speed) {
      if(speed >= 0.0D && !Double.isNaN(speed)) {
         if(speed < this.speedEncoder.factor / 2.0D) {
            return this.setLowSpeed(flags, speed, false);
         } else {
            if(speed > this.getMaxSpeed()) {
               speed = this.getMaxSpeed();
            }

            return this.speedEncoder.setDoubleValue(flags, speed);
         }
      } else {
         throw new IllegalArgumentException("Speed cannot be negative or NaN: " + speed + ", flags:" + BitUtil.LITTLE.toBitString(flags));
      }
   }

   protected long setLowSpeed(long flags, double speed, boolean reverse) {
      return this.setAccess(this.speedEncoder.setDoubleValue(flags, 0.0D), false, false);
   }

   public double getSpeed(long flags) {
      double speedVal = this.speedEncoder.getDoubleValue(flags);
      if(speedVal < 0.0D) {
         throw new IllegalStateException("Speed was negative!? " + speedVal);
      } else {
         return speedVal;
      }
   }

   public long setReverseSpeed(long flags, double speed) {
      return this.setSpeed(flags, speed);
   }

   public double getReverseSpeed(long flags) {
      return this.getSpeed(flags);
   }

   public long setProperties(double speed, boolean forward, boolean backward) {
      return this.setAccess(this.setSpeed(0L, speed), forward, backward);
   }

   public double getMaxSpeed() {
      return (double)this.speedEncoder.getMaxValue();
   }

   protected double getMaxSpeed(OSMWay way) {
      double maxSpeed = this.parseSpeed(way.getTag("maxspeed"));
      double fwdSpeed = this.parseSpeed(way.getTag("maxspeed:forward"));
      if(fwdSpeed >= 0.0D && (maxSpeed < 0.0D || fwdSpeed < maxSpeed)) {
         maxSpeed = fwdSpeed;
      }

      double backSpeed = this.parseSpeed(way.getTag("maxspeed:backward"));
      if(backSpeed >= 0.0D && (maxSpeed < 0.0D || backSpeed < maxSpeed)) {
         maxSpeed = backSpeed;
      }

      return maxSpeed;
   }

   public int hashCode() {
      byte hash = 7;
      int hash1 = 61 * hash + (int)this.directionBitMask;
      hash1 = 61 * hash1 + this.toString().hashCode();
      return hash1;
   }

   public boolean equals(Object obj) {
      if(obj == null) {
         return false;
      } else {
         AbstractFlagEncoder other = (AbstractFlagEncoder)obj;
         return this.directionBitMask != other.directionBitMask?false:this.toString().equals(other.toString());
      }
   }

   protected double parseSpeed(String str) {
      if(Helper.isEmpty(str)) {
         return -1.0D;
      } else if("none".equals(str)) {
         return 140.0D;
      } else if(!str.endsWith(":rural") && !str.endsWith(":trunk")) {
         if(str.endsWith(":urban")) {
            return 50.0D;
         } else if(!str.equals("walk") && !str.endsWith(":living_street")) {
            try {
               int mpInteger = str.indexOf("mp");
               int ex;
               if(mpInteger > 0) {
                  str = str.substring(0, mpInteger).trim();
                  ex = Integer.parseInt(str);
                  return (double)ex * 1.609344D;
               } else {
                  int knotInteger = str.indexOf("knots");
                  if(knotInteger > 0) {
                     str = str.substring(0, knotInteger).trim();
                     ex = Integer.parseInt(str);
                     return (double)ex * 1.852D;
                  } else {
                     int kmInteger = str.indexOf("km");
                     if(kmInteger > 0) {
                        str = str.substring(0, kmInteger).trim();
                     } else {
                        kmInteger = str.indexOf("kph");
                        if(kmInteger > 0) {
                           str = str.substring(0, kmInteger).trim();
                        }
                     }

                     return (double)Integer.parseInt(str);
                  }
               }
            } catch (Exception var6) {
               return -1.0D;
            }
         } else {
            return 6.0D;
         }
      } else {
         return 80.0D;
      }
   }

   protected static int parseDuration(String str) {
      if(str == null) {
         return 0;
      } else {
         try {
            if(str.startsWith("P")) {
               return 0;
            } else {
               int ex = str.indexOf(":");
               if(ex > 0) {
                  String hourStr = str.substring(0, ex);
                  String minStr = str.substring(ex + 1);
                  ex = minStr.indexOf(":");
                  int minutes = 0;
                  if(ex > 0) {
                     String dayStr = hourStr;
                     hourStr = minStr.substring(0, ex);
                     minStr = minStr.substring(ex + 1);
                     minutes = Integer.parseInt(dayStr) * 60 * 24;
                  }

                  minutes += Integer.parseInt(hourStr) * 60;
                  minutes += Integer.parseInt(minStr);
                  return minutes;
               } else {
                  return Integer.parseInt(str);
               }
            }
         } catch (Exception var6) {
//            logger.warn("Cannot parse " + str + " using 0 minutes");
            return 0;
         }
      }
   }

   public void applyWayTags(OSMWay way, EdgeIteratorState edge) {
   }

   protected long handleFerryTags(OSMWay way, double unknownSpeed, double shortTripsSpeed, double longTripsSpeed) {
      double durationInHours = (double)parseDuration(way.getTag("duration")) / 60.0D;
      if(durationInHours > 0.0D) {
         try {
            Number estimatedLength = (Number)way.getTag("estimated_distance", (Object)null);
            if(estimatedLength != null) {
               double val = estimatedLength.doubleValue() / 1000.0D;
               shortTripsSpeed = (double)Math.round(val / durationInHours / 1.4D);
               if(shortTripsSpeed > this.getMaxSpeed()) {
                  shortTripsSpeed = this.getMaxSpeed();
               }

               longTripsSpeed = shortTripsSpeed;
            }
         } catch (Exception var13) {
            ;
         }
      }

      return durationInHours == 0.0D?this.setSpeed(0L, unknownSpeed):(durationInHours > 1.0D?this.setSpeed(0L, longTripsSpeed):this.setSpeed(0L, shortTripsSpeed));
   }

   void setWayBitMask(int usedBits, int shift) {
      this.wayBitMask = (1L << usedBits) - 1L;
      this.wayBitMask <<= shift;
   }

   long getWayBitMask() {
      return this.wayBitMask;
   }

   void setRelBitMask(int usedBits, int shift) {
      this.relBitMask = (1L << usedBits) - 1L;
      this.relBitMask <<= shift;
   }

   long getRelBitMask() {
      return this.relBitMask;
   }

   void setNodeBitMask(int usedBits, int shift) {
      this.nodeBitMask = (1L << usedBits) - 1L;
      this.nodeBitMask <<= shift;
   }

   long getNodeBitMask() {
      return this.nodeBitMask;
   }

   public int defineTurnBits(int index, final int shift) {
      if(this.maxTurnCosts == 0) {
         return shift;
      } else if(this.maxTurnCosts == 1) {
         this.turnRestrictionBit = 1L << shift;
         return shift + 1;
      } else {
         final int turnBits = Helper.countBitValue(this.maxTurnCosts);
         this.turnCostEncoder = new EncodedValue("TurnCost", shift, turnBits, 1.0D, 0L, this.maxTurnCosts) {
            public final long getValue(long flags) {
               flags &= this.mask;
               flags >>>= (int)this.shift;
               return flags;
            }
         };
         return shift + turnBits;
      }
   }

   public boolean isTurnRestricted(long flags) {
      return this.maxTurnCosts == 0?false:(this.maxTurnCosts == 1?(flags & this.turnRestrictionBit) != 0L:this.turnCostEncoder.getValue(flags) == (long)this.maxTurnCosts);
   }

   public double getTurnCost(long flags) {
      if(this.maxTurnCosts == 0) {
         return 0.0D;
      } else if(this.maxTurnCosts == 1) {
         return (flags & this.turnRestrictionBit) == 0L?0.0D:Double.POSITIVE_INFINITY;
      } else {
         long cost = this.turnCostEncoder.getValue(flags);
         return cost == (long)this.maxTurnCosts?Double.POSITIVE_INFINITY:(double)cost;
      }
   }

   public long getTurnFlags(boolean restricted, double costs) {
      if(this.maxTurnCosts == 0) {
         return 0L;
      } else if(this.maxTurnCosts == 1) {
         if(costs != 0.0D) {
            throw new IllegalArgumentException("Only restrictions are supported");
         } else {
            return restricted?this.turnRestrictionBit:0L;
         }
      } else {
         if(restricted) {
            if(costs != 0.0D || Double.isInfinite(costs)) {
               throw new IllegalArgumentException("Restricted turn can only have infinite costs (or use 0)");
            }
         } else if(costs >= (double)this.maxTurnCosts) {
            throw new IllegalArgumentException("Cost is too high. Or specifiy restricted == true");
         }

         if(costs < 0.0D) {
            throw new IllegalArgumentException("Turn costs cannot be negative");
         } else {
            if(costs >= (double)this.maxTurnCosts || restricted) {
               costs = (double)this.maxTurnCosts;
            }

            return this.turnCostEncoder.setValue(0L, (long)((int)costs));
         }
      }
   }

   protected boolean isFerry(long internalFlags) {
      return (internalFlags & this.ferryBit) != 0L;
   }

   protected boolean isAccept(long internalFlags) {
      return (internalFlags & this.acceptBit) != 0L;
   }

   public boolean isBackward(long flags) {
      return (flags & this.backwardBit) != 0L;
   }

   public boolean isForward(long flags) {
      return (flags & this.forwardBit) != 0L;
   }

   public long setBool(long flags, int key, boolean value) {
      switch(key) {
      case 0:
         return value?flags | this.forwardBit:flags & ~this.forwardBit;
      case 1:
         return value?flags | this.backwardBit:flags & ~this.backwardBit;
      case 2:
         return value?flags | this.roundaboutBit:flags & ~this.roundaboutBit;
      default:
         throw new IllegalArgumentException("Unknown key " + key + " for boolean value");
      }
   }

   public boolean isBool(long flags, int key) {
      switch(key) {
      case 0:
         return this.isForward(flags);
      case 1:
         return this.isBackward(flags);
      case 2:
         return (flags & this.roundaboutBit) != 0L;
      default:
         throw new IllegalArgumentException("Unknown key " + key + " for boolean value");
      }
   }

   public long setLong(long flags, int key, long value) {
      throw new UnsupportedOperationException("Unknown key " + key + " for long value.");
   }

   public long getLong(long flags, int key) {
      throw new UnsupportedOperationException("Unknown key " + key + " for long value.");
   }

   public long setDouble(long flags, int key, double value) {
      throw new UnsupportedOperationException("Unknown key " + key + " for double value.");
   }

   public double getDouble(long flags, int key) {
      throw new UnsupportedOperationException("Unknown key " + key + " for double value.");
   }

   /** @deprecated */
   @Deprecated
   protected static double parseDouble(String str, String key, double defaultD) {
      String val = getStr(str, key);
      return val.isEmpty()?defaultD:Double.parseDouble(val);
   }

   /** @deprecated */
   @Deprecated
   protected static long parseLong(String str, String key, long defaultL) {
      String val = getStr(str, key);
      return val.isEmpty()?defaultL:Long.parseLong(val);
   }

   /** @deprecated */
   @Deprecated
   protected static boolean parseBoolean(String str, String key, boolean defaultB) {
      String val = getStr(str, key);
      return val.isEmpty()?defaultB:Boolean.parseBoolean(val);
   }

   /** @deprecated */
   @Deprecated
   protected static String getStr(String str, String key) {
      key = key.toLowerCase();
      String[] arr$ = str.split("\\|");
      int len$ = arr$.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         String s = arr$[i$];
         s = s.trim().toLowerCase();
         int index = s.indexOf("=");
         if(index >= 0) {
            String field = s.substring(0, index);
            String valueStr = s.substring(index + 1);
            if(key.equals(field)) {
               return valueStr;
            }
         }
      }

      return "";
   }

   protected double applyMaxSpeed(OSMWay way, double speed, boolean force) {
      double maxSpeed = this.getMaxSpeed(way);
      return maxSpeed < 0.0D || !force && maxSpeed >= speed?speed:maxSpeed * 0.9D;
   }

   protected String getPropertiesString() {
      return "speedFactor=" + this.speedFactor + "|speedBits=" + this.speedBits + "|turnCosts=" + (this.maxTurnCosts > 0);
   }

   public boolean supports(Class feature) {
      return TurnWeighting.class.isAssignableFrom(feature)?this.maxTurnCosts > 0:false;
   }
}
