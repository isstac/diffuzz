package com.graphhopper.storage.index;

import com.graphhopper.coll.GHBitSet;
import com.graphhopper.coll.GHTBitSet;
import com.graphhopper.geohash.SpatialKeyAlgo;
import com.graphhopper.routing.util.AllEdgesIterator;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.storage.CHGraph;
import com.graphhopper.storage.DataAccess;
import com.graphhopper.storage.Directory;
import com.graphhopper.storage.Graph;
import com.graphhopper.storage.NodeAccess;
import com.graphhopper.storage.index.BresenhamLine;
import com.graphhopper.storage.index.LocationIndex;
import com.graphhopper.storage.index.PointEmitter;
import com.graphhopper.storage.index.QueryResult;
import com.graphhopper.util.BitUtil;
import com.graphhopper.util.BreadthFirstSearch;
import com.graphhopper.util.DistanceCalc;
import com.graphhopper.util.EdgeExplorer;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.Helper;
import com.graphhopper.util.PointList;
import com.graphhopper.util.StopWatch;
import com.graphhopper.util.shapes.BBox;
import com.graphhopper.util.shapes.GHPoint;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.procedure.TIntProcedure;
import gnu.trove.set.hash.TIntHashSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

public class LocationIndexTree implements LocationIndex {
//   private final Logger logger = LoggerFactory.getLogger(this.getClass());
   private final int MAGIC_INT;
   protected DistanceCalc distCalc;
   private DistanceCalc preciseDistCalc;
   protected final Graph graph;
   private final NodeAccess nodeAccess;
   final DataAccess dataAccess;
   private int[] entries;
   private byte[] shifts;
   private long[] bitmasks;
   protected SpatialKeyAlgo keyAlgo;
   private int minResolutionInMeter;
   private double deltaLat;
   private double deltaLon;
   private int initSizeLeafEntries;
   private boolean initialized;
   static final int START_POINTER = 1;
   int maxRegionSearch;
   private double equalNormedDelta;

   public LocationIndexTree(Graph g, Directory dir) {
      this.distCalc = Helper.DIST_PLANE;
      this.preciseDistCalc = Helper.DIST_EARTH;
      this.minResolutionInMeter = 300;
      this.initSizeLeafEntries = 4;
      this.initialized = false;
      this.maxRegionSearch = 4;
      if(g instanceof CHGraph) {
         throw new IllegalArgumentException("Use base graph for LocationIndexTree instead of CHGraph");
      } else {
         this.MAGIC_INT = 96230;
         this.graph = g;
         this.nodeAccess = g.getNodeAccess();
         this.dataAccess = dir.find("location_index");
      }
   }

   public int getMinResolutionInMeter() {
      return this.minResolutionInMeter;
   }

   public LocationIndexTree setMinResolutionInMeter(int minResolutionInMeter) {
      this.minResolutionInMeter = minResolutionInMeter;
      return this;
   }

   public LocationIndexTree setMaxRegionSearch(int numTiles) {
      if(numTiles < 1) {
         throw new IllegalArgumentException("Region of location index must be at least 1 but was " + numTiles);
      } else {
         if(numTiles % 2 == 1) {
            ++numTiles;
         }

         this.maxRegionSearch = numTiles;
         return this;
      }
   }

