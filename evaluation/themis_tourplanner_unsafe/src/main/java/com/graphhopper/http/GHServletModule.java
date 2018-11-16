package com.graphhopper.http;

import com.google.inject.servlet.ServletModule;
import com.graphhopper.http.CORSFilter;
import com.graphhopper.http.GHGZIPHook;
import com.graphhopper.http.GraphHopperServlet;
import com.graphhopper.http.I18NServlet;
import com.graphhopper.http.IPFilter;
import com.graphhopper.http.InfoServlet;
import com.graphhopper.http.NearestServlet;
import com.graphhopper.http.PlacesServlet;
import com.graphhopper.http.TourServlet;
import com.graphhopper.util.CmdArgs;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Singleton;

public class GHServletModule extends ServletModule {
   protected Map params = new HashMap();
   protected final CmdArgs args;

   public GHServletModule(CmdArgs args) {
      this.args = args;
      this.params.put("mimeTypes", "text/html,text/plain,text/xml,application/xhtml+xml,text/css,application/json,application/javascript,image/svg+xml");
   }

   protected void configureServlets() {
      this.filter("*", new String[0]).through(GHGZIPHook.class, this.params);
      this.bind(GHGZIPHook.class).in(Singleton.class);
      this.filter("*", new String[0]).through(CORSFilter.class, this.params);
      this.bind(CORSFilter.class).in(Singleton.class);
      this.filter("*", new String[0]).through(IPFilter.class);
      this.bind(IPFilter.class).toInstance(new IPFilter(this.args.get("jetty.whiteips", ""), this.args.get("jetty.blackips", "")));
      this.serve("/i18n*", new String[0]).with(I18NServlet.class);
      this.bind(I18NServlet.class).in(Singleton.class);
      this.serve("/info*", new String[0]).with(InfoServlet.class);
      this.bind(InfoServlet.class).in(Singleton.class);
      this.serve("/route*", new String[0]).with(GraphHopperServlet.class);
      this.bind(GraphHopperServlet.class).in(Singleton.class);
      this.serve("/nearest*", new String[0]).with(NearestServlet.class);
      this.bind(NearestServlet.class).in(Singleton.class);
      this.serve("/places*", new String[0]).with(PlacesServlet.class);
      this.bind(PlacesServlet.class).in(Singleton.class);
      this.serve("/tour*", new String[0]).with(TourServlet.class);
      this.bind(TourServlet.class).in(Singleton.class);
   }
}
