package com.graphhopper.http;

import com.graphhopper.GHResponse;
import com.graphhopper.util.PointList;
import java.util.Map;

public interface RouteSerializer {
   Map toJSON(GHResponse var1, boolean var2, boolean var3, boolean var4, boolean var5);

   Object createPoints(PointList var1, boolean var2, boolean var3);
}