   void prepareAlgo() {
//	   System.out.println("prepareAlgo()");
      this.equalNormedDelta = this.distCalc.calcNormalizedDist(0.1D);
//      System.out.println("prepareAlgo() 2");
      BBox bounds = this.graph.getBounds();
//      System.out.println("prepareAlgo() 3");
      if(this.graph.getNodes() == 0) {
//    	  System.out.println("Cannot create location index of empty graph!");
         throw new IllegalStateException("Cannot create location index of empty graph!");
      } else if(!bounds.isValid()) {
//    	  System.out.println("Cannot create location index when graph has invalid bounds: " + bounds);
         throw new IllegalStateException("Cannot create location index when graph has invalid bounds: " + bounds);
      } else {
//    	  System.out.println("prepareAlgo() else 2");
         double lat = Math.min(Math.abs(bounds.maxLat), Math.abs(bounds.minLat));
//         System.out.println("prepareAlgo() else 3");
         double maxDistInMeter = Math.max((bounds.maxLat - bounds.minLat) / 360.0D * 4.003017359204114E7D, (bounds.maxLon - bounds.minLon) / 360.0D * this.preciseDistCalc.calcCircumference(lat));
//         System.out.println("prepareAlgo() else 4");
         double tmp = maxDistInMeter / (double)this.minResolutionInMeter;
         tmp *= tmp;
         TIntArrayList tmpEntries = new TIntArrayList();
         
//         System.out.println("prepareAlgo() else 5");

         byte shiftSum;
         for(tmp /= 4.0D; tmp > 1.0D; tmp /= (double)shiftSum) {
            if(tmp >= 64.0D) {
               shiftSum = 64;
            } else if(tmp >= 16.0D) {
               shiftSum = 16;
            } else {
               if(tmp < 4.0D) {
                  break;
               }

               shiftSum = 4;
            }

            tmpEntries.add(shiftSum);
         }
         
//         System.out.println("prepareAlgo() else 6");

         tmpEntries.add(4);
         this.initEntries(tmpEntries.toArray());
         int var13 = 0;
         long parts = 1L;
         
//         System.out.println("prepareAlgo() else 7");

         for(int i = 0; i < this.shifts.length; ++i) {
            var13 += this.shifts[i];
            parts *= (long)this.entries[i];
         }
         
//         System.out.println("prepareAlgo() else 8");

         if(var13 > 64) {
//        	 System.out.println("sum of all shifts does not fit into a long variable");
            throw new IllegalStateException("sum of all shifts does not fit into a long variable");
         } else {
//        	 System.out.println("prepareAlgo() in nested else");
            this.keyAlgo = (new SpatialKeyAlgo(var13)).bounds(bounds);
//            System.out.println("prepareAlgo() in nested else 2");
            parts = Math.round(Math.sqrt((double)parts));
//            System.out.println("prepareAlgo() in nested else 3");
            this.deltaLat = (bounds.maxLat - bounds.minLat) / (double)parts;
            this.deltaLon = (bounds.maxLon - bounds.minLon) / (double)parts;
         }
//         System.out.println("prepareAlgo() done");
      }
   }

   private LocationIndexTree initEntries(int[] entries) {
      if(entries.length < 1) {
         throw new IllegalStateException("depth needs to be at least 1");
      } else {
         this.entries = entries;
         int depth = entries.length;
         this.shifts = new byte[depth];
         this.bitmasks = new long[depth];
         int lastEntry = entries[0];

         for(int i = 0; i < depth; ++i) {
            if(lastEntry < entries[i]) {
               throw new IllegalStateException("entries should decrease or stay but was:" + Arrays.toString(entries));
            }

            lastEntry = entries[i];
            this.shifts[i] = this.getShift(entries[i]);
            this.bitmasks[i] = this.getBitmask(this.shifts[i]);
         }

         return this;
      }
   }

   private byte getShift(int entries) {
      byte b = (byte)((int)Math.round(Math.log((double)entries) / Math.log(2.0D)));
      if(b <= 0) {
         throw new IllegalStateException("invalid shift:" + b);
      } else {
         return b;
      }
   }

   private long getBitmask(int shift) {
      long bm = (1L << shift) - 1L;
      if(bm <= 0L) {
         throw new IllegalStateException("invalid bitmask:" + bm);
      } else {
         return bm;
      }
   }

   LocationIndexTree.InMemConstructionIndex getPrepareInMemIndex() {
      LocationIndexTree.InMemConstructionIndex memIndex = new LocationIndexTree.InMemConstructionIndex(this.entries[0]);
      memIndex.prepare();
      return memIndex;
   }

   public int findID(double lat, double lon) {
      QueryResult res = this.findClosest(lat, lon, EdgeFilter.ALL_EDGES);
      return res == null?-1:res.getClosestNode();
   }

   public LocationIndex setResolution(int minResolutionInMeter) {
      if(minResolutionInMeter <= 0) {
         throw new IllegalStateException("Negative precision is not allowed!");
      } else {
         this.setMinResolutionInMeter(minResolutionInMeter);
         return this;
      }
   }

