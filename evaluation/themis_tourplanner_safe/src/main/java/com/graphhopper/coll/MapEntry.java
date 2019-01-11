package com.graphhopper.coll;

import java.io.Serializable;
import java.util.Map.Entry;

public class MapEntry implements Entry, Serializable {
   private static final long serialVersionUID = 1L;
   private Object key;
   private Object value;

   public MapEntry(Object key, Object value) {
      this.key = key;
      this.value = value;
   }

   public Object getKey() {
      return this.key;
   }

   public Object getValue() {
      return this.value;
   }

   public Object setValue(Object value) {
      this.value = value;
      return value;
   }

   public String toString() {
      return this.getKey() + ", " + this.getValue();
   }

   public boolean equals(Object obj) {
      if(obj == null) {
         return false;
      } else if(this.getClass() != obj.getClass()) {
         return false;
      } else {
         MapEntry other = (MapEntry)obj;
         return this.key == other.key || this.key != null && this.key.equals(other.key)?this.value == other.value || this.value != null && this.value.equals(other.value):false;
      }
   }

   public int hashCode() {
      byte hash = 7;
      int hash1 = 19 * hash + (this.key != null?this.key.hashCode():0);
      hash1 = 19 * hash1 + (this.value != null?this.value.hashCode():0);
      return hash1;
   }
}
