package com.graphhopper.routing;

import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.util.CHEdgeIteratorState;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.PointList;

public class VirtualEdgeIteratorState implements EdgeIteratorState, CHEdgeIteratorState {
   private final PointList pointList;
   private final int edgeId;
   private double distance;
   private long flags;
   private String name;
   private final int baseNode;
   private final int adjNode;
   private final int originalTraversalKey;
   private boolean unfavored;

   public VirtualEdgeIteratorState(int originalTraversalKey, int edgeId, int baseNode, int adjNode, double distance, long flags, String name, PointList pointList) {
      this.originalTraversalKey = originalTraversalKey;
      this.edgeId = edgeId;
      this.baseNode = baseNode;
      this.adjNode = adjNode;
      this.distance = distance;
      this.flags = flags;
      this.name = name;
      this.pointList = pointList;
   }

   public int getOriginalTraversalKey() {
      return this.originalTraversalKey;
   }

   public int getEdge() {
      return this.edgeId;
   }

   public int getBaseNode() {
      return this.baseNode;
   }

   public int getAdjNode() {
      return this.adjNode;
   }

   public PointList fetchWayGeometry(int mode) {
      if(this.pointList.getSize() == 0) {
         return PointList.EMPTY;
      } else if(mode == 3) {
         return this.pointList.clone(false);
      } else if(mode == 1) {
         return this.pointList.copy(0, this.pointList.getSize() - 1);
      } else if(mode == 2) {
         return this.pointList.copy(1, this.pointList.getSize());
      } else if(mode == 0) {
         return this.pointList.getSize() == 1?PointList.EMPTY:this.pointList.copy(1, this.pointList.getSize() - 1);
      } else {
         throw new UnsupportedOperationException("Illegal mode:" + mode);
      }
   }

   public EdgeIteratorState setWayGeometry(PointList list) {
      throw new UnsupportedOperationException("Not supported for virtual edge. Set when creating it.");
   }

   public double getDistance() {
      return this.distance;
   }

   public EdgeIteratorState setDistance(double dist) {
      this.distance = dist;
      return this;
   }

   public long getFlags() {
      return this.flags;
   }

   public EdgeIteratorState setFlags(long flags) {
      this.flags = flags;
      return this;
   }

   public String getName() {
      return this.name;
   }

   public EdgeIteratorState setName(String name) {
      this.name = name;
      return this;
   }

   public boolean getBoolean(int key, boolean reverse, boolean _default) {
      return key == -1?this.unfavored:_default;
   }

   public void setVirtualEdgePreference(boolean unfavored) {
      this.unfavored = unfavored;
   }

   public String toString() {
      return this.baseNode + "->" + this.adjNode;
   }

   public boolean isShortcut() {
      return false;
   }

   public boolean isForward(FlagEncoder encoder) {
      return encoder.isForward(this.getFlags());
   }

   public boolean isBackward(FlagEncoder encoder) {
      return encoder.isBackward(this.getFlags());
   }

   public int getAdditionalField() {
      throw new UnsupportedOperationException("Not supported.");
   }

   public boolean canBeOverwritten(long flags) {
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

   public EdgeIteratorState detach(boolean reverse) {
      throw new UnsupportedOperationException("Not supported.");
   }

   public EdgeIteratorState setAdditionalField(int value) {
      throw new UnsupportedOperationException("Not supported.");
   }

   public EdgeIteratorState copyPropertiesTo(EdgeIteratorState edge) {
      throw new UnsupportedOperationException("Not supported.");
   }

   public CHEdgeIteratorState setWeight(double weight) {
      throw new UnsupportedOperationException("Not supported.");
   }

   public double getWeight() {
      throw new UnsupportedOperationException("Not supported.");
   }
}