   public LocationIndex setApproximation(boolean approx) {
      if(approx) {
         this.distCalc = Helper.DIST_PLANE;
      } else {
         this.distCalc = Helper.DIST_EARTH;
      }

      return this;
   }

   public LocationIndexTree create(long size) {
      throw new UnsupportedOperationException("Not supported. Use prepareIndex instead.");
   }

   public boolean loadExisting() {
//	   System.out.println("LocationIndexTree.loadExisting()");
      if(this.initialized) {
    	  System.out.println("Call loadExisting only once");
         throw new IllegalStateException("Call loadExisting only once");
      } else if(!this.dataAccess.loadExisting()) {
//    	  System.out.println("LocationIndexTree.loadExisting() returning false");
         return false;
      } else if(this.dataAccess.getHeader(0) != this.MAGIC_INT) {
//    	  System.out.println("incorrect location index version, expected:" + this.MAGIC_INT);
         throw new IllegalStateException("incorrect location index version, expected:" + this.MAGIC_INT);
      } else if(this.dataAccess.getHeader(4) != this.calcChecksum()) {
//    	  System.out.println("location index was opened with incorrect graph: " + this.dataAccess.getHeader(4) + " vs. " + this.calcChecksum());
         throw new IllegalStateException("location index was opened with incorrect graph: " + this.dataAccess.getHeader(4) + " vs. " + this.calcChecksum());
      } else {
//    	  System.out.println("LocationIndexTree.loadExisting() else 1");
         this.setMinResolutionInMeter(this.dataAccess.getHeader(8));
//         System.out.println("LocationIndexTree.loadExisting() else 2");
         this.prepareAlgo();
//         System.out.println("LocationIndexTree.loadExisting() else 3");
         this.initialized = true;
         return true;
      }
   }

   public void flush() {
      this.dataAccess.setHeader(0, this.MAGIC_INT);
      this.dataAccess.setHeader(4, this.calcChecksum());
      this.dataAccess.setHeader(8, this.minResolutionInMeter);
      this.dataAccess.flush();
   }

   public LocationIndex prepareIndex() {
      if(this.initialized) {
         throw new IllegalStateException("Call prepareIndex only once");
      } else {
         StopWatch sw = (new StopWatch()).start();
         this.prepareAlgo();
         LocationIndexTree.InMemConstructionIndex inMem = this.getPrepareInMemIndex();
         this.dataAccess.create(65536L);

         try {
            inMem.store(inMem.root, 1);
            this.flush();
         } catch (Exception var4) {
            throw new IllegalStateException("Problem while storing location index. " + Helper.getMemInfo(), var4);
         }

         float entriesPerLeaf = (float)inMem.size / (float)inMem.leafs;
         this.initialized = true;
//         this.logger.info("location index created in " + sw.stop().getSeconds() + "s, size:" + Helper.nf((long)inMem.size) + ", leafs:" + Helper.nf((long)inMem.leafs) + ", precision:" + this.minResolutionInMeter + ", depth:" + this.entries.length + ", checksum:" + this.calcChecksum() + ", entries:" + Arrays.toString(this.entries) + ", entriesPerLeaf:" + entriesPerLeaf);
         return this;
      }
   }

   int calcChecksum() {
      return this.graph.getNodes();
   }

   public void close() {
      this.dataAccess.close();
   }

   public boolean isClosed() {
      return this.dataAccess.isClosed();
   }

   public long getCapacity() {
      return this.dataAccess.getCapacity();
   }

   public void setSegmentSize(int bytes) {
      this.dataAccess.setSegmentSize(bytes);
   }

   TIntArrayList getEntries() {
      return new TIntArrayList(this.entries);
   }

   final void fillIDs(long keyPart, int intIndex, TIntHashSet set, int depth) {
      long pointer = (long)intIndex << 2;
      int offset;
      if(depth != this.entries.length) {
         offset = (int)(this.bitmasks[depth] & keyPart) << 2;
         int value1 = this.dataAccess.getInt(pointer + (long)offset);
         if(value1 > 0) {
            this.fillIDs(keyPart >>> this.shifts[depth], value1, set, depth + 1);
         }

      } else {
         offset = this.dataAccess.getInt(pointer);
         if(offset < 0) {
            set.add(-(offset + 1));
         } else {
            long value = (long)offset * 4L;

            for(long leafIndex = pointer + 4L; leafIndex < value; leafIndex += 4L) {
               set.add(this.dataAccess.getInt(leafIndex));
            }
         }

      }
   }

