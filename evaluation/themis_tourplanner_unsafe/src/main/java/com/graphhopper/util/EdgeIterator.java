package com.graphhopper.util;

import com.graphhopper.util.EdgeIteratorState;

public interface EdgeIterator extends EdgeIteratorState {
   int NO_EDGE = -1;

   boolean next();

   public static class Edge {
      public static boolean isValid(int edgeId) {
         return edgeId > -1;
      }
   }
}
