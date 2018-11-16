package com.graphhopper.tour;

import com.graphhopper.GraphHopper;
import com.graphhopper.routing.AlgorithmOptions;
import com.graphhopper.routing.Path;
import com.graphhopper.routing.QueryGraph;
import com.graphhopper.routing.RoutingAlgorithm;
import com.graphhopper.routing.RoutingAlgorithmFactory;
import com.graphhopper.routing.RoutingAlgorithmFactorySimple;
import com.graphhopper.routing.util.DefaultEdgeFilter;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.TraversalMode;
import com.graphhopper.routing.util.Weighting;
import com.graphhopper.routing.util.WeightingMap;
import com.graphhopper.storage.Graph;
import com.graphhopper.storage.GraphHopperStorage;
import com.graphhopper.storage.index.LocationIndex;
import com.graphhopper.storage.index.QueryResult;
import com.graphhopper.util.shapes.GHPoint;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

public class PathCalculator {
//   private final Logger logger = LoggerFactory.getLogger(this.getClass());
   private final Graph graph;
   private final LocationIndex locationIndex;
   private final EdgeFilter edgeFilter;
   private final RoutingAlgorithmFactory algoFactory;
   private final AlgorithmOptions algoOpts;

   public PathCalculator(GraphHopper hopper) {
      String algorithm = "dijkstrabi";
      GraphHopperStorage ghStorage = hopper.getGraphHopperStorage();
      this.locationIndex = hopper.getLocationIndex();
      EncodingManager encodingManager = hopper.getEncodingManager();
      FlagEncoder flagEncoder = (FlagEncoder)encodingManager.fetchEdgeEncoders().get(0);
      this.edgeFilter = new DefaultEdgeFilter(flagEncoder);
      WeightingMap weightingMap = new WeightingMap();
      Weighting weighting = hopper.createWeighting(weightingMap, flagEncoder);
      this.graph = ghStorage;
      TraversalMode traversalMode = hopper.getTraversalMode();
      this.algoFactory = new RoutingAlgorithmFactorySimple();
      this.algoOpts = new AlgorithmOptions(algorithm, flagEncoder, weighting, traversalMode);
   }

   public Path calcPath(GHPoint from, GHPoint to) {
      QueryResult fromQR = this.locationIndex.findClosest(from.lat, from.lon, this.edgeFilter);
      QueryResult toQR = this.locationIndex.findClosest(to.lat, to.lon, this.edgeFilter);
      QueryGraph queryGraph = new QueryGraph(this.graph);
      queryGraph.lookup(fromQR, toQR);
      RoutingAlgorithm algo = this.algoFactory.createAlgo(queryGraph, this.algoOpts);
      Path path = algo.calcPath(fromQR.getClosestNode(), toQR.getClosestNode());
      return path;
   }
}