   final long createReverseKey(double lat, double lon) {
      return BitUtil.BIG.reverse(this.keyAlgo.encode(lat, lon), this.keyAlgo.getBits());
   }

   final long createReverseKey(long key) {
      return BitUtil.BIG.reverse(key, this.keyAlgo.getBits());
   }

   final double calculateRMin(double lat, double lon) {
      return this.calculateRMin(lat, lon, 0);
   }

   final double calculateRMin(double lat, double lon, int paddingTiles) {
      GHPoint query = new GHPoint(lat, lon);
      long key = this.keyAlgo.encode(query);
      GHPoint center = new GHPoint();
      this.keyAlgo.decode(key, center);
      double minLat = center.lat - (0.5D + (double)paddingTiles) * this.deltaLat;
      double maxLat = center.lat + (0.5D + (double)paddingTiles) * this.deltaLat;
      double minLon = center.lon - (0.5D + (double)paddingTiles) * this.deltaLon;
      double maxLon = center.lon + (0.5D + (double)paddingTiles) * this.deltaLon;
      double dSouthernLat = query.lat - minLat;
      double dNorthernLat = maxLat - query.lat;
      double dWesternLon = query.lon - minLon;
      double dEasternLon = maxLon - query.lon;
      double dMinLat;
      if(dSouthernLat < dNorthernLat) {
         dMinLat = this.distCalc.calcDist(query.lat, query.lon, minLat, query.lon);
      } else {
         dMinLat = this.distCalc.calcDist(query.lat, query.lon, maxLat, query.lon);
      }

      double dMinLon;
      if(dWesternLon < dEasternLon) {
         dMinLon = this.distCalc.calcDist(query.lat, query.lon, query.lat, minLon);
      } else {
         dMinLon = this.distCalc.calcDist(query.lat, query.lon, query.lat, maxLon);
      }

      double rMin = Math.min(dMinLat, dMinLon);
      return rMin;
   }

   double getDeltaLat() {
      return this.deltaLat;
   }

   double getDeltaLon() {
      return this.deltaLon;
   }

   GHPoint getCenter(double lat, double lon) {
      GHPoint query = new GHPoint(lat, lon);
      long key = this.keyAlgo.encode(query);
      GHPoint center = new GHPoint();
      this.keyAlgo.decode(key, center);
      return center;
   }

   public final boolean findNetworkEntries(double queryLat, double queryLon, TIntHashSet foundEntries, int iteration) {
      int rMin;
      double subqueryLon;
      double subqueryLatA;
      double subqueryLatB;
      for(rMin = -iteration; rMin <= iteration; ++rMin) {
         subqueryLon = queryLat + (double)rMin * this.deltaLat;
         subqueryLatA = queryLon - (double)iteration * this.deltaLon;
         subqueryLatB = queryLon + (double)iteration * this.deltaLon;
         this.findNetworkEntriesSingleRegion(foundEntries, subqueryLon, subqueryLatA);
         if(iteration > 0) {
            this.findNetworkEntriesSingleRegion(foundEntries, subqueryLon, subqueryLatB);
         }
      }

      for(rMin = -iteration + 1; rMin <= iteration - 1; ++rMin) {
         subqueryLon = queryLon + (double)rMin * this.deltaLon;
         subqueryLatA = queryLat - (double)iteration * this.deltaLat;
         subqueryLatB = queryLat + (double)iteration * this.deltaLat;
         this.findNetworkEntriesSingleRegion(foundEntries, subqueryLatA, subqueryLon);
         this.findNetworkEntriesSingleRegion(foundEntries, subqueryLatB, subqueryLon);
      }

      if(iteration % 2 == 1 && !foundEntries.isEmpty()) {
         double var14 = this.calculateRMin(queryLat, queryLon, iteration);
         double minDistance = this.calcMinDistance(queryLat, queryLon, foundEntries);
         if(minDistance < var14) {
            return true;
         }
      }

      return false;
   }

