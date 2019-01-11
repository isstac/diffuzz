package com.graphhopper.util;

import com.graphhopper.coll.GHBitSet;
import com.graphhopper.util.EdgeExplorer;
import com.graphhopper.util.EdgeIterator;
import com.graphhopper.util.SimpleIntDeque;
import com.graphhopper.util.XFirstSearch;

public class BreadthFirstSearch extends XFirstSearch {
   public void start(EdgeExplorer explorer, int startNode) {
      SimpleIntDeque fifo = new SimpleIntDeque();
      GHBitSet visited = this.createBitSet();
      visited.add(startNode);
      fifo.push(startNode);

      while(true) {
         int current;
         do {
            if(fifo.isEmpty()) {
               return;
            }

            current = fifo.pop();
         } while(!this.goFurther(current));

         EdgeIterator iter = explorer.setBaseNode(current);

         while(iter.next()) {
            int connectedId = iter.getAdjNode();
            if(this.checkAdjacent(iter) && !visited.contains(connectedId)) {
               visited.add(connectedId);
               fifo.push(connectedId);
            }
         }
      }
   }
}
