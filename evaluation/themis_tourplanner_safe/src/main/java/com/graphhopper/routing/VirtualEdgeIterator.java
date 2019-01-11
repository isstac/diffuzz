package com.graphhopper.routing;

import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.util.CHEdgeIteratorState;
import com.graphhopper.util.EdgeIterator;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.PointList;
import java.util.ArrayList;
import java.util.List;

class VirtualEdgeIterator implements EdgeIterator, CHEdgeIteratorState {
   private final List edges;
   private int current;

   public VirtualEdgeIterator(int edgeCount) {
      this.edges = new ArrayList(edgeCount);
      this.reset();
   }

   void add(EdgeIteratorState edge) {
      this.edges.add(edge);
   }

   EdgeIterator reset() {
      this.current = -1;
      return this;
   }

   int count() {
      return this.edges.size();
   }

   public boolean next() {
      ++this.current;
      return this.current < this.edges.size();
   }

   public EdgeIteratorState detach(boolean reverse) {
      if(reverse) {
         throw new IllegalStateException("Not yet supported");
      } else {
         return (EdgeIteratorState)this.edges.get(this.current);
      }
   }

   public int getEdge() {
      return ((EdgeIteratorState)this.edges.get(this.current)).getEdge();
   }

   public int getBaseNode() {
      return ((EdgeIteratorState)this.edges.get(this.current)).getBaseNode();
   }

   public int getAdjNode() {
      return ((EdgeIteratorState)this.edges.get(this.current)).getAdjNode();
   }

   public PointList fetchWayGeometry(int mode) {
      return ((EdgeIteratorState)this.edges.get(this.current)).fetchWayGeometry(mode);
   }

   public EdgeIteratorState setWayGeometry(PointList list) {
      return ((EdgeIteratorState)this.edges.get(this.current)).setWayGeometry(list);
   }

   public double getDistance() {
      return ((EdgeIteratorState)this.edges.get(this.current)).getDistance();
   }

   public EdgeIteratorState setDistance(double dist) {
      return ((EdgeIteratorState)this.edges.get(this.current)).setDistance(dist);
   }

   public long getFlags() {
      return ((EdgeIteratorState)this.edges.get(this.current)).getFlags();
   }

   public EdgeIteratorState setFlags(long flags) {
      return ((EdgeIteratorState)this.edges.get(this.current)).setFlags(flags);
   }

   public String getName() {
      return ((EdgeIteratorState)this.edges.get(this.current)).getName();
   }

   public EdgeIteratorState setName(String name) {
      return ((EdgeIteratorState)this.edges.get(this.current)).setName(name);
   }

   public boolean getBoolean(int key, boolean reverse, boolean _default) {
      return ((EdgeIteratorState)this.edges.get(this.current)).getBoolean(key, reverse, _default);
   }

   public String toString() {
      return this.edges.toString();
   }

   public int getAdditionalField() {
      return ((EdgeIteratorState)this.edges.get(this.current)).getAdditionalField();
   }

   public EdgeIteratorState setAdditionalField(int value) {
      return ((EdgeIteratorState)this.edges.get(this.current)).setAdditionalField(value);
   }

   public EdgeIteratorState copyPropertiesTo(EdgeIteratorState edge) {
      return ((EdgeIteratorState)this.edges.get(this.current)).copyPropertiesTo(edge);
   }

   public boolean isBackward(FlagEncoder encoder) {
      return ((EdgeIteratorState)this.edges.get(this.current)).isBackward(encoder);
   }

   public boolean isForward(FlagEncoder encoder) {
      return ((EdgeIteratorState)this.edges.get(this.current)).isForward(encoder);
   }

   public boolean isShortcut() {
      EdgeIteratorState edge = (EdgeIteratorState)this.edges.get(this.current);
      return edge instanceof CHEdgeIteratorState && ((CHEdgeIteratorState)edge).isShortcut();
   }

   public double getWeight() {
      return ((CHEdgeIteratorState)this.edges.get(this.current)).getWeight();
   }

   public CHEdgeIteratorState setWeight(double weight) {
      throw new UnsupportedOperationException("Not supported.");
   }

   public int getSkippedEdge1() {
      throw new UnsupportedOperationException("Not supported.");
   }

   public int getSkippedEdge2() {
      throw new UnsupportedOperationException("Not supported.");
   }

   public void setSkippedEdges(int edge1, int edge2) {
      throw new UnsupportedOperationException("Not supported.");
   }

   public boolean canBeOverwritten(long flags) {
      throw new UnsupportedOperationException("Not supported.");
   }
}
