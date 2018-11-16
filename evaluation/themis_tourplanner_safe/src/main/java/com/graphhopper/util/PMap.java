package com.graphhopper.util;

import com.graphhopper.util.Helper;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class PMap {
   private final Map map;

   public PMap() {
      this(5);
   }

   public PMap(int capacity) {
      this((Map)(new HashMap(capacity)));
   }

   public PMap(Map map) {
      this.map = map;
   }

   public PMap(String propertiesString) {
      this.map = new HashMap(5);
      String[] arr$ = propertiesString.split("\\|");
      int len$ = arr$.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         String s = arr$[i$];
         s = s.trim();
         int index = s.indexOf("=");
         if(index >= 0) {
            this.map.put(s.substring(0, index).toLowerCase(), s.substring(index + 1));
         }
      }

   }

   public PMap put(PMap map) {
      this.map.putAll(map.map);
      return this;
   }

   public PMap put(String key, Object str) {
      if(str == null) {
         throw new NullPointerException("Value cannot be null. Use remove instead.");
      } else {
         this.map.put(key.toLowerCase(), str.toString());
         return this;
      }
   }

   public PMap remove(String key) {
      this.map.remove(key);
      return this;
   }

   public long getLong(String key, long _default) {
      String str = this.get(key);
      if(!Helper.isEmpty(str)) {
         try {
            return Long.parseLong(str);
         } catch (Exception var6) {
            ;
         }
      }

      return _default;
   }

   public int getInt(String key, int _default) {
      String str = this.get(key);
      if(!Helper.isEmpty(str)) {
         try {
            return Integer.parseInt(str);
         } catch (Exception var5) {
            ;
         }
      }

      return _default;
   }

   public boolean getBool(String key, boolean _default) {
      String str = this.get(key);
      if(!Helper.isEmpty(str)) {
         try {
            return Boolean.parseBoolean(str);
         } catch (Exception var5) {
            ;
         }
      }

      return _default;
   }

   public double getDouble(String key, double _default) {
      String str = this.get(key);
      if(!Helper.isEmpty(str)) {
         try {
            return Double.parseDouble(str);
         } catch (Exception var6) {
            ;
         }
      }

      return _default;
   }

   public String get(String key, String _default) {
      String str = this.get(key);
      return Helper.isEmpty(str)?_default:str;
   }

   String get(String key) {
      if(Helper.isEmpty(key)) {
         return "";
      } else {
         String val = (String)this.map.get(key.toLowerCase());
         return val == null?"":val;
      }
   }

   public Map toMap() {
      return new HashMap(this.map);
   }

   private Map getMap() {
      return this.map;
   }

   public PMap merge(PMap read) {
      return this.merge(read.getMap());
   }

   PMap merge(Map map) {
      Iterator i$ = map.entrySet().iterator();

      while(i$.hasNext()) {
         Entry e = (Entry)i$.next();
         if(!Helper.isEmpty((String)e.getKey())) {
            this.getMap().put(((String)e.getKey()).toLowerCase(), e.getValue());
         }
      }

      return this;
   }

   public boolean has(String key) {
      return this.getMap().containsKey(key);
   }

   public String toString() {
      return this.getMap().toString();
   }
}
