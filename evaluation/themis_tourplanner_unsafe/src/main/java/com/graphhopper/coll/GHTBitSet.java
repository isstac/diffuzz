package com.graphhopper.coll;

import com.graphhopper.coll.GHBitSet;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.set.hash.TIntHashSet;

public class GHTBitSet implements GHBitSet {
   private final TIntHashSet tHash;

   public GHTBitSet(TIntHashSet set) {
      this.tHash = set;
   }

   public GHTBitSet(int no) {
      this.tHash = new TIntHashSet(no, 0.7F, -1);
   }

   public GHTBitSet() {
      this(1000);
   }

   public final boolean contains(int index) {
      return this.tHash.contains(index);
   }

   public final void add(int index) {
      this.tHash.add(index);
   }

   public final String toString() {
      return this.tHash.toString();
   }

   public final int getCardinality() {
      return this.tHash.size();
   }

   public final void clear() {
      this.tHash.clear();
   }

   public final GHBitSet copyTo(GHBitSet bs) {
      bs.clear();
      if(bs instanceof GHTBitSet) {
         ((GHTBitSet)bs).tHash.addAll(this.tHash);
      } else {
         TIntIterator iter = this.tHash.iterator();

         while(iter.hasNext()) {
            bs.add(iter.next());
         }
      }

      return bs;
   }

   public int next(int index) {
      throw new UnsupportedOperationException("Not supported yet.");
   }
}
