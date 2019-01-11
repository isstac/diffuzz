package com.graphhopper.storage;

import com.graphhopper.storage.DAType;
import com.graphhopper.storage.GHDirectory;

public class RAMDirectory extends GHDirectory {
   public RAMDirectory() {
      this("", false);
   }

   public RAMDirectory(String location) {
      this(location, false);
   }

   public RAMDirectory(String _location, boolean store) {
      super(_location, store?DAType.RAM_STORE:DAType.RAM);
   }
}
