package com.graphhopper.routing.util;

import com.graphhopper.coll.GHBitSetImpl;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.storage.GraphHopperStorage;
import com.graphhopper.util.EdgeIterator;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.stack.array.TIntArrayStack;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class TarjansStronglyConnectedComponentsAlgorithm {
   private final GraphHopperStorage graph;
   private final TIntArrayStack nodeStack;
   private final GHBitSetImpl onStack;
   private final int[] nodeIndex;
   private final int[] nodeLowLink;
   private final ArrayList components = new ArrayList();
   private int index = 1;
   private final EdgeFilter edgeFilter;

   public TarjansStronglyConnectedComponentsAlgorithm(GraphHopperStorage graph, EdgeFilter edgeFilter) {
      this.graph = graph;
      this.nodeStack = new TIntArrayStack();
      this.onStack = new GHBitSetImpl(graph.getNodes());
      this.nodeIndex = new int[graph.getNodes()];
      this.nodeLowLink = new int[graph.getNodes()];
      this.edgeFilter = edgeFilter;
   }

   public List findComponents() {
      int nodes = this.graph.getNodes();

      for(int start = 0; start < nodes; ++start) {
         if(this.nodeIndex[start] == 0 && !this.graph.isNodeRemoved(start)) {
            this.strongConnect(start);
         }
      }

      return this.components;
   }

   private void strongConnect(int firstNode) {
      Stack stateStack = new Stack();
      stateStack.push(TarjansStronglyConnectedComponentsAlgorithm.TarjanState.startState(firstNode));

      while(true) {
         label43:
         while(!stateStack.empty()) {
            TarjansStronglyConnectedComponentsAlgorithm.TarjanState state = (TarjansStronglyConnectedComponentsAlgorithm.TarjanState)stateStack.pop();
            int start = state.start;
            EdgeIterator iter;
            int component;
            if(state.isStart()) {
               this.nodeIndex[start] = this.index;
               this.nodeLowLink[start] = this.index++;
               this.nodeStack.push(start);
               this.onStack.set(start);
               iter = this.graph.createEdgeExplorer(this.edgeFilter).setBaseNode(start);
            } else {
               iter = state.iter;
               component = iter.getAdjNode();
               this.nodeLowLink[start] = Math.min(this.nodeLowLink[start], this.nodeLowLink[component]);
            }

            while(iter.next()) {
               component = iter.getAdjNode();
               if(this.nodeIndex[component] == 0) {
                  stateStack.push(TarjansStronglyConnectedComponentsAlgorithm.TarjanState.resumeState(start, iter));
                  stateStack.push(TarjansStronglyConnectedComponentsAlgorithm.TarjanState.startState(component));
                  continue label43;
               }

               if(this.onStack.contains(component)) {
                  this.nodeLowLink[start] = Math.min(this.nodeLowLink[start], this.nodeIndex[component]);
               }
            }

            if(this.nodeIndex[start] == this.nodeLowLink[start]) {
               TIntArrayList component1 = new TIntArrayList();

               int node;
               while((node = this.nodeStack.pop()) != start) {
                  component1.add(node);
                  this.onStack.clear(node);
               }

               component1.add(start);
               component1.trimToSize();
               this.onStack.clear(start);
               this.components.add(component1);
            }
         }

         return;
      }
   }

   private static class TarjanState {
      final int start;
      final EdgeIterator iter;

      boolean isStart() {
         return this.iter == null;
      }

      private TarjanState(int start, EdgeIterator iter) {
         this.start = start;
         this.iter = iter;
      }

      public static TarjansStronglyConnectedComponentsAlgorithm.TarjanState startState(int start) {
         return new TarjansStronglyConnectedComponentsAlgorithm.TarjanState(start, (EdgeIterator)null);
      }

      public static TarjansStronglyConnectedComponentsAlgorithm.TarjanState resumeState(int start, EdgeIterator iter) {
         return new TarjansStronglyConnectedComponentsAlgorithm.TarjanState(start, iter);
      }
   }
}
