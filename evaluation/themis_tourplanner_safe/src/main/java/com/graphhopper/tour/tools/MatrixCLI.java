package com.graphhopper.tour.tools;

import com.graphhopper.GraphHopper;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.tour.Matrix;
import com.graphhopper.tour.MatrixCalculator;
import com.graphhopper.tour.Places;
import com.graphhopper.tour.tools.Command;
import com.graphhopper.util.CmdArgs;
import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.xml.stream.XMLStreamException;

public class MatrixCLI extends Command {
   public void run() throws IOException, XMLStreamException {
      if(this.cmdArgs.has("osmreader.osm")) {
         this.readPlacesAndCalculateMatrix();
      } else {
         this.readAndRewriteMatrix();
      }

   }

   private void readAndRewriteMatrix() throws IOException {
      Matrix matrix = Matrix.load(this.cmdArgs);
      Matrix.writeCsv(matrix, System.out);
   }

   private void readPlacesAndCalculateMatrix() throws IOException, XMLStreamException {
      this.cmdArgs = CmdArgs.readFromConfigAndMerge(this.cmdArgs, "config", "graphhopper.config");
      GraphHopper hopper = (new GraphHopper()).forServer().init(this.cmdArgs).setEncodingManager(new EncodingManager("car")).importOrLoad();
      List places = Places.load(this.cmdArgs);
      if(this.ownArgs.size() == 1 && ((String)this.ownArgs.get(0)).endsWith(".txt")) {
         places = Places.selectByName(places, new File((String)this.ownArgs.get(0)));
      } else if(this.ownArgs.size() > 0) {
         places = Places.selectByName(places, this.ownArgs);
      }

      Matrix matrix = (new MatrixCalculator(hopper)).calcMatrix(places);
      Matrix.writeCsv(matrix, System.out);
   }

   public static void main(String[] args) throws Exception {
      (new MatrixCLI()).parseArgs(args).run();
   }
}
