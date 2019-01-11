package com.graphhopper.storage;

public interface IntIterator {
   boolean next();

   int getValue();

   void remove();

   public static class Helper {
      public static int count(IntIterator iter) {
         int counter;
         for(counter = 0; iter.next(); ++counter) {
            ;
         }

         return counter;
      }
   }
}
