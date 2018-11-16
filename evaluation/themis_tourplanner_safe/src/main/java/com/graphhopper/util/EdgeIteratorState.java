package com.graphhopper.util;

import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.util.PointList;

public interface EdgeIteratorState {
   int K_UNFAVORED_EDGE = -1;

   int getEdge();

   int getBaseNode();

   int getAdjNode();

   PointList fetchWayGeometry(int var1);

   EdgeIteratorState setWayGeometry(PointList var1);

   double getDistance();

   EdgeIteratorState setDistance(double var1);

   long getFlags();

   EdgeIteratorState setFlags(long var1);

   int getAdditionalField();

   boolean isForward(FlagEncoder var1);

   boolean isBackward(FlagEncoder var1);

   boolean getBoolean(int var1, boolean var2, boolean var3);

   EdgeIteratorState setAdditionalField(int var1);

   String getName();

   EdgeIteratorState setName(String var1);

   EdgeIteratorState detach(boolean var1);

   EdgeIteratorState copyPropertiesTo(EdgeIteratorState var1);
}
