package com.graphhopper.storage.index;

import com.graphhopper.coll.GHBitSet;
import com.graphhopper.coll.GHBitSetImpl;
import com.graphhopper.coll.GHTBitSet;
import com.graphhopper.geohash.KeyAlgo;
import com.graphhopper.geohash.LinearKeyAlgo;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.storage.DataAccess;
import com.graphhopper.storage.Directory;
import com.graphhopper.storage.Graph;
import com.graphhopper.storage.NodeAccess;
import com.graphhopper.storage.RAMDirectory;
import com.graphhopper.storage.index.LocationIndex;
import com.graphhopper.storage.index.QueryResult;
import com.graphhopper.util.BreadthFirstSearch;
import com.graphhopper.util.DistanceCalc;
import com.graphhopper.util.Helper;
import com.graphhopper.util.StopWatch;
import com.graphhopper.util.shapes.BBox;
import com.graphhopper.util.shapes.GHPoint;
import java.util.Arrays;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

class Location2IDQuadtree implements LocationIndex {
   private static final int MAGIC_INT = 174507;
//   private final Logger logger = LoggerFactory.getLogger(this.getClass());
   private KeyAlgo keyAlgo;
   protected DistanceCalc distCalc;
   private final DataAccess index;
   private double maxRasterWidth2InMeterNormed;
   private final Graph graph;
   private final NodeAccess nodeAccess;
   private int lonSize;
   private int latSize;

   public Location2IDQuadtree(Graph g, Directory dir) {
      this.distCalc = Helper.DIST_PLANE;
      this.graph = g;
      this.nodeAccess = g.getNodeAccess();
      this.index = dir.find("loc2idIndex");
      this.setResolution(10000);
   }

   public LocationIndex setApproximation(boolean approxDist) {
      if(approxDist) {
         this.distCalc = Helper.DIST_PLANE;
      } else {
         this.distCalc = Helper.DIST_EARTH;
      }

      return this;
   }

   public long getCapacity() {
      return this.index.getCapacity() / 4L;
   }

   public boolean loadExisting() {
      if(!this.index.loadExisting()) {
         return false;
      } else if(this.index.getHeader(0) != 174507) {
         throw new IllegalStateException("incorrect loc2id index version");
      } else {
         int lat = this.index.getHeader(4);
         int lon = this.index.getHeader(8);
         int checksum = this.index.getHeader(12);
         if(checksum != this.graph.getNodes()) {
            throw new IllegalStateException("index was created from a different graph with " + checksum + ". Current nodes:" + this.graph.getNodes());
         } else {
            this.initAlgo(lat, lon);
            return true;
         }
      }
   }

   public LocationIndex create(long size) {
      throw new UnsupportedOperationException("Not supported. Use prepareIndex instead.");
   }

   public LocationIndex setResolution(int resolution) {
      this.initLatLonSize(resolution);
      return this;
   }

   public LocationIndex prepareIndex() {
      this.initBuffer();
      this.initAlgo(this.latSize, this.lonSize);
      StopWatch sw = (new StopWatch()).start();
      GHBitSet filledIndices = this.fillQuadtree(this.latSize * this.lonSize);
      int fillQT = filledIndices.getCardinality();
      float res1 = sw.stop().getSeconds();
      sw = (new StopWatch()).start();
      int counter = this.fillEmptyIndices(filledIndices);
      float fillEmpty = sw.stop().getSeconds();
//      this.logger.info("filled quadtree index array in " + res1 + "s. size is " + this.getCapacity() + " (" + fillQT + "). filled empty " + counter + " in " + fillEmpty + "s");
      this.flush();
      return this;
   }

   private void initLatLonSize(int size) {
      this.latSize = this.lonSize = (int)Math.sqrt((double)size);
      if(this.latSize * this.lonSize < size) {
         ++this.lonSize;
      }

   }

   private void initBuffer() {
      this.index.setSegmentSize(this.latSize * this.lonSize * 4);
      this.index.create((long)(this.latSize * this.lonSize * 4));
   }

   void initAlgo(int lat, int lon) {
      this.latSize = lat;
      this.lonSize = lon;
      BBox b = this.graph.getBounds();
      this.keyAlgo = (new LinearKeyAlgo(lat, lon)).setBounds(b);
      double max = Math.max(this.distCalc.calcDist(b.minLat, b.minLon, b.minLat, b.maxLon), this.distCalc.calcDist(b.minLat, b.minLon, b.maxLat, b.minLon));
      this.maxRasterWidth2InMeterNormed = this.distCalc.calcNormalizedDist(max / Math.sqrt((double)this.getCapacity()) * 2.0D);
   }

   protected double getMaxRasterWidthMeter() {
      return this.distCalc.calcDenormalizedDist(this.maxRasterWidth2InMeterNormed) / 2.0D;
   }

   private GHBitSet fillQuadtree(int size) {
      int locs = this.graph.getNodes();
      if(locs <= 0) {
         throw new IllegalStateException("check your graph - it is empty!");
      } else {
         GHBitSetImpl filledIndices = new GHBitSetImpl(size);
         GHPoint coord = new GHPoint();

         for(int nodeId = 0; nodeId < locs; ++nodeId) {
            double lat = this.nodeAccess.getLatitude(nodeId);
            double lon = this.nodeAccess.getLongitude(nodeId);
            int key = (int)this.keyAlgo.encode(lat, lon);
            long bytePos = (long)key * 4L;
            if(filledIndices.contains(key)) {
               int oldNodeId = this.index.getInt(bytePos);
               this.keyAlgo.decode((long)key, coord);
               double distNew = this.distCalc.calcNormalizedDist(coord.lat, coord.lon, lat, lon);
               double oldLat = this.nodeAccess.getLatitude(oldNodeId);
               double oldLon = this.nodeAccess.getLongitude(oldNodeId);
               double distOld = this.distCalc.calcNormalizedDist(coord.lat, coord.lon, oldLat, oldLon);
               if(distNew < distOld) {
                  this.index.setInt(bytePos, nodeId);
               }
            } else {
               this.index.setInt(bytePos, nodeId);
               filledIndices.add(key);
            }
         }

         return filledIndices;
      }
   }

