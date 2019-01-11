package com.graphhopper.http;

import com.graphhopper.GHResponse;
import com.graphhopper.http.RouteSerializer;
import com.graphhopper.http.WebHelper;
import com.graphhopper.util.Helper;
import com.graphhopper.util.InstructionList;
import com.graphhopper.util.PointList;
import com.graphhopper.util.shapes.BBox;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class SimpleRouteSerializer implements RouteSerializer {
   private final BBox maxBounds;

   public SimpleRouteSerializer(BBox maxBounds) {
      this.maxBounds = maxBounds;
   }

   public Map toJSON(GHResponse rsp, boolean calcPoints, boolean pointsEncoded, boolean includeElevation, boolean enableInstructions) {
      HashMap json = new HashMap();
      if(rsp.hasErrors()) {
         json.put("message", ((Throwable)rsp.getErrors().get(0)).getMessage());
         ArrayList jsonInfo = new ArrayList();
         Iterator jsonPath = rsp.getErrors().iterator();

         while(jsonPath.hasNext()) {
            Throwable points = (Throwable)jsonPath.next();
            HashMap instructions = new HashMap();
            instructions.put("message", points.getMessage());
            instructions.put("details", points.getClass().getName());
            jsonInfo.add(instructions);
         }

         json.put("hints", jsonInfo);
      } else {
         HashMap jsonInfo1 = new HashMap();
         json.put("info", jsonInfo1);
         json.put("hints", rsp.getHints().toMap());
         jsonInfo1.put("copyrights", Arrays.asList(new String[]{"GraphHopper", "OpenStreetMap contributors"}));
         HashMap jsonPath1 = new HashMap();
         jsonPath1.put("distance", Double.valueOf(Helper.round(rsp.getDistance(), 3)));
         jsonPath1.put("weight", Double.valueOf(Helper.round6(rsp.getDistance())));
         jsonPath1.put("time", Long.valueOf(rsp.getTime()));
         if(calcPoints) {
            jsonPath1.put("points_encoded", Boolean.valueOf(pointsEncoded));
            PointList points1 = rsp.getPoints();
            if(points1.getSize() >= 2) {
               BBox instructions1 = new BBox(this.maxBounds.minLon, this.maxBounds.maxLon, this.maxBounds.minLat, this.maxBounds.maxLat);
               jsonPath1.put("bbox", rsp.calcRouteBBox(instructions1).toGeoJson());
            }

            jsonPath1.put("points", this.createPoints(points1, pointsEncoded, includeElevation));
            if(enableInstructions) {
               InstructionList instructions2 = rsp.getInstructions();
               jsonPath1.put("instructions", instructions2.createJson());
            }
         }

         json.put("paths", Collections.singletonList(jsonPath1));
      }

      return json;
   }

   public Object createPoints(PointList points, boolean pointsEncoded, boolean includeElevation) {
      if(pointsEncoded) {
         return WebHelper.encodePolyline(points, includeElevation);
      } else {
         HashMap jsonPoints = new HashMap();
         jsonPoints.put("type", "LineString");
         jsonPoints.put("coordinates", points.toGeoJson(includeElevation));
         return jsonPoints;
      }
   }
}
