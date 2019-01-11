package com.graphhopper.storage;

public class DAType {
   public static final DAType RAM;
   public static final DAType RAM_INT;
   public static final DAType RAM_STORE;
   public static final DAType RAM_INT_STORE;
   public static final DAType MMAP;
   public static final DAType MMAP_RO;
   public static final DAType UNSAFE_STORE;
   private final DAType.MemRef memRef;
   private final boolean storing;
   private final boolean integ;
   private final boolean synched;
   private final boolean allowWrites;

   public DAType(DAType type, boolean synched) {
      this(type.getMemRef(), type.isStoring(), type.isInteg(), type.isAllowWrites(), synched);
      if(!synched) {
         throw new IllegalStateException("constructor can only be used with synched=true");
      } else if(type.isSynched()) {
         throw new IllegalStateException("something went wrong as DataAccess object is already synched!?");
      }
   }

   public DAType(DAType.MemRef memRef, boolean storing, boolean integ, boolean allowWrites, boolean synched) {
      this.memRef = memRef;
      this.storing = storing;
      this.integ = integ;
      this.allowWrites = allowWrites;
      this.synched = synched;
   }

   DAType.MemRef getMemRef() {
      return this.memRef;
   }

   public boolean isAllowWrites() {
      return this.allowWrites;
   }

   public boolean isInMemory() {
      return this.memRef == DAType.MemRef.HEAP;
   }

   public boolean isMMap() {
      return this.memRef == DAType.MemRef.MMAP;
   }

   public boolean isStoring() {
      return this.storing;
   }

   public boolean isInteg() {
      return this.integ;
   }

   public boolean isSynched() {
      return this.synched;
   }

   public String toString() {
      String str;
      if(this.getMemRef() == DAType.MemRef.MMAP) {
         str = "MMAP";
      } else if(this.getMemRef() == DAType.MemRef.HEAP) {
         str = "RAM";
      } else {
         str = "UNSAFE";
      }

      if(this.isInteg()) {
         str = str + "_INT";
      }

      if(this.isStoring()) {
         str = str + "_STORE";
      }

      if(this.isSynched()) {
         str = str + "_SYNC";
      }

      return str;
   }

   public static DAType fromString(String dataAccess) {
      dataAccess = dataAccess.toUpperCase();
      DAType type;
      if(dataAccess.contains("MMAP")) {
         type = MMAP;
      } else if(dataAccess.contains("UNSAFE")) {
         type = UNSAFE_STORE;
      } else if(dataAccess.contains("RAM_STORE")) {
         type = RAM_STORE;
      } else {
         type = RAM;
      }

      if(dataAccess.contains("SYNC")) {
         type = new DAType(type, true);
      }

      return type;
   }

   public int hashCode() {
      byte hash = 7;
      int hash1 = 59 * hash + 37 * this.memRef.hashCode();
      hash1 = 59 * hash1 + (this.storing?1:0);
      hash1 = 59 * hash1 + (this.integ?1:0);
      hash1 = 59 * hash1 + (this.synched?1:0);
      return hash1;
   }

   public boolean equals(Object obj) {
      if(obj == null) {
         return false;
      } else if(this.getClass() != obj.getClass()) {
         return false;
      } else {
         DAType other = (DAType)obj;
         return this.memRef != other.memRef?false:(this.storing != other.storing?false:(this.integ != other.integ?false:this.synched == other.synched));
      }
   }

   static {
      RAM = new DAType(DAType.MemRef.HEAP, false, false, true, false);
      RAM_INT = new DAType(DAType.MemRef.HEAP, false, true, true, false);
      RAM_STORE = new DAType(DAType.MemRef.HEAP, true, false, true, false);
      RAM_INT_STORE = new DAType(DAType.MemRef.HEAP, true, true, true, false);
      MMAP = new DAType(DAType.MemRef.MMAP, true, false, true, false);
      MMAP_RO = new DAType(DAType.MemRef.MMAP, true, false, false, false);
      UNSAFE_STORE = new DAType(DAType.MemRef.UNSAFE, true, false, true, false);
   }

   public static enum MemRef {
      HEAP,
      MMAP,
      UNSAFE;
   }
}
