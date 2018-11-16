package com.graphhopper.http;

import com.graphhopper.http.GHBaseServlet;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONObject;

public class InvalidRequestServlet extends GHBaseServlet {
   protected void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
      JSONObject json = new JSONObject();
      json.put("message", "Not found");
      this.writeJsonError(res, 404, json);
   }
}
