package com.graphhopper.storage.index;

import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.storage.Storable;
import com.graphhopper.storage.index.QueryResult;

public interface LocationIndex extends Storable {
   LocationIndex setResolution(int var1);

   LocationIndex prepareIndex();

   int findID(double var1, double var3);

   QueryResult findClosest(double var1, double var3, EdgeFilter var5);

   LocationIndex setApproximation(boolean var1);

   void setSegmentSize(int var1);
}
