package com.graphhopper.routing.util;

import com.graphhopper.util.PMap;

public class WeightingMap extends PMap {
   public WeightingMap() {
   }

   public WeightingMap(String weighting) {
      super(5);
      this.setWeighting(weighting);
   }

   public WeightingMap put(String key, Object str) {
      super.put(key, str);
      return this;
   }

   public WeightingMap setWeighting(String w) {
      if(w != null) {
         super.put("weighting", w);
      }

      return this;
   }

   public String getWeighting() {
      return super.get("weighting", "");
   }
}
