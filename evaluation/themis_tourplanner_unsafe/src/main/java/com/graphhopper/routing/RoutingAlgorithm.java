package com.graphhopper.routing;

import com.graphhopper.routing.Path;
import com.graphhopper.util.NotThreadSafe;

@NotThreadSafe
public interface RoutingAlgorithm {
   Path calcPath(int var1, int var2);

   void setWeightLimit(double var1);

   String getName();

   int getVisitedNodes();
}
