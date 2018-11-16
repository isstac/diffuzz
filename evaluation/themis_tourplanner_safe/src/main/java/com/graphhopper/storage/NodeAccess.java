package com.graphhopper.storage;

import com.graphhopper.util.PointAccess;

public interface NodeAccess extends PointAccess {
   int getAdditionalNodeField(int var1);

   void setAdditionalNodeField(int var1, int var2);
}
