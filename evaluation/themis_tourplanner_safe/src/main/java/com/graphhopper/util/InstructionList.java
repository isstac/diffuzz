package com.graphhopper.util;

import com.graphhopper.util.DistanceCalc;
import com.graphhopper.util.GPXEntry;
import com.graphhopper.util.Helper;
import com.graphhopper.util.Instruction;
import com.graphhopper.util.InstructionAnnotation;
import com.graphhopper.util.PointList;
import com.graphhopper.util.Translation;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

public class InstructionList implements Iterable {
   public static final InstructionList EMPTY = new InstructionList();
   private final List instructions;
   private final Translation tr;

   private InstructionList() {
      this(0, (Translation)null);
   }

   public InstructionList(Translation tr) {
      this(10, tr);
   }

   public InstructionList(int cap, Translation tr) {
      this.instructions = new ArrayList(cap);
      this.tr = tr;
   }

   public void replaceLast(Instruction instr) {
      if(this.instructions.isEmpty()) {
         throw new IllegalStateException("Cannot replace last instruction as list is empty");
      } else {
         this.instructions.set(this.instructions.size() - 1, instr);
      }
   }

   public void add(Instruction instr) {
      this.instructions.add(instr);
   }

   public int getSize() {
      return this.instructions.size();
   }

   public int size() {
      return this.instructions.size();
   }

   public List createJson() {
      ArrayList instrList = new ArrayList(this.instructions.size());
      int pointsIndex = 0;
      int counter = 0;

      for(Iterator i$ = this.instructions.iterator(); i$.hasNext(); ++counter) {
         Instruction instruction = (Instruction)i$.next();
         HashMap instrJson = new HashMap();
         instrList.add(instrJson);
         InstructionAnnotation ia = instruction.getAnnotation();
         String str = instruction.getTurnDescription(this.tr);
         if(Helper.isEmpty(str)) {
            str = ia.getMessage();
         }

         instrJson.put("text", Helper.firstBig(str));
         if(!ia.isEmpty()) {
            instrJson.put("annotation_text", ia.getMessage());
            instrJson.put("annotation_importance", Integer.valueOf(ia.getImportance()));
         }

         instrJson.put("time", Long.valueOf(instruction.getTime()));
         instrJson.put("distance", Double.valueOf(Helper.round(instruction.getDistance(), 3)));
         instrJson.put("sign", Integer.valueOf(instruction.getSign()));
         instrJson.putAll(instruction.getExtraInfoJSON());
         int tmpIndex = pointsIndex + instruction.getPoints().size();
         if(counter + 1 == this.instructions.size()) {
            --tmpIndex;
         }

         instrJson.put("interval", Arrays.asList(new Integer[]{Integer.valueOf(pointsIndex), Integer.valueOf(tmpIndex)}));
         pointsIndex = tmpIndex;
      }

      return instrList;
   }

   public boolean isEmpty() {
      return this.instructions.isEmpty();
   }

   public Iterator iterator() {
      return this.instructions.iterator();
   }

   public Instruction get(int index) {
      return (Instruction)this.instructions.get(index);
   }

   public String toString() {
      return this.instructions.toString();
   }

   public List createGPXList() {
      if(this.isEmpty()) {
         return Collections.emptyList();
      } else {
         ArrayList gpxList = new ArrayList();
         long timeOffset = 0L;

         for(int lastI = 0; lastI < this.size() - 1; ++lastI) {
            Instruction lastLat = lastI > 0?this.get(lastI - 1):null;
            boolean instrIsFirst = lastLat == null;
            Instruction lastLon = this.get(lastI + 1);
            lastLon.checkOne();
            timeOffset = this.get(lastI).fillGPXList(gpxList, timeOffset, lastLat, lastLon, instrIsFirst);
         }

         Instruction var11 = this.get(this.size() - 1);
         if(var11.points.size() != 1) {
            throw new IllegalStateException("Last instruction must have exactly one point but was " + var11.points.size());
         } else {
            double var12 = var11.getFirstLat();
            double var13 = var11.getFirstLon();
            double lastEle = var11.getPoints().is3D()?var11.getFirstEle():Double.NaN;
            gpxList.add(new GPXEntry(var12, var13, lastEle, timeOffset));
            return gpxList;
         }
      }
   }

   public String createGPX() {
      return this.createGPX("GraphHopper", (new Date()).getTime());
   }

   public String createGPX(String trackName, long startTimeMillis) {
      boolean includeElevation = this.getSize() > 0?this.get(0).getPoints().is3D():false;
      return this.createGPX(trackName, startTimeMillis, includeElevation);
   }

