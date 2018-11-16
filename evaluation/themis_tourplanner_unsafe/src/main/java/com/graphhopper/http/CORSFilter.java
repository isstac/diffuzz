package com.graphhopper.http;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.servlets.UserAgentFilter;

public class CORSFilter extends UserAgentFilter {
   public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
      if(!"jsonp".equals(request.getParameter("type"))) {
         HttpServletResponse rsp = (HttpServletResponse)response;
         rsp.setHeader("Access-Control-Allow-Origin", "*");
      }

      super.doFilter(request, response, chain);
   }
}
