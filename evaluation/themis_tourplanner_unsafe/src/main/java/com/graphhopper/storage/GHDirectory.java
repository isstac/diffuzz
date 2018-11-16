package com.graphhopper.storage;

import com.graphhopper.storage.DAType;
import com.graphhopper.storage.DataAccess;
import com.graphhopper.storage.Directory;
import com.graphhopper.storage.MMapDataAccess;
import com.graphhopper.storage.RAMDataAccess;
import com.graphhopper.storage.RAMIntDataAccess;
import com.graphhopper.storage.SynchedDAWrapper;
import com.graphhopper.util.Helper;
import java.io.File;
import java.nio.ByteOrder;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class GHDirectory implements Directory {
   protected Map map = new HashMap();
   protected Map types = new HashMap();
   protected final String location;
   private final DAType defaultType;
   private final ByteOrder byteOrder;

   public GHDirectory(String _location, DAType defaultType) {
      this.byteOrder = ByteOrder.LITTLE_ENDIAN;
      this.defaultType = defaultType;
      if(Helper.isEmpty(_location)) {
         _location = (new File("")).getAbsolutePath();
      }

      if(!_location.endsWith("/")) {
         _location = _location + "/";
      }

      this.location = _location;
      File dir = new File(this.location);
      if(dir.exists() && !dir.isDirectory()) {
         throw new RuntimeException("file \'" + dir + "\' exists but is not a directory");
      } else {
         if(this.defaultType.isInMemory()) {
            if(this.isStoring()) {
               this.put("locationIndex", DAType.RAM_INT_STORE);
               this.put("edges", DAType.RAM_INT_STORE);
               this.put("nodes", DAType.RAM_INT_STORE);
            } else {
               this.put("locationIndex", DAType.RAM_INT);
               this.put("edges", DAType.RAM_INT);
               this.put("nodes", DAType.RAM_INT);
            }
         }

         this.mkdirs();
      }
   }

   public ByteOrder getByteOrder() {
      return this.byteOrder;
   }

   public Directory put(String name, DAType type) {
      this.types.put(name, type);
      return this;
   }

   public DataAccess find(String name) {
      DAType type = (DAType)this.types.get(name);
      if(type == null) {
         type = this.defaultType;
      }

      return this.find(name, type);
   }

   public DataAccess find(String name, DAType type) {
      DataAccess da = (DataAccess)this.map.get(name);
      if(da != null) {
         if(!type.equals(da.getType())) {
            throw new IllegalStateException("Found existing DataAccess object \'" + name + "\' but types did not match. Requested:" + type + ", was:" + da.getType());
         } else {
            return da;
         }
      } else {
         Object da1;
         if(type.isInMemory()) {
            if(type.isInteg()) {
               if(type.isStoring()) {
                  da1 = new RAMIntDataAccess(name, this.location, true, this.byteOrder);
               } else {
                  da1 = new RAMIntDataAccess(name, this.location, false, this.byteOrder);
               }
            } else if(type.isStoring()) {
               da1 = new RAMDataAccess(name, this.location, true, this.byteOrder);
            } else {
               da1 = new RAMDataAccess(name, this.location, false, this.byteOrder);
            }
         } else {
            if(!type.isMMap()) {
               throw new IllegalArgumentException("Data access type UNSAFE_STORE not supported");
            }

            da1 = new MMapDataAccess(name, this.location, this.byteOrder, type.isAllowWrites());
         }

         if(type.isSynched()) {
        	 System.out.println("GHDirectory.find() isSynched");
            da1 = new SynchedDAWrapper((DataAccess)da1);
         }

         this.map.put(name, da1);
         return (DataAccess)da1;
      }
   }

   public void clear() {
      MMapDataAccess mmapDA = null;

      DataAccess da;
      for(Iterator i$ = this.map.values().iterator(); i$.hasNext(); this.removeDA(da, da.getName(), false)) {
         da = (DataAccess)i$.next();
         if(da instanceof MMapDataAccess) {
            mmapDA = (MMapDataAccess)da;
         }
      }

      if(mmapDA != null) {
         Helper.cleanHack();
      }

      this.map.clear();
   }

   public void remove(DataAccess da) {
      this.removeFromMap(da.getName());
      this.removeDA(da, da.getName(), true);
   }

   void removeDA(DataAccess da, String name, boolean forceClean) {
      if(da instanceof MMapDataAccess) {
         ((MMapDataAccess)da).close(forceClean);
      } else {
         da.close();
      }

      if(da.getType().isStoring()) {
         Helper.removeDir(new File(this.location + name));
      }

   }

   void removeFromMap(String name) {
      DataAccess da = (DataAccess)this.map.remove(name);
      if(da == null) {
         throw new IllegalStateException("Couldn\'t remove dataAccess object:" + name);
      }
   }

   public DAType getDefaultType() {
      return this.defaultType;
   }

   public boolean isStoring() {
      return this.defaultType.isStoring();
   }

   protected void mkdirs() {
      if(this.isStoring()) {
         (new File(this.location)).mkdirs();
      }

   }

   public Collection getAll() {
      return this.map.values();
   }

   public String toString() {
      return this.getLocation();
   }

   public String getLocation() {
      return this.location;
   }
}
