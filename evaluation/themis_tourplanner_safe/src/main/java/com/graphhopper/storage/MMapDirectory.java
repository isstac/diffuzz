package com.graphhopper.storage;

import com.graphhopper.storage.DAType;
import com.graphhopper.storage.GHDirectory;

public class MMapDirectory extends GHDirectory {
   private MMapDirectory() {
      this("");
      throw new IllegalStateException("reserved for direct mapped memory");
   }

   public MMapDirectory(String _location) {
      super(_location, DAType.MMAP);
   }
}
