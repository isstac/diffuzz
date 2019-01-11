package com.graphhopper.http;

import com.graphhopper.http.GHBaseServlet;
import com.graphhopper.util.shapes.GHPlace;
import com.graphhopper.util.shapes.GHPoint;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONArray;
import org.json.JSONObject;

public class PlacesServlet extends GHBaseServlet {
   @Inject
   protected List places;

   public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
      this.writeJson(req, res, this.getJson());
   }

   protected JSONArray getJson() {
      JSONArray json = new JSONArray();
      Iterator i$ = this.places.iterator();

      while(i$.hasNext()) {
         GHPlace p = (GHPlace)i$.next();
         json.put(this.pointToJson(p));
      }

      return json;
   }

   protected JSONObject pointToJson(GHPoint p) {
      JSONObject json = new JSONObject();
      if(p instanceof GHPlace) {
         json.put("name", ((GHPlace)p).getName());
      }

      json.put("lat", p.getLat());
      json.put("lon", p.getLon());
      return json;
   }
}
