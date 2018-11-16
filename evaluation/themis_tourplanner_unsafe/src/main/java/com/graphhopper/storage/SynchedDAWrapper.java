package com.graphhopper.storage;

import com.graphhopper.storage.DAType;
import com.graphhopper.storage.DataAccess;

class SynchedDAWrapper implements DataAccess {
   private final DataAccess inner;
   private final DAType type;

   public SynchedDAWrapper(DataAccess inner) {
      this.inner = inner;
      this.type = new DAType(inner.getType(), true);
   }

   public synchronized String getName() {
      return this.inner.getName();
   }

   public synchronized void rename(String newName) {
      this.inner.rename(newName);
   }

   public synchronized void setInt(long bytePos, int value) {
      this.inner.setInt(bytePos, value);
   }

   public synchronized int getInt(long bytePos) {
      return this.inner.getInt(bytePos);
   }

   public synchronized void setShort(long bytePos, short value) {
      this.inner.setShort(bytePos, value);
   }

   public synchronized short getShort(long bytePos) {
      return this.inner.getShort(bytePos);
   }

   public synchronized void setBytes(long bytePos, byte[] values, int length) {
      this.inner.setBytes(bytePos, values, length);
   }

   public synchronized void getBytes(long bytePos, byte[] values, int length) {
      this.inner.getBytes(bytePos, values, length);
   }

   public synchronized void setHeader(int bytePos, int value) {
      this.inner.setHeader(bytePos, value);
   }

   public synchronized int getHeader(int bytePos) {
      return this.inner.getHeader(bytePos);
   }

   public synchronized DataAccess create(long bytes) {
      return this.inner.create(bytes);
   }

   public synchronized boolean ensureCapacity(long bytes) {
      return this.inner.ensureCapacity(bytes);
   }

   public synchronized void trimTo(long bytes) {
      this.inner.trimTo(bytes);
   }

   public synchronized DataAccess copyTo(DataAccess da) {
      return this.inner.copyTo(da);
   }

   public synchronized DataAccess setSegmentSize(int bytes) {
      return this.inner.setSegmentSize(bytes);
   }

   public synchronized int getSegmentSize() {
      return this.inner.getSegmentSize();
   }

   public synchronized int getSegments() {
      return this.inner.getSegments();
   }

   public synchronized boolean loadExisting() {
      return this.inner.loadExisting();
   }

   public synchronized void flush() {
      this.inner.flush();
   }

   public synchronized void close() {
      this.inner.close();
   }

   public boolean isClosed() {
      return this.inner.isClosed();
   }

   public synchronized long getCapacity() {
      return this.inner.getCapacity();
   }

   public DAType getType() {
      return this.type;
   }
}
