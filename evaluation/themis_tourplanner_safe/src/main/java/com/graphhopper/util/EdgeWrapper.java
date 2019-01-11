package com.graphhopper.util;

import com.graphhopper.util.NotThreadSafe;
import gnu.trove.map.hash.TIntIntHashMap;
import java.util.Arrays;

@NotThreadSafe
public class EdgeWrapper {
   private static final float GROW_FACTOR = 1.5F;
   private int refCounter;
   private int[] nodes;
   private int[] edgeIds;
   private int[] parents;
   private float[] weights;
   protected TIntIntHashMap node2ref;

   public EdgeWrapper() {
      this(10);
   }

   public EdgeWrapper(int size) {
      this.nodes = new int[size];
      this.parents = new int[size];
      this.edgeIds = new int[size];
      this.weights = new float[size];
      this.node2ref = new TIntIntHashMap(size, 1.5F, -1, -1);
   }

   public int add(int nodeId, double distance, int edgeId) {
      int ref = this.refCounter++;
      this.node2ref.put(nodeId, ref);
      this.ensureCapacity(ref);
      this.weights[ref] = (float)distance;
      this.nodes[ref] = nodeId;
      this.parents[ref] = -1;
      this.edgeIds[ref] = edgeId;
      return ref;
   }

   public void putWeight(int ref, double dist) {
      if(ref < 1) {
         throw new IllegalStateException("You cannot save a reference with values smaller 1. 0 is reserved");
      } else {
         this.weights[ref] = (float)dist;
      }
   }

   public void putEdgeId(int ref, int edgeId) {
      if(ref < 1) {
         throw new IllegalStateException("You cannot save a reference with values smaller 1. 0 is reserved");
      } else {
         this.edgeIds[ref] = edgeId;
      }
   }

   public void putParent(int ref, int link) {
      if(ref < 1) {
         throw new IllegalStateException("You cannot save a reference with values smaller 1. 0 is reserved");
      } else {
         this.parents[ref] = link;
      }
   }

   public double getWeight(int ref) {
      return (double)this.weights[ref];
   }

   public int getNode(int ref) {
      return this.nodes[ref];
   }

   public int getParent(int ref) {
      return this.parents[ref];
   }

   public int getEdgeId(int ref) {
      return this.edgeIds[ref];
   }

   private void ensureCapacity(int size) {
      if(size >= this.nodes.length) {
         this.resize(Math.round(1.5F * (float)size));
      }
   }

   private void resize(int cap) {
      this.weights = Arrays.copyOf(this.weights, cap);
      this.nodes = Arrays.copyOf(this.nodes, cap);
      this.parents = Arrays.copyOf(this.parents, cap);
      this.edgeIds = Arrays.copyOf(this.edgeIds, cap);
      this.node2ref.ensureCapacity(cap);
   }

   public void clear() {
      this.refCounter = 0;
      Arrays.fill(this.weights, 0.0F);
      Arrays.fill(this.nodes, 0);
      Arrays.fill(this.parents, 0);
      Arrays.fill(this.edgeIds, -1);
      this.node2ref.clear();
   }

   public int getRef(int node) {
      return this.node2ref.get(node);
   }

   public boolean isEmpty() {
      return this.refCounter == 0;
   }
}
