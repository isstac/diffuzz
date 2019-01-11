package com.graphhopper.routing;

import com.graphhopper.routing.AStar;
import com.graphhopper.routing.AStarBidirection;
import com.graphhopper.routing.AlgorithmOptions;
import com.graphhopper.routing.Dijkstra;
import com.graphhopper.routing.DijkstraBidirectionRef;
import com.graphhopper.routing.DijkstraOneToMany;
import com.graphhopper.routing.RoutingAlgorithm;
import com.graphhopper.routing.RoutingAlgorithmFactory;
import com.graphhopper.routing.util.BeelineWeightApproximator;
import com.graphhopper.routing.util.WeightApproximator;
import com.graphhopper.storage.Graph;
import com.graphhopper.storage.NodeAccess;
import com.graphhopper.util.Helper;

public class RoutingAlgorithmFactorySimple implements RoutingAlgorithmFactory {
   public RoutingAlgorithm createAlgo(Graph g, AlgorithmOptions opts) {
      String algoStr = opts.getAlgorithm();
      if("dijkstrabi".equalsIgnoreCase(algoStr)) {
         return new DijkstraBidirectionRef(g, opts.getFlagEncoder(), opts.getWeighting(), opts.getTraversalMode());
      } else if("dijkstra".equalsIgnoreCase(algoStr)) {
         return new Dijkstra(g, opts.getFlagEncoder(), opts.getWeighting(), opts.getTraversalMode());
      } else if("astarbi".equalsIgnoreCase(algoStr)) {
         AStarBidirection aStar1 = new AStarBidirection(g, opts.getFlagEncoder(), opts.getWeighting(), opts.getTraversalMode());
         aStar1.setApproximation(this.getApproximation("astarbi", opts, g.getNodeAccess()));
         return aStar1;
      } else if("dijkstraOneToMany".equalsIgnoreCase(algoStr)) {
         return new DijkstraOneToMany(g, opts.getFlagEncoder(), opts.getWeighting(), opts.getTraversalMode());
      } else if("astar".equalsIgnoreCase(algoStr)) {
         AStar aStar = new AStar(g, opts.getFlagEncoder(), opts.getWeighting(), opts.getTraversalMode());
         aStar.setApproximation(this.getApproximation("astar", opts, g.getNodeAccess()));
         return aStar;
      } else {
         throw new IllegalArgumentException("Algorithm " + algoStr + " not found in " + this.getClass().getName());
      }
   }

   private WeightApproximator getApproximation(String prop, AlgorithmOptions opts, NodeAccess na) {
      String approxAsStr = opts.getHints().get(prop + ".approximation", "BeelineSimplification");
      BeelineWeightApproximator approx;
      if("BeelineSimplification".equals(approxAsStr)) {
         approx = new BeelineWeightApproximator(na, opts.getWeighting());
         approx.setDistanceCalc(Helper.DIST_PLANE);
         return approx;
      } else if("BeelineAccurate".equals(approxAsStr)) {
         approx = new BeelineWeightApproximator(na, opts.getWeighting());
         approx.setDistanceCalc(Helper.DIST_EARTH);
         return approx;
      } else {
         throw new IllegalArgumentException("Approximation " + approxAsStr + " not found in " + this.getClass().getName());
      }
   }
}
