package com.graphhopper.http;

import com.graphhopper.http.GHBaseServlet;
import com.graphhopper.http.TourSerializer;
import com.graphhopper.tour.Places;
import com.graphhopper.tour.TourCalculator;
import com.graphhopper.tour.TourResponse;
import com.graphhopper.tour.util.ProgressReporter;
import com.graphhopper.util.shapes.GHPoint;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONObject;

public class TourServlet extends GHBaseServlet {
   @Inject
   private List places;
   @Inject
   private TourCalculator tourCalculator;
   @Inject
   private TourSerializer tourSerializer;
   private Map nameIndex;

   public void init() {
      this.nameIndex = Places.nameIndex(this.places);
   }

   public void doGet(HttpServletRequest req, final HttpServletResponse res) throws ServletException, IOException {
      TourResponse tourRsp = new TourResponse();

      List points;
      Map map;
      try {
         points = this.getPoints(req, "point");
      } catch (Exception var9) {
         tourRsp.addError(var9);
         map = this.tourSerializer.toJSON(tourRsp);
         this.writeJsonError(res, 400, new JSONObject(map));
         return;
      }

      res.setContentType("text/event-stream");
      res.setCharacterEncoding("UTF-8");
      res.setStatus(200);
      res.getWriter().flush();
      res.flushBuffer();
      ProgressReporter progressReporter = new ProgressReporter() {
         public void reportProgress(int complete, int total) throws IOException {
            JSONObject json = new JSONObject();
            json.put("complete", complete);
            json.put("total", total);
            PrintWriter writer = res.getWriter();
            writer.append("event: progress\r\n");
            writer.append("data: " + json.toString() + "\r\n\r\n");
            writer.flush();
         }
      };
      tourRsp = this.tourCalculator.calcTour(points, progressReporter);
      map = this.tourSerializer.toJSON(tourRsp);
      JSONObject json = new JSONObject(map);
      PrintWriter writer = res.getWriter();
      writer.append("event: result\r\n");
      writer.append("data: " + json.toString() + "\r\n\r\n");
   }

   protected List getPoints(HttpServletRequest req, String key) {
      String[] pointsAsStr = this.getParams(req, key);
      ArrayList points = new ArrayList(pointsAsStr.length);
      String[] arr$ = pointsAsStr;
      int len$ = pointsAsStr.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         String str = arr$[i$];
         String[] fromStrs = str.split(",");
         GHPoint point;
         if(fromStrs.length == 2) {
            point = GHPoint.parse(str);
            if(point != null) {
               points.add(point);
            }
         } else if(fromStrs.length == 1) {
            point = (GHPoint)this.nameIndex.get(str);
            if(point == null) {
               throw new IllegalArgumentException("unknown place \"" + str + "\"");
            }

            points.add(point);
         }
      }

      return points;
   }
}
