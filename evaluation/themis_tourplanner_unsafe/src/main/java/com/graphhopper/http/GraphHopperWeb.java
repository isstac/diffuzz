package com.graphhopper.http;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopperAPI;
import com.graphhopper.http.WebHelper;
import com.graphhopper.util.Downloader;
import com.graphhopper.util.FinishInstruction;
import com.graphhopper.util.Instruction;
import com.graphhopper.util.InstructionAnnotation;
import com.graphhopper.util.InstructionList;
import com.graphhopper.util.PointList;
import com.graphhopper.util.RoundaboutInstruction;
import com.graphhopper.util.StopWatch;
import com.graphhopper.util.Translation;
import com.graphhopper.util.ViaInstruction;
import com.graphhopper.util.shapes.GHPoint;
import java.util.Iterator;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GraphHopperWeb implements GraphHopperAPI {
   private final Logger logger = LoggerFactory.getLogger(this.getClass());
   private Downloader downloader = new Downloader("GraphHopper Java Client");
   private String routeServiceUrl = "https://graphhopper.com/api/1/route";
   private String key = "";
   private boolean instructions = true;
   private boolean calcPoints = true;
   private boolean elevation = false;

   public void setDownloader(Downloader downloader) {
      this.downloader = downloader;
   }

   public boolean load(String serviceUrl) {
      this.routeServiceUrl = serviceUrl;
      return true;
   }

   public GraphHopperWeb setKey(String key) {
      if(key != null && !key.isEmpty()) {
         this.key = key;
         return this;
      } else {
         throw new IllegalStateException("Key cannot be empty");
      }
   }

   public GraphHopperWeb setCalcPoints(boolean calcPoints) {
      this.calcPoints = calcPoints;
      return this;
   }

   public GraphHopperWeb setInstructions(boolean b) {
      this.instructions = b;
      return this;
   }

   public GraphHopperWeb setElevation(boolean withElevation) {
      this.elevation = withElevation;
      return this;
   }

   public GHResponse route(GHRequest request) {
      StopWatch sw = (new StopWatch()).start();

      try {
         String ex = "";

         GHPoint tmpCalcPoints;
         for(Iterator tmpInstructions = request.getPoints().iterator(); tmpInstructions.hasNext(); ex = ex + "point=" + tmpCalcPoints.lat + "," + tmpCalcPoints.lon + "&") {
            tmpCalcPoints = (GHPoint)tmpInstructions.next();
         }

         boolean tmpInstructions1 = request.getHints().getBool("instructions", this.instructions);
         boolean tmpCalcPoints1 = request.getHints().getBool("calcPoints", this.calcPoints);
         if(tmpInstructions1 && !tmpCalcPoints1) {
            throw new IllegalStateException("Cannot calculate instructions without points (only points without instructions). Use calcPoints=false and instructions=false to disable point and instruction calculation");
         } else {
            boolean tmpElevation = request.getHints().getBool("elevation", this.elevation);
            String tmpKey = request.getHints().get("key", this.key);
            String url = this.routeServiceUrl + "?" + ex + "&type=json" + "&instructions=" + tmpInstructions1 + "&points_encoded=true" + "&calc_points=" + tmpCalcPoints1 + "&algo=" + request.getAlgorithm() + "&locale=" + request.getLocale().toString() + "&elevation=" + tmpElevation;
            if(!request.getVehicle().isEmpty()) {
               url = url + "&vehicle=" + request.getVehicle();
            }

            if(!tmpKey.isEmpty()) {
               url = url + "&key=" + tmpKey;
            }

            String str = this.downloader.downloadAsString(url, true);
            JSONObject json = new JSONObject(str);
            GHResponse res = new GHResponse();
            readErrors(res.getErrors(), json);
            if(res.hasErrors()) {
               return res;
            } else {
               JSONArray paths = json.getJSONArray("paths");
               JSONObject firstPath = paths.getJSONObject(0);
               readPath(res, firstPath, tmpCalcPoints1, tmpInstructions1, tmpElevation);
               return res;
            }
         }
      } catch (Exception var14) {
         throw new RuntimeException("Problem while fetching path " + request.getPoints() + ": " + var14.getMessage(), var14);
      }
   }

   public static void readPath(GHResponse res, JSONObject firstPath, boolean tmpCalcPoints, boolean tmpInstructions, boolean tmpElevation) {
      double distance = firstPath.getDouble("distance");
      long time = firstPath.getLong("time");
      if(tmpCalcPoints) {
         String pointStr = firstPath.getString("points");
         PointList pointList = WebHelper.decodePolyline(pointStr, 100, tmpElevation);
         res.setPoints(pointList);
         if(tmpInstructions) {
            JSONArray instrArr = firstPath.getJSONArray("instructions");
            InstructionList il = new InstructionList((Translation)null);
            int viaCount = 1;

            for(int instrIndex = 0; instrIndex < instrArr.length(); ++instrIndex) {
               JSONObject jsonObj = instrArr.getJSONObject(instrIndex);
               double instDist = jsonObj.getDouble("distance");
               String text = jsonObj.getString("text");
               long instTime = jsonObj.getLong("time");
               int sign = jsonObj.getInt("sign");
               JSONArray iv = jsonObj.getJSONArray("interval");
               int from = iv.getInt(0);
               int to = iv.getInt(1);
               PointList instPL = new PointList(to - from, tmpElevation);

               for(int ia = from; ia <= to; ++ia) {
                  instPL.add(pointList, ia);
               }

               InstructionAnnotation var29 = InstructionAnnotation.EMPTY;
               if(jsonObj.has("annotation_importance") && jsonObj.has("annotation_text")) {
                  var29 = new InstructionAnnotation(jsonObj.getInt("annotation_importance"), jsonObj.getString("annotation_text"));
               }

               Object instr;
               if(sign != 6 && sign != -6) {
                  if(sign == 5) {
                     ViaInstruction tmpInstr = new ViaInstruction(text, var29, instPL);
                     tmpInstr.setViaCount(viaCount);
                     ++viaCount;
                     instr = tmpInstr;
                  } else if(sign == 4) {
                     instr = new FinishInstruction(instPL, 0);
                  } else {
                     instr = new Instruction(sign, text, var29, instPL);
                  }
               } else {
                  instr = new RoundaboutInstruction(sign, text, var29, instPL);
               }

               ((Instruction)instr).setUseRawName();
               ((Instruction)instr).setDistance(instDist).setTime(instTime);
               il.add((Instruction)instr);
            }

            res.setInstructions(il);
         }
      }

      res.setDistance(distance).setTime(time);
   }

   public static void readErrors(List errors, JSONObject json) {
      JSONArray errorJson;
      if(json.has("message")) {
         if(!json.has("hints")) {
            errors.add(new RuntimeException(json.getString("message")));
            return;
         }

         errorJson = json.getJSONArray("hints");
      } else {
         if(!json.has("info")) {
            return;
         }

         JSONObject i = json.getJSONObject("info");
         if(!i.has("errors")) {
            return;
         }

         errorJson = i.getJSONArray("errors");
      }

      for(int var7 = 0; var7 < errorJson.length(); ++var7) {
         JSONObject error = errorJson.getJSONObject(var7);
         String exClass = "";
         if(error.has("details")) {
            exClass = error.getString("details");
         }

         String exMessage = error.getString("message");
         if(exClass.equals(UnsupportedOperationException.class.getName())) {
            errors.add(new UnsupportedOperationException(exMessage));
         } else if(exClass.equals(IllegalStateException.class.getName())) {
            errors.add(new IllegalStateException(exMessage));
         } else if(exClass.equals(RuntimeException.class.getName())) {
            errors.add(new RuntimeException(exMessage));
         } else if(exClass.equals(IllegalArgumentException.class.getName())) {
            errors.add(new IllegalArgumentException(exMessage));
         } else if(exClass.isEmpty()) {
            errors.add(new RuntimeException(exMessage));
         } else {
            errors.add(new RuntimeException(exClass + " " + exMessage));
         }
      }

      if(json.has("message") && errors.isEmpty()) {
         errors.add(new RuntimeException(json.getString("message")));
      }

   }
}
