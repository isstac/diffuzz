package com.graphhopper.search;

import com.graphhopper.util.shapes.GHPlace;
import java.util.List;

public interface Geocoding {
   List names2places(GHPlace... var1);
}
