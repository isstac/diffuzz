package com.graphhopper.storage;

import com.graphhopper.routing.util.AllCHEdgesIterator;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.storage.Graph;
import com.graphhopper.util.CHEdgeExplorer;
import com.graphhopper.util.CHEdgeIteratorState;

public interface CHGraph extends Graph {
   void setLevel(int var1, int var2);

   int getLevel(int var1);

   boolean isShortcut(int var1);

   CHEdgeIteratorState shortcut(int var1, int var2);

   CHEdgeIteratorState getEdgeIteratorState(int var1, int var2);

   CHEdgeExplorer createEdgeExplorer();

   CHEdgeExplorer createEdgeExplorer(EdgeFilter var1);

   AllCHEdgesIterator getAllEdges();
}
