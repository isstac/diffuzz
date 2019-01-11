package com.graphhopper.tour;

import com.graphhopper.GraphHopper;
import com.graphhopper.routing.Path;
import com.graphhopper.tour.Matrix;
import com.graphhopper.tour.PathCalculator;
import com.graphhopper.util.shapes.GHPoint;
import java.util.List;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

public class MatrixCalculator {
//   private final Logger logger = LoggerFactory.getLogger(this.getClass());
   private final GraphHopper hopper;

   public MatrixCalculator(GraphHopper hopper) {
      this.hopper = hopper;
   }

   public Matrix calcMatrix(List points) {
      PathCalculator pc = new PathCalculator(this.hopper);
      Matrix matrix = new Matrix(points);
      int size = points.size();
      int numPaths = size * (size - 1);
//      this.logger.info("Calculating " + numPaths + " pairwise paths");
      int i = 0;

      for(int c = 0; i < size; ++i) {
         GHPoint from = (GHPoint)points.get(i);

         for(int j = 0; j < size; ++j) {
            if(j != i) {
               GHPoint to = (GHPoint)points.get(j);
               Path path = pc.calcPath(from, to);
               matrix.setWeight(i, j, path.getWeight());
               ++c;
               if(c % 100 == 0) {
//                  this.logger.info(c + "/" + numPaths);
               }
            }
         }
      }

      return matrix;
   }
}
