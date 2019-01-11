package com.graphhopper.tour.tools;

import com.graphhopper.tour.Places;
import com.graphhopper.tour.tools.Command;
import java.io.File;
import java.util.List;

public class PlacesCLI extends Command {
   public void run() throws Exception {
      List places = Places.load(this.cmdArgs);
      if(this.ownArgs.size() == 1 && ((String)this.ownArgs.get(0)).endsWith(".txt")) {
         places = Places.selectByName(places, new File((String)this.ownArgs.get(0)));
      } else if(this.ownArgs.size() > 0) {
         places = Places.selectByName(places, this.ownArgs);
      }

      Places.writeCsv(places, System.out);
   }

   public static void main(String[] args) throws Exception {
      (new PlacesCLI()).parseArgs(args).run();
   }
}
