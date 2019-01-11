package com.graphhopper.util;

import com.graphhopper.util.CHEdgeIterator;
import com.graphhopper.util.EdgeExplorer;

public interface CHEdgeExplorer extends EdgeExplorer {
   CHEdgeIterator setBaseNode(int var1);
}
