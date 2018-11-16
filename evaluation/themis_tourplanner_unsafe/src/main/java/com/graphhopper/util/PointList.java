package com.graphhopper.util;

import com.graphhopper.util.DistanceCalc;
import com.graphhopper.util.DistanceCalc3D;
import com.graphhopper.util.Helper;
import com.graphhopper.util.NumHelper;
import com.graphhopper.util.PointAccess;
import com.graphhopper.util.shapes.GHPoint;
import com.graphhopper.util.shapes.GHPoint3D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class PointList implements Iterable, PointAccess {
   private static final DistanceCalc3D distCalc3D;
   private static String ERR_MSG;
   private double[] latitudes;
   private double[] longitudes;
   private double[] elevations;
   protected int size;
   protected boolean is3D;
   public static PointList EMPTY;

   public PointList() {
      this(10, false);
   }

   public PointList(int cap, boolean is3D) {
      this.size = 0;
      this.latitudes = new double[cap];
      this.longitudes = new double[cap];
      this.is3D = is3D;
      if(is3D) {
         this.elevations = new double[cap];
      }

   }

   public boolean is3D() {
      return this.is3D;
   }

   public int getDimension() {
      return this.is3D?3:2;
   }

   public void ensureNode(int nodeId) {
      this.incCap(nodeId + 1);
   }

   public void setNode(int nodeId, double lat, double lon) {
      this.set(nodeId, lat, lon, Double.NaN);
   }

   public void setNode(int nodeId, double lat, double lon, double ele) {
      this.set(nodeId, lat, lon, ele);
   }

   public void set(int index, double lat, double lon, double ele) {
      if(index >= this.size) {
         throw new ArrayIndexOutOfBoundsException("index has to be smaller than size " + this.size);
      } else {
         this.latitudes[index] = lat;
         this.longitudes[index] = lon;
         if(this.is3D) {
            this.elevations[index] = ele;
         } else if(!Double.isNaN(ele)) {
            throw new IllegalStateException("This is a 2D list we cannot store elevation: " + ele);
         }

      }
   }

   private void incCap(int newSize) {
      if(newSize > this.latitudes.length) {
         int cap = newSize * 2;
         if(cap < 15) {
            cap = 15;
         }

         this.latitudes = Arrays.copyOf(this.latitudes, cap);
         this.longitudes = Arrays.copyOf(this.longitudes, cap);
         if(this.is3D) {
            this.elevations = Arrays.copyOf(this.elevations, cap);
         }

      }
   }

   public void add(double lat, double lon) {
      if(this.is3D) {
         throw new IllegalStateException("Cannot add point without elevation data in 3D mode");
      } else {
         this.add(lat, lon, Double.NaN);
      }
   }

   public void add(double lat, double lon, double ele) {
      int newSize = this.size + 1;
      this.incCap(newSize);
      this.latitudes[this.size] = lat;
      this.longitudes[this.size] = lon;
      if(this.is3D) {
         this.elevations[this.size] = ele;
      } else if(!Double.isNaN(ele)) {
         throw new IllegalStateException("This is a 2D list we cannot store elevation: " + ele);
      }

      this.size = newSize;
   }

   public void add(PointAccess nodeAccess, int index) {
      if(this.is3D) {
         this.add(nodeAccess.getLatitude(index), nodeAccess.getLongitude(index), nodeAccess.getElevation(index));
      } else {
         this.add(nodeAccess.getLatitude(index), nodeAccess.getLongitude(index));
      }

   }

   public void add(GHPoint point) {
      if(this.is3D) {
         this.add(point.lat, point.lon, ((GHPoint3D)point).ele);
      } else {
         this.add(point.lat, point.lon);
      }

   }

   public void add(PointList points) {
      int newSize = this.size + points.getSize();
      this.incCap(newSize);

      for(int i = 0; i < points.getSize(); ++i) {
         int tmp = this.size + i;
         this.latitudes[tmp] = points.getLatitude(i);
         this.longitudes[tmp] = points.getLongitude(i);
         if(this.is3D) {
            this.elevations[tmp] = points.getElevation(i);
         }
      }

      this.size = newSize;
   }

   public int size() {
      return this.size;
   }

   public int getSize() {
      return this.size;
   }

   public boolean isEmpty() {
      return this.size == 0;
   }

   public double getLat(int index) {
      return this.getLatitude(index);
   }

   public double getLatitude(int index) {
      if(index >= this.size) {
         throw new ArrayIndexOutOfBoundsException(ERR_MSG + " index:" + index + ", size:" + this.size);
      } else {
         return this.latitudes[index];
      }
   }

   public double getLon(int index) {
      return this.getLongitude(index);
   }

   public double getLongitude(int index) {
      if(index >= this.size) {
         throw new ArrayIndexOutOfBoundsException(ERR_MSG + " index:" + index + ", size:" + this.size);
      } else {
         return this.longitudes[index];
      }
   }

   public double getElevation(int index) {
      if(index >= this.size) {
         throw new ArrayIndexOutOfBoundsException(ERR_MSG + " index:" + index + ", size:" + this.size);
      } else {
         return !this.is3D?Double.NaN:this.elevations[index];
      }
   }

   public double getEle(int index) {
      return this.getElevation(index);
   }

   public void reverse() {
      int max = this.size / 2;

      for(int i = 0; i < max; ++i) {
         int swapIndex = this.size - i - 1;
         double tmp = this.latitudes[i];
         this.latitudes[i] = this.latitudes[swapIndex];
         this.latitudes[swapIndex] = tmp;
         tmp = this.longitudes[i];
         this.longitudes[i] = this.longitudes[swapIndex];
         this.longitudes[swapIndex] = tmp;
         if(this.is3D) {
            tmp = this.elevations[i];
            this.elevations[i] = this.elevations[swapIndex];
            this.elevations[swapIndex] = tmp;
         }
      }

   }

   public void clear() {
      this.size = 0;
   }

   public void trimToSize(int newSize) {
      if(newSize > this.size) {
         throw new IllegalArgumentException("new size needs be smaller than old size");
      } else {
         this.size = newSize;
      }
   }

   public String toString() {
      StringBuilder sb = new StringBuilder();

      for(int i = 0; i < this.size; ++i) {
         if(i > 0) {
            sb.append(", ");
         }

         sb.append('(');
         sb.append(this.latitudes[i]);
         sb.append(',');
         sb.append(this.longitudes[i]);
         if(this.is3D) {
            sb.append(',');
            sb.append(this.elevations[i]);
         }

         sb.append(')');
      }

      return sb.toString();
   }

   public List toGeoJson() {
      return this.toGeoJson(this.is3D);
   }

   public List toGeoJson(boolean includeElevation) {
      ArrayList points = new ArrayList(this.size);

      for(int i = 0; i < this.size; ++i) {
         if(includeElevation) {
            points.add(new Double[]{Double.valueOf(Helper.round6(this.getLongitude(i))), Double.valueOf(Helper.round6(this.getLatitude(i))), Double.valueOf(Helper.round2(this.getElevation(i)))});
         } else {
            points.add(new Double[]{Double.valueOf(Helper.round6(this.getLongitude(i))), Double.valueOf(Helper.round6(this.getLatitude(i)))});
         }
      }

      return points;
   }

   public boolean equals(Object obj) {
      if(obj == null) {
         return false;
      } else {
         PointList other = (PointList)obj;
         if(other.isEmpty() && other.isEmpty()) {
            return true;
         } else if(this.getSize() == other.getSize() && this.is3D() == other.is3D()) {
            for(int i = 0; i < this.size; ++i) {
               if(!NumHelper.equalsEps(this.latitudes[i], other.latitudes[i])) {
                  return false;
               }

               if(!NumHelper.equalsEps(this.longitudes[i], other.longitudes[i])) {
                  return false;
               }

               if(this.is3D && !NumHelper.equalsEps(this.elevations[i], other.elevations[i])) {
                  return false;
               }
            }

            return true;
         } else {
            return false;
         }
      }
   }

   public PointList clone(boolean reverse) {
      PointList clonePL = new PointList(this.size, this.is3D);
      int i;
      if(this.is3D) {
         for(i = 0; i < this.size; ++i) {
            clonePL.add(this.latitudes[i], this.longitudes[i], this.elevations[i]);
         }
      } else {
         for(i = 0; i < this.size; ++i) {
            clonePL.add(this.latitudes[i], this.longitudes[i]);
         }
      }

      if(reverse) {
         clonePL.reverse();
      }

      return clonePL;
   }

   public PointList copy(int from, int end) {
      if(from > end) {
         throw new IllegalArgumentException("from must be smaller or equals to end");
      } else if(from >= 0 && end <= this.size) {
         PointList copyPL = new PointList(this.size, this.is3D);
         int i;
         if(this.is3D) {
            for(i = from; i < end; ++i) {
               copyPL.add(this.latitudes[i], this.longitudes[i], this.elevations[i]);
            }
         } else {
            for(i = from; i < end; ++i) {
               copyPL.add(this.latitudes[i], this.longitudes[i], Double.NaN);
            }
         }

         return copyPL;
      } else {
         throw new IllegalArgumentException("Illegal interval: " + from + ", " + end + ", size:" + this.size);
      }
   }

   public int hashCode() {
      int hash = 5;

      for(int i = 0; i < this.latitudes.length; ++i) {
         hash = 73 * hash + (int)Math.round(this.latitudes[i] * 1000000.0D);
         hash = 73 * hash + (int)Math.round(this.longitudes[i] * 1000000.0D);
      }

      hash = 73 * hash + this.size;
      return hash;
   }

   public double calcDistance(DistanceCalc calc) {
      double prevLat = Double.NaN;
      double prevLon = Double.NaN;
      double prevEle = Double.NaN;
      double dist = 0.0D;

      for(int i = 0; i < this.size; ++i) {
         if(i > 0) {
            if(this.is3D) {
               dist += distCalc3D.calcDist(prevLat, prevLon, prevEle, this.latitudes[i], this.longitudes[i], this.elevations[i]);
            } else {
               dist += calc.calcDist(prevLat, prevLon, this.latitudes[i], this.longitudes[i]);
            }
         }

         prevLat = this.latitudes[i];
         prevLon = this.longitudes[i];
         if(this.is3D) {
            prevEle = this.elevations[i];
         }
      }

      return dist;
   }

   public void parse2DJSON(String str) {
      String[] arr$ = str.split("\\[");
      int len$ = arr$.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         String latlon = arr$[i$];
         if(latlon.trim().length() != 0) {
            String[] ll = latlon.split(",");
            String lat = ll[1].replace("]", "").trim();
            this.add(Double.parseDouble(lat), Double.parseDouble(ll[0].trim()), Double.NaN);
         }
      }

   }

   public GHPoint3D toGHPoint(int index) {
      return new GHPoint3D(this.getLatitude(index), this.getLongitude(index), this.getElevation(index));
   }

   int getCapacity() {
      return this.latitudes.length;
   }

   public Iterator iterator() {
      return new Iterator() {
         int counter = 0;

         public boolean hasNext() {
            return this.counter < PointList.this.getSize();
         }

         public GHPoint3D next() {
            GHPoint3D point = PointList.this.toGHPoint(this.counter);
            ++this.counter;
            return point;
         }

         public void remove() {
            throw new UnsupportedOperationException("Not supported.");
         }
      };
   }

   static {
      distCalc3D = Helper.DIST_3D;
      ERR_MSG = "Tried to access PointList with too big index!";
      EMPTY = new PointList(0, true) {
         public void set(int index, double lat, double lon, double ele) {
            throw new RuntimeException("cannot change EMPTY PointList");
         }

         public void add(double lat, double lon, double ele) {
            throw new RuntimeException("cannot change EMPTY PointList");
         }

         public double getLatitude(int index) {
            throw new RuntimeException("cannot access EMPTY PointList");
         }

         public double getLongitude(int index) {
            throw new RuntimeException("cannot access EMPTY PointList");
         }

         public boolean isEmpty() {
            return true;
         }

         public void clear() {
            throw new RuntimeException("cannot change EMPTY PointList");
         }

         public void trimToSize(int newSize) {
            throw new RuntimeException("cannot change EMPTY PointList");
         }

         public void parse2DJSON(String str) {
            throw new RuntimeException("cannot change EMPTY PointList");
         }

         public double calcDistance(DistanceCalc calc) {
            throw new UnsupportedOperationException("cannot access EMPTY PointList");
         }

         public PointList copy(int from, int end) {
            throw new RuntimeException("cannot copy EMPTY PointList");
         }

         public PointList clone(boolean reverse) {
            throw new UnsupportedOperationException("cannot access EMPTY PointList");
         }

         public double getElevation(int index) {
            throw new UnsupportedOperationException("cannot access EMPTY PointList");
         }

         public double getLat(int index) {
            throw new UnsupportedOperationException("cannot access EMPTY PointList");
         }

         public double getLon(int index) {
            throw new UnsupportedOperationException("cannot access EMPTY PointList");
         }

         public double getEle(int index) {
            throw new UnsupportedOperationException("cannot access EMPTY PointList");
         }

         public List toGeoJson() {
            throw new UnsupportedOperationException("cannot access EMPTY PointList");
         }

         public void reverse() {
            throw new UnsupportedOperationException("cannot change EMPTY PointList");
         }

         public int getSize() {
            return 0;
         }

         public int size() {
            return 0;
         }

         public GHPoint3D toGHPoint(int index) {
            throw new UnsupportedOperationException("cannot access EMPTY PointList");
         }

         public boolean is3D() {
            throw new UnsupportedOperationException("cannot access EMPTY PointList");
         }
      };
   }
}
