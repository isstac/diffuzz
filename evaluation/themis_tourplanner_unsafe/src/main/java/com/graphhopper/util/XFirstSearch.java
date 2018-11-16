package com.graphhopper.util;

import com.graphhopper.coll.GHBitSet;
import com.graphhopper.coll.GHBitSetImpl;
import com.graphhopper.util.EdgeExplorer;
import com.graphhopper.util.EdgeIteratorState;

public abstract class XFirstSearch {
   protected GHBitSet createBitSet() {
      return new GHBitSetImpl();
   }

   public abstract void start(EdgeExplorer var1, int var2);

   protected boolean goFurther(int nodeId) {
      return true;
   }

   protected boolean checkAdjacent(EdgeIteratorState edge) {
      return true;
   }
}
