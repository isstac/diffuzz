package com.graphhopper.reader;

import com.graphhopper.storage.DataAccess;
import com.graphhopper.storage.Directory;
import com.graphhopper.util.Helper;
import com.graphhopper.util.PointAccess;

public class PillarInfo implements PointAccess {
   private final int LAT = 0;
   private final int LON = 4;
   private final int ELE = 8;
   private final boolean enabled3D;
   private final DataAccess da;
   private final int rowSizeInBytes;
   private final Directory dir;

   public PillarInfo(boolean enabled3D, Directory dir) {
      this.enabled3D = enabled3D;
      this.dir = dir;
      this.da = dir.find("tmpPillarInfo").create(100L);
      this.rowSizeInBytes = this.getDimension() * 4;
   }

   public boolean is3D() {
      return this.enabled3D;
   }

   public int getDimension() {
      return this.enabled3D?3:2;
   }

   public void ensureNode(int nodeId) {
      long tmp = (long)nodeId * (long)this.rowSizeInBytes;
      this.da.ensureCapacity(tmp + (long)this.rowSizeInBytes);
   }

   public void setNode(int nodeId, double lat, double lon) {
      this._setNode(nodeId, lat, lon, Double.NaN);
   }

   public void setNode(int nodeId, double lat, double lon, double ele) {
      this._setNode(nodeId, lat, lon, ele);
   }

   private void _setNode(int nodeId, double lat, double lon, double ele) {
      this.ensureNode(nodeId);
      long tmp = (long)nodeId * (long)this.rowSizeInBytes;
      this.da.setInt(tmp + 0L, Helper.degreeToInt(lat));
      this.da.setInt(tmp + 4L, Helper.degreeToInt(lon));
      if(this.is3D()) {
         this.da.setInt(tmp + 8L, Helper.eleToInt(ele));
      }

   }

   public double getLatitude(int id) {
      int intVal = this.da.getInt((long)id * (long)this.rowSizeInBytes + 0L);
      return Helper.intToDegree(intVal);
   }

   public double getLat(int id) {
      return this.getLatitude(id);
   }

   public double getLongitude(int id) {
      int intVal = this.da.getInt((long)id * (long)this.rowSizeInBytes + 4L);
      return Helper.intToDegree(intVal);
   }

   public double getLon(int id) {
      return this.getLongitude(id);
   }

   public double getElevation(int id) {
      if(!this.is3D()) {
         return Double.NaN;
      } else {
         int intVal = this.da.getInt((long)id * (long)this.rowSizeInBytes + 8L);
         return Helper.intToEle(intVal);
      }
   }

   public double getEle(int id) {
      return this.getElevation(id);
   }

   public void clear() {
      this.dir.remove(this.da);
   }
}
