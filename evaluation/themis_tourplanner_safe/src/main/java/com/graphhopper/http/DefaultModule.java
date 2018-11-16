package com.graphhopper.http;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.graphhopper.GraphHopper;
import com.graphhopper.http.RouteSerializer;
import com.graphhopper.http.SimpleRouteSerializer;
import com.graphhopper.http.TourSerializer;
import com.graphhopper.tour.Matrix;
import com.graphhopper.tour.TourCalculator;
import com.graphhopper.util.CmdArgs;
import com.graphhopper.util.TranslationMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultModule extends AbstractModule {
   private final Logger logger = LoggerFactory.getLogger(this.getClass());
   protected final CmdArgs args;
   private GraphHopper graphHopper;

   public DefaultModule(CmdArgs args) {
      this.args = CmdArgs.readFromConfigAndMerge(args, "config", "graphhopper.config");
   }

   public GraphHopper getGraphHopper() {
      if(this.graphHopper == null) {
         throw new IllegalStateException("createGraphHopper not called");
      } else {
         return this.graphHopper;
      }
   }

   protected GraphHopper createGraphHopper(CmdArgs args) {
      GraphHopper tmp = (new GraphHopper()).forServer().init(args);
      tmp.importOrLoad();
      this.logger.info("loaded graph at:" + tmp.getGraphHopperLocation() + ", source:" + tmp.getOSMFile() + ", flagEncoders:" + tmp.getEncodingManager() + ", class:" + tmp.getGraphHopperStorage().toDetailsString());
      return tmp;
   }

   protected void configure() {
      try {
         this.graphHopper = this.createGraphHopper(this.args);
         this.bind(GraphHopper.class).toInstance(this.graphHopper);
         this.bind(TranslationMap.class).toInstance(this.graphHopper.getTranslationMap());
         long ex = this.args.getLong("web.timeout", 3000L);
         this.bind(Long.class).annotatedWith(Names.named("timeout")).toInstance(Long.valueOf(ex));
         boolean jsonpAllowed = this.args.getBool("web.jsonpAllowed", false);
         if(!jsonpAllowed) {
            this.logger.info("jsonp disabled");
         }

         this.bind(Boolean.class).annotatedWith(Names.named("jsonpAllowed")).toInstance(Boolean.valueOf(jsonpAllowed));
         this.bind(RouteSerializer.class).toInstance(new SimpleRouteSerializer(this.graphHopper.getGraphHopperStorage().getBounds()));
         Matrix matrix = Matrix.load(this.args);
         this.bind(new TypeLiteral<Object>() {}).toInstance(matrix.getPoints());
         this.bind(TourCalculator.class).toInstance(new TourCalculator(matrix, this.graphHopper));
         this.bind(TourSerializer.class).toInstance(new TourSerializer());
      } catch (Exception var5) {
         throw new IllegalStateException("Couldn\'t load graph", var5);
      }
   }
}
