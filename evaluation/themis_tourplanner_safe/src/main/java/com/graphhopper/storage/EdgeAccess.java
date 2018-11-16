package com.graphhopper.storage;

import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.storage.BaseGraph;
import com.graphhopper.storage.DataAccess;
import com.graphhopper.util.BitUtil;
import com.graphhopper.util.EdgeIteratorState;

abstract class EdgeAccess {
   private static final double INT_DIST_FACTOR = 1000.0D;
   static final int NO_NODE = -1;
   int E_NODEA;
   int E_NODEB;
   int E_LINKA;
   int E_LINKB;
   int E_DIST;
   int E_FLAGS;
   final DataAccess edges;
   private final BitUtil bitUtil;
   private boolean flagsSizeIsLong;

   EdgeAccess(DataAccess edges, BitUtil bitUtil) {
      this.edges = edges;
      this.bitUtil = bitUtil;
   }

   final void init(int E_NODEA, int E_NODEB, int E_LINKA, int E_LINKB, int E_DIST, int E_FLAGS, boolean flagsSizeIsLong) {
      this.E_NODEA = E_NODEA;
      this.E_NODEB = E_NODEB;
      this.E_LINKA = E_LINKA;
      this.E_LINKB = E_LINKB;
      this.E_DIST = E_DIST;
      this.E_FLAGS = E_FLAGS;
      this.flagsSizeIsLong = flagsSizeIsLong;
   }

   abstract BaseGraph.EdgeIterable createSingleEdge(EdgeFilter var1);

   abstract long toPointer(int var1);

   abstract boolean isInBounds(int var1);

   abstract long reverseFlags(long var1, long var3);

   abstract int getEdgeRef(int var1);

   abstract void setEdgeRef(int var1, int var2);

   abstract int getEntryBytes();

   final void invalidateEdge(long edgePointer) {
      this.edges.setInt(edgePointer + (long)this.E_NODEA, -1);
   }

   final void setDist(long edgePointer, double distance) {
      this.edges.setInt(edgePointer + (long)this.E_DIST, this.distToInt(distance));
   }

   private int distToInt(double distance) {
      int integ = (int)(distance * 1000.0D);
      if(integ < 0) {
         throw new IllegalArgumentException("Distance cannot be empty: " + distance + ", maybe overflow issue? integer: " + integ);
      } else {
         return integ;
      }
   }

   final double getDist(long pointer) {
      int val = this.edges.getInt(pointer + (long)this.E_DIST);
      return val == Integer.MAX_VALUE?Double.POSITIVE_INFINITY:(double)val / 1000.0D;
   }

   final long getFlags_(long edgePointer, boolean reverse) {
      int low = this.edges.getInt(edgePointer + (long)this.E_FLAGS);
      long resFlags = (long)low;
      if(this.flagsSizeIsLong) {
         int high = this.edges.getInt(edgePointer + (long)this.E_FLAGS + 4L);
         resFlags = this.bitUtil.combineIntsToLong(low, high);
      }

      if(reverse) {
         resFlags = this.reverseFlags(edgePointer, resFlags);
      }

      return resFlags;
   }

   final long setFlags_(long edgePointer, boolean reverse, long flags) {
      if(reverse) {
         flags = this.reverseFlags(edgePointer, flags);
      }

      this.edges.setInt(edgePointer + (long)this.E_FLAGS, this.bitUtil.getIntLow(flags));
      if(this.flagsSizeIsLong) {
         this.edges.setInt(edgePointer + (long)this.E_FLAGS + 4L, this.bitUtil.getIntHigh(flags));
      }

      return flags;
   }

   final int internalEdgeAdd(int newEdgeId, int fromNodeId, int toNodeId) {
      this.writeEdge(newEdgeId, fromNodeId, toNodeId, -1, -1);
      this.connectNewEdge(fromNodeId, newEdgeId);
      if(fromNodeId != toNodeId) {
         this.connectNewEdge(toNodeId, newEdgeId);
      }

      return newEdgeId;
   }

   final int getOtherNode(int nodeThis, long edgePointer) {
      int nodeA = this.edges.getInt(edgePointer + (long)this.E_NODEA);
      return nodeA == nodeThis?this.edges.getInt(edgePointer + (long)this.E_NODEB):nodeA;
   }

   private long _getLinkPosInEdgeArea(int nodeThis, int nodeOther, long edgePointer) {
      return nodeThis <= nodeOther?edgePointer + (long)this.E_LINKA:edgePointer + (long)this.E_LINKB;
   }

   final int getEdgeRef(int nodeThis, int nodeOther, long edgePointer) {
      return this.edges.getInt(this._getLinkPosInEdgeArea(nodeThis, nodeOther, edgePointer));
   }

   final void connectNewEdge(int fromNode, int newOrExistingEdge) {
      int edge = this.getEdgeRef(fromNode);
      if(edge > -1) {
         long edgePointer = this.toPointer(newOrExistingEdge);
         int otherNode = this.getOtherNode(fromNode, edgePointer);
         long lastLink = this._getLinkPosInEdgeArea(fromNode, otherNode, edgePointer);
         this.edges.setInt(lastLink, edge);
      }

      this.setEdgeRef(fromNode, newOrExistingEdge);
   }

   final long writeEdge(int edgeId, int nodeThis, int nodeOther, int nextEdge, int nextEdgeOther) {
      if(nodeThis > nodeOther) {
         int edgePointer = nodeThis;
         nodeThis = nodeOther;
         nodeOther = edgePointer;
         edgePointer = nextEdge;
         nextEdge = nextEdgeOther;
         nextEdgeOther = edgePointer;
      }

      if(edgeId >= 0 && edgeId != -1) {
         long edgePointer1 = this.toPointer(edgeId);
         this.edges.setInt(edgePointer1 + (long)this.E_NODEA, nodeThis);
         this.edges.setInt(edgePointer1 + (long)this.E_NODEB, nodeOther);
         this.edges.setInt(edgePointer1 + (long)this.E_LINKA, nextEdge);
         this.edges.setInt(edgePointer1 + (long)this.E_LINKB, nextEdgeOther);
         return edgePointer1;
      } else {
         throw new IllegalStateException("Cannot write edge with illegal ID:" + edgeId + "; nodeThis:" + nodeThis + ", nodeOther:" + nodeOther);
      }
   }

   final long internalEdgeDisconnect(int edgeToRemove, long edgeToUpdatePointer, int baseNode, int adjNode) {
      long edgeToRemovePointer = this.toPointer(edgeToRemove);
      int nextEdgeId = this.getEdgeRef(baseNode, adjNode, edgeToRemovePointer);
      if(edgeToUpdatePointer < 0L) {
         this.setEdgeRef(baseNode, nextEdgeId);
      } else {
         long link = this.edges.getInt(edgeToUpdatePointer + (long)this.E_NODEA) == baseNode?edgeToUpdatePointer + (long)this.E_LINKA:edgeToUpdatePointer + (long)this.E_LINKB;
         this.edges.setInt(link, nextEdgeId);
      }

      return edgeToRemovePointer;
   }

   final EdgeIteratorState getEdgeProps(int edgeId, int adjNode) {
      if(edgeId <= -1) {
         throw new IllegalStateException("edgeId invalid " + edgeId + ", " + this);
      } else {
         BaseGraph.EdgeIterable edge = this.createSingleEdge(EdgeFilter.ALL_EDGES);
         return edge.init(edgeId, adjNode)?edge:null;
      }
   }
}
