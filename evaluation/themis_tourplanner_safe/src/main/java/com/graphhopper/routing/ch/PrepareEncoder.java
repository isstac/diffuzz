package com.graphhopper.routing.ch;

public class PrepareEncoder {
   private static final long scFwdDir = 1L;
   private static final long scBwdDir = 2L;
   private static final long scDirMask = 3L;

   public static final long getScDirMask() {
      return 3L;
   }

   public static final long getScFwdDir() {
      return 1L;
   }

   public static final long getScBwdDir() {
      return 2L;
   }

   public static final boolean canBeOverwritten(long flags1, long flags2) {
      return (flags2 & 3L) == 3L || (flags1 & 3L) == (flags2 & 3L);
   }
}
