package com.graphhopper.http;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GHBaseServlet extends HttpServlet {
   protected static Logger logger = LoggerFactory.getLogger(GHBaseServlet.class);
   @Inject
   @Named("jsonpAllowed")
   private boolean jsonpAllowed;

   protected void writeJson(HttpServletRequest req, HttpServletResponse res, JSONObject json) throws JSONException, IOException {
      this.writeJson(req, res, (Object)json);
   }

   protected void writeJson(HttpServletRequest req, HttpServletResponse res, JSONArray json) throws JSONException, IOException {
      this.writeJson(req, res, (Object)json);
   }

   private void writeJson(HttpServletRequest req, HttpServletResponse res, Object json) throws JSONException, IOException {
      String type = this.getParam(req, "type", "json");
      res.setCharacterEncoding("UTF-8");
      boolean var10000;
      if(!this.getBooleanParam(req, "debug", false) && !this.getBooleanParam(req, "pretty", false)) {
         var10000 = false;
      } else {
         var10000 = true;
      }

      if("jsonp".equals(type)) {
         res.setContentType("application/javascript");
         if(!this.jsonpAllowed) {
            this.writeError(res, 400, "Server is not configured to allow jsonp!");
            return;
         }

         String callbackName = this.getParam(req, "callback", (String)null);
         if(callbackName == null) {
            this.writeError(res, 400, "No callback provided, necessary if type=jsonp");
            return;
         }

         this.writeResponse(res, callbackName + "(" + json.toString() + ")");
      } else {
         this.writeResponse(res, json.toString());
      }

   }

   protected void writeError(HttpServletResponse res, int code, String message) {
      JSONObject json = new JSONObject();
      json.put("message", message);
      this.writeJsonError(res, code, json);
   }

   protected void writeJsonError(HttpServletResponse res, int code, JSONObject json) {
      try {
         res.setContentType("application/json");
         res.setCharacterEncoding("UTF-8");
         res.setStatus(code);
         res.getWriter().append(json.toString(2));
      } catch (IOException var5) {
         logger.error("Cannot write error " + var5.getMessage());
      }

   }

   protected String getParam(HttpServletRequest req, String key, String _default) {
      String[] l = (String[])req.getParameterMap().get(key);
      return l != null && l.length > 0?l[0]:_default;
   }

   protected String[] getParams(HttpServletRequest req, String key) {
      String[] l = (String[])req.getParameterMap().get(key);
      return l != null && l.length > 0?l:new String[0];
   }

   protected List getDoubleParamList(HttpServletRequest req, String key) {
      String[] l = (String[])req.getParameterMap().get(key);
      if(l != null && l.length > 0) {
         ArrayList doubleList = new ArrayList(l.length);
         String[] arr$ = l;
         int len$ = l.length;

         for(int i$ = 0; i$ < len$; ++i$) {
            String s = arr$[i$];
            doubleList.add(Double.valueOf(s));
         }

         return doubleList;
      } else {
         return Collections.emptyList();
      }
   }

   protected long getLongParam(HttpServletRequest req, String key, long _default) {
      try {
         return Long.parseLong(this.getParam(req, key, "" + _default));
      } catch (Exception var6) {
         return _default;
      }
   }

   protected boolean getBooleanParam(HttpServletRequest req, String key, boolean _default) {
      try {
         return Boolean.parseBoolean(this.getParam(req, key, "" + _default));
      } catch (Exception var5) {
         return _default;
      }
   }

   protected double getDoubleParam(HttpServletRequest req, String key, double _default) {
      try {
         return Double.parseDouble(this.getParam(req, key, "" + _default));
      } catch (Exception var6) {
         return _default;
      }
   }

   public void writeResponse(HttpServletResponse res, String str) {
      try {
         res.setStatus(200);
         res.getWriter().append(str);
      } catch (IOException var4) {
         logger.error("Cannot write message:" + str, var4);
      }

   }
}
