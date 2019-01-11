package com.graphhopper.routing.util;

import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.util.EdgeIteratorState;

public interface Weighting {
   double getMinWeight(double var1);

   double calcWeight(EdgeIteratorState var1, boolean var2, int var3);

   FlagEncoder getFlagEncoder();
}
