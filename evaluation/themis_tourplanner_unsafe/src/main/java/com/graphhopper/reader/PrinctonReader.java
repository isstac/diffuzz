package com.graphhopper.reader;

import com.graphhopper.storage.Graph;
import com.graphhopper.util.Helper;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class PrinctonReader {
   private Graph g;
   private InputStream is;

   public PrinctonReader(Graph graph) {
      this.g = graph;
   }

   public PrinctonReader setStream(InputStream is) {
      this.is = is;
      return this;
   }

   public void read() {
      BufferedReader reader = new BufferedReader(new InputStreamReader(this.is), 8192);
      byte lineNo = 0;

      try {
         int var19 = lineNo + 1;
         int ex = Integer.parseInt(reader.readLine());
         ++var19;
         int edges = Integer.parseInt(reader.readLine());

         for(int i = 0; i < edges; ++i) {
            ++var19;
            String line = reader.readLine();
            String[] args = line.split(" ");
            int from = -1;
            int to = -1;
            double dist = -1.0D;
            int counter = 0;

            for(int j = 0; j < args.length; ++j) {
               if(!Helper.isEmpty(args[j])) {
                  if(counter == 0) {
                     from = Integer.parseInt(args[j]);
                  } else if(counter == 1) {
                     to = Integer.parseInt(args[j]);
                  } else {
                     dist = Double.parseDouble(args[j]);
                  }

                  ++counter;
               }
            }

            if(counter != 3) {
               throw new RuntimeException("incorrect read!? from:" + from + ", to:" + to + ", dist:" + dist);
            }

            this.g.edge(from, to, dist, false);
         }
      } catch (Exception var17) {
         throw new RuntimeException("Problem in line " + lineNo, var17);
      } finally {
         Helper.close(reader);
      }

   }
}
