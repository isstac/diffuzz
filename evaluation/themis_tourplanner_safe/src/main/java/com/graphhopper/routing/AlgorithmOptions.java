package com.graphhopper.routing;

import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.TraversalMode;
import com.graphhopper.routing.util.Weighting;
import com.graphhopper.util.PMap;

public class AlgorithmOptions {
   public static final String DIJKSTRA_BI = "dijkstrabi";
   public static final String DIJKSTRA = "dijkstra";
   public static final String DIJKSTRA_ONE_TO_MANY = "dijkstraOneToMany";
   public static final String ASTAR = "astar";
   public static final String ASTAR_BI = "astarbi";
   private String algorithm;
   private Weighting weighting;
   private TraversalMode traversalMode;
   private FlagEncoder flagEncoder;
   private final PMap hints;

   private AlgorithmOptions() {
      this.algorithm = "dijkstrabi";
      this.traversalMode = TraversalMode.NODE_BASED;
      this.hints = new PMap(5);
   }

   public AlgorithmOptions(String algorithm, FlagEncoder flagEncoder, Weighting weighting) {
      this.algorithm = "dijkstrabi";
      this.traversalMode = TraversalMode.NODE_BASED;
      this.hints = new PMap(5);
      this.algorithm = algorithm;
      this.weighting = weighting;
      this.flagEncoder = flagEncoder;
   }

   public AlgorithmOptions(String algorithm, FlagEncoder flagEncoder, Weighting weighting, TraversalMode tMode) {
      this.algorithm = "dijkstrabi";
      this.traversalMode = TraversalMode.NODE_BASED;
      this.hints = new PMap(5);
      this.algorithm = algorithm;
      this.weighting = weighting;
      this.flagEncoder = flagEncoder;
      this.traversalMode = tMode;
   }

   public TraversalMode getTraversalMode() {
      return this.traversalMode;
   }

   public Weighting getWeighting() {
      this.assertNotNull(this.weighting, "weighting");
      return this.weighting;
   }

   public String getAlgorithm() {
      this.assertNotNull(this.algorithm, "algorithm");
      return this.algorithm;
   }

   public FlagEncoder getFlagEncoder() {
      this.assertNotNull(this.flagEncoder, "flagEncoder");
      return this.flagEncoder;
   }

   public PMap getHints() {
      return this.hints;
   }

   private void assertNotNull(Object optionValue, String optionName) {
      if(optionValue == null) {
         throw new NullPointerException("Option \'" + optionName + "\' must NOT be null");
      }
   }

   public String toString() {
      return this.algorithm + ", " + this.weighting + ", " + this.flagEncoder + ", " + this.traversalMode;
   }

   public static AlgorithmOptions.Builder start() {
      return new AlgorithmOptions.Builder();
   }

   public static AlgorithmOptions.Builder start(AlgorithmOptions opts) {
      AlgorithmOptions.Builder b = new AlgorithmOptions.Builder();
      if(opts.algorithm != null) {
         b.algorithm(opts.getAlgorithm());
      }

      if(opts.flagEncoder != null) {
         b.flagEncoder(opts.getFlagEncoder());
      }

      if(opts.traversalMode != null) {
         b.traversalMode(opts.getTraversalMode());
      }

      if(opts.weighting != null) {
         b.weighting(opts.getWeighting());
      }

      return b;
   }

   // $FF: synthetic method
   AlgorithmOptions(AlgorithmOptions.SyntheticClass_1 x0) {
      this();
   }

   // $FF: synthetic class
   static class SyntheticClass_1 {
   }

   public static class Builder {
      private final AlgorithmOptions opts = new AlgorithmOptions();

      public AlgorithmOptions.Builder traversalMode(TraversalMode traversalMode) {
         if(traversalMode == null) {
            throw new IllegalArgumentException("null as traversal mode is not allowed");
         } else {
            this.opts.traversalMode = traversalMode;
            return this;
         }
      }

      public AlgorithmOptions.Builder weighting(Weighting weighting) {
         this.opts.weighting = weighting;
         return this;
      }

      public AlgorithmOptions.Builder algorithm(String algorithm) {
         this.opts.algorithm = algorithm;
         return this;
      }

      public AlgorithmOptions.Builder flagEncoder(FlagEncoder flagEncoder) {
         this.opts.flagEncoder = flagEncoder;
         return this;
      }

      public AlgorithmOptions.Builder hints(PMap hints) {
         this.opts.hints.put(hints);
         return this;
      }

      public AlgorithmOptions build() {
         return this.opts;
      }
   }
}
