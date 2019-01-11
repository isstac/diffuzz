package com.graphhopper.routing;

import com.graphhopper.routing.Path;
import com.graphhopper.routing.RoutingAlgorithm;
import com.graphhopper.routing.util.DefaultEdgeFilter;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.TraversalMode;
import com.graphhopper.routing.util.Weighting;
import com.graphhopper.storage.EdgeEntry;
import com.graphhopper.storage.Graph;
import com.graphhopper.storage.NodeAccess;
import com.graphhopper.util.EdgeExplorer;
import com.graphhopper.util.EdgeIterator;
import com.graphhopper.util.EdgeIteratorState;

public abstract class AbstractRoutingAlgorithm implements RoutingAlgorithm {
   private EdgeFilter additionalEdgeFilter;
   protected final Graph graph;
   protected NodeAccess nodeAccess;
   protected EdgeExplorer inEdgeExplorer;
   protected EdgeExplorer outEdgeExplorer;
   protected final Weighting weighting;
   protected final FlagEncoder flagEncoder;
   protected final TraversalMode traversalMode;
   protected double weightLimit = Double.MAX_VALUE;
   private boolean alreadyRun;

   public AbstractRoutingAlgorithm(Graph graph, FlagEncoder encoder, Weighting weighting, TraversalMode traversalMode) {
      this.weighting = weighting;
      this.flagEncoder = encoder;
      this.traversalMode = traversalMode;
      this.graph = graph;
      this.nodeAccess = graph.getNodeAccess();
      this.outEdgeExplorer = graph.createEdgeExplorer(new DefaultEdgeFilter(this.flagEncoder, false, true));
      this.inEdgeExplorer = graph.createEdgeExplorer(new DefaultEdgeFilter(this.flagEncoder, true, false));
   }

   public void setWeightLimit(double weight) {
      this.weightLimit = weight;
   }

   public RoutingAlgorithm setEdgeFilter(EdgeFilter additionalEdgeFilter) {
      this.additionalEdgeFilter = additionalEdgeFilter;
      return this;
   }

   protected boolean accept(EdgeIterator iter, int prevOrNextEdgeId) {
      return !this.traversalMode.hasUTurnSupport() && iter.getEdge() == prevOrNextEdgeId?false:this.additionalEdgeFilter == null || this.additionalEdgeFilter.accept(iter);
   }

   protected void updateBestPath(EdgeIteratorState edgeState, EdgeEntry bestEdgeEntry, int traversalId) {
   }

   protected void checkAlreadyRun() {
      if(this.alreadyRun) {
         throw new IllegalStateException("Create a new instance per call");
      } else {
         this.alreadyRun = true;
      }
   }

   protected EdgeEntry createEdgeEntry(int node, double weight) {
      return new EdgeEntry(-1, node, weight);
   }

   protected abstract boolean finished();

   protected abstract Path extractPath();

   protected abstract boolean isWeightLimitExceeded();

   protected Path createEmptyPath() {
      return new Path(this.graph, this.flagEncoder);
   }

   public String getName() {
      return this.getClass().getSimpleName();
   }

   public String toString() {
      return this.getName() + "|" + this.weighting;
   }
}
