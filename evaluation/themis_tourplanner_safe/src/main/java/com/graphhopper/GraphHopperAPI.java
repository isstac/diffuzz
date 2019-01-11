package com.graphhopper;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;

public interface GraphHopperAPI {
   boolean load(String var1);

   GHResponse route(GHRequest var1);
}