   final double calcMinDistance(double queryLat, double queryLon, TIntHashSet pointset) {
      double min = Double.MAX_VALUE;
      TIntIterator itr = pointset.iterator();

      while(itr.hasNext()) {
         int node = itr.next();
         double lat = this.nodeAccess.getLat(node);
         double lon = this.nodeAccess.getLon(node);
         double dist = this.distCalc.calcDist(queryLat, queryLon, lat, lon);
         if(dist < min) {
            min = dist;
         }
      }

      return min;
   }

   final void findNetworkEntriesSingleRegion(TIntHashSet storedNetworkEntryIds, double queryLat, double queryLon) {
      long keyPart = this.createReverseKey(queryLat, queryLon);
      this.fillIDs(keyPart, 1, storedNetworkEntryIds, 0);
   }

   public QueryResult findClosest(final double queryLat, final double queryLon, final EdgeFilter edgeFilter) {
      if(this.isClosed()) {
         throw new IllegalStateException("You need to create a new LocationIndex instance as it is already closed");
      } else {
         TIntHashSet allCollectedEntryIds = new TIntHashSet();
         final QueryResult closestMatch = new QueryResult(queryLat, queryLon);

         for(int iteration = 0; iteration < this.maxRegionSearch; ++iteration) {
            TIntHashSet storedNetworkEntryIds = new TIntHashSet();
            boolean earlyFinish = this.findNetworkEntries(queryLat, queryLon, storedNetworkEntryIds, iteration);
            storedNetworkEntryIds.removeAll(allCollectedEntryIds);
            allCollectedEntryIds.addAll(storedNetworkEntryIds);
            final GHTBitSet checkBitset = new GHTBitSet(new TIntHashSet(storedNetworkEntryIds));
            final EdgeExplorer explorer = this.graph.createEdgeExplorer();
            storedNetworkEntryIds.forEach(new TIntProcedure() {
               public boolean execute(int networkEntryNodeId) {
                  (new XFirstSearchCheck(queryLat, queryLon, checkBitset, edgeFilter) {
                     protected double getQueryDistance() {
                        return closestMatch.getQueryDistance();
                     }

                     protected boolean check(int node, double normedDist, int wayIndex, EdgeIteratorState edge, QueryResult.Position pos) {
                        if(normedDist < closestMatch.getQueryDistance()) {
                           closestMatch.setQueryDistance(normedDist);
                           closestMatch.setClosestNode(node);
                           closestMatch.setClosestEdge(edge.detach(false));
                           closestMatch.setWayIndex(wayIndex);
                           closestMatch.setSnappedPosition(pos);
                           return true;
                        } else {
                           return false;
                        }
                     }
                  }).start(explorer, networkEntryNodeId);
                  return true;
               }
            });
            if(earlyFinish && closestMatch.isValid()) {
               break;
            }
         }

         if(closestMatch.isValid()) {
            closestMatch.setQueryDistance(this.distCalc.calcDenormalizedDist(closestMatch.getQueryDistance()));
            closestMatch.calcSnappedPoint(this.distCalc);
         }

         return closestMatch;
      }
   }

   static class InMemTreeEntry implements LocationIndexTree.InMemEntry {
      LocationIndexTree.InMemEntry[] subEntries;

      public InMemTreeEntry(int subEntryNo) {
         this.subEntries = new LocationIndexTree.InMemEntry[subEntryNo];
      }

      public LocationIndexTree.InMemEntry getSubEntry(int index) {
         return this.subEntries[index];
      }

      public void setSubEntry(int index, LocationIndexTree.InMemEntry subEntry) {
         this.subEntries[index] = subEntry;
      }

      public Collection getSubEntriesForDebug() {
         ArrayList list = new ArrayList();
         LocationIndexTree.InMemEntry[] arr$ = this.subEntries;
         int len$ = arr$.length;

         for(int i$ = 0; i$ < len$; ++i$) {
            LocationIndexTree.InMemEntry e = arr$[i$];
            if(e != null) {
               list.add(e);
            }
         }

         return list;
      }

      public final boolean isLeaf() {
         return false;
      }

      public String toString() {
         return "TREE";
      }
   }

   static class SortedIntSet extends TIntArrayList {
      public SortedIntSet() {
      }

