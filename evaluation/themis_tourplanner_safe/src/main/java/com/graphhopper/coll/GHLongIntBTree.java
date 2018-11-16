package com.graphhopper.coll;

import com.graphhopper.coll.LongIntMap;
import java.util.Arrays;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

public class GHLongIntBTree implements LongIntMap {
//   private Logger logger = LoggerFactory.getLogger(this.getClass());
   private final int noNumberValue = -1;
   private long size;
   private int maxLeafEntries;
   private int initLeafSize;
   private int splitIndex;
   private float factor;
   private int height;
   private GHLongIntBTree.BTreeEntry root;

   public GHLongIntBTree(int maxLeafEntries) {
      this.maxLeafEntries = maxLeafEntries;
      if(maxLeafEntries < 1) {
         throw new IllegalArgumentException("illegal maxLeafEntries:" + maxLeafEntries);
      } else {
         if(maxLeafEntries % 2 == 0) {
            ++maxLeafEntries;
         }

         this.splitIndex = maxLeafEntries / 2;
         if(maxLeafEntries < 10) {
            this.factor = 2.0F;
            this.initLeafSize = 1;
         } else if(maxLeafEntries < 20) {
            this.factor = 2.0F;
            this.initLeafSize = 4;
         } else {
            this.factor = 1.7F;
            this.initLeafSize = maxLeafEntries / 10;
         }

         this.clear();
      }
   }

   public int put(long key, int value) {
      if(key == -1L) {
         throw new IllegalArgumentException("Illegal key " + key);
      } else {
         GHLongIntBTree.ReturnValue rv = this.root.put(key, value);
         if(rv.tree != null) {
            ++this.height;
            this.root = rv.tree;
         }

         if(rv.oldValue == -1) {
            ++this.size;
            if(this.size % 1000000L == 0L) {
               this.optimize();
            }
         }

         return rv.oldValue;
      }
   }

   public int get(long key) {
      return this.root.get(key);
   }

   int height() {
      return this.height;
   }

   public long getSize() {
      return this.size;
   }

   public int getMemoryUsage() {
      return Math.round((float)(this.root.getCapacity() / 1048576L));
   }

   void clear() {
      this.size = 0L;
      this.height = 1;
      this.root = new GHLongIntBTree.BTreeEntry(this.initLeafSize, true);
   }

   int getNoNumberValue() {
      return -1;
   }

   void flush() {
      throw new IllegalStateException("not supported yet");
   }

   private int getEntries() {
      return this.root.getEntries();
   }

   public void optimize() {
      if(this.getSize() > 10000L) {
         this.root.compact();
      }

   }

   public String toString() {
      return "Height:" + this.height() + ", entries:" + this.getEntries();
   }

   void print() {
//      this.logger.info(this.root.toString(1));
   }

   static int binarySearch(long[] keys, int start, int len, long key) {
      int high = start + len;
      int low = start - 1;

      long highKey;
      while(high - low > 1) {
         int guess = high + low >>> 1;
         highKey = keys[guess];
         if(highKey < key) {
            low = guess;
         } else {
            high = guess;
         }
      }

      if(high == start + len) {
         return ~(start + len);
      } else {
         highKey = keys[high];
         if(highKey == key) {
            return high;
         } else {
            return ~high;
         }
      }
   }

   class BTreeEntry {
      int entrySize;
      long[] keys;
      int[] values;
      GHLongIntBTree.BTreeEntry[] children;
      boolean isLeaf;

      public BTreeEntry(int tmpSize, boolean leaf) {
         this.isLeaf = leaf;
         this.keys = new long[tmpSize];
         this.values = new int[tmpSize];
         if(!this.isLeaf) {
            this.children = new GHLongIntBTree.BTreeEntry[tmpSize + 1];
         }

      }

      GHLongIntBTree.ReturnValue put(long key, int newValue) {
         int index = GHLongIntBTree.binarySearch(this.keys, 0, this.entrySize, key);
         if(index >= 0) {
            int downTreeRV1 = this.values[index];
            this.values[index] = newValue;
            return new GHLongIntBTree.ReturnValue(downTreeRV1);
         } else {
            index = ~index;
            GHLongIntBTree.ReturnValue downTreeRV;
            if(!this.isLeaf && this.children[index] != null) {
               downTreeRV = this.children[index].put(key, newValue);
               if(downTreeRV.oldValue != -1) {
                  return downTreeRV;
               } else {
                  if(downTreeRV.tree != null) {
                     GHLongIntBTree.BTreeEntry returnTree;
                     GHLongIntBTree.BTreeEntry downTree = returnTree = this.checkSplitEntry();
                     if(downTree == null) {
                        this.insertTree(index, downTreeRV.tree);
                     } else if(index <= GHLongIntBTree.this.splitIndex) {
                        downTree.children[0].insertTree(index, downTreeRV.tree);
                     } else {
                        downTree.children[1].insertTree(index - GHLongIntBTree.this.splitIndex - 1, downTreeRV.tree);
                     }

                     downTreeRV.tree = returnTree;
                  }

                  return downTreeRV;
               }
            } else {
               downTreeRV = new GHLongIntBTree.ReturnValue(-1);
               downTreeRV.tree = this.checkSplitEntry();
               if(downTreeRV.tree == null) {
                  this.insertKeyValue(index, key, newValue);
               } else if(index <= GHLongIntBTree.this.splitIndex) {
                  downTreeRV.tree.children[0].insertKeyValue(index, key, newValue);
               } else {
                  downTreeRV.tree.children[1].insertKeyValue(index - GHLongIntBTree.this.splitIndex - 1, key, newValue);
               }

               return downTreeRV;
            }
         }
      }

