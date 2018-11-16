package com.graphhopper.routing.util;

import com.graphhopper.routing.util.EncodedValue;

public class EncodedDoubleValue extends EncodedValue {
   public EncodedDoubleValue(String name, int shift, int bits, double factor, long defaultValue, int maxValue) {
      this(name, shift, bits, factor, defaultValue, maxValue, true);
   }

   public EncodedDoubleValue(String name, int shift, int bits, double factor, long defaultValue, int maxValue, boolean allowZero) {
      super(name, shift, bits, factor, defaultValue, maxValue, allowZero);
   }

   public long setValue(long flags, long value) {
      throw new IllegalStateException("Use setDoubleValue instead");
   }

   public long getValue(long flags) {
      throw new IllegalStateException("Use setDoubleValue instead");
   }

   public long setDefaultValue(long flags) {
      return this.setDoubleValue(flags, (double)this.defaultValue);
   }

   public long setDoubleValue(long flags, double value) {
      if(Double.isNaN(value)) {
         throw new IllegalStateException("Value cannot be NaN");
      } else {
         long tmpValue = Math.round(value / this.factor);
         this.checkValue(Math.round((double)tmpValue * this.factor));
         tmpValue <<= (int)this.shift;
         flags &= ~this.mask;
         return flags | tmpValue;
      }
   }

   public double getDoubleValue(long flags) {
      flags &= this.mask;
      flags >>>= (int)this.shift;
      return (double)flags * this.factor;
   }

   public long swap(long flags, EncodedDoubleValue otherEncoder) {
      double otherValue = otherEncoder.getDoubleValue(flags);
      flags = otherEncoder.setDoubleValue(flags, this.getDoubleValue(flags));
      return this.setDoubleValue(flags, otherValue);
   }
}
