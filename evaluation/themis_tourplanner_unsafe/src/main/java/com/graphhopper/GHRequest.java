package com.graphhopper;

import com.graphhopper.routing.util.WeightingMap;
import com.graphhopper.util.Helper;
import com.graphhopper.util.shapes.GHPoint;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class GHRequest {
   private String algo;
   private final List points;
   private final WeightingMap hints;
   private String vehicle;
   private boolean possibleToAdd;
   private Locale locale;
   private final List favoredHeadings;

   public GHRequest() {
      this(5);
   }

   public GHRequest(int size) {
      this.algo = "";
      this.hints = new WeightingMap();
      this.vehicle = "";
      this.possibleToAdd = false;
      this.locale = Locale.US;
      this.points = new ArrayList(size);
      this.favoredHeadings = new ArrayList(size);
      this.possibleToAdd = true;
   }

   public GHRequest(double fromLat, double fromLon, double toLat, double toLon, double startHeading, double endHeading) {
      this(new GHPoint(fromLat, fromLon), new GHPoint(toLat, toLon), startHeading, endHeading);
   }

   public GHRequest(double fromLat, double fromLon, double toLat, double toLon) {
      this(new GHPoint(fromLat, fromLon), new GHPoint(toLat, toLon));
   }

   public GHRequest(GHPoint startPlace, GHPoint endPlace, double startHeading, double endHeading) {
      this.algo = "";
      this.hints = new WeightingMap();
      this.vehicle = "";
      this.possibleToAdd = false;
      this.locale = Locale.US;
      if(startPlace == null) {
         throw new IllegalStateException("\'from\' cannot be null");
      } else if(endPlace == null) {
         throw new IllegalStateException("\'to\' cannot be null");
      } else {
         this.points = new ArrayList(2);
         this.points.add(startPlace);
         this.points.add(endPlace);
         this.favoredHeadings = new ArrayList(2);
         this.validateAzimuthValue(startHeading);
         this.favoredHeadings.add(Double.valueOf(startHeading));
         this.validateAzimuthValue(endHeading);
         this.favoredHeadings.add(Double.valueOf(endHeading));
      }
   }

   public GHRequest(GHPoint startPlace, GHPoint endPlace) {
      this(startPlace, endPlace, Double.NaN, Double.NaN);
   }

   public GHRequest(List points, List favoredHeadings) {
      this.algo = "";
      this.hints = new WeightingMap();
      this.vehicle = "";
      this.possibleToAdd = false;
      this.locale = Locale.US;
      if(points.size() != favoredHeadings.size()) {
         throw new IllegalArgumentException("Size of headings (" + favoredHeadings.size() + ") must match size of points (" + points.size() + ")");
      } else {
         Iterator i$ = favoredHeadings.iterator();

         while(i$.hasNext()) {
            Double heading = (Double)i$.next();
            this.validateAzimuthValue(heading.doubleValue());
         }

         this.points = points;
         this.favoredHeadings = favoredHeadings;
      }
   }

   public GHRequest(List points) {
      this(points, Collections.nCopies(points.size(), Double.valueOf(Double.NaN)));
   }

   public GHRequest addPoint(GHPoint point, double favoredHeading) {
      if(point == null) {
         throw new IllegalArgumentException("point cannot be null");
      } else if(!this.possibleToAdd) {
         throw new IllegalStateException("Please call empty constructor if you intent to use more than two places via addPoint method.");
      } else {
         this.points.add(point);
         this.validateAzimuthValue(favoredHeading);
         this.favoredHeadings.add(Double.valueOf(favoredHeading));
         return this;
      }
   }

   public GHRequest addPoint(GHPoint point) {
      this.addPoint(point, Double.NaN);
      return this;
   }

   public double getFavoredHeading(int i) {
      return ((Double)this.favoredHeadings.get(i)).doubleValue();
   }

   public boolean hasFavoredHeading(int i) {
      return i >= this.favoredHeadings.size()?false:!Double.isNaN(((Double)this.favoredHeadings.get(i)).doubleValue());
   }

   private void validateAzimuthValue(double heading) {
      if(!Double.isNaN(heading) && (Double.compare(heading, 360.0D) > 0 || Double.compare(heading, 0.0D) < 0)) {
         throw new IllegalArgumentException("Heading " + heading + " must be in range (0,360) or NaN");
      }
   }

   public List getPoints() {
      return this.points;
   }

   public GHRequest setAlgorithm(String algo) {
      if(algo != null) {
         this.algo = algo;
      }

      return this;
   }

   public String getAlgorithm() {
      return this.algo;
   }

   public Locale getLocale() {
      return this.locale;
   }

   public GHRequest setLocale(Locale locale) {
      if(locale != null) {
         this.locale = locale;
      }

      return this;
   }

   public GHRequest setLocale(String localeStr) {
      return this.setLocale(Helper.getLocale(localeStr));
   }

   public GHRequest setWeighting(String w) {
      this.hints.setWeighting(w);
      return this;
   }

   public String getWeighting() {
      return this.hints.getWeighting();
   }

   public GHRequest setVehicle(String vehicle) {
      if(vehicle != null) {
         this.vehicle = vehicle;
      }

      return this;
   }

   public String getVehicle() {
      return this.vehicle;
   }

   public String toString() {
      String res = "";
      Iterator i$ = this.points.iterator();

      while(i$.hasNext()) {
         GHPoint point = (GHPoint)i$.next();
         if(res.isEmpty()) {
            res = point.toString();
         } else {
            res = res + "; " + point.toString();
         }
      }

      return res + "(" + this.algo + ")";
   }

   public WeightingMap getHints() {
      return this.hints;
   }
}
