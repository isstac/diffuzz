package com.graphhopper.routing.util;

import com.graphhopper.routing.util.WeightApproximator;

public class ConsistentWeightApproximator {
   private final WeightApproximator uniDirApproximatorForward;
   private final WeightApproximator uniDirApproximatorReverse;

   public ConsistentWeightApproximator(WeightApproximator weightApprox) {
      this.uniDirApproximatorForward = weightApprox;
      this.uniDirApproximatorReverse = weightApprox.duplicate();
   }

   public void setSourceNode(int sourceNode) {
      this.uniDirApproximatorReverse.setGoalNode(sourceNode);
   }

   public void setGoalNode(int goalNode) {
      this.uniDirApproximatorForward.setGoalNode(goalNode);
   }

   public double approximate(int fromNode, boolean reverse) {
      double weightApproximation = 0.5D * (this.uniDirApproximatorForward.approximate(fromNode) - this.uniDirApproximatorReverse.approximate(fromNode));
      if(reverse) {
         weightApproximation *= -1.0D;
      }

      return weightApproximation;
   }
}