      GHLongIntBTree.BTreeEntry checkSplitEntry() {
         if(this.entrySize < GHLongIntBTree.this.maxLeafEntries) {
            return null;
         } else {
            int count = this.entrySize - GHLongIntBTree.this.splitIndex - 1;
            GHLongIntBTree.BTreeEntry newRightChild = GHLongIntBTree.this.new BTreeEntry(Math.max(GHLongIntBTree.this.initLeafSize, count), this.isLeaf);
            this.copy(this, newRightChild, GHLongIntBTree.this.splitIndex + 1, count);
            GHLongIntBTree.BTreeEntry newLeftChild = GHLongIntBTree.this.new BTreeEntry(Math.max(GHLongIntBTree.this.initLeafSize, GHLongIntBTree.this.splitIndex), this.isLeaf);
            this.copy(this, newLeftChild, 0, GHLongIntBTree.this.splitIndex);
            GHLongIntBTree.BTreeEntry newTree = GHLongIntBTree.this.new BTreeEntry(1, false);
            newTree.entrySize = 1;
            newTree.keys[0] = this.keys[GHLongIntBTree.this.splitIndex];
            newTree.values[0] = this.values[GHLongIntBTree.this.splitIndex];
            newTree.children[0] = newLeftChild;
            newTree.children[1] = newRightChild;
            return newTree;
         }
      }

      void copy(GHLongIntBTree.BTreeEntry fromChild, GHLongIntBTree.BTreeEntry toChild, int from, int count) {
         System.arraycopy(fromChild.keys, from, toChild.keys, 0, count);
         System.arraycopy(fromChild.values, from, toChild.values, 0, count);
         if(!fromChild.isLeaf) {
            System.arraycopy(fromChild.children, from, toChild.children, 0, count + 1);
         }

         toChild.entrySize = count;
      }

      void insertKeyValue(int index, long key, int newValue) {
         this.ensureSize(this.entrySize + 1);
         int count = this.entrySize - index;
         if(count > 0) {
            System.arraycopy(this.keys, index, this.keys, index + 1, count);
            System.arraycopy(this.values, index, this.values, index + 1, count);
            if(!this.isLeaf) {
               System.arraycopy(this.children, index + 1, this.children, index + 2, count);
            }
         }

         this.keys[index] = key;
         this.values[index] = newValue;
         ++this.entrySize;
      }

      void insertTree(int index, GHLongIntBTree.BTreeEntry tree) {
         this.insertKeyValue(index, tree.keys[0], tree.values[0]);
         if(!this.isLeaf) {
            this.children[index] = tree.children[0];
            this.children[index + 1] = tree.children[1];
         }

      }

      int get(long key) {
         int index = GHLongIntBTree.binarySearch(this.keys, 0, this.entrySize, key);
         if(index >= 0) {
            return this.values[index];
         } else {
            index = ~index;
            return !this.isLeaf && this.children[index] != null?this.children[index].get(key):-1;
         }
      }

      long getCapacity() {
         long cap = (long)(this.keys.length * 12 + 36 + 4 + 1);
         if(!this.isLeaf) {
            cap += (long)(this.children.length * 4);

            for(int i = 0; i < this.children.length; ++i) {
               if(this.children[i] != null) {
                  cap += this.children[i].getCapacity();
               }
            }
         }

         return cap;
      }

      int getEntries() {
         int entries = 1;
         if(!this.isLeaf) {
            for(int i = 0; i < this.children.length; ++i) {
               if(this.children[i] != null) {
                  entries += this.children[i].getEntries();
               }
            }
         }

         return entries;
      }

      void ensureSize(int size) {
         if(size > this.keys.length) {
            int newSize = Math.min(GHLongIntBTree.this.maxLeafEntries, Math.max(size + 1, Math.round((float)size * GHLongIntBTree.this.factor)));
            this.keys = Arrays.copyOf(this.keys, newSize);
            this.values = Arrays.copyOf(this.values, newSize);
            if(!this.isLeaf) {
               this.children = (GHLongIntBTree.BTreeEntry[])Arrays.copyOf(this.children, newSize + 1);
            }

         }
      }

      void compact() {
         byte tolerance = 1;
         if(this.entrySize + tolerance < this.keys.length) {
            this.keys = Arrays.copyOf(this.keys, this.entrySize);
            this.values = Arrays.copyOf(this.values, this.entrySize);
            if(!this.isLeaf) {
               this.children = (GHLongIntBTree.BTreeEntry[])Arrays.copyOf(this.children, this.entrySize + 1);
            }
         }

         if(!this.isLeaf) {
            for(int i = 0; i < this.children.length; ++i) {
               if(this.children[i] != null) {
                  this.children[i].compact();
               }
            }
         }

      }

      String toString(int height) {
         String str = height + ": ";

         int i;
         for(i = 0; i < this.entrySize; ++i) {
            if(i > 0) {
               str = str + ",";
            }

            if(this.keys[i] == -1L) {
               str = str + "-";
            } else {
               str = str + this.keys[i];
            }
         }

         str = str + "\n";
         if(!this.isLeaf) {
            for(i = 0; i < this.entrySize + 1; ++i) {
               if(this.children[i] != null) {
                  str = str + this.children[i].toString(height + 1) + "| ";
               }
            }
         }

         return str;
      }
   }

   static class ReturnValue {
      int oldValue;
      GHLongIntBTree.BTreeEntry tree;

      public ReturnValue() {
      }

      public ReturnValue(int oldValue) {
         this.oldValue = oldValue;
      }
   }
}
