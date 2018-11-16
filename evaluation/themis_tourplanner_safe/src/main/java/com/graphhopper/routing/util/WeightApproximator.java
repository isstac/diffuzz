package com.graphhopper.routing.util;

public interface WeightApproximator {
   double approximate(int var1);

   void setGoalNode(int var1);

   WeightApproximator duplicate();
}
