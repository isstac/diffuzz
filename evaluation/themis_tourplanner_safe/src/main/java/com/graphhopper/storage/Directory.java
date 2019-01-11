package com.graphhopper.storage;

import com.graphhopper.storage.DAType;
import com.graphhopper.storage.DataAccess;
import java.nio.ByteOrder;
import java.util.Collection;

public interface Directory {
   String getLocation();

   ByteOrder getByteOrder();

   DataAccess find(String var1);

   DataAccess find(String var1, DAType var2);

   void remove(DataAccess var1);

   DAType getDefaultType();

   void clear();

   Collection getAll();
}
