package com.graphhopper.routing.util;

import com.graphhopper.coll.GHBitSet;
import com.graphhopper.coll.GHBitSetImpl;
import com.graphhopper.routing.util.DefaultEdgeFilter;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.TarjansStronglyConnectedComponentsAlgorithm;
import com.graphhopper.storage.GraphHopperStorage;
import com.graphhopper.util.BreadthFirstSearch;
import com.graphhopper.util.EdgeExplorer;
import com.graphhopper.util.EdgeIterator;
import com.graphhopper.util.EdgeIteratorState;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PrepareRoutingSubnetworks {
   private final Logger logger = LoggerFactory.getLogger(this.getClass());
   private final GraphHopperStorage ghStorage;
   private int minNetworkSize = 200;
   private int minOneWayNetworkSize = 0;
   private int subnetworks = -1;
   private final AtomicInteger maxEdgesPerNode = new AtomicInteger(0);
   private final List encoders;

   public PrepareRoutingSubnetworks(GraphHopperStorage ghStorage, Collection encoders) {
      this.ghStorage = ghStorage;
      this.encoders = new ArrayList(encoders);
   }

   public PrepareRoutingSubnetworks setMinNetworkSize(int minNetworkSize) {
      this.minNetworkSize = minNetworkSize;
      return this;
   }

   public PrepareRoutingSubnetworks setMinOneWayNetworkSize(int minOnewayNetworkSize) {
      this.minOneWayNetworkSize = minOnewayNetworkSize;
      return this;
   }

   public void doWork() {
      if(this.minNetworkSize > 0 || this.minOneWayNetworkSize > 0) {
         int unvisitedDeadEnds = 0;

         List components;
         for(Iterator i$ = this.encoders.iterator(); i$.hasNext(); this.subnetworks = Math.max(components.size(), this.subnetworks)) {
            FlagEncoder encoder = (FlagEncoder)i$.next();
            PrepareRoutingSubnetworks.PrepEdgeFilter filter = new PrepareRoutingSubnetworks.PrepEdgeFilter(encoder);
            if(this.minOneWayNetworkSize > 0) {
               unvisitedDeadEnds += this.removeDeadEndUnvisitedNetworks(filter);
            }

            components = this.findSubnetworks(filter);
            this.keepLargeNetworks(filter, components);
         }

         this.markNodesRemovedIfUnreachable();
         this.logger.info("optimize to remove subnetworks (" + this.subnetworks + "), " + "unvisited-dead-end-nodes (" + unvisitedDeadEnds + "), " + "maxEdges/node (" + this.maxEdgesPerNode.get() + ")");
         this.ghStorage.optimize();
      }
   }

   public int getMaxSubnetworks() {
      return this.subnetworks;
   }

   List findSubnetworks(PrepareRoutingSubnetworks.PrepEdgeFilter filter) {
      final FlagEncoder encoder = filter.getEncoder();
      EdgeExplorer explorer = this.ghStorage.createEdgeExplorer(filter);
      int locs = this.ghStorage.getNodes();
      ArrayList list = new ArrayList(100);
      final GHBitSetImpl bs = new GHBitSetImpl(locs);

      for(int start = 0; start < locs; ++start) {
         if(!bs.contains(start)) {
            final TIntArrayList intList = new TIntArrayList(20);
            list.add(intList);
            (new BreadthFirstSearch() {
               int tmpCounter = 0;

               protected GHBitSet createBitSet() {
                  return bs;
               }

               protected final boolean goFurther(int nodeId) {
                  if(this.tmpCounter > PrepareRoutingSubnetworks.this.maxEdgesPerNode.get()) {
                     PrepareRoutingSubnetworks.this.maxEdgesPerNode.set(this.tmpCounter);
                  }

                  this.tmpCounter = 0;
                  intList.add(nodeId);
                  return true;
               }

               protected final boolean checkAdjacent(EdgeIteratorState edge) {
                  if(!encoder.isForward(edge.getFlags()) && !encoder.isBackward(edge.getFlags())) {
                     return false;
                  } else {
                     ++this.tmpCounter;
                     return true;
                  }
               }
            }).start(explorer, start);
            intList.trimToSize();
         }
      }

      return list;
   }

   int keepLargeNetworks(PrepareRoutingSubnetworks.PrepEdgeFilter filter, List components) {
      if(components.size() <= 1) {
         return 0;
      } else {
         int maxCount = -1;
         TIntArrayList oldComponent = null;
         int allRemoved = 0;
         FlagEncoder encoder = filter.getEncoder();
         EdgeExplorer explorer = this.ghStorage.createEdgeExplorer(filter);
         Iterator i$ = components.iterator();

         while(i$.hasNext()) {
            TIntArrayList component = (TIntArrayList)i$.next();
            if(maxCount < 0) {
               maxCount = component.size();
               oldComponent = component;
            } else {
               int removedEdges;
               if(maxCount < component.size()) {
                  removedEdges = this.removeEdges(explorer, encoder, oldComponent, this.minNetworkSize);
                  maxCount = component.size();
                  oldComponent = component;
               } else {
                  removedEdges = this.removeEdges(explorer, encoder, component, this.minNetworkSize);
               }

               allRemoved += removedEdges;
            }
         }

         if(allRemoved > this.ghStorage.getAllEdges().getMaxId() / 2) {
            throw new IllegalStateException("Too many total edges were removed: " + allRemoved + ", all edges:" + this.ghStorage.getAllEdges().getMaxId());
         } else {
            return allRemoved;
         }
      }
   }

   String toString(FlagEncoder encoder, EdgeIterator iter) {
      String str;
      for(str = ""; iter.next(); str = str + ";\n ") {
         int adjNode = iter.getAdjNode();
         str = str + adjNode + " (" + this.ghStorage.getNodeAccess().getLat(adjNode) + "," + this.ghStorage.getNodeAccess().getLon(adjNode) + "), ";
         str = str + "speed  (fwd:" + encoder.getSpeed(iter.getFlags()) + ", rev:" + encoder.getReverseSpeed(iter.getFlags()) + "), ";
         str = str + "access (fwd:" + encoder.isForward(iter.getFlags()) + ", rev:" + encoder.isBackward(iter.getFlags()) + "), ";
         str = str + "distance:" + iter.getDistance();
      }

      return str;
   }

   int removeDeadEndUnvisitedNetworks(PrepareRoutingSubnetworks.PrepEdgeFilter bothFilter) {
      DefaultEdgeFilter outFilter = new DefaultEdgeFilter(bothFilter.getEncoder(), false, true);
      List components = (new TarjansStronglyConnectedComponentsAlgorithm(this.ghStorage, outFilter)).findComponents();
      return this.removeEdges(bothFilter, components, this.minOneWayNetworkSize);
   }

   int removeEdges(PrepareRoutingSubnetworks.PrepEdgeFilter bothFilter, List components, int min) {
      FlagEncoder encoder = bothFilter.getEncoder();
      EdgeExplorer explorer = this.ghStorage.createEdgeExplorer(bothFilter);
      int removedEdges = 0;

      TIntArrayList component;
      for(Iterator i$ = components.iterator(); i$.hasNext(); removedEdges += this.removeEdges(explorer, encoder, component, min)) {
         component = (TIntArrayList)i$.next();
      }

      return removedEdges;
   }

   int removeEdges(EdgeExplorer explorer, FlagEncoder encoder, TIntList component, int min) {
      int removedEdges = 0;
      if(component.size() < min) {
         for(int i = 0; i < component.size(); ++i) {
            for(EdgeIterator edge = explorer.setBaseNode(component.get(i)); edge.next(); ++removedEdges) {
               edge.setFlags(encoder.setAccess(edge.getFlags(), false, false));
            }
         }
      }

      return removedEdges;
   }

   void markNodesRemovedIfUnreachable() {
      EdgeExplorer edgeExplorer = this.ghStorage.createEdgeExplorer();

      for(int nodeIndex = 0; nodeIndex < this.ghStorage.getNodes(); ++nodeIndex) {
         if(this.detectNodeRemovedForAllEncoders(edgeExplorer, nodeIndex)) {
            this.ghStorage.markNodeRemoved(nodeIndex);
         }
      }

   }

   boolean detectNodeRemovedForAllEncoders(EdgeExplorer edgeExplorerAllEdges, int nodeIndex) {
      EdgeIterator iter = edgeExplorerAllEdges.setBaseNode(nodeIndex);

      while(iter.next()) {
         Iterator i$ = this.encoders.iterator();

         while(i$.hasNext()) {
            FlagEncoder encoder = (FlagEncoder)i$.next();
            if(encoder.isBackward(iter.getFlags()) || encoder.isForward(iter.getFlags())) {
               return false;
            }
         }
      }

      return true;
   }

   static class PrepEdgeFilter extends DefaultEdgeFilter {
      FlagEncoder encoder;

      public PrepEdgeFilter(FlagEncoder encoder) {
         super(encoder);
         this.encoder = encoder;
      }

      public FlagEncoder getEncoder() {
         return this.encoder;
      }
   }
}
