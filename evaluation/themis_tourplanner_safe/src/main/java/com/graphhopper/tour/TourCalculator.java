package com.graphhopper.tour;

import com.graphhopper.GraphHopper;
import com.graphhopper.routing.util.DefaultEdgeFilter;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.storage.index.LocationIndex;
import com.graphhopper.storage.index.QueryResult;
import com.graphhopper.tour.Matrix;
import com.graphhopper.tour.TourResponse;
import com.graphhopper.tour.util.Edge;
import com.graphhopper.tour.util.Graph;
import com.graphhopper.tour.util.ProgressReporter;
import com.graphhopper.util.DistanceCalcEarth;
import com.graphhopper.util.shapes.GHPlace;
import com.graphhopper.util.shapes.GHPoint;
import com.graphhopper.util.shapes.GHPoint3D;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

public class TourCalculator {
//   private final Logger logger;
   private final Matrix matrix;
   private final GraphHopper graphHopper;
   private final List sortedEdges;
   private final Map knownPoints;
   private final Map queryResults;

   public TourCalculator(Matrix matrix) {
      this(matrix, (GraphHopper)null);
   }

   public TourCalculator(Matrix matrix, GraphHopper graphHopper) {
//      this.logger = LoggerFactory.getLogger(this.getClass());
      this.matrix = matrix;
      this.graphHopper = graphHopper;
      this.sortedEdges = matrix.symmetricEdges();
      Collections.sort(this.sortedEdges, new Edge.WeightComparator());
      LocationIndex locationIndex = graphHopper.getLocationIndex();
      EncodingManager encodingManager = graphHopper.getEncodingManager();
      FlagEncoder flagEncoder = (FlagEncoder)encodingManager.fetchEdgeEncoders().get(0);
      DefaultEdgeFilter edgeFilter = new DefaultEdgeFilter(flagEncoder);
      this.knownPoints = new HashMap();
      this.queryResults = new HashMap();
      Iterator i$ = matrix.getPoints().iterator();

      while(i$.hasNext()) {
         GHPlace p = (GHPlace)i$.next();
         this.knownPoints.put(p, p);
         this.queryResults.put(p, locationIndex.findClosest(p.lat, p.lon, edgeFilter));
      }

   }

   public TourResponse calcTour(List points) {
      return this.calcTour(points, (ProgressReporter)null);
   }

   public TourResponse calcTour(List points, ProgressReporter progressReporter) {
      TourResponse rsp = new TourResponse();
      if(points.size() < 2) {
         rsp.addError(new IllegalArgumentException("At least two points must be specified"));
         return rsp;
      } else {
         Iterator root = points.iterator();

         GHPoint reqPoints;
         do {
            if(!root.hasNext()) {
               if(progressReporter == null) {
                  progressReporter = ProgressReporter.SILENT;
               }

               GHPlace root1 = (GHPlace)this.knownPoints.get(points.get(0));
               HashSet reqPoints1 = new HashSet();
               reqPoints1.addAll(points);
               Graph minSpanningTree = this.calcMinSpanningTree(root1, reqPoints1, progressReporter);
               List rspPoints = minSpanningTree.depthFirstWalk(root1);
               rspPoints.add(root1);
               rsp.setPoints(rspPoints);
               return rsp;
            }

            reqPoints = (GHPoint)root.next();
         } while(this.knownPoints.containsKey(reqPoints));

         rsp.addError(new IllegalArgumentException("Unknown point " + reqPoints));
         return rsp;
      }
   }

   protected Graph calcMinSpanningTree(GHPlace root, Set reqPoints, ProgressReporter progressReporter) {
      Graph result = (new Graph()).add((Object)root);
      int complete = 0;
      int total = reqPoints.size() - 1;

      try {
         progressReporter.reportProgress(complete, total);
      } catch (IOException var16) {
         ;
      }

      while(true) {
         while(result.size() < reqPoints.size()) {
            Iterator i$ = this.sortedEdges.iterator();

            while(i$.hasNext()) {
               Edge e = (Edge)i$.next();
               if(result.contains(e.to) && !result.contains(e.from)) {
                  e.reverse();
               }

               QueryResult fromQR = (QueryResult)this.queryResults.get(e.from);
               QueryResult toQR = (QueryResult)this.queryResults.get(e.to);
               DistanceCalcEarth distanceCalc = new DistanceCalcEarth();
               fromQR.calcSnappedPoint(distanceCalc);
               toQR.calcSnappedPoint(distanceCalc);
               GHPoint3D fromSnappedPoint = fromQR.getSnappedPoint();
               GHPoint3D toSnappedPoint = toQR.getSnappedPoint();
               if(result.contains(e.from) && !result.contains(e.to) && reqPoints.contains(e.to)) {
                  assert reqPoints.contains(e.from);

                  if(!result.contains(e) && reqPoints.contains(e.to)) {
                     result.add(e);

                     try {
                        ++complete;
                        progressReporter.reportProgress(complete, total);
                     } catch (IOException var15) {
                        ;
                     }
                     break;
                  }
               }
            }
         }

         return result;
      }
   }
}
