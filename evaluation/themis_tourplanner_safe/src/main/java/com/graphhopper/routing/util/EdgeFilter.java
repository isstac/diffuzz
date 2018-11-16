package com.graphhopper.routing.util;

import com.graphhopper.util.EdgeIteratorState;

public interface EdgeFilter {
   EdgeFilter ALL_EDGES = new EdgeFilter() {
      public final boolean accept(EdgeIteratorState edgeState) {
         return true;
      }
   };

   boolean accept(EdgeIteratorState var1);
}
