package com.graphhopper.http;

import com.graphhopper.GraphHopper;
import com.graphhopper.http.GHBaseServlet;
import com.graphhopper.storage.StorableProperties;
import com.graphhopper.util.Constants;
import com.graphhopper.util.Helper;
import com.graphhopper.util.shapes.BBox;
import java.io.IOException;
import java.util.ArrayList;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONObject;

public class InfoServlet extends GHBaseServlet {
   @Inject
   private GraphHopper hopper;

   public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
      BBox bb = this.hopper.getGraphHopperStorage().getBounds();
      ArrayList list = new ArrayList(4);
      list.add(Double.valueOf(bb.minLon));
      list.add(Double.valueOf(bb.minLat));
      list.add(Double.valueOf(bb.maxLon));
      list.add(Double.valueOf(bb.maxLat));
      JSONObject json = new JSONObject();
      json.put("bbox", list);
      String[] vehicles = this.hopper.getGraphHopperStorage().getEncodingManager().toString().split(",");
      json.put("supported_vehicles", vehicles);
      JSONObject features = new JSONObject();
      String[] props = vehicles;
      int len$ = vehicles.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         String v = props[i$];
         JSONObject perVehicleJson = new JSONObject();
         perVehicleJson.put("elevation", this.hopper.hasElevation());
         features.put(v, perVehicleJson);
      }

      json.put("features", features);
      json.put("version", Constants.VERSION);
      json.put("build_date", Constants.BUILD_DATE);
      StorableProperties var13 = this.hopper.getGraphHopperStorage().getProperties();
      json.put("import_date", var13.get("osmreader.import.date"));
      if(!Helper.isEmpty(var13.get("prepare.date"))) {
         json.put("prepare_date", var13.get("prepare.date"));
      }

      this.writeJson(req, res, json);
   }
}
