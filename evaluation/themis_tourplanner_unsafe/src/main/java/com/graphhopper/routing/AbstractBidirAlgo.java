package com.graphhopper.routing;

import com.graphhopper.routing.AbstractRoutingAlgorithm;
import com.graphhopper.routing.Path;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.TraversalMode;
import com.graphhopper.routing.util.Weighting;
import com.graphhopper.storage.Graph;

public abstract class AbstractBidirAlgo extends AbstractRoutingAlgorithm {
   int visitedCountFrom;
   int visitedCountTo;
   protected boolean finishedFrom;
   protected boolean finishedTo;

   abstract void initFrom(int var1, double var2);

   abstract void initTo(int var1, double var2);

   protected abstract Path createAndInitPath();

   protected abstract double getCurrentFromWeight();

   protected abstract double getCurrentToWeight();

   abstract boolean fillEdgesFrom();

   abstract boolean fillEdgesTo();

   public AbstractBidirAlgo(Graph graph, FlagEncoder encoder, Weighting weighting, TraversalMode tMode) {
      super(graph, encoder, weighting, tMode);
   }

   public Path calcPath(int from, int to) {
      this.checkAlreadyRun();
      this.createAndInitPath();
      this.initFrom(from, 0.0D);
      this.initTo(to, 0.0D);
      this.runAlgo();
      return this.extractPath();
   }

   protected void runAlgo() {
      while(!this.finished() && !this.isWeightLimitExceeded()) {
         if(!this.finishedFrom && !this.finishedTo) {
            if(this.getCurrentFromWeight() < this.getCurrentToWeight()) {
               this.finishedFrom = !this.fillEdgesFrom();
            } else {
               this.finishedTo = !this.fillEdgesTo();
            }
         } else if(!this.finishedFrom) {
            this.finishedFrom = !this.fillEdgesFrom();
         } else {
            this.finishedTo = !this.fillEdgesTo();
         }
      }

   }

   public int getVisitedNodes() {
      return this.visitedCountFrom + this.visitedCountTo;
   }
}