   public String createGPX(String trackName, long startTimeMillis, boolean includeElevation) {
      SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd\'T\'HH:mm:ss\'Z\'");
      formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
      String header = "<?xml version=\'1.0\' encoding=\'UTF-8\' standalone=\'no\' ?><gpx xmlns=\'http://www.topografix.com/GPX/1/1\' xmlns:xsi=\'http://www.w3.org/2001/XMLSchema-instance\' creator=\'Graphhopper\' version=\'1.1\' xmlns:gh=\'https://graphhopper.com/public/schema/gpx/1.1\'>\n<metadata><copyright author=\"OpenStreetMap contributors\"/><link href=\'http://graphhopper.com\'><text>GraphHopper GPX</text></link><time>" + formatter.format(Long.valueOf(startTimeMillis)) + "</time>" + "</metadata>";
      StringBuilder track = new StringBuilder(header);
      if(!this.isEmpty()) {
         track.append("\n<rte>");
         Instruction i$ = null;

         Instruction currInstr;
         for(Iterator entry = this.instructions.iterator(); entry.hasNext(); i$ = currInstr) {
            currInstr = (Instruction)entry.next();
            if(null != i$) {
               this.createRteptBlock(track, i$, currInstr);
            }
         }

         this.createRteptBlock(track, i$, (Instruction)null);
         track.append("</rte>");
      }

      track.append("\n<trk><name>").append(trackName).append("</name>");
      track.append("<trkseg>");
      Iterator i$1 = this.createGPXList().iterator();

      while(i$1.hasNext()) {
         GPXEntry entry1 = (GPXEntry)i$1.next();
         track.append("\n<trkpt lat=\'").append(Helper.round6(entry1.getLat()));
         track.append("\' lon=\'").append(Helper.round6(entry1.getLon())).append("\'>");
         if(includeElevation) {
            track.append("<ele>").append(Helper.round2(entry1.getEle())).append("</ele>");
         }

         track.append("<time>").append(formatter.format(Long.valueOf(startTimeMillis + entry1.getTime()))).append("</time>");
         track.append("</trkpt>");
      }

      track.append("</trkseg>");
      track.append("</trk>");
      track.append("</gpx>");
      return track.toString().replaceAll("\\\'", "\"");
   }

   private void createRteptBlock(StringBuilder output, Instruction instruction, Instruction nextI) {
      output.append("\n<rtept lat=\"").append(Helper.round6(instruction.getFirstLat())).append("\" lon=\"").append(Helper.round6(instruction.getFirstLon())).append("\">");
      if(!instruction.getName().isEmpty()) {
         output.append("<desc>").append(instruction.getTurnDescription(this.tr)).append("</desc>");
      }

      output.append("<extensions>");
      output.append("<gh:distance>").append(Helper.round(instruction.getDistance(), 1)).append("</gh:distance>");
      output.append("<gh:time>").append(instruction.getTime()).append("</gh:time>");
      String direction = instruction.calcDirection(nextI);
      if(!direction.isEmpty()) {
         output.append("<gh:direction>").append(direction).append("</gh:direction>");
      }

      double azimuth = instruction.calcAzimuth(nextI);
      if(!Double.isNaN(azimuth)) {
         output.append("<gh:azimuth>").append(Helper.round2(azimuth)).append("</gh:azimuth>");
      }

      output.append("<gh:sign>").append(instruction.getSign()).append("</gh:sign>");
      output.append("</extensions>");
      output.append("</rtept>");
   }

   List createStartPoints() {
      ArrayList res = new ArrayList(this.instructions.size());
      Iterator i$ = this.instructions.iterator();

      while(i$.hasNext()) {
         Instruction instruction = (Instruction)i$.next();
         res.add(Arrays.asList(new Double[]{Double.valueOf(instruction.getFirstLat()), Double.valueOf(instruction.getFirstLon())}));
      }

      return res;
   }

   public Instruction find(double lat, double lon, double maxDistance) {
      if(this.getSize() == 0) {
         return null;
      } else {
         PointList points = this.get(0).getPoints();
         double prevLat = points.getLatitude(0);
         double prevLon = points.getLongitude(0);
         DistanceCalc distCalc = Helper.DIST_EARTH;
         double foundMinDistance = distCalc.calcNormalizedDist(lat, lon, prevLat, prevLon);
         int foundInstruction = 0;
         if(this.getSize() > 1) {
            for(int instructionIndex = 0; instructionIndex < this.getSize(); ++instructionIndex) {
               points = this.get(instructionIndex).getPoints();

               for(int pointIndex = 0; pointIndex < points.size(); ++pointIndex) {
                  double currLat = points.getLatitude(pointIndex);
                  double currLon = points.getLongitude(pointIndex);
                  if(instructionIndex != 0 || pointIndex != 0) {
                     int index = instructionIndex;
                     double distance;
                     if(distCalc.validEdgeDistance(lat, lon, currLat, currLon, prevLat, prevLon)) {
                        distance = distCalc.calcNormalizedEdgeDistance(lat, lon, currLat, currLon, prevLat, prevLon);
                        if(pointIndex > 0) {
                           index = instructionIndex + 1;
                        }
                     } else {
                        distance = distCalc.calcNormalizedDist(lat, lon, currLat, currLon);
                        if(pointIndex > 0) {
                           index = instructionIndex + 1;
                        }
                     }

                     if(distance < foundMinDistance) {
                        foundMinDistance = distance;
                        foundInstruction = index;
                     }
                  }

                  prevLat = currLat;
                  prevLon = currLon;
               }
            }
         }

         if(distCalc.calcDenormalizedDist(foundMinDistance) > maxDistance) {
            return null;
         } else {
            if(foundInstruction == this.getSize()) {
               --foundInstruction;
            }

            return this.get(foundInstruction);
         }
      }
   }
}
