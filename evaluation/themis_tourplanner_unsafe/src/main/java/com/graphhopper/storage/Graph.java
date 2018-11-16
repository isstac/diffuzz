package com.graphhopper.storage;

import com.graphhopper.routing.util.AllEdgesIterator;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.storage.GraphExtension;
import com.graphhopper.storage.NodeAccess;
import com.graphhopper.util.EdgeExplorer;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.shapes.BBox;

public interface Graph {
   Graph getBaseGraph();

   int getNodes();

   NodeAccess getNodeAccess();

   BBox getBounds();

   EdgeIteratorState edge(int var1, int var2);

   EdgeIteratorState edge(int var1, int var2, double var3, boolean var5);

   EdgeIteratorState getEdgeIteratorState(int var1, int var2);

   AllEdgesIterator getAllEdges();

   EdgeExplorer createEdgeExplorer(EdgeFilter var1);

   EdgeExplorer createEdgeExplorer();

   Graph copyTo(Graph var1);

   GraphExtension getExtension();
}