      public SortedIntSet(int capacity) {
         super(capacity);
      }

      public boolean addOnce(int value) {
         int foundIndex = this.binarySearch(value);
         if(foundIndex >= 0) {
            return false;
         } else {
            foundIndex = -foundIndex - 1;
            this.insert(foundIndex, value);
            return true;
         }
      }
   }

   static class InMemLeafEntry extends LocationIndexTree.SortedIntSet implements LocationIndexTree.InMemEntry {
      public InMemLeafEntry(int count, long key) {
         super(count);
      }

      public boolean addNode(int nodeId) {
         return this.addOnce(nodeId);
      }

      public final boolean isLeaf() {
         return true;
      }

      public String toString() {
         return "LEAF  " + super.toString();
      }

      TIntArrayList getResults() {
         return this;
      }
   }

   interface InMemEntry {
      boolean isLeaf();
   }

   protected abstract class XFirstSearchCheck extends BreadthFirstSearch {
      boolean goFurther = true;
      double currNormedDist;
      double currLat;
      double currLon;
      int currNode;
      final double queryLat;
      final double queryLon;
      final GHBitSet checkBitset;
      final EdgeFilter edgeFilter;

      public XFirstSearchCheck(double queryLat, double queryLon, GHBitSet checkBitset, EdgeFilter edgeFilter) {
         this.queryLat = queryLat;
         this.queryLon = queryLon;
         this.checkBitset = checkBitset;
         this.edgeFilter = edgeFilter;
      }

      protected GHBitSet createBitSet() {
         return this.checkBitset;
      }

      protected boolean goFurther(int baseNode) {
         this.currNode = baseNode;
         this.currLat = LocationIndexTree.this.nodeAccess.getLatitude(baseNode);
         this.currLon = LocationIndexTree.this.nodeAccess.getLongitude(baseNode);
         this.currNormedDist = LocationIndexTree.this.distCalc.calcNormalizedDist(this.queryLat, this.queryLon, this.currLat, this.currLon);
         return this.goFurther;
      }

      protected boolean checkAdjacent(EdgeIteratorState currEdge) {
         this.goFurther = false;
         if(!this.edgeFilter.accept(currEdge)) {
            return true;
         } else {
            int tmpClosestNode = this.currNode;
            if(this.check(tmpClosestNode, this.currNormedDist, 0, currEdge, QueryResult.Position.TOWER) && this.currNormedDist <= LocationIndexTree.this.equalNormedDelta) {
               return false;
            } else {
               int adjNode = currEdge.getAdjNode();
               double adjLat = LocationIndexTree.this.nodeAccess.getLatitude(adjNode);
               double adjLon = LocationIndexTree.this.nodeAccess.getLongitude(adjNode);
               double adjDist = LocationIndexTree.this.distCalc.calcNormalizedDist(adjLat, adjLon, this.queryLat, this.queryLon);
               if(adjDist < this.currNormedDist) {
                  tmpClosestNode = adjNode;
               }

               double tmpLat = this.currLat;
               double tmpLon = this.currLon;
               PointList pointList = currEdge.fetchWayGeometry(2);
               int len = pointList.getSize();

               for(int pointIndex = 0; pointIndex < len; ++pointIndex) {
                  double wayLat = pointList.getLatitude(pointIndex);
                  double wayLon = pointList.getLongitude(pointIndex);
                  QueryResult.Position pos = QueryResult.Position.EDGE;
                  double tmpNormedDist;
                  if(LocationIndexTree.this.distCalc.validEdgeDistance(this.queryLat, this.queryLon, tmpLat, tmpLon, wayLat, wayLon)) {
                     tmpNormedDist = LocationIndexTree.this.distCalc.calcNormalizedEdgeDistance(this.queryLat, this.queryLon, tmpLat, tmpLon, wayLat, wayLon);
                     this.check(tmpClosestNode, tmpNormedDist, pointIndex, currEdge, pos);
                  } else {
                     if(pointIndex + 1 == len) {
                        tmpNormedDist = adjDist;
                        pos = QueryResult.Position.TOWER;
                     } else {
                        tmpNormedDist = LocationIndexTree.this.distCalc.calcNormalizedDist(this.queryLat, this.queryLon, wayLat, wayLon);
                        pos = QueryResult.Position.PILLAR;
                     }

                     this.check(tmpClosestNode, tmpNormedDist, pointIndex + 1, currEdge, pos);
                  }

                  if(tmpNormedDist <= LocationIndexTree.this.equalNormedDelta) {
                     return false;
                  }

                  tmpLat = wayLat;
                  tmpLon = wayLon;
               }

               return this.getQueryDistance() > LocationIndexTree.this.equalNormedDelta;
            }
         }
      }

