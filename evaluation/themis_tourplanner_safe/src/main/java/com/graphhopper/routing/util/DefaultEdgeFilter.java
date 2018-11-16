package com.graphhopper.routing.util;

import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.util.EdgeIteratorState;

public class DefaultEdgeFilter implements EdgeFilter {
   private final boolean in;
   private final boolean out;
   private FlagEncoder encoder;

   public DefaultEdgeFilter(FlagEncoder encoder) {
      this(encoder, true, true);
   }

   public DefaultEdgeFilter(FlagEncoder encoder, boolean in, boolean out) {
      this.encoder = encoder;
      this.in = in;
      this.out = out;
   }

   public final boolean accept(EdgeIteratorState iter) {
      return this.out && iter.isForward(this.encoder) || this.in && iter.isBackward(this.encoder);
   }

   public String toString() {
      return this.encoder.toString() + ", in:" + this.in + ", out:" + this.out;
   }
}
