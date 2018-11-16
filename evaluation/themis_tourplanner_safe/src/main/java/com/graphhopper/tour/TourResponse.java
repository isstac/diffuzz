package com.graphhopper.tour;

import java.util.ArrayList;
import java.util.List;

public class TourResponse {
   private final List errors = new ArrayList(4);
   private List points = new ArrayList(0);

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

   public TourResponse addError(Throwable error) {
      this.errors.add(error);
      return this;
   }

   public TourResponse setPoints(List points) {
      this.points = points;
      return this;
   }

   public List getPoints() {
      this.check("getPoints");
      return this.points;
   }
}
