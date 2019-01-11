package com.graphhopper.routing.util;

import com.graphhopper.GHResponse;
import com.graphhopper.routing.AlgorithmOptions;
import com.graphhopper.routing.Path;
import com.graphhopper.routing.QueryGraph;
import com.graphhopper.routing.RoutingAlgorithm;
import com.graphhopper.routing.RoutingAlgorithmFactorySimple;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.TurnWeighting;
import com.graphhopper.storage.CHGraph;
import com.graphhopper.storage.Graph;
import com.graphhopper.storage.TurnCostExtension;
import com.graphhopper.storage.index.LocationIndex;
import com.graphhopper.storage.index.QueryResult;
import com.graphhopper.util.DistanceCalc;
import com.graphhopper.util.Helper;
import com.graphhopper.util.PathMerger;
import com.graphhopper.util.PointList;
import com.graphhopper.util.TranslationMap;
import com.graphhopper.util.shapes.GHPoint3D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class TestAlgoCollector {
   private final String name;
   private final DistanceCalc distCalc;
   private final TranslationMap trMap;
   public final List errors;

   public TestAlgoCollector(String name) {
      this.distCalc = Helper.DIST_EARTH;
      this.trMap = (new TranslationMap()).doImport();
      this.errors = new ArrayList();
      this.name = name;
   }

   public TestAlgoCollector assertDistance(TestAlgoCollector.AlgoHelperEntry algoEntry, List queryList, TestAlgoCollector.OneRun oneRun) {
      ArrayList viaPaths = new ArrayList();
      QueryGraph queryGraph = new QueryGraph(algoEntry.getQueryGraph());
      queryGraph.lookup(queryList);
      AlgorithmOptions opts = algoEntry.opts;
      FlagEncoder encoder = opts.getFlagEncoder();
      if(encoder.supports(TurnWeighting.class)) {
         algoEntry.setAlgorithmOptions(AlgorithmOptions.start(opts).weighting(new TurnWeighting(opts.getWeighting(), opts.getFlagEncoder(), (TurnCostExtension)queryGraph.getExtension())).build());
      }

      for(int pathMerger = 0; pathMerger < queryList.size() - 1; ++pathMerger) {
         Path rsp = algoEntry.createAlgo(queryGraph).calcPath(((QueryResult)queryList.get(pathMerger)).getClosestNode(), ((QueryResult)queryList.get(pathMerger + 1)).getClosestNode());
         viaPaths.add(rsp);
      }

      PathMerger var13 = (new PathMerger()).setCalcPoints(true).setSimplifyResponse(false).setEnableInstructions(true);
      GHResponse var14 = new GHResponse();
      var13.doWork(var14, viaPaths, this.trMap.getWithFallBack(Locale.US));
      if(var14.hasErrors()) {
         this.errors.add(algoEntry + " response contains errors. Expected distance: " + var14.getDistance() + ", expected points: " + oneRun + ". " + queryList + ", errors:" + var14.getErrors());
         return this;
      } else {
         PointList pointList = var14.getPoints();
         double tmpDist = pointList.calcDistance(this.distCalc);
         if(Math.abs(var14.getDistance() - tmpDist) > 2.0D) {
            this.errors.add(algoEntry + " path.getDistance was  " + var14.getDistance() + "\t pointList.calcDistance was " + tmpDist + "\t (expected points " + oneRun.getLocs() + ", expected distance " + oneRun.getDistance() + ") " + queryList);
         }

         if(Math.abs(var14.getDistance() - oneRun.getDistance()) > 2.0D) {
            this.errors.add(algoEntry + " returns path not matching the expected distance of " + oneRun.getDistance() + "\t Returned was " + var14.getDistance() + "\t (expected points " + oneRun.getLocs() + ", was " + pointList.getSize() + ") " + queryList);
         }

         if(Math.abs(pointList.getSize() - oneRun.getLocs()) > 1) {
            this.errors.add(algoEntry + " returns path not matching the expected points of " + oneRun.getLocs() + "\t Returned was " + pointList.getSize() + "\t (expected distance " + oneRun.getDistance() + ", was " + var14.getDistance() + ") " + queryList);
         }

         return this;
      }
   }

   void queryIndex(Graph g, LocationIndex idx, double lat, double lon, double expectedDist) {
      QueryResult res = idx.findClosest(lat, lon, EdgeFilter.ALL_EDGES);
      if(!res.isValid()) {
         this.errors.add("node not found for " + lat + "," + lon);
      } else {
         GHPoint3D found = res.getSnappedPoint();
         double dist = this.distCalc.calcDist(lat, lon, found.lat, found.lon);
         if(Math.abs(dist - expectedDist) > 0.1D) {
            this.errors.add("queried lat,lon=" + (float)lat + "," + (float)lon + " (found: " + (float)found.lat + "," + (float)found.lon + ")" + "\n   expected distance:" + expectedDist + ", but was:" + dist);
         }

      }
   }

   public String toString() {
      String str = "";
      str = str + "FOUND " + this.errors.size() + " ERRORS.\n";

      String s;
      for(Iterator i$ = this.errors.iterator(); i$.hasNext(); str = str + s + ".\n") {
         s = (String)i$.next();
      }

      return str;
   }

   void printSummary() {
      if(this.errors.size() > 0) {
         System.out.println("\n-------------------------------\n");
         System.out.println(this.toString());
      } else {
         System.out.println("SUCCESS for " + this.name + "!");
      }

   }

   static class AssumptionPerPath {
      double lat;
      double lon;
      int locs;
      double distance;

      public AssumptionPerPath(double lat, double lon, double distance, int locs) {
         this.lat = lat;
         this.lon = lon;
         this.locs = locs;
         this.distance = distance;
      }

      public String toString() {
         return this.lat + ", " + this.lon + ", locs:" + this.locs + ", dist:" + this.distance;
      }
   }

   public static class OneRun {
      private final List assumptions = new ArrayList();

      public OneRun() {
      }

      public OneRun(double fromLat, double fromLon, double toLat, double toLon, double dist, int locs) {
         this.add(fromLat, fromLon, 0.0D, 0);
         this.add(toLat, toLon, dist, locs);
      }

      public TestAlgoCollector.OneRun add(double lat, double lon, double dist, int locs) {
         this.assumptions.add(new TestAlgoCollector.AssumptionPerPath(lat, lon, dist, locs));
         return this;
      }

      public int getLocs() {
         int sum = 0;

         TestAlgoCollector.AssumptionPerPath as;
         for(Iterator i$ = this.assumptions.iterator(); i$.hasNext(); sum += as.locs) {
            as = (TestAlgoCollector.AssumptionPerPath)i$.next();
         }

         return sum;
      }

      public void setLocs(int index, int locs) {
         ((TestAlgoCollector.AssumptionPerPath)this.assumptions.get(index)).locs = locs;
      }

      public double getDistance() {
         double sum = 0.0D;

         TestAlgoCollector.AssumptionPerPath as;
         for(Iterator i$ = this.assumptions.iterator(); i$.hasNext(); sum += as.distance) {
            as = (TestAlgoCollector.AssumptionPerPath)i$.next();
         }

         return sum;
      }

      public void setDistance(int index, double dist) {
         ((TestAlgoCollector.AssumptionPerPath)this.assumptions.get(index)).distance = dist;
      }

      public List getList(LocationIndex idx, EdgeFilter edgeFilter) {
         ArrayList qr = new ArrayList();
         Iterator i$ = this.assumptions.iterator();

         while(i$.hasNext()) {
            TestAlgoCollector.AssumptionPerPath p = (TestAlgoCollector.AssumptionPerPath)i$.next();
            qr.add(idx.findClosest(p.lat, p.lon, edgeFilter));
         }

         return qr;
      }

      public String toString() {
         return this.assumptions.toString();
      }
   }

   public static class AlgoHelperEntry {
      private Graph queryGraph;
      private final Graph baseGraph;
      private final LocationIndex idx;
      private AlgorithmOptions opts;

      public AlgoHelperEntry(Graph g, Graph baseGraph, AlgorithmOptions opts, LocationIndex idx) {
         this.queryGraph = g;
         this.baseGraph = baseGraph;
         this.opts = opts;
         this.idx = idx;
      }

      public Graph getQueryGraph() {
         return this.queryGraph;
      }

      public void setQueryGraph(Graph queryGraph) {
         this.queryGraph = queryGraph;
      }

      public Graph getBaseGraph() {
         return this.baseGraph;
      }

      public void setAlgorithmOptions(AlgorithmOptions opts) {
         this.opts = opts;
      }

      public LocationIndex getIdx() {
         return this.idx;
      }

      public RoutingAlgorithm createAlgo(Graph qGraph) {
         return (new RoutingAlgorithmFactorySimple()).createAlgo(qGraph, this.opts);
      }

      public String toString() {
         return this.opts.getAlgorithm() + (this.queryGraph instanceof CHGraph?"CH":"");
      }
   }
}
