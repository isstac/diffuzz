package com.graphhopper.tour.tools;

import com.graphhopper.GraphHopper;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.tour.Matrix;
import com.graphhopper.tour.Places;
import com.graphhopper.tour.TourCalculator;
import com.graphhopper.tour.TourResponse;
import com.graphhopper.tour.tools.Command;
import com.graphhopper.tour.util.ProgressReporter;
import com.graphhopper.util.CmdArgs;
import com.graphhopper.util.shapes.GHPlace;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import javax.xml.stream.XMLStreamException;

public class TourCLI extends Command {
   protected void checkArgs() {
      if(this.ownArgs.size() < 2) {
         throw new IllegalArgumentException("At least two place names must be specified");
      }
   }

   public void run() throws IOException, XMLStreamException {
      this.cmdArgs = CmdArgs.readFromConfigAndMerge(this.cmdArgs, "config", "graphhopper.config");
      GraphHopper hopper = (new GraphHopper()).forServer().init(this.cmdArgs).setEncodingManager(new EncodingManager("car")).importOrLoad();
      Matrix matrix = Matrix.load(this.cmdArgs);
      List places = matrix.getPoints();
      List placesToVisit = Places.selectByName(places, this.ownArgs);
      TourCalculator tourCalculator = new TourCalculator(matrix, hopper);
      TourResponse rsp = tourCalculator.calcTour(placesToVisit, ProgressReporter.STDERR);
      Iterator i$;
      if(rsp.hasErrors()) {
         i$ = rsp.getErrors().iterator();

         while(i$.hasNext()) {
            Throwable p = (Throwable)i$.next();
            System.err.println(p);
         }
      } else {
         i$ = rsp.getPoints().iterator();

         while(i$.hasNext()) {
            GHPlace p1 = (GHPlace)i$.next();
            System.out.println(p1);
         }
      }

   }

   public static void main(String[] args) throws Exception {
      (new TourCLI()).parseArgs(args).run();
   }
}
