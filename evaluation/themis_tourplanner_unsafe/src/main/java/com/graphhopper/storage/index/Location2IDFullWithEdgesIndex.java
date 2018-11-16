package com.graphhopper.storage.index;

import com.graphhopper.routing.util.AllEdgesIterator;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.storage.Graph;
import com.graphhopper.storage.NodeAccess;
import com.graphhopper.storage.index.LocationIndex;
import com.graphhopper.storage.index.QueryResult;
import com.graphhopper.util.DistanceCalc;
import com.graphhopper.util.Helper;

public class Location2IDFullWithEdgesIndex implements LocationIndex {
   private DistanceCalc calc;
   private final Graph graph;
   private final NodeAccess nodeAccess;
   private boolean closed;

   public Location2IDFullWithEdgesIndex(Graph g) {
      this.calc = Helper.DIST_EARTH;
      this.closed = false;
      this.graph = g;
      this.nodeAccess = g.getNodeAccess();
   }

   public boolean loadExisting() {
      return true;
   }

   public LocationIndex setResolution(int resolution) {
      return this;
   }

   public LocationIndex setApproximation(boolean approxDist) {
      if(approxDist) {
         this.calc = Helper.DIST_PLANE;
      } else {
         this.calc = Helper.DIST_EARTH;
      }

      return this;
   }

   public LocationIndex prepareIndex() {
      return this;
   }

   public int findID(double lat, double lon) {
      return this.findClosest(lat, lon, EdgeFilter.ALL_EDGES).getClosestNode();
   }

   public QueryResult findClosest(double queryLat, double queryLon, EdgeFilter filter) {
      if(this.isClosed()) {
         throw new IllegalStateException("You need to create a new LocationIndex instance as it is already closed");
      } else {
         QueryResult res = new QueryResult(queryLat, queryLon);
         double foundDist = Double.MAX_VALUE;
         AllEdgesIterator iter = this.graph.getAllEdges();

         while(true) {
            do {
               if(!iter.next()) {
                  return res;
               }
            } while(!filter.accept(iter));

            for(int i = 0; i < 2; ++i) {
               int node;
               if(i == 0) {
                  node = iter.getBaseNode();
               } else {
                  node = iter.getAdjNode();
               }

               double fromLat = this.nodeAccess.getLatitude(node);
               double fromLon = this.nodeAccess.getLongitude(node);
               double fromDist = this.calc.calcDist(fromLat, fromLon, queryLat, queryLon);
               if(fromDist >= 0.0D) {
                  if(fromDist < foundDist) {
                     res.setQueryDistance(fromDist);
                     res.setClosestEdge(iter.detach(false));
                     res.setClosestNode(node);
                     foundDist = fromDist;
                  }

                  if(i <= 0) {
                     int toNode = iter.getAdjNode();
                     double toLat = this.nodeAccess.getLatitude(toNode);
                     double toLon = this.nodeAccess.getLongitude(toNode);
                     if(this.calc.validEdgeDistance(queryLat, queryLon, fromLat, fromLon, toLat, toLon)) {
                        double distEdge = this.calc.calcDenormalizedDist(this.calc.calcNormalizedEdgeDistance(queryLat, queryLon, fromLat, fromLon, toLat, toLon));
                        if(distEdge < foundDist) {
                           res.setQueryDistance(distEdge);
                           res.setClosestNode(node);
                           res.setClosestEdge(iter);
                           if(fromDist > this.calc.calcDist(toLat, toLon, queryLat, queryLon)) {
                              res.setClosestNode(toNode);
                           }

                           foundDist = distEdge;
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public LocationIndex create(long size) {
      return this;
   }

   public void flush() {
   }

   public void close() {
      this.closed = true;
   }

   public boolean isClosed() {
      return this.closed;
   }

   public long getCapacity() {
      return 0L;
   }

   public void setSegmentSize(int bytes) {
   }
}
