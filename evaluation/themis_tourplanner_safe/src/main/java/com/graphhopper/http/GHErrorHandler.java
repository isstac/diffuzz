package com.graphhopper.http;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GHErrorHandler extends ErrorHandler {
   private static final long serialVersionUID = 1L;
   private final Logger logger = LoggerFactory.getLogger(GHErrorHandler.class);

   public void handle(String str, Request req, HttpServletRequest httpReq, HttpServletResponse httpRes) throws IOException {
      Throwable throwable = (Throwable)httpReq.getAttribute("javax.servlet.error.exception");
      String message;
      if(throwable != null) {
         message = throwable.getMessage();
         this.logger.error(message + ", via:" + httpReq.getRequestURL(), throwable);
      } else {
         message = (String)httpReq.getAttribute("javax.servlet.error.message");
         if(message != null) {
            this.logger.error("Internal error " + message + "! Via:" + httpReq.getRequestURL());
         } else {
            this.logger.error("Internal error " + str + ", throwable not known! Via:" + httpReq.getRequestURL());
         }
      }

      httpRes.setStatus(500);
   }
}
