package com.graphhopper.coll;

import com.graphhopper.coll.GHBitSet;
import java.util.BitSet;

public class GHBitSetImpl extends BitSet implements GHBitSet {
   public GHBitSetImpl() {
   }

   public GHBitSetImpl(int nbits) {
      super(nbits);
   }

   public final boolean contains(int index) {
      return super.get(index);
   }

   public final void add(int index) {
      super.set(index);
   }

   public final int getCardinality() {
      return super.cardinality();
   }

   public final int next(int index) {
      return super.nextSetBit(index);
   }

   public final int nextClear(int index) {
      return super.nextClearBit(index);
   }

   public final GHBitSet copyTo(GHBitSet bs) {
      bs.clear();
      if(bs instanceof GHBitSetImpl) {
         ((GHBitSetImpl)bs).or(this);
      } else {
         int len = this.size();

         for(int index = super.nextSetBit(0); index >= 0; index = super.nextSetBit(index + 1)) {
            bs.add(index);
         }
      }

      return bs;
   }
}