   private int fillEmptyIndices(GHBitSet filledIndices) {
      int len = this.latSize * this.lonSize;
      DataAccess indexCopy = (new RAMDirectory()).find("tempIndexCopy");
      indexCopy.setSegmentSize(this.index.getSegmentSize()).create(this.index.getCapacity());
      GHBitSetImpl indicesCopy = new GHBitSetImpl(len);
      int initializedCounter = filledIndices.getCardinality();
      int[] takenFrom = new int[len];
      Arrays.fill(takenFrom, -1);

      int tmp;
      for(tmp = filledIndices.next(0); tmp >= 0; tmp = filledIndices.next(tmp + 1)) {
         takenFrom[tmp] = tmp;
      }

      if(initializedCounter == 0) {
         throw new IllegalStateException("at least one entry has to be != null, which should have happened in initIndex");
      } else {
         tmp = initializedCounter;

         while(initializedCounter < len) {
            this.index.copyTo(indexCopy);
            filledIndices.copyTo(indicesCopy);
            initializedCounter = filledIndices.getCardinality();

            for(int i = 0; i < len; ++i) {
               int to = -1;
               int from = -1;
               if(indicesCopy.contains(i)) {
                  if((i + 1) % this.lonSize != 0 && !indicesCopy.contains(i + 1)) {
                     from = i;
                     to = i + 1;
                  } else if(i + this.lonSize < len && !indicesCopy.contains(i + this.lonSize)) {
                     from = i;
                     to = i + this.lonSize;
                  }
               } else if((i + 1) % this.lonSize != 0 && indicesCopy.contains(i + 1)) {
                  from = i + 1;
                  to = i;
               } else if(i + this.lonSize < len && indicesCopy.contains(i + this.lonSize)) {
                  from = i + this.lonSize;
                  to = i;
               }

               if(to >= 0 && (takenFrom[to] < 0 || takenFrom[to] != to && this.getNormedDist(from, to) < this.getNormedDist(takenFrom[to], to))) {
                  this.index.setInt((long)(to * 4), indexCopy.getInt((long)(from * 4)));
                  takenFrom[to] = takenFrom[from];
                  filledIndices.add(to);
                  ++initializedCounter;
               }
            }
         }

         return initializedCounter - tmp;
      }
   }

   double getNormedDist(int from, int to) {
      int fromX = from % this.lonSize;
      int fromY = from / this.lonSize;
      int toX = to % this.lonSize;
      int toY = to / this.lonSize;
      int dx = toX - fromX;
      int dy = toY - fromY;
      return (double)(dx * dx + dy * dy);
   }

   public int findID(double lat, double lon) {
      return this.findClosest(lat, lon, EdgeFilter.ALL_EDGES).getClosestNode();
   }

   public QueryResult findClosest(final double queryLat, final double queryLon, EdgeFilter edgeFilter) {
      if(this.isClosed()) {
         throw new IllegalStateException("You need to create a new LocationIndex instance as it is already closed");
      } else if(edgeFilter != EdgeFilter.ALL_EDGES) {
         throw new UnsupportedOperationException("edge filters are not yet implemented for " + Location2IDQuadtree.class.getSimpleName());
      } else {
         long key = this.keyAlgo.encode(queryLat, queryLon);
         final int id = this.index.getInt(key * 4L);
         double mainLat = this.nodeAccess.getLatitude(id);
         double mainLon = this.nodeAccess.getLongitude(id);
         final QueryResult res = new QueryResult(queryLat, queryLon);
         res.setClosestNode(id);
         res.setQueryDistance(this.distCalc.calcNormalizedDist(queryLat, queryLon, mainLat, mainLon));
         this.goFurtherHook(id);
         (new BreadthFirstSearch() {
            protected GHBitSet createBitSet() {
               return new GHTBitSet(10);
            }

            protected boolean goFurther(int baseNode) {
               if(baseNode == id) {
                  return true;
               } else {
                  Location2IDQuadtree.this.goFurtherHook(baseNode);
                  double currLat = Location2IDQuadtree.this.nodeAccess.getLatitude(baseNode);
                  double currLon = Location2IDQuadtree.this.nodeAccess.getLongitude(baseNode);
                  double currNormedDist = Location2IDQuadtree.this.distCalc.calcNormalizedDist(queryLat, queryLon, currLat, currLon);
                  if(currNormedDist < res.getQueryDistance()) {
                     res.setQueryDistance(currNormedDist);
                     res.setClosestNode(baseNode);
                     return true;
                  } else {
                     return currNormedDist < Location2IDQuadtree.this.maxRasterWidth2InMeterNormed;
                  }
               }
            }
         }).start(this.graph.createEdgeExplorer(), id);
         res.setQueryDistance(this.distCalc.calcDenormalizedDist(res.getQueryDistance()));
         return res;
      }
   }

   public void goFurtherHook(int n) {
   }

   public void flush() {
      this.index.setHeader(0, 174507);
      this.index.setHeader(4, this.latSize);
      this.index.setHeader(8, this.lonSize);
      this.index.setHeader(12, this.graph.getNodes());
      this.index.flush();
   }

   public void close() {
      this.index.close();
   }

   public boolean isClosed() {
      return this.index.isClosed();
   }

   public void setSegmentSize(int bytes) {
      this.index.setSegmentSize(bytes);
   }
}
