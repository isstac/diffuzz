package com.graphhopper.tour;

import com.graphhopper.tour.Places;
import com.graphhopper.tour.util.Edge;
import com.graphhopper.util.CmdArgs;
import com.graphhopper.util.Helper;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

public class Matrix {
//   private static final Logger logger = LoggerFactory.getLogger(Matrix.class);
   private final List points;
   private final double[][] weights;

   public Matrix(List points) {
      this(points, new double[points.size()][points.size()]);
   }

   public Matrix(List points, double[][] weights) {
      if(weights.length == points.size() && weights[0].length == points.size()) {
         this.points = points;
         this.weights = weights;
      } else {
         throw new IllegalArgumentException("Points and weights must have same size.");
      }
   }

   public int size() {
      return this.points.size();
   }

   public List getPoints() {
      return Collections.unmodifiableList(this.points);
   }

   public double[][] getWeights() {
      return this.weights;
   }

   public double getWeight(int fromIndex, int toIndex) {
      return this.weights[fromIndex][toIndex];
   }

   public Matrix setWeight(int fromIndex, int toIndex, double weight) {
      this.weights[fromIndex][toIndex] = weight;
      return this;
   }

   public List edges() {
      int size = this.points.size();
      ArrayList edges = new ArrayList(size * (size - 1));

      for(int i = 0; i < size; ++i) {
         for(int j = 0; j < size; ++j) {
            if(j != i) {
               edges.add(new Edge(this.points.get(i), this.points.get(j), this.weights[i][j]));
            }
         }
      }

      assert edges.size() == size * (size - 1);

      return edges;
   }

   public List symmetricEdges() {
      int size = this.points.size();
      ArrayList edges = new ArrayList(size * (size - 1) / 2);

      for(int i = 0; i < size; ++i) {
         for(int j = i + 1; j < size; ++j) {
            double w1 = this.weights[i][j];
            double w2 = this.weights[j][i];
            double wm = (w1 + w2) / 2.0D;
            edges.add(new Edge(this.points.get(i), this.points.get(j), wm));
         }
      }

      assert edges.size() == size * (size - 1) / 2;

      return edges;
   }

   public static Matrix load(CmdArgs cmdArgs) throws IOException {
      String csvFile = cmdArgs.get("matrix.csv", "");
      if(Helper.isEmpty(csvFile)) {
         throw new IllegalArgumentException("You must specify a matrix file (matrix.csv=FILE).");
      } else {
         return readCsv(new File(csvFile));
      }
   }

   public static Matrix readCsv(File csvFile) throws IOException {
      if(!csvFile.exists()) {
         throw new IllegalStateException("Matrix file does not exist: " + csvFile.getAbsolutePath());
      } else {
//         logger.info("Loading matrix file " + csvFile.getAbsolutePath());
         FileReader in = new FileReader(csvFile);
         Throwable var2 = null;

         Matrix var3;
         try {
            var3 = readCsv(new BufferedReader(in));
         } catch (IOException var12) {
            var2 = var12;
            throw var12;
         } finally {
            if(in != null) {
               if(var2 != null) {
                  try {
                     in.close();
                  } catch (Throwable var11) {
                     var2.addSuppressed(var11);
                  }
               } else {
                  in.close();
               }
            }

         }

         return var3;
      }
   }

   public static Matrix readCsv(BufferedReader in) throws IOException {
      List places = Places.readCsv(in);
      Matrix matrix = new Matrix(places);
      List names = Places.names(places);
      String expected = "," + StringUtils.join(names, ',');
      String line = in.readLine();
      if(line != null && line.equals(expected)) {
         int size = places.size();

         int i;
         for(i = 0; i < size && (line = in.readLine()) != null; ++i) {
            line = StringUtils.strip(line);
            if(line.equals("")) {
               break;
            }

            String[] cols = StringUtils.split(line, ',');
            if(cols.length != size + 1) {
               throw new IllegalArgumentException("Expected " + (size + 1) + " columns, got " + cols.length + ": " + line);
            }

            expected = (String)names.get(i);
            if(!cols[0].equals(expected)) {
               throw new IllegalArgumentException("Expected " + expected + ", got " + cols[0]);
            }

            for(int j = 0; j < size; ++j) {
               double weight = Double.parseDouble(cols[j + 1]);
               matrix.setWeight(i, j, weight);
            }
         }

         if(i != size) {
            throw new IllegalArgumentException("Expected " + size + " rows, got " + i);
         } else {
            return matrix;
         }
      } else {
         throw new IllegalArgumentException("Expected header row, got " + line);
      }
   }

   public static void writeCsv(Matrix matrix, File csvFile) throws IOException {
      PrintStream out = new PrintStream(csvFile);
      Throwable var3 = null;

      try {
         writeCsv(matrix, out);
      } catch (IOException var12) {
         var3 = var12;
         throw var12;
      } finally {
         if(out != null) {
            if(var3 != null) {
               try {
                  out.close();
               } catch (Throwable var11) {
                  var3.addSuppressed(var11);
               }
            } else {
               out.close();
            }
         }

      }

   }

   public static void writeCsv(Matrix matrix, PrintStream out) throws IOException {
      List places = matrix.getPoints();
      double[][] weights = matrix.weights;
      Places.writeCsv(matrix.getPoints(), out);
      out.println();
      List names = Places.names(places);
      out.println("," + StringUtils.join(names, ','));

      for(int i = 0; i < places.size(); ++i) {
         out.println((String)names.get(i) + "," + StringUtils.join(weights[i], ','));
      }

   }
}
