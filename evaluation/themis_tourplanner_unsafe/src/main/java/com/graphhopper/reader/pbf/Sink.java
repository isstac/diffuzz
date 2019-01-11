package com.graphhopper.reader.pbf;

import com.graphhopper.reader.OSMElement;

public interface Sink {
   void process(OSMElement var1);

   void complete();
}
