package com.graphhopper.util.shapes;

import com.graphhopper.util.shapes.BBox;

public interface Shape {
   boolean intersect(Shape var1);

   boolean contains(double var1, double var3);

   boolean contains(Shape var1);

   BBox getBounds();
}
