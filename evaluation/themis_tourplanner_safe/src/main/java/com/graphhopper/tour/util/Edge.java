package com.graphhopper.tour.util;

import java.util.Comparator;

public class Edge {
   public Object from;
   public Object to;
   public double weight;

   public Edge(Object from, Object to, double weight) {
      this.from = from;
      this.to = to;
      this.weight = weight;
   }

   public Edge reverse() {
      Object tmp = this.from;
      this.from = this.to;
      this.to = tmp;
      return this;
   }

   public String toString() {
      return this.from.toString() + " -> " + this.to.toString();
   }

   public static class WeightComparator implements Comparator {
      public int compare(Object e1, Object e2) {
         return Double.compare(((Edge)e1).weight, ((Edge)e2).weight);
      }
   }
}
