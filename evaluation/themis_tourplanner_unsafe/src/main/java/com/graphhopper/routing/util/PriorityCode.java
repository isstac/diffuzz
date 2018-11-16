package com.graphhopper.routing.util;

public enum PriorityCode {
   WORST(0),
   AVOID_AT_ALL_COSTS(1),
   REACH_DEST(2),
   AVOID_IF_POSSIBLE(3),
   UNCHANGED(4),
   PREFER(5),
   VERY_NICE(6),
   BEST(7);

   private final int value;

   private PriorityCode(int value) {
      this.value = value;
   }

   public int getValue() {
      return this.value;
   }
}