      protected abstract double getQueryDistance();

      protected abstract boolean check(int var1, double var2, int var4, EdgeIteratorState var5, QueryResult.Position var6);
   }

   class InMemConstructionIndex {
      int size;
      int leafs;
      LocationIndexTree.InMemTreeEntry root;

      public InMemConstructionIndex(int noOfSubEntries) {
         this.root = new LocationIndexTree.InMemTreeEntry(noOfSubEntries);
      }

      void prepare() {
         AllEdgesIterator allIter = LocationIndexTree.this.graph.getAllEdges();

         try {
            while(allIter.next()) {
               int ex = allIter.getBaseNode();
               int nodeB = allIter.getAdjNode();
               double lat1 = LocationIndexTree.this.nodeAccess.getLatitude(ex);
               double lon1 = LocationIndexTree.this.nodeAccess.getLongitude(ex);
               PointList points = allIter.fetchWayGeometry(0);
               int len = points.getSize();

               double lat2;
               double lon2;
               for(int i = 0; i < len; ++i) {
                  lat2 = points.getLatitude(i);
                  lon2 = points.getLongitude(i);
                  this.addNode(ex, nodeB, lat1, lon1, lat2, lon2);
                  lat1 = lat2;
                  lon1 = lon2;
               }

               lat2 = LocationIndexTree.this.nodeAccess.getLatitude(nodeB);
               lon2 = LocationIndexTree.this.nodeAccess.getLongitude(nodeB);
               this.addNode(ex, nodeB, lat1, lon1, lat2, lon2);
            }
         } catch (Exception var15) {
//            LocationIndexTree.this.logger.error("Problem! base:" + allIter.getBaseNode() + ", adj:" + allIter.getAdjNode() + ", edge:" + allIter.getEdge(), var15);
         }

      }

      void addNode(final int nodeA, int nodeB, double lat1, double lon1, double lat2, double lon2) {
         PointEmitter pointEmitter = new PointEmitter() {
            public void set(double lat, double lon) {
               long key = LocationIndexTree.this.keyAlgo.encode(lat, lon);
               long keyPart = LocationIndexTree.this.createReverseKey(key);
               InMemConstructionIndex.this.addNode(InMemConstructionIndex.this.root, nodeA, 0, keyPart, key);
            }
         };
         BresenhamLine.calcPoints(lat1, lon1, lat2, lon2, pointEmitter, LocationIndexTree.this.graph.getBounds().minLat, LocationIndexTree.this.graph.getBounds().minLon, LocationIndexTree.this.deltaLat, LocationIndexTree.this.deltaLon);
      }

      void addNode(LocationIndexTree.InMemEntry entry, int nodeId, int depth, long keyPart, long key) {
         if(entry.isLeaf()) {
            LocationIndexTree.InMemLeafEntry index = (LocationIndexTree.InMemLeafEntry)entry;
            index.addNode(nodeId);
         } else {
            int var11 = (int)(LocationIndexTree.this.bitmasks[depth] & keyPart);
            keyPart >>>= LocationIndexTree.this.shifts[depth];
            LocationIndexTree.InMemTreeEntry treeEntry = (LocationIndexTree.InMemTreeEntry)entry;
            Object subentry = treeEntry.getSubEntry(var11);
            ++depth;
            if(subentry == null) {
               if(depth == LocationIndexTree.this.entries.length) {
                  subentry = new LocationIndexTree.InMemLeafEntry(LocationIndexTree.this.initSizeLeafEntries, key);
               } else {
                  subentry = new LocationIndexTree.InMemTreeEntry(LocationIndexTree.this.entries[depth]);
               }

               treeEntry.setSubEntry(var11, (LocationIndexTree.InMemEntry)subentry);
            }

            this.addNode((LocationIndexTree.InMemEntry)subentry, nodeId, depth, keyPart, key);
         }

      }

