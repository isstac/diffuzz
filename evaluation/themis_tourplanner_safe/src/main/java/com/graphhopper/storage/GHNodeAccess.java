package com.graphhopper.storage;

import com.graphhopper.storage.BaseGraph;
import com.graphhopper.storage.NodeAccess;
import com.graphhopper.util.Helper;

class GHNodeAccess implements NodeAccess {
   private final BaseGraph that;
   private final boolean elevation;

   public GHNodeAccess(BaseGraph that, boolean withElevation) {
      this.that = that;
      this.elevation = withElevation;
   }

   public void ensureNode(int nodeId) {
      this.that.ensureNodeIndex(nodeId);
   }

   public final void setNode(int nodeId, double lat, double lon) {
      this.setNode(nodeId, lat, lon, Double.NaN);
   }

   public final void setNode(int nodeId, double lat, double lon, double ele) {
      this.that.ensureNodeIndex(nodeId);
      long tmp = (long)nodeId * (long)this.that.nodeEntryBytes;
      this.that.nodes.setInt(tmp + (long)this.that.N_LAT, Helper.degreeToInt(lat));
      this.that.nodes.setInt(tmp + (long)this.that.N_LON, Helper.degreeToInt(lon));
      if(this.is3D()) {
         this.that.nodes.setInt(tmp + (long)this.that.N_ELE, Helper.eleToInt(ele));
         this.that.bounds.update(lat, lon, ele);
      } else {
         this.that.bounds.update(lat, lon);
      }

      if(this.that.extStorage.isRequireNodeField()) {
         this.that.nodes.setInt(tmp + (long)this.that.N_ADDITIONAL, this.that.extStorage.getDefaultNodeFieldValue());
      }

   }

   public final double getLatitude(int nodeId) {
      return Helper.intToDegree(this.that.nodes.getInt((long)nodeId * (long)this.that.nodeEntryBytes + (long)this.that.N_LAT));
   }

   public final double getLongitude(int nodeId) {
      return Helper.intToDegree(this.that.nodes.getInt((long)nodeId * (long)this.that.nodeEntryBytes + (long)this.that.N_LON));
   }

   public final double getElevation(int nodeId) {
      if(!this.elevation) {
         throw new IllegalStateException("Cannot access elevation - 3D is not enabled");
      } else {
         return Helper.intToEle(this.that.nodes.getInt((long)nodeId * (long)this.that.nodeEntryBytes + (long)this.that.N_ELE));
      }
   }

   public final double getEle(int nodeId) {
      return this.getElevation(nodeId);
   }

   public final double getLat(int nodeId) {
      return this.getLatitude(nodeId);
   }

   public final double getLon(int nodeId) {
      return this.getLongitude(nodeId);
   }

   public final void setAdditionalNodeField(int index, int additionalValue) {
      if(this.that.extStorage.isRequireNodeField() && this.that.N_ADDITIONAL >= 0) {
         this.that.ensureNodeIndex(index);
         long tmp = (long)index * (long)this.that.nodeEntryBytes;
         this.that.nodes.setInt(tmp + (long)this.that.N_ADDITIONAL, additionalValue);
      } else {
         throw new AssertionError("This graph does not provide an additional node field");
      }
   }

   public final int getAdditionalNodeField(int index) {
      if(this.that.extStorage.isRequireNodeField() && this.that.N_ADDITIONAL >= 0) {
         return this.that.nodes.getInt((long)index * (long)this.that.nodeEntryBytes + (long)this.that.N_ADDITIONAL);
      } else {
         throw new AssertionError("This graph does not provide an additional node field");
      }
   }

   public final boolean is3D() {
      return this.elevation;
   }

   public int getDimension() {
      return this.elevation?3:2;
   }
}
