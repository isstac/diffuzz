package com.graphhopper.util;

import com.graphhopper.util.EdgeIteratorState;

public interface CHEdgeIteratorState extends EdgeIteratorState {
   int getSkippedEdge1();

   int getSkippedEdge2();

   void setSkippedEdges(int var1, int var2);

   boolean isShortcut();

   boolean canBeOverwritten(long var1);

   CHEdgeIteratorState setWeight(double var1);

   double getWeight();
}
