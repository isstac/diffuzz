package com.graphhopper.util;

public class InstructionAnnotation {
   public static final InstructionAnnotation EMPTY = new InstructionAnnotation();
   private boolean empty;
   private int importance;
   private String message;

   private InstructionAnnotation() {
      this.setEmpty();
   }

   public InstructionAnnotation(int importance, String message) {
      if(message.isEmpty() && importance == 0) {
         this.setEmpty();
      } else {
         this.empty = false;
         this.importance = importance;
         this.message = message;
      }

   }

   private void setEmpty() {
      this.empty = true;
      this.importance = 0;
      this.message = "";
   }

   public boolean isEmpty() {
      return this.empty;
   }

   public int getImportance() {
      return this.importance;
   }

   public String getMessage() {
      return this.message;
   }

   public String toString() {
      return this.importance + ": " + this.getMessage();
   }

   public int hashCode() {
      byte hash = 3;
      int hash1 = 83 * hash + this.importance;
      hash1 = 83 * hash1 + (this.message != null?this.message.hashCode():0);
      return hash1;
   }

   public boolean equals(Object obj) {
      if(obj == null) {
         return false;
      } else if(this.getClass() != obj.getClass()) {
         return false;
      } else {
         InstructionAnnotation other = (InstructionAnnotation)obj;
         if(this.importance != other.importance) {
            return false;
         } else {
            if(this.message == null) {
               if(other.message != null) {
                  return false;
               }
            } else if(!this.message.equals(other.message)) {
               return false;
            }

            return true;
         }
      }
   }
}
