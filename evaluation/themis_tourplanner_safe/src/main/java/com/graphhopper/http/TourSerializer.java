package com.graphhopper.http;

import com.graphhopper.tour.TourResponse;
import com.graphhopper.util.shapes.GHPlace;
import com.graphhopper.util.shapes.GHPoint;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class TourSerializer {
   public Map toJSON(TourResponse rsp) {
      HashMap json = new HashMap();
      ArrayList jsonPoints;
      Iterator i$;
      if(rsp.hasErrors()) {
         json.put("message", ((Throwable)rsp.getErrors().get(0)).getMessage());
         jsonPoints = new ArrayList();
         i$ = rsp.getErrors().iterator();

         while(i$.hasNext()) {
            Throwable p = (Throwable)i$.next();
            HashMap map = new HashMap();
            map.put("message", p.getMessage());
            map.put("details", p.getClass().getName());
            jsonPoints.add(map);
         }

         json.put("hints", jsonPoints);
      } else {
         jsonPoints = new ArrayList();
         json.put("points", jsonPoints);
         i$ = rsp.getPoints().iterator();

         while(i$.hasNext()) {
            GHPoint p1 = (GHPoint)i$.next();
            jsonPoints.add(this.pointToJSON(p1));
         }
      }

      return json;
   }

   private Map pointToJSON(GHPoint p) {
      HashMap jsonPoint = new HashMap();
      if(p instanceof GHPlace) {
         jsonPoint.put("name", ((GHPlace)p).getName());
      }

      jsonPoint.put("lat", Double.valueOf(p.getLat()));
      jsonPoint.put("lon", Double.valueOf(p.getLon()));
      return jsonPoint;
   }
   
   /* Yannic */
   public List<String> toList(TourResponse rsp) {
       List<String> list = new ArrayList<String>();
       Iterator i$;
       if(rsp.hasErrors()) {
           list.add(((Throwable)rsp.getErrors().get(0)).getMessage());
           i$ = rsp.getErrors().iterator();
           while(i$.hasNext()) {
              Throwable p = (Throwable)i$.next();
              list.add(p.getMessage());
              list.add(p.getClass().getName());
           }
        } else {
            i$ = rsp.getPoints().iterator();
            while (i$.hasNext()) {
                GHPoint p1 = (GHPoint)i$.next();
                list.add(p1.toString());
            }
        }
       return list;
   }
}
