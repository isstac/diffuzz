package com.graphhopper;

import com.graphhopper.util.InstructionList;
import com.graphhopper.util.PMap;
import com.graphhopper.util.PointList;
import com.graphhopper.util.shapes.BBox;
import java.util.ArrayList;
import java.util.List;

public class GHResponse {
   private String debugInfo = "";
   private final List errors = new ArrayList(4);
   private PointList list;
   private double distance;
   private double routeWeight;
   private long time;
   private InstructionList instructions;
   private final PMap hintsMap;

   public GHResponse() {
      this.list = PointList.EMPTY;
      this.hintsMap = new PMap();
   }

   public String getDebugInfo() {
      this.check("getDebugInfo");
      return this.debugInfo;
   }

   public GHResponse setDebugInfo(String debugInfo) {
      if(debugInfo != null) {
         this.debugInfo = debugInfo;
      }

      return this;
   }

   private void check(String method) {
      if(this.hasErrors()) {
         throw new RuntimeException("You cannot call " + method + " if response contains errors. Check this with ghResponse.hasErrors(). " + "Errors are: " + this.getErrors());
      }
   }

   public boolean hasErrors() {
      return !this.errors.isEmpty();
   }

   public List getErrors() {
      return this.errors;
   }

   public GHResponse addError(Throwable error) {
      this.errors.add(error);
      return this;
   }

   public GHResponse setPoints(PointList points) {
      this.list = points;
      return this;
   }

   public PointList getPoints() {
      this.check("getPoints");
      return this.list;
   }

   public GHResponse setDistance(double distance) {
      this.distance = distance;
      return this;
   }

   public double getDistance() {
      this.check("getDistance");
      return this.distance;
   }

   public GHResponse setTime(long timeInMillis) {
      this.time = timeInMillis;
      return this;
   }

   /** @deprecated */
   public long getMillis() {
      this.check("getMillis");
      return this.time;
   }

   public long getTime() {
      this.check("getTimes");
      return this.time;
   }

   public GHResponse setRouteWeight(double weight) {
      this.routeWeight = weight;
      return this;
   }

   public double getRouteWeight() {
      this.check("getRouteWeight");
      return this.routeWeight;
   }

   public BBox calcRouteBBox(BBox _fallback) {
      this.check("calcRouteBBox");
      BBox bounds = BBox.createInverse(_fallback.hasElevation());
      int len = this.list.getSize();
      if(len == 0) {
         return _fallback;
      } else {
         for(int i = 0; i < len; ++i) {
            double lat = this.list.getLatitude(i);
            double lon = this.list.getLongitude(i);
            if(bounds.hasElevation()) {
               double ele = this.list.getEle(i);
               bounds.update(lat, lon, ele);
            } else {
               bounds.update(lat, lon);
            }
         }

         return bounds;
      }
   }

   public String toString() {
      String str = "nodes:" + this.list.getSize() + "; " + this.list.toString();
      if(this.instructions != null && !this.instructions.isEmpty()) {
         str = str + ", " + this.instructions.toString();
      }

      if(this.hasErrors()) {
         str = str + ", " + this.errors.toString();
      }

      return str;
   }

   public void setInstructions(InstructionList instructions) {
      this.instructions = instructions;
   }

   public InstructionList getInstructions() {
      this.check("getInstructions");
      if(this.instructions == null) {
         throw new IllegalArgumentException("To access instructions you need to enable creation before routing");
      } else {
         return this.instructions;
      }
   }

   public PMap getHints() {
      return this.hintsMap;
   }
}
