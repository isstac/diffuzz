package com.graphhopper.storage;

import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.storage.Directory;
import com.graphhopper.storage.Storable;
import com.graphhopper.storage.StorableProperties;

public interface GraphStorage extends Storable {
   Directory getDirectory();

   EncodingManager getEncodingManager();

   void setSegmentSize(int var1);

   String toDetailsString();

   StorableProperties getProperties();

   void markNodeRemoved(int var1);

   boolean isNodeRemoved(int var1);

   void optimize();
}
