package com.graphhopper.routing;

import com.graphhopper.routing.AlgorithmOptions;
import com.graphhopper.routing.RoutingAlgorithm;
import com.graphhopper.storage.Graph;

public interface RoutingAlgorithmFactory {
   RoutingAlgorithm createAlgo(Graph var1, AlgorithmOptions var2);
}