      Collection getEntriesOf(int selectDepth) {
         ArrayList list = new ArrayList();
         this.fillLayer(list, selectDepth, 0, this.root.getSubEntriesForDebug());
         return list;
      }

      void fillLayer(Collection list, int selectDepth, int depth, Collection entries) {
         Iterator i$ = entries.iterator();

         while(i$.hasNext()) {
            LocationIndexTree.InMemEntry entry = (LocationIndexTree.InMemEntry)i$.next();
            if(selectDepth == depth) {
               list.add(entry);
            } else if(entry instanceof LocationIndexTree.InMemTreeEntry) {
               this.fillLayer(list, selectDepth, depth + 1, ((LocationIndexTree.InMemTreeEntry)entry).getSubEntriesForDebug());
            }
         }

      }

      String print() {
         StringBuilder sb = new StringBuilder();
         this.print(this.root, sb, 0L, 0);
         return sb.toString();
      }

      void print(LocationIndexTree.InMemEntry e, StringBuilder sb, long key, int depth) {
         int counter;
         if(e.isLeaf()) {
            LocationIndexTree.InMemLeafEntry tree = (LocationIndexTree.InMemLeafEntry)e;
            counter = LocationIndexTree.this.keyAlgo.getBits();
            sb.append(BitUtil.BIG.toBitString(BitUtil.BIG.reverse(key, counter), counter)).append("  ");
            TIntArrayList sube = tree.getResults();

            for(int i = 0; i < sube.size(); ++i) {
               sb.append(tree.get(i)).append(',');
            }

            sb.append('\n');
         } else {
            LocationIndexTree.InMemTreeEntry var10 = (LocationIndexTree.InMemTreeEntry)e;
            key <<= LocationIndexTree.this.shifts[depth];

            for(counter = 0; counter < var10.subEntries.length; ++counter) {
               LocationIndexTree.InMemEntry var11 = var10.subEntries[counter];
               if(var11 != null) {
                  this.print(var11, sb, key | (long)counter, depth + 1);
               }
            }
         }

      }

      int store(LocationIndexTree.InMemEntry entry, int intIndex) {
         long refPointer = (long)intIndex * 4L;
         int subCounter;
         if(entry.isLeaf()) {
            LocationIndexTree.InMemLeafEntry treeEntry = (LocationIndexTree.InMemLeafEntry)entry;
            TIntArrayList len = treeEntry.getResults();
            subCounter = len.size();
            if(subCounter == 0) {
               return intIndex;
            }

            this.size += subCounter;
            ++intIndex;
            ++this.leafs;
            LocationIndexTree.this.dataAccess.ensureCapacity((long)(intIndex + subCounter + 1) * 4L);
            if(subCounter == 1) {
               LocationIndexTree.this.dataAccess.setInt(refPointer, -len.get(0) - 1);
            } else {
               for(int subEntry = 0; subEntry < subCounter; ++intIndex) {
                  LocationIndexTree.this.dataAccess.setInt((long)intIndex * 4L, len.get(subEntry));
                  ++subEntry;
               }

               LocationIndexTree.this.dataAccess.setInt(refPointer, intIndex);
            }
         } else {
            LocationIndexTree.InMemTreeEntry var10 = (LocationIndexTree.InMemTreeEntry)entry;
            int var11 = var10.subEntries.length;
            intIndex += var11;

            for(subCounter = 0; subCounter < var11; refPointer += 4L) {
               LocationIndexTree.InMemEntry var12 = var10.subEntries[subCounter];
               if(var12 != null) {
                  LocationIndexTree.this.dataAccess.ensureCapacity((long)(intIndex + 1) * 4L);
                  int beforeIntIndex = intIndex;
                  intIndex = this.store(var12, intIndex);
                  if(intIndex == beforeIntIndex) {
                     LocationIndexTree.this.dataAccess.setInt(refPointer, 0);
                  } else {
                     LocationIndexTree.this.dataAccess.setInt(refPointer, beforeIntIndex);
                  }
               }

               ++subCounter;
            }
         }

         return intIndex;
      }
   }
}
