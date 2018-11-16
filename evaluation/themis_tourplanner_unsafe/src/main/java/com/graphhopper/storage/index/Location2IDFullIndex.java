package com.graphhopper.storage.index;

import com.graphhopper.routing.util.AllEdgesIterator;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.storage.Graph;
import com.graphhopper.storage.NodeAccess;
import com.graphhopper.storage.index.LocationIndex;
import com.graphhopper.storage.index.QueryResult;
import com.graphhopper.util.DistanceCalc;
import com.graphhopper.util.Helper;
import com.graphhopper.util.shapes.Circle;

public class Location2IDFullIndex implements LocationIndex {
   private DistanceCalc calc;
   private final Graph graph;
   private final NodeAccess nodeAccess;
   private boolean closed;

   public Location2IDFullIndex(Graph g) {
      this.calc = Helper.DIST_PLANE;
      this.closed = false;
      this.graph = g;
      this.nodeAccess = g.getNodeAccess();
   }

   public boolean loadExisting() {
      return true;
   }

   public LocationIndex setApproximation(boolean approxDist) {
      if(approxDist) {
         this.calc = Helper.DIST_PLANE;
      } else {
         this.calc = Helper.DIST_EARTH;
      }

      return this;
   }

   public LocationIndex setResolution(int resolution) {
      return this;
   }

   public LocationIndex prepareIndex() {
      return this;
   }

   public QueryResult findClosest(double queryLat, double queryLon, EdgeFilter edgeFilter) {
      if(this.isClosed()) {
         throw new IllegalStateException("You need to create a new LocationIndex instance as it is already closed");
      } else {
         QueryResult res = new QueryResult(queryLat, queryLon);
         Circle circle = null;
         AllEdgesIterator iter = this.graph.getAllEdges();

         while(true) {
            do {
               if(!iter.next()) {
                  return res;
               }
            } while(!edgeFilter.accept(iter));

            for(int i = 0; i < 2; ++i) {
               int node;
               if(i == 0) {
                  node = iter.getBaseNode();
               } else {
                  node = iter.getAdjNode();
               }

               double tmpLat = this.nodeAccess.getLatitude(node);
               double tmpLon = this.nodeAccess.getLongitude(node);
               double dist = this.calc.calcDist(tmpLat, tmpLon, queryLat, queryLon);
               if(circle == null || dist < this.calc.calcDist(circle.getLat(), circle.getLon(), queryLat, queryLon)) {
                  res.setClosestEdge(iter.detach(false));
                  res.setClosestNode(node);
                  res.setQueryDistance(dist);
                  if(dist <= 0.0D) {
                     break;
                  }

                  circle = new Circle(tmpLat, tmpLon, dist, this.calc);
               }
            }
         }
      }
   }

   public int findID(double lat, double lon) {
      return this.findClosest(lat, lon, EdgeFilter.ALL_EDGES).getClosestNode();
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
