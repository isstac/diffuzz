package com.graphhopper.routing.util;

public interface TurnCostEncoder {
   boolean isTurnRestricted(long var1);

   double getTurnCost(long var1);

   long getTurnFlags(boolean var1, double var2);

   public static class NoTurnCostsEncoder implements TurnCostEncoder {
      public boolean isTurnRestricted(long flags) {
         return false;
      }

      public double getTurnCost(long flags) {
         return 0.0D;
      }

      public long getTurnFlags(boolean restriction, double costs) {
         return 0L;
      }
   }
}
