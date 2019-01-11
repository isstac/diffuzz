package com.graphhopper.http;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IPFilter implements Filter {
   private final Logger logger = LoggerFactory.getLogger(this.getClass());
   private final Set whites;
   private final Set blacks;

   public IPFilter(String whiteList, String blackList) {
      this.whites = this.createSet(whiteList.split(","));
      this.blacks = this.createSet(blackList.split(","));
      if(!this.whites.isEmpty()) {
         this.logger.debug("whitelist:" + this.whites);
      }

      if(!blackList.isEmpty()) {
         this.logger.debug("blacklist:" + this.blacks);
      }

      if(!this.blacks.isEmpty() && !this.whites.isEmpty()) {
         throw new IllegalArgumentException("blacklist and whitelist at the same time?");
      }
   }

   public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
      String ip = request.getRemoteAddr();
      if(this.accept(ip)) {
         chain.doFilter(request, response);
      } else {
         this.logger.warn("Did not accept IP " + ip);
         ((HttpServletResponse)response).sendError(403);
      }

   }

   public boolean accept(String ip) {
      if(this.whites.isEmpty() && this.blacks.isEmpty()) {
         return true;
      } else {
         Iterator i$;
         String b;
         if(!this.whites.isEmpty()) {
            i$ = this.whites.iterator();

            do {
               if(!i$.hasNext()) {
                  return false;
               }

               b = (String)i$.next();
            } while(!this.simpleMatch(ip, b));

            return true;
         } else if(this.blacks.isEmpty()) {
            throw new IllegalStateException("cannot happen");
         } else {
            i$ = this.blacks.iterator();

            do {
               if(!i$.hasNext()) {
                  return true;
               }

               b = (String)i$.next();
            } while(!this.simpleMatch(ip, b));

            return false;
         }
      }
   }

   public void init(FilterConfig filterConfig) throws ServletException {
   }

   public void destroy() {
   }

   private Set createSet(String[] split) {
      HashSet set = new HashSet(split.length);
      String[] arr$ = split;
      int len$ = split.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         String str = arr$[i$];
         str = str.trim();
         if(!str.isEmpty()) {
            set.add(str);
         }
      }

      return set;
   }

   public boolean simpleMatch(String ip, String pattern) {
      String[] ipParts = pattern.split("\\*");
      String[] arr$ = ipParts;
      int len$ = ipParts.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         String ipPart = arr$[i$];
         int idx = ip.indexOf(ipPart);
         if(idx == -1) {
            return false;
         }

         ip = ip.substring(idx + ipPart.length());
      }

      return true;
   }
}
