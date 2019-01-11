package com.graphhopper.util;

import com.graphhopper.coll.GHBitSet;
import com.graphhopper.util.EdgeExplorer;
import com.graphhopper.util.EdgeIterator;
import com.graphhopper.util.XFirstSearch;
import gnu.trove.stack.array.TIntArrayStack;

public class DepthFirstSearch extends XFirstSearch {
   public void start(EdgeExplorer explorer, int startNode) {
      TIntArrayStack stack = new TIntArrayStack();
      GHBitSet explored = this.createBitSet();
      stack.push(startNode);

      while(true) {
         int current;
         do {
            do {
               if(stack.size() <= 0) {
                  return;
               }

               current = stack.pop();
            } while(explored.contains(current));
         } while(!this.goFurther(current));

         EdgeIterator iter = explorer.setBaseNode(current);

         while(iter.next()) {
            int connectedId = iter.getAdjNode();
            if(this.checkAdjacent(iter)) {
               stack.push(connectedId);
            }
         }

         explored.add(current);
      }
   }
}
