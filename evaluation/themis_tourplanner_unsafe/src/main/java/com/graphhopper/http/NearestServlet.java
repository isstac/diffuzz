package com.graphhopper.http;

import com.graphhopper.GraphHopper;
import com.graphhopper.http.GHBaseServlet;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.storage.index.LocationIndex;
import com.graphhopper.storage.index.QueryResult;
import com.graphhopper.util.DistanceCalc;
import com.graphhopper.util.Helper;
import com.graphhopper.util.shapes.GHPoint;
import com.graphhopper.util.shapes.GHPoint3D;
import java.io.IOException;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONArray;
import org.json.JSONObject;

public class NearestServlet extends GHBaseServlet {
   @Inject
   private GraphHopper hopper;
   private final DistanceCalc calc;

   public NearestServlet() {
      this.calc = Helper.DIST_EARTH;
   }

   public void doGet(HttpServletRequest httpReq, HttpServletResponse httpRes) throws ServletException, IOException {
      String pointStr = this.getParam(httpReq, "point", (String)null);
      boolean enabledElevation = this.getBooleanParam(httpReq, "elevation", false);
      JSONObject result = new JSONObject();
      if(pointStr != null && !pointStr.equalsIgnoreCase("")) {
         GHPoint place = GHPoint.parse(pointStr);
         LocationIndex index = this.hopper.getLocationIndex();
         QueryResult qr = index.findClosest(place.lat, place.lon, EdgeFilter.ALL_EDGES);
         if(!qr.isValid()) {
            result.put("error", "Nearest point cannot be found!");
         } else {
            GHPoint3D snappedPoint = qr.getSnappedPoint();
            result.put("type", "Point");
            JSONArray coord = new JSONArray();
            coord.put(snappedPoint.lon);
            coord.put(snappedPoint.lat);
            if(this.hopper.hasElevation() && enabledElevation) {
               coord.put(snappedPoint.ele);
            }

            result.put("coordinates", coord);
            result.put("distance", this.calc.calcDist(place.lat, place.lon, snappedPoint.lat, snappedPoint.lon));
         }
      } else {
         result.put("error", "No lat/lon specified!");
      }

      this.writeJson(httpReq, httpRes, result);
   }
}
