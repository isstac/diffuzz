package com.graphhopper.util.shapes;

import com.graphhopper.util.Helper;
import com.graphhopper.util.shapes.GHPoint;

public class GHPlace extends GHPoint {
   private String name = "";

   public GHPlace() {
   }

   public GHPlace(String name) {
      this.setName(name);
   }

   public GHPlace(double lat, double lon) {
      super(lat, lon);
   }

   public void setValue(String t) {
      this.setName(t);
   }

   public GHPlace setName(String name) {
      this.name = name;
      return this;
   }

   public String getName() {
      return this.name;
   }

   public boolean isValidName() {
      return !Helper.isEmpty(this.name);
   }

   public String toString() {
      String str = "";
      if(this.isValidName()) {
         str = str + this.name + " ";
      }

      if(this.isValid()) {
         str = str + super.toString();
      }

      return str.trim();
   }
}
