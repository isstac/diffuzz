package com.graphhopper.routing.util;

import com.graphhopper.reader.OSMNode;
import com.graphhopper.reader.OSMRelation;
import com.graphhopper.reader.OSMWay;
import com.graphhopper.routing.util.AbstractFlagEncoder;
import com.graphhopper.routing.util.Bike2WeightFlagEncoder;
import com.graphhopper.routing.util.BikeFlagEncoder;
import com.graphhopper.routing.util.CarFlagEncoder;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.FootFlagEncoder;
import com.graphhopper.routing.util.MotorcycleFlagEncoder;
import com.graphhopper.routing.util.MountainBikeFlagEncoder;
import com.graphhopper.routing.util.RacingBikeFlagEncoder;
import com.graphhopper.routing.util.TurnWeighting;
import com.graphhopper.storage.RAMDirectory;
import com.graphhopper.storage.StorableProperties;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.Helper;
import com.graphhopper.util.PMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class EncodingManager {
   public static final String CAR = "car";
   public static final String BIKE = "bike";
   public static final String BIKE2 = "bike2";
   public static final String RACINGBIKE = "racingbike";
   public static final String MOUNTAINBIKE = "mtb";
   public static final String FOOT = "foot";
   public static final String MOTORCYCLE = "motorcycle";
   private final List edgeEncoders;
   private int nextWayBit;
   private int nextNodeBit;
   private int nextRelBit;
   private int nextTurnBit;
   private final int bitsForEdgeFlags;
   private final int bitsForTurnFlags;
   private boolean enableInstructions;
   private static final String ERR = "Encoders are requesting more than %s bits of %s flags. ";
   private static final String WAY_ERR = "Decrease the number of vehicles or increase the flags to take long via graph.bytesForFlags=8";

   public EncodingManager(String flagEncodersStr) {
      this((String)flagEncodersStr, 4);
   }

   public EncodingManager(String flagEncodersStr, int bytesForFlags) {
      this(parseEncoderString(flagEncodersStr), bytesForFlags);
   }

   public EncodingManager(FlagEncoder... flagEncoders) {
      this(Arrays.asList(flagEncoders));
   }

   public EncodingManager(List flagEncoders) {
      this((List)flagEncoders, 4);
   }

   public EncodingManager(List flagEncoders, int bytesForEdgeFlags) {
      this.edgeEncoders = new ArrayList();
      this.nextWayBit = 0;
      this.nextNodeBit = 0;
      this.nextRelBit = 0;
      this.nextTurnBit = 0;
      this.bitsForTurnFlags = 32;
      this.enableInstructions = true;
      if(bytesForEdgeFlags != 4 && bytesForEdgeFlags != 8) {
         throw new IllegalStateException("For \'edge flags\' currently only 4 or 8 bytes supported");
      } else {
         this.bitsForEdgeFlags = bytesForEdgeFlags * 8;
         Iterator i$ = flagEncoders.iterator();

         while(i$.hasNext()) {
            FlagEncoder flagEncoder = (FlagEncoder)i$.next();
            this.registerEncoder((AbstractFlagEncoder)flagEncoder);
         }

         if(this.edgeEncoders.isEmpty()) {
            throw new IllegalStateException("No vehicles found");
         }
      }
   }

   public int getBytesForFlags() {
      return this.bitsForEdgeFlags / 8;
   }

   static List parseEncoderString(String encoderList) {
      if(encoderList.contains(":")) {
         throw new IllegalArgumentException("EncodingManager does no longer use reflection instantiate encoders directly.");
      } else {
         String[] entries = encoderList.split(",");
         ArrayList resultEncoders = new ArrayList();
         String[] arr$ = entries;
         int len$ = entries.length;

         for(int i$ = 0; i$ < len$; ++i$) {
            String entry = arr$[i$];
            entry = entry.trim().toLowerCase();
            if(!entry.isEmpty()) {
               String entryVal = "";
               if(entry.contains("|")) {
                  entryVal = entry;
                  entry = entry.split("\\|")[0];
               }

               PMap configuration = new PMap(entryVal);
               Object fe;
               if(entry.equals("car")) {
                  fe = new CarFlagEncoder(configuration);
               } else if(entry.equals("bike")) {
                  fe = new BikeFlagEncoder(configuration);
               } else if(entry.equals("bike2")) {
                  fe = new Bike2WeightFlagEncoder(configuration);
               } else if(entry.equals("racingbike")) {
                  fe = new RacingBikeFlagEncoder(configuration);
               } else if(entry.equals("mtb")) {
                  fe = new MountainBikeFlagEncoder(configuration);
               } else if(entry.equals("foot")) {
                  fe = new FootFlagEncoder(configuration);
               } else {
                  if(!entry.equals("motorcycle")) {
                     throw new IllegalArgumentException("entry in encoder list not supported " + entry);
                  }

                  fe = new MotorcycleFlagEncoder(configuration);
               }

               if(configuration.has("version") && ((AbstractFlagEncoder)fe).getVersion() != configuration.getInt("version", -1)) {
                  throw new IllegalArgumentException("Encoder " + entry + " was used in version " + configuration.getLong("version", -1L) + ", but current version is " + ((AbstractFlagEncoder)fe).getVersion());
               }

               resultEncoders.add(fe);
            }
         }

         return resultEncoders;
      }
   }

   private void registerEncoder(AbstractFlagEncoder encoder) {
      int encoderCount = this.edgeEncoders.size();
      int usedBits = encoder.defineNodeBits(encoderCount, this.nextNodeBit);
      if(usedBits > this.bitsForEdgeFlags) {
         throw new IllegalArgumentException(String.format("Encoders are requesting more than %s bits of %s flags. ", new Object[]{Integer.valueOf(this.bitsForEdgeFlags), "node"}));
      } else {
         encoder.setNodeBitMask(usedBits - this.nextNodeBit, this.nextNodeBit);
         this.nextNodeBit = usedBits;
         usedBits = encoder.defineWayBits(encoderCount, this.nextWayBit);
         if(usedBits > this.bitsForEdgeFlags) {
            throw new IllegalArgumentException(String.format("Encoders are requesting more than %s bits of %s flags. ", new Object[]{Integer.valueOf(this.bitsForEdgeFlags), "way"}) + "Decrease the number of vehicles or increase the flags to take long via graph.bytesForFlags=8");
         } else {
            encoder.setWayBitMask(usedBits - this.nextWayBit, this.nextWayBit);
            this.nextWayBit = usedBits;
            usedBits = encoder.defineRelationBits(encoderCount, this.nextRelBit);
            if(usedBits > this.bitsForEdgeFlags) {
               throw new IllegalArgumentException(String.format("Encoders are requesting more than %s bits of %s flags. ", new Object[]{Integer.valueOf(this.bitsForEdgeFlags), "relation"}));
            } else {
               encoder.setRelBitMask(usedBits - this.nextRelBit, this.nextRelBit);
               this.nextRelBit = usedBits;
               usedBits = encoder.defineTurnBits(encoderCount, this.nextTurnBit);
               if(usedBits > 32) {
                  throw new IllegalArgumentException(String.format("Encoders are requesting more than %s bits of %s flags. ", new Object[]{Integer.valueOf(this.bitsForEdgeFlags), "turn"}));
               } else {
                  this.nextTurnBit = usedBits;
                  this.edgeEncoders.add(encoder);
               }
            }
         }
      }
   }

   public boolean supports(String encoder) {
      return this.getEncoder(encoder, false) != null;
   }

   public FlagEncoder getEncoder(String name) {
      return this.getEncoder(name, true);
   }

   private FlagEncoder getEncoder(String name, boolean throwExc) {
      Iterator i$ = this.edgeEncoders.iterator();

      AbstractFlagEncoder encoder;
      do {
         if(!i$.hasNext()) {
            if(throwExc) {
               throw new IllegalArgumentException("Encoder for " + name + " not found. Existing: " + this.toDetailsString());
            }

            return null;
         }

         encoder = (AbstractFlagEncoder)i$.next();
      } while(!name.equalsIgnoreCase(encoder.toString()));

      return encoder;
   }

   public long acceptWay(OSMWay way) {
      long includeWay = 0L;

      AbstractFlagEncoder encoder;
      for(Iterator i$ = this.edgeEncoders.iterator(); i$.hasNext(); includeWay |= encoder.acceptWay(way)) {
         encoder = (AbstractFlagEncoder)i$.next();
      }

      return includeWay;
   }

   public long handleRelationTags(OSMRelation relation, long oldRelationFlags) {
      long flags = 0L;

      AbstractFlagEncoder encoder;
      for(Iterator i$ = this.edgeEncoders.iterator(); i$.hasNext(); flags |= encoder.handleRelationTags(relation, oldRelationFlags)) {
         encoder = (AbstractFlagEncoder)i$.next();
      }

      return flags;
   }

   public long handleWayTags(OSMWay way, long includeWay, long relationFlags) {
      long flags = 0L;

      AbstractFlagEncoder encoder;
      for(Iterator i$ = this.edgeEncoders.iterator(); i$.hasNext(); flags |= encoder.handleWayTags(way, includeWay, relationFlags & encoder.getRelBitMask())) {
         encoder = (AbstractFlagEncoder)i$.next();
      }

      return flags;
   }

   public String toString() {
      StringBuilder str = new StringBuilder();

      AbstractFlagEncoder encoder;
      for(Iterator i$ = this.edgeEncoders.iterator(); i$.hasNext(); str.append(encoder.toString())) {
         encoder = (AbstractFlagEncoder)i$.next();
         if(str.length() > 0) {
            str.append(",");
         }
      }

      return str.toString();
   }

   public String toDetailsString() {
      StringBuilder str = new StringBuilder();

      AbstractFlagEncoder encoder;
      for(Iterator i$ = this.edgeEncoders.iterator(); i$.hasNext(); str.append(encoder.toString()).append("|").append(encoder.getPropertiesString()).append("|version=").append(encoder.getVersion())) {
         encoder = (AbstractFlagEncoder)i$.next();
         if(str.length() > 0) {
            str.append(",");
         }
      }

      return str.toString();
   }

   public long flagsDefault(boolean forward, boolean backward) {
      long flags = 0L;

      AbstractFlagEncoder encoder;
      for(Iterator i$ = this.edgeEncoders.iterator(); i$.hasNext(); flags |= encoder.flagsDefault(forward, backward)) {
         encoder = (AbstractFlagEncoder)i$.next();
      }

      return flags;
   }

   public long reverseFlags(long flags) {
      int len = this.edgeEncoders.size();

      for(int i = 0; i < len; ++i) {
         flags = ((AbstractFlagEncoder)this.edgeEncoders.get(i)).reverseFlags(flags);
      }

      return flags;
   }

   public int hashCode() {
      byte hash = 5;
      int hash1 = 53 * hash + (this.edgeEncoders != null?this.edgeEncoders.hashCode():0);
      return hash1;
   }

   public boolean equals(Object obj) {
      if(obj == null) {
         return false;
      } else if(this.getClass() != obj.getClass()) {
         return false;
      } else {
         EncodingManager other = (EncodingManager)obj;
         return this.edgeEncoders == other.edgeEncoders || this.edgeEncoders != null && this.edgeEncoders.equals(other.edgeEncoders);
      }
   }

   public long handleNodeTags(OSMNode node) {
      long flags = 0L;

      AbstractFlagEncoder encoder;
      for(Iterator i$ = this.edgeEncoders.iterator(); i$.hasNext(); flags |= encoder.handleNodeTags(node)) {
         encoder = (AbstractFlagEncoder)i$.next();
      }

      return flags;
   }

   public EncodingManager setEnableInstructions(boolean enableInstructions) {
      this.enableInstructions = enableInstructions;
      return this;
   }

   public void applyWayTags(OSMWay way, EdgeIteratorState edge) {
      if(this.enableInstructions) {
         String i$ = fixWayName(way.getTag("name"));
         String encoder = fixWayName(way.getTag("ref"));
         if(!Helper.isEmpty(encoder)) {
            if(Helper.isEmpty(i$)) {
               i$ = encoder;
            } else {
               i$ = i$ + ", " + encoder;
            }
         }

         edge.setName(i$);
      }

      Iterator i$1 = this.edgeEncoders.iterator();

      while(i$1.hasNext()) {
         AbstractFlagEncoder encoder1 = (AbstractFlagEncoder)i$1.next();
         encoder1.applyWayTags(way, edge);
      }

   }

   public List fetchEdgeEncoders() {
      ArrayList list = new ArrayList();
      list.addAll(this.edgeEncoders);
      return list;
   }

   static String fixWayName(String str) {
      return str == null?"":str.replaceAll(";[ ]*", ", ");
   }

   public boolean needsTurnCostsSupport() {
      Iterator i$ = this.edgeEncoders.iterator();

      FlagEncoder encoder;
      do {
         if(!i$.hasNext()) {
            return false;
         }

         encoder = (FlagEncoder)i$.next();
      } while(!encoder.supports(TurnWeighting.class));

      return true;
   }

   public static EncodingManager create(String ghLoc) {
      RAMDirectory dir = new RAMDirectory(ghLoc, true);
      StorableProperties properties = new StorableProperties(dir);
      if(!properties.loadExisting()) {
         throw new IllegalStateException("Cannot load properties to fetch EncodingManager configuration at: " + dir.getLocation());
      } else {
         properties.checkVersions(false);
         String acceptStr = properties.get("graph.flagEncoders");
         if(acceptStr.isEmpty()) {
            throw new IllegalStateException("EncodingManager was not configured. And no one was found in the graph: " + dir.getLocation());
         } else {
            byte bytesForFlags = 4;
            if("8".equals(properties.get("graph.bytesForFlags"))) {
               bytesForFlags = 8;
            }

            return new EncodingManager(acceptStr, bytesForFlags);
         }
      }
   }
}
