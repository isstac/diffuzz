package com.graphhopper.storage;

import com.graphhopper.storage.DataAccess;
import com.graphhopper.storage.Directory;
import com.graphhopper.storage.Storable;
import com.graphhopper.util.Helper;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.Map;

public class StorableProperties implements Storable {
   private final Map map = new LinkedHashMap();
   private final DataAccess da;

   public StorableProperties(Directory dir) {
      this.da = dir.find("properties");
      this.da.setSegmentSize(1 << 15);
   }

   public boolean loadExisting() {

      if(!this.da.loadExisting()) {
         return false;
      } else {
         int len = (int)this.da.getCapacity();
         byte[] bytes = new byte[len];
         this.da.getBytes(0L, bytes, len);

         try {
            Helper.loadProperties(this.map, new StringReader(new String(bytes, Helper.UTF_CS)));
            return true;
         } catch (IOException var4) {
            throw new IllegalStateException(var4);
         }
      }
   }

   public void flush() {
      try {
         StringWriter ex = new StringWriter();
         Helper.saveProperties(this.map, ex);
         byte[] bytes = ex.toString().getBytes(Helper.UTF_CS);
         this.da.setBytes(0L, bytes, bytes.length);
         this.da.flush();
      } catch (IOException var3) {
         throw new RuntimeException(var3);
      }
   }

   public StorableProperties put(String key, String val) {
      this.map.put(key, val);
      return this;
   }

   public StorableProperties put(String key, Object val) {
      this.map.put(key, val.toString());
      return this;
   }

   public String get(String key) {
      String ret = (String)this.map.get(key);
      return ret == null?"":ret;
   }

   public void close() {
      this.da.close();
   }

   public boolean isClosed() {
      return this.da.isClosed();
   }

   public StorableProperties create(long size) {
      this.da.create(size);
      return this;
   }

   public long getCapacity() {
      return this.da.getCapacity();
   }

   public void putCurrentVersions() {
      this.put("nodes.version", (Object)Integer.valueOf(4));
      this.put("edges.version", (Object)Integer.valueOf(12));
      this.put("geometry.version", (Object)Integer.valueOf(3));
      this.put("locationIndex.version", (Object)Integer.valueOf(2));
      this.put("nameIndex.version", (Object)Integer.valueOf(2));
      this.put("shortcuts.version", (Object)Integer.valueOf(1));
   }

   public String versionsToString() {
      return this.get("nodes.version") + "," + this.get("edges.version") + "," + this.get("geometry.version") + "," + this.get("locationIndex.version") + "," + this.get("nameIndex.version");
   }

   public boolean checkVersions(boolean silent) {
      return !this.check("nodes", 4, silent)?false:(!this.check("edges", 12, silent)?false:(!this.check("geometry", 3, silent)?false:(!this.check("locationIndex", 2, silent)?false:(!this.check("nameIndex", 2, silent)?false:this.check("shortcuts", 1, silent)))));
   }

   boolean check(String key, int vers, boolean silent) {
      String str = this.get(key + ".version");
      if(!str.equals(vers + "")) {
         if(silent) {
            return false;
         } else {
            throw new IllegalStateException("Version of " + key + " unsupported: " + str + ", expected:" + vers);
         }
      } else {
         return true;
      }
   }

   public void copyTo(StorableProperties properties) {
      properties.map.clear();
      properties.map.putAll(this.map);
      this.da.copyTo(properties.da);
   }

   public String toString() {
      return this.da.toString();
   }
}
