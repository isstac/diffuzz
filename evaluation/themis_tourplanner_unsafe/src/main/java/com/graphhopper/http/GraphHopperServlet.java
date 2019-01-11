package com.graphhopper.http;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.http.GHBaseServlet;
import com.graphhopper.http.RouteSerializer;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.WeightingMap;
import com.graphhopper.util.StopWatch;
import com.graphhopper.util.shapes.GHPoint;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class GraphHopperServlet extends GHBaseServlet {
   @Inject
   private GraphHopper hopper;
   @Inject
   private RouteSerializer routeSerializer;

   public void doGet(HttpServletRequest httpReq, HttpServletResponse httpRes) throws ServletException, IOException {
      List requestPoints = this.getPoints(httpReq, "point");
      GHResponse ghRsp = new GHResponse();
      double minPathPrecision = this.getDoubleParam(httpReq, "way_point_max_distance", 1.0D);
      boolean writeGPX = "gpx".equalsIgnoreCase(this.getParam(httpReq, "type", "json"));
      boolean enableInstructions = writeGPX || this.getBooleanParam(httpReq, "instructions", true);
      boolean calcPoints = this.getBooleanParam(httpReq, "calc_points", true);
      boolean enableElevation = this.getBooleanParam(httpReq, "elevation", false);
      boolean pointsEncoded = this.getBooleanParam(httpReq, "points_encoded", true);
      String vehicleStr = this.getParam(httpReq, "vehicle", "car");
      String weighting = this.getParam(httpReq, "weighting", "fastest");
      String algoStr = this.getParam(httpReq, "algorithm", "");
      String localeStr = this.getParam(httpReq, "locale", "en");
      List favoredHeadings = Collections.EMPTY_LIST;

      try {
         favoredHeadings = this.getDoubleParamList(httpReq, "heading");
      } catch (NumberFormatException var23) {
         throw new RuntimeException(var23);
      }

      StopWatch sw = (new StopWatch()).start();
      if(!this.hopper.getEncodingManager().supports(vehicleStr)) {
         ghRsp.addError(new IllegalArgumentException("Vehicle not supported: " + vehicleStr));
      } else if(enableElevation && !this.hopper.hasElevation()) {
         ghRsp.addError(new IllegalArgumentException("Elevation not supported!"));
      } else if(favoredHeadings.size() > 1 && favoredHeadings.size() != requestPoints.size()) {
         ghRsp.addError(new IllegalArgumentException("The number of \'heading\' parameters must be <= 1 or equal to the number of points (" + requestPoints.size() + ")"));
      }

      if(!ghRsp.hasErrors()) {
         FlagEncoder took = this.hopper.getEncodingManager().getEncoder(vehicleStr);
         GHRequest infoStr;
         if(favoredHeadings.size() > 0) {
            if(favoredHeadings.size() == 1) {
               ArrayList logStr = new ArrayList(Collections.nCopies(requestPoints.size(), Double.valueOf(Double.NaN)));
               logStr.set(0, favoredHeadings.get(0));
               infoStr = new GHRequest(requestPoints, logStr);
            } else {
               infoStr = new GHRequest(requestPoints, favoredHeadings);
            }
         } else {
            infoStr = new GHRequest(requestPoints);
         }

         this.initHints(infoStr, httpReq.getParameterMap());
         infoStr.setVehicle(took.toString()).setWeighting(weighting).setAlgorithm(algoStr).setLocale(localeStr).getHints().put("calcPoints", Boolean.valueOf(calcPoints)).put("instructions", Boolean.valueOf(enableInstructions)).put("wayPointMaxDistance", Double.valueOf(minPathPrecision));
         ghRsp = this.hopper.route(infoStr);
      }

      float took1 = sw.stop().getSeconds();
      String infoStr1 = httpReq.getRemoteAddr() + " " + httpReq.getLocale() + " " + httpReq.getHeader("User-Agent");
      String logStr1 = httpReq.getQueryString() + " " + infoStr1 + " " + requestPoints + ", took:" + took1 + ", " + algoStr + ", " + weighting + ", " + vehicleStr;
      httpRes.setHeader("X-GH-Took", "" + Math.round(took1 * 1000.0F));
      if(ghRsp.hasErrors()) {
         logger.error(logStr1 + ", errors:" + ghRsp.getErrors());
      } else {
         logger.info(logStr1 + ", distance: " + ghRsp.getDistance() + ", time:" + Math.round((float)ghRsp.getTime() / 60000.0F) + "min, points:" + ghRsp.getPoints().getSize() + ", debug - " + ghRsp.getDebugInfo());
      }

      if(writeGPX) {
         String map = this.createGPXString(httpReq, httpRes, ghRsp);
         if(ghRsp.hasErrors()) {
            httpRes.setStatus(400);
            httpRes.getWriter().append(map);
         } else {
            this.writeResponse(httpRes, map);
         }
      } else {
         Map map1 = this.routeSerializer.toJSON(ghRsp, calcPoints, pointsEncoded, enableElevation, enableInstructions);
         Object infoMap = map1.get("info");
         if(infoMap != null) {
            ((Map)infoMap).put("took", Integer.valueOf(Math.round(took1 * 1000.0F)));
         }

         if(ghRsp.hasErrors()) {
            this.writeJsonError(httpRes, 400, new JSONObject(map1));
         } else {
            this.writeJson(httpReq, httpRes, new JSONObject(map1));
         }
      }

   }

   protected String createGPXString(HttpServletRequest req, HttpServletResponse res, GHResponse rsp) {
      boolean includeElevation = this.getBooleanParam(req, "elevation", false);
      res.setCharacterEncoding("UTF-8");
      res.setContentType("application/xml");
      String trackName = this.getParam(req, "track", "GraphHopper Track");
      res.setHeader("Content-Disposition", "attachment;filename=GraphHopper.gpx");
      long time = this.getLongParam(req, "millis", System.currentTimeMillis());
      return rsp.hasErrors()?this.errorsToXML(rsp.getErrors()):rsp.getInstructions().createGPX(trackName, time, includeElevation);
   }

   String errorsToXML(List list) {
      try {
         DocumentBuilderFactory ex = DocumentBuilderFactory.newInstance();
         DocumentBuilder builder = ex.newDocumentBuilder();
         Document doc = builder.newDocument();
         Element gpxElement = doc.createElement("gpx");
         gpxElement.setAttribute("creator", "GraphHopper");
         gpxElement.setAttribute("version", "1.1");
         doc.appendChild(gpxElement);
         Element mdElement = doc.createElement("metadata");
         gpxElement.appendChild(mdElement);
         Element extensionsElement = doc.createElement("extensions");
         mdElement.appendChild(extensionsElement);
         Element messageElement = doc.createElement("message");
         extensionsElement.appendChild(messageElement);
         messageElement.setTextContent(((Throwable)list.get(0)).getMessage());
         Element hintsElement = doc.createElement("hints");
         extensionsElement.appendChild(hintsElement);
         Iterator transformerFactory = list.iterator();

         while(transformerFactory.hasNext()) {
            Throwable transformer = (Throwable)transformerFactory.next();
            Element writer = doc.createElement("error");
            hintsElement.appendChild(writer);
            writer.setAttribute("message", transformer.getMessage());
            writer.setAttribute("details", transformer.getClass().getName());
         }

         TransformerFactory transformerFactory1 = TransformerFactory.newInstance();
         Transformer transformer1 = transformerFactory1.newTransformer();
         StringWriter writer1 = new StringWriter();
         transformer1.transform(new DOMSource(doc), new StreamResult(writer1));
         return writer1.toString();
      } catch (Exception var13) {
         throw new RuntimeException(var13);
      }
   }

   protected List getPoints(HttpServletRequest req, String key) {
      String[] pointsAsStr = this.getParams(req, key);
      ArrayList infoPoints = new ArrayList(pointsAsStr.length);
      String[] arr$ = pointsAsStr;
      int len$ = pointsAsStr.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         String str = arr$[i$];
         String[] fromStrs = str.split(",");
         if(fromStrs.length == 2) {
            GHPoint point = GHPoint.parse(str);
            if(point != null) {
               infoPoints.add(point);
            }
         }
      }

      return infoPoints;
   }

   protected void initHints(GHRequest request, Map parameterMap) {
      WeightingMap m = request.getHints();
      Iterator i$ = parameterMap.entrySet().iterator();

      while(i$.hasNext()) {
         Entry e = (Entry)i$.next();
         if(((String[])e.getValue()).length == 1) {
            m.put((String)e.getKey(), ((String[])e.getValue())[0]);
         }
      }

   }
}
