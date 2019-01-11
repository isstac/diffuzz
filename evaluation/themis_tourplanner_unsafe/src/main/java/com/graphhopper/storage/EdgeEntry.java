package com.graphhopper.storage;

public class EdgeEntry implements Cloneable, Comparable {
   public int edge;
   public int adjNode;
   public double weight;
   public EdgeEntry parent;

   public EdgeEntry(int edgeId, int adjNode, double weight) {
      this.edge = edgeId;
      this.adjNode = adjNode;
      this.weight = weight;
   }

   public EdgeEntry clone() {
      return new EdgeEntry(this.edge, this.adjNode, this.weight);
   }

   public EdgeEntry cloneFull() {
      EdgeEntry de = this.clone();
      EdgeEntry tmpPrev = this.parent;

      for(EdgeEntry cl = de; tmpPrev != null; tmpPrev = tmpPrev.parent) {
         cl.parent = tmpPrev.clone();
         cl = cl.parent;
      }

      return de;
   }

   public int compareTo(Object o) {
	   EdgeEntry obj = (EdgeEntry) o;
      return this.weight < obj.weight?-1:(this.weight > obj.weight?1:0);
   }

   public String toString() {
      return this.adjNode + " (" + this.edge + ") weight: " + this.weight;
   }
}
