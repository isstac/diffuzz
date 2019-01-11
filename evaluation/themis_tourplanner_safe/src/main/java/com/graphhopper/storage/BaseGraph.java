package com.graphhopper.storage;

import com.graphhopper.coll.GHBitSet;
import com.graphhopper.coll.GHBitSetImpl;
import com.graphhopper.coll.SparseIntIntArray;
import com.graphhopper.routing.util.AllEdgesIterator;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.search.NameIndex;
import com.graphhopper.storage.DataAccess;
import com.graphhopper.storage.Directory;
import com.graphhopper.storage.EdgeAccess;
import com.graphhopper.storage.GHNodeAccess;
import com.graphhopper.storage.Graph;
import com.graphhopper.storage.GraphExtension;
import com.graphhopper.storage.InternalGraphEventListener;
import com.graphhopper.storage.NodeAccess;
import com.graphhopper.util.BitUtil;
import com.graphhopper.util.EdgeExplorer;
import com.graphhopper.util.EdgeIterator;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.GHUtility;
import com.graphhopper.util.Helper;
import com.graphhopper.util.PointList;
import com.graphhopper.util.shapes.BBox;
//import org.slf4j.LoggerFactory;

class BaseGraph implements Graph {
   private static final int MAX_EDGES = 1000;
   int E_GEO;
   int E_NAME;
   int E_ADDITIONAL;
   int edgeEntryBytes;
   private boolean initialized = false;
   final DataAccess edges;
   protected int edgeCount;
   protected int N_EDGE_REF;
   protected int N_LAT;
   protected int N_LON;
   protected int N_ELE;
   protected int N_ADDITIONAL;
   int nodeEntryBytes;
   final DataAccess nodes;
   private int nodeCount;
   final BBox bounds;
   private GHBitSet removedNodes;
   private int edgeEntryIndex;
   private int nodeEntryIndex;
   final NodeAccess nodeAccess;
   final GraphExtension extStorage;
   final DataAccess wayGeometry;
   private int maxGeoRef;
   final NameIndex nameIndex;
   final BitUtil bitUtil;
   private final Directory dir;
   final EncodingManager encodingManager;
   private final InternalGraphEventListener listener;
   private boolean frozen = false;
   final EdgeAccess edgeAccess;

   public BaseGraph(Directory dir, final EncodingManager encodingManager, boolean withElevation, InternalGraphEventListener listener, GraphExtension extendedStorage) {
      this.dir = dir;
      this.encodingManager = encodingManager;
      this.bitUtil = BitUtil.get(dir.getByteOrder());
      this.wayGeometry = dir.find("geometry");
      this.nameIndex = new NameIndex(dir);
      this.nodes = dir.find("nodes");
      this.edges = dir.find("edges");
      this.listener = listener;
      this.edgeAccess = new EdgeAccess(this.edges, this.bitUtil) {
         final BaseGraph.EdgeIterable createSingleEdge(EdgeFilter filter) {
            return new BaseGraph.EdgeIterable(BaseGraph.this, this, filter);
         }

         final int getEdgeRef(int nodeId) {
            return BaseGraph.this.nodes.getInt((long)nodeId * (long)BaseGraph.this.nodeEntryBytes + (long)BaseGraph.this.N_EDGE_REF);
         }

         final void setEdgeRef(int nodeId, int edgeId) {
            BaseGraph.this.nodes.setInt((long)nodeId * (long)BaseGraph.this.nodeEntryBytes + (long)BaseGraph.this.N_EDGE_REF, edgeId);
         }

         final int getEntryBytes() {
            return BaseGraph.this.edgeEntryBytes;
         }

         final long toPointer(int edgeId) {
            assert this.isInBounds(edgeId) : "edgeId " + edgeId + " not in bounds [0," + BaseGraph.this.edgeCount + ")";

            return (long)edgeId * (long)BaseGraph.this.edgeEntryBytes;
         }

         final boolean isInBounds(int edgeId) {
            return edgeId < BaseGraph.this.edgeCount && edgeId >= 0;
         }

         final long reverseFlags(long edgePointer, long flags) {
            return encodingManager.reverseFlags(flags);
         }

         public String toString() {
            return "base edge access";
         }
      };
      this.bounds = BBox.createInverse(withElevation);
      this.nodeAccess = new GHNodeAccess(this, withElevation);
      this.extStorage = extendedStorage;
      this.extStorage.init(this, dir);
   }

   public Graph getBaseGraph() {
      return this;
   }

   void checkInit() {
      if(this.initialized) {
         throw new IllegalStateException("You cannot configure this GraphStorage after calling create or loadExisting. Calling one of the methods twice is also not allowed.");
      }
   }

   protected int loadNodesHeader() {
      this.nodeEntryBytes = this.nodes.getHeader(4);
      this.nodeCount = this.nodes.getHeader(8);
      this.bounds.minLon = Helper.intToDegree(this.nodes.getHeader(12));
      this.bounds.maxLon = Helper.intToDegree(this.nodes.getHeader(16));
      this.bounds.minLat = Helper.intToDegree(this.nodes.getHeader(20));
      this.bounds.maxLat = Helper.intToDegree(this.nodes.getHeader(24));
      if(this.bounds.hasElevation()) {
         this.bounds.minEle = Helper.intToEle(this.nodes.getHeader(28));
         this.bounds.maxEle = Helper.intToEle(this.nodes.getHeader(32));
      }

      this.frozen = this.nodes.getHeader(36) == 1;
      return 10;
   }

   protected int setNodesHeader() {
      this.nodes.setHeader(4, this.nodeEntryBytes);
      this.nodes.setHeader(8, this.nodeCount);
      this.nodes.setHeader(12, Helper.degreeToInt(this.bounds.minLon));
      this.nodes.setHeader(16, Helper.degreeToInt(this.bounds.maxLon));
      this.nodes.setHeader(20, Helper.degreeToInt(this.bounds.minLat));
      this.nodes.setHeader(24, Helper.degreeToInt(this.bounds.maxLat));
      if(this.bounds.hasElevation()) {
         this.nodes.setHeader(28, Helper.eleToInt(this.bounds.minEle));
         this.nodes.setHeader(32, Helper.eleToInt(this.bounds.maxEle));
      }

      this.nodes.setHeader(36, this.isFrozen()?1:0);
      return 10;
   }

   protected int loadEdgesHeader() {
      this.edgeEntryBytes = this.edges.getHeader(0);
      this.edgeCount = this.edges.getHeader(4);
      return 5;
   }

   protected int setEdgesHeader() {
      this.edges.setHeader(0, this.edgeEntryBytes);
      this.edges.setHeader(4, this.edgeCount);
      this.edges.setHeader(8, this.encodingManager.hashCode());
      this.edges.setHeader(12, this.extStorage.hashCode());
      return 5;
   }

   protected int loadWayGeometryHeader() {
      this.maxGeoRef = this.wayGeometry.getHeader(0);
      return 1;
   }

   protected int setWayGeometryHeader() {
      this.wayGeometry.setHeader(0, this.maxGeoRef);
      return 1;
   }

   void initStorage() {
      this.edgeEntryIndex = 0;
      this.nodeEntryIndex = 0;
      boolean flagsSizeIsLong = this.encodingManager.getBytesForFlags() == 8;
      this.edgeAccess.init(this.nextEdgeEntryIndex(4), this.nextEdgeEntryIndex(4), this.nextEdgeEntryIndex(4), this.nextEdgeEntryIndex(4), this.nextEdgeEntryIndex(4), this.nextEdgeEntryIndex(this.encodingManager.getBytesForFlags()), flagsSizeIsLong);
      this.E_GEO = this.nextEdgeEntryIndex(4);
      this.E_NAME = this.nextEdgeEntryIndex(4);
      if(this.extStorage.isRequireEdgeField()) {
         this.E_ADDITIONAL = this.nextEdgeEntryIndex(4);
      } else {
         this.E_ADDITIONAL = -1;
      }

      this.N_EDGE_REF = this.nextNodeEntryIndex(4);
      this.N_LAT = this.nextNodeEntryIndex(4);
      this.N_LON = this.nextNodeEntryIndex(4);
      if(this.nodeAccess.is3D()) {
         this.N_ELE = this.nextNodeEntryIndex(4);
      } else {
         this.N_ELE = -1;
      }

      if(this.extStorage.isRequireNodeField()) {
         this.N_ADDITIONAL = this.nextNodeEntryIndex(4);
      } else {
         this.N_ADDITIONAL = -1;
      }

      this.initNodeAndEdgeEntrySize();
      this.listener.initStorage();
      this.initialized = true;
   }

   void initNodeRefs(long oldCapacity, long newCapacity) {
      long pointer;
      for(pointer = oldCapacity + (long)this.N_EDGE_REF; pointer < newCapacity; pointer += (long)this.nodeEntryBytes) {
         this.nodes.setInt(pointer, -1);
      }

      if(this.extStorage.isRequireNodeField()) {
         for(pointer = oldCapacity + (long)this.N_ADDITIONAL; pointer < newCapacity; pointer += (long)this.nodeEntryBytes) {
            this.nodes.setInt(pointer, this.extStorage.getDefaultNodeFieldValue());
         }
      }

   }

   protected final int nextEdgeEntryIndex(int sizeInBytes) {
      int tmp = this.edgeEntryIndex;
      this.edgeEntryIndex += sizeInBytes;
      return tmp;
   }

   protected final int nextNodeEntryIndex(int sizeInBytes) {
      int tmp = this.nodeEntryIndex;
      this.nodeEntryIndex += sizeInBytes;
      return tmp;
   }

   protected final void initNodeAndEdgeEntrySize() {
      this.nodeEntryBytes = this.nodeEntryIndex;
      this.edgeEntryBytes = this.edgeEntryIndex;
   }

   final void ensureNodeIndex(int nodeIndex) {
      if(!this.initialized) {
         throw new AssertionError("The graph has not yet been initialized.");
      } else if(nodeIndex >= this.nodeCount) {
         long oldNodes = (long)this.nodeCount;
         this.nodeCount = nodeIndex + 1;
         boolean capacityIncreased = this.nodes.ensureCapacity((long)this.nodeCount * (long)this.nodeEntryBytes);
         if(capacityIncreased) {
            long newBytesCapacity = this.nodes.getCapacity();
            this.initNodeRefs(oldNodes * (long)this.nodeEntryBytes, newBytesCapacity);
         }

      }
   }

   public int getNodes() {
      return this.nodeCount;
   }

   public NodeAccess getNodeAccess() {
      return this.nodeAccess;
   }

   public BBox getBounds() {
      return this.bounds;
   }

   public EdgeIteratorState edge(int a, int b, double distance, boolean bothDirection) {
      return this.edge(a, b).setDistance(distance).setFlags(this.encodingManager.flagsDefault(true, bothDirection));
   }

   void setSegmentSize(int bytes) {
      this.checkInit();
      this.nodes.setSegmentSize(bytes);
      this.edges.setSegmentSize(bytes);
      this.wayGeometry.setSegmentSize(bytes);
      this.nameIndex.setSegmentSize(bytes);
      this.extStorage.setSegmentSize(bytes);
   }

   void freeze() {
      if(this.isFrozen()) {
         throw new IllegalStateException("base graph already frozen");
      } else {
         this.frozen = true;
         this.listener.freeze();
      }
   }

   boolean isFrozen() {
      return this.frozen;
   }

   public void checkFreeze() {
      if(this.isFrozen()) {
         throw new IllegalStateException("Cannot add edge or node after baseGraph.freeze was called");
      }
   }

   void create(long initSize) {
      this.nodes.create(initSize);
      this.edges.create(initSize);
      this.wayGeometry.create(initSize);
      this.nameIndex.create(1000L);
      this.extStorage.create(initSize);
      this.initStorage();
      this.maxGeoRef = 4;
      this.initNodeRefs(0L, this.nodes.getCapacity());
   }

   String toDetailsString() {
      return "edges:" + Helper.nf((long)this.edgeCount) + "(" + this.edges.getCapacity() / 1048576L + "MB), " + "nodes:" + Helper.nf((long)this.getNodes()) + "(" + this.nodes.getCapacity() / 1048576L + "MB), " + "name:(" + this.nameIndex.getCapacity() / 1048576L + "MB), " + "geo:" + Helper.nf((long)this.maxGeoRef) + "(" + this.wayGeometry.getCapacity() / 1048576L + "MB), " + "bounds:" + this.bounds;
   }

   void flush() {
      this.setNodesHeader();
      this.setEdgesHeader();
      this.setWayGeometryHeader();
      this.wayGeometry.flush();
      this.nameIndex.flush();
      this.edges.flush();
      this.nodes.flush();
      this.extStorage.flush();
   }

   void close() {
      this.wayGeometry.close();
      this.nameIndex.close();
      this.edges.close();
      this.nodes.close();
      this.extStorage.close();
   }

   long getCapacity() {
      return this.edges.getCapacity() + this.nodes.getCapacity() + this.nameIndex.getCapacity() + this.wayGeometry.getCapacity() + this.extStorage.getCapacity();
   }

   void loadExisting(String dim) {
      if(!this.nodes.loadExisting()) {
    	  System.out.println("Cannot load nodes. corrupt file or directory? " + this.dir);
         throw new IllegalStateException("Cannot load nodes. corrupt file or directory? " + this.dir);
      } else if(!dim.equalsIgnoreCase("" + this.nodeAccess.getDimension())) {
    	  System.out.println("Configured dimension (" + this.nodeAccess.getDimension() + ") is not equal " + "to dimension of loaded graph (" + dim + ")");
    	  throw new IllegalStateException("Configured dimension (" + this.nodeAccess.getDimension() + ") is not equal " + "to dimension of loaded graph (" + dim + ")");
      } else if(!this.edges.loadExisting()) {
    	  System.out.println("Cannot load edges. corrupt file or directory? " + this.dir);
    	  throw new IllegalStateException("Cannot load edges. corrupt file or directory? " + this.dir);
      } else if(!this.wayGeometry.loadExisting()) {
        
    	  System.out.println("Cannot load geometry. corrupt file or directory? " + this.dir);
    	  throw new IllegalStateException("Cannot load geometry. corrupt file or directory? " + this.dir);
      } else if(!this.nameIndex.loadExisting()) {
    	  System.out.println("Cannot load name index. corrupt file or directory? " + this.dir);
    	  throw new IllegalStateException("Cannot load name index. corrupt file or directory? " + this.dir);
      } else if(!this.extStorage.loadExisting()) {
    	  System.out.println("Cannot load extended storage. corrupt file or directory? " + this.dir);
    	  throw new IllegalStateException("Cannot load extended storage. corrupt file or directory? " + this.dir);
      } else {
         this.initStorage();
         this.loadNodesHeader();
         this.loadEdgesHeader();
         this.loadWayGeometryHeader();
      }
   }

   EdgeIteratorState copyProperties(BaseGraph.CommonEdgeIterator from, EdgeIteratorState to) {
      to.setDistance(from.getDistance()).setName(from.getName()).setFlags(from.getDirectFlags()).setWayGeometry(from.fetchWayGeometry(0));
      if(this.E_ADDITIONAL >= 0) {
         to.setAdditionalField(from.getAdditionalField());
      }

      return to;
   }

   public EdgeIteratorState edge(int nodeA, int nodeB) {
      if(this.isFrozen()) {
         throw new IllegalStateException("Cannot create edge if graph is already frozen");
      } else {
         this.ensureNodeIndex(Math.max(nodeA, nodeB));
         int edgeId = this.edgeAccess.internalEdgeAdd(this.nextEdgeId(), nodeA, nodeB);
         BaseGraph.EdgeIterable iter = new BaseGraph.EdgeIterable(this, this.edgeAccess, EdgeFilter.ALL_EDGES);
         boolean ret = iter.init(edgeId, nodeB);

         assert ret;

         if(this.extStorage.isRequireEdgeField()) {
            iter.setAdditionalField(this.extStorage.getDefaultEdgeFieldValue());
         }

         return iter;
      }
   }

   void setEdgeCount(int cnt) {
      this.edgeCount = cnt;
   }

   protected int nextEdgeId() {
      int nextEdge = this.edgeCount++;
      if(this.edgeCount < 0) {
         throw new IllegalStateException("too many edges. new edge id would be negative. " + this.toString());
      } else {
         this.edges.ensureCapacity(((long)this.edgeCount + 1L) * (long)this.edgeEntryBytes);
         return nextEdge;
      }
   }

   public EdgeIteratorState getEdgeIteratorState(int edgeId, int adjNode) {
      if(!this.edgeAccess.isInBounds(edgeId)) {
         throw new IllegalStateException("edgeId " + edgeId + " out of bounds");
      } else {
         this.checkAdjNodeBounds(adjNode);
         return this.edgeAccess.getEdgeProps(edgeId, adjNode);
      }
   }

   final void checkAdjNodeBounds(int adjNode) {
      if(adjNode < 0 && adjNode != Integer.MIN_VALUE || adjNode >= this.nodeCount) {
         throw new IllegalStateException("adjNode " + adjNode + " out of bounds [0," + Helper.nf((long)this.nodeCount) + ")");
      }
   }

   public EdgeExplorer createEdgeExplorer(EdgeFilter filter) {
      return new BaseGraph.EdgeIterable(this, this.edgeAccess, filter);
   }

   public EdgeExplorer createEdgeExplorer() {
      return this.createEdgeExplorer(EdgeFilter.ALL_EDGES);
   }

   public AllEdgesIterator getAllEdges() {
      return new BaseGraph.AllEdgeIterator(this, this.edgeAccess, null);
   }

   public Graph copyTo(Graph g) {
      this.initialized = true;
      if(g.getClass().equals(this.getClass())) {
         this._copyTo((BaseGraph)g);
         return g;
      } else {
         return GHUtility.copyTo(this, g);
      }
   }

   void _copyTo(BaseGraph clonedG) {
      if(clonedG.edgeEntryBytes != this.edgeEntryBytes) {
         throw new IllegalStateException("edgeEntryBytes cannot be different for cloned graph. Cloned: " + clonedG.edgeEntryBytes + " vs " + this.edgeEntryBytes);
      } else if(clonedG.nodeEntryBytes != this.nodeEntryBytes) {
         throw new IllegalStateException("nodeEntryBytes cannot be different for cloned graph. Cloned: " + clonedG.nodeEntryBytes + " vs " + this.nodeEntryBytes);
      } else if(clonedG.nodeAccess.getDimension() != this.nodeAccess.getDimension()) {
         throw new IllegalStateException("dimension cannot be different for cloned graph. Cloned: " + clonedG.nodeAccess.getDimension() + " vs " + this.nodeAccess.getDimension());
      } else {
         this.setNodesHeader();
         this.nodes.copyTo(clonedG.nodes);
         clonedG.loadNodesHeader();
         this.setEdgesHeader();
         this.edges.copyTo(clonedG.edges);
         clonedG.loadEdgesHeader();
         this.nameIndex.copyTo(clonedG.nameIndex);
         this.setWayGeometryHeader();
         this.wayGeometry.copyTo(clonedG.wayGeometry);
         clonedG.loadWayGeometryHeader();
         this.extStorage.copyTo(clonedG.extStorage);
         if(this.removedNodes == null) {
            clonedG.removedNodes = null;
         } else {
            clonedG.removedNodes = this.removedNodes.copyTo(new GHBitSetImpl());
         }

      }
   }

   protected void trimToSize() {
      long nodeCap = (long)this.nodeCount * (long)this.nodeEntryBytes;
      this.nodes.trimTo(nodeCap);
   }

   void inPlaceNodeRemove(int removeNodeCount) {
      int toMoveNodes = this.getNodes();
      int itemsToMove = 0;
      SparseIntIntArray oldToNewMap = new SparseIntIntArray(removeNodeCount);
      GHBitSetImpl toRemoveSet = new GHBitSetImpl(removeNodeCount);
      this.removedNodes.copyTo(toRemoveSet);
      EdgeExplorer delExplorer = this.createEdgeExplorer(EdgeFilter.ALL_EDGES);

      for(int adjNodesToDelIter = this.removedNodes.next(0); adjNodesToDelIter >= 0; adjNodesToDelIter = this.removedNodes.next(adjNodesToDelIter + 1)) {
         EdgeIterator toMoveSet = delExplorer.setBaseNode(adjNodesToDelIter);

         while(toMoveSet.next()) {
            toRemoveSet.add(toMoveSet.getAdjNode());
         }

         --toMoveNodes;

         while(toMoveNodes >= 0 && this.removedNodes.contains(toMoveNodes)) {
            --toMoveNodes;
         }

         if(toMoveNodes >= adjNodesToDelIter) {
            oldToNewMap.put(toMoveNodes, adjNodesToDelIter);
         }

         ++itemsToMove;
      }

      BaseGraph.EdgeIterable var24 = (BaseGraph.EdgeIterable)this.createEdgeExplorer();

      int explorer;
      int base;
      long adj;
      label162:
      for(int var25 = toRemoveSet.next(0); var25 >= 0; var25 = toRemoveSet.next(var25 + 1)) {
         var24.setBaseNode(var25);
         long movedEdgeExplorer = -1L;

         while(true) {
            while(true) {
               if(!var24.next()) {
                  continue label162;
               }

               explorer = var24.getAdjNode();
               if(explorer != -1 && this.removedNodes.contains(explorer)) {
                  base = var24.getEdge();
                  adj = this.edgeAccess.toPointer(base);
                  this.edgeAccess.internalEdgeDisconnect(base, movedEdgeExplorer, var25, explorer);
                  this.edgeAccess.invalidateEdge(adj);
               } else {
                  movedEdgeExplorer = var24.edgePointer;
               }
            }
         }
      }

      GHBitSetImpl var26 = new GHBitSetImpl(removeNodeCount * 3);
      EdgeExplorer var27 = this.createEdgeExplorer();

      int iter;
      int var31;
      for(iter = 0; iter < itemsToMove; ++iter) {
         explorer = oldToNewMap.keyAt(iter);
         EdgeIterator var29 = var27.setBaseNode(explorer);

         while(var29.next()) {
            var31 = var29.getAdjNode();
            if(var31 != -1) {
               if(this.removedNodes.contains(var31)) {
                  throw new IllegalStateException("shouldn\'t happen the edge to the node " + var31 + " should be already deleted. " + explorer);
               }

               var26.add(var31);
            }
         }
      }

      for(iter = 0; iter < itemsToMove; ++iter) {
         explorer = oldToNewMap.keyAt(iter);
         base = oldToNewMap.valueAt(iter);
         adj = (long)base * (long)this.nodeEntryBytes;
         long ex = (long)explorer * (long)this.nodeEntryBytes;

         for(long j = 0L; j < (long)this.nodeEntryBytes; j += 4L) {
            this.nodes.setInt(adj + j, this.nodes.getInt(ex + j));
         }
      }

      AllEdgesIterator var28 = this.getAllEdges();

      while(true) {
         do {
            if(!var28.next()) {
               if(removeNodeCount >= this.nodeCount) {
                  throw new IllegalStateException("graph is empty after in-place removal but was " + removeNodeCount);
               }

               this.nodeCount -= removeNodeCount;
               EdgeExplorer var30 = this.createEdgeExplorer();
               if(isTestingEnabled()) {
                  var28 = this.getAllEdges();

                  while(var28.next()) {
                     base = var28.getBaseNode();
                     var31 = var28.getAdjNode();
                     String var32 = var28.getEdge() + ", r.contains(" + base + "):" + this.removedNodes.contains(base) + ", r.contains(" + var31 + "):" + this.removedNodes.contains(var31) + ", tr.contains(" + base + "):" + toRemoveSet.contains(base) + ", tr.contains(" + var31 + "):" + toRemoveSet.contains(var31) + ", base:" + base + ", adj:" + var31 + ", nodeCount:" + this.nodeCount;
                     if(var31 >= this.nodeCount) {
                        throw new RuntimeException("Adj.node problem with edge " + var32);
                     }

                     if(base >= this.nodeCount) {
                        throw new RuntimeException("Base node problem with edge " + var32);
                     }

                     try {
                        var30.setBaseNode(var31).toString();
                     } catch (Exception var23) {
//                        LoggerFactory.getLogger(this.getClass()).error("adj:" + var31);
                     }

                     try {
                        var30.setBaseNode(base).toString();
                     } catch (Exception var22) {
//                        LoggerFactory.getLogger(this.getClass()).error("base:" + base);
                     }
                  }

                  var30.setBaseNode(this.nodeCount - 1).toString();
               }

               this.removedNodes = null;
               return;
            }

            explorer = var28.getBaseNode();
            base = var28.getAdjNode();
         } while(!var26.contains(explorer) && !var26.contains(base));

         var31 = oldToNewMap.get(explorer);
         if(var31 < 0) {
            var31 = explorer;
         }

         int str = oldToNewMap.get(base);
         if(str < 0) {
            str = base;
         }

         int var33 = var28.getEdge();
         long edgePointer = this.edgeAccess.toPointer(var33);
         int linkA = this.edgeAccess.getEdgeRef(explorer, base, edgePointer);
         int linkB = this.edgeAccess.getEdgeRef(base, explorer, edgePointer);
         long flags = this.edgeAccess.getFlags_(edgePointer, false);
         this.edgeAccess.writeEdge(var33, var31, str, linkA, linkB);
         this.edgeAccess.setFlags_(edgePointer, var31 > str, flags);
         if(var31 < str != explorer < base) {
            this.setWayGeometry_(this.fetchWayGeometry_(edgePointer, true, 0, -1, -1), edgePointer, false);
         }
      }
   }

   public GraphExtension getExtension() {
      return this.extStorage;
   }

   public void setAdditionalEdgeField(long edgePointer, int value) {
      if(this.extStorage.isRequireEdgeField() && this.E_ADDITIONAL >= 0) {
         this.edges.setInt(edgePointer + (long)this.E_ADDITIONAL, value);
      } else {
         throw new AssertionError("This graph does not support an additional edge field.");
      }
   }

   private void setWayGeometry_(PointList pillarNodes, long edgePointer, boolean reverse) {
      if(pillarNodes != null && !pillarNodes.isEmpty()) {
         if(pillarNodes.getDimension() != this.nodeAccess.getDimension()) {
            throw new IllegalArgumentException("Cannot use pointlist which is " + pillarNodes.getDimension() + "D for graph which is " + this.nodeAccess.getDimension() + "D");
         }

         int len = pillarNodes.getSize();
         int dim = this.nodeAccess.getDimension();
         int tmpRef = this.nextGeoRef(len * dim);
         this.edges.setInt(edgePointer + (long)this.E_GEO, tmpRef);
         long geoRef = (long)tmpRef * 4L;
         byte[] bytes = new byte[len * dim * 4 + 4];
         this.ensureGeometry(geoRef, bytes.length);
         this.bitUtil.fromInt(bytes, len, 0);
         if(reverse) {
            pillarNodes.reverse();
         }

         int tmpOffset = 4;
         boolean is3D = this.nodeAccess.is3D();

         for(int i = 0; i < len; ++i) {
            double lat = pillarNodes.getLatitude(i);
            this.bitUtil.fromInt(bytes, Helper.degreeToInt(lat), tmpOffset);
            tmpOffset += 4;
            this.bitUtil.fromInt(bytes, Helper.degreeToInt(pillarNodes.getLongitude(i)), tmpOffset);
            tmpOffset += 4;
            if(is3D) {
               this.bitUtil.fromInt(bytes, Helper.eleToInt(pillarNodes.getElevation(i)), tmpOffset);
               tmpOffset += 4;
            }
         }

         this.wayGeometry.setBytes(geoRef, bytes, bytes.length);
      } else {
         this.edges.setInt(edgePointer + (long)this.E_GEO, 0);
      }

   }

   private PointList fetchWayGeometry_(long edgePointer, boolean reverse, int mode, int baseNode, int adjNode) {
      long geoRef = (long)this.edges.getInt(edgePointer + (long)this.E_GEO);
      int count = 0;
      byte[] bytes = null;
      if(geoRef > 0L) {
         geoRef *= 4L;
         count = this.wayGeometry.getInt(geoRef);
         geoRef += 4L;
         bytes = new byte[count * this.nodeAccess.getDimension() * 4];
         this.wayGeometry.getBytes(geoRef, bytes, bytes.length);
      } else if(mode == 0) {
         return PointList.EMPTY;
      }

      PointList pillarNodes = new PointList(count + mode, this.nodeAccess.is3D());
      if(reverse) {
         if((mode & 2) != 0) {
            pillarNodes.add(this.nodeAccess, adjNode);
         }
      } else if((mode & 1) != 0) {
         pillarNodes.add(this.nodeAccess, baseNode);
      }

      int index = 0;

      for(int i = 0; i < count; ++i) {
         double lat = Helper.intToDegree(this.bitUtil.toInt(bytes, index));
         index += 4;
         double lon = Helper.intToDegree(this.bitUtil.toInt(bytes, index));
         index += 4;
         if(this.nodeAccess.is3D()) {
            pillarNodes.add(lat, lon, Helper.intToEle(this.bitUtil.toInt(bytes, index)));
            index += 4;
         } else {
            pillarNodes.add(lat, lon);
         }
      }

      if(reverse) {
         if((mode & 1) != 0) {
            pillarNodes.add(this.nodeAccess, baseNode);
         }

         pillarNodes.reverse();
      } else if((mode & 2) != 0) {
         pillarNodes.add(this.nodeAccess, adjNode);
      }

      return pillarNodes;
   }

   private void setName(long edgePointer, String name) {
      int nameIndexRef = (int)this.nameIndex.put(name);
      if(nameIndexRef < 0) {
         throw new IllegalStateException("Too many names are stored, currently limited to int pointer");
      } else {
         this.edges.setInt(edgePointer + (long)this.E_NAME, nameIndexRef);
      }
   }

   GHBitSet getRemovedNodes() {
      if(this.removedNodes == null) {
         this.removedNodes = new GHBitSetImpl(this.getNodes());
      }

      return this.removedNodes;
   }

   private static boolean isTestingEnabled() {
      boolean enableIfAssert = false;
      assert enableIfAssert = true : true;
      return enableIfAssert;
   }

   private void ensureGeometry(long bytePos, int byteLength) {
      this.wayGeometry.ensureCapacity(bytePos + (long)byteLength);
   }

   private int nextGeoRef(int arrayLength) {
      int tmp = this.maxGeoRef;
      this.maxGeoRef += arrayLength + 1;
      return tmp;
   }

   // $FF: synthetic method
   static void access$100(BaseGraph x0, PointList x1, long x2, boolean x3) {
      x0.setWayGeometry_(x1, x2, x3);
   }

   // $FF: synthetic method
   static PointList access$200(BaseGraph x0, long x1, boolean x2, int x3, int x4, int x5) {
      return x0.fetchWayGeometry_(x1, x2, x3, x4, x5);
   }

   // $FF: synthetic method
   static void access$300(BaseGraph x0, long x1, String x2) {
      x0.setName(x1, x2);
   }

   abstract static class CommonEdgeIterator implements EdgeIteratorState {
      protected long edgePointer;
      protected int baseNode;
      protected int adjNode;
      boolean reverse = false;
      protected EdgeAccess edgeAccess;
      final BaseGraph baseGraph;
      boolean freshFlags;
      private long cachedFlags;
      int edgeId = -1;

      public CommonEdgeIterator(long edgePointer, EdgeAccess edgeAccess, BaseGraph baseGraph) {
         this.edgePointer = edgePointer;
         this.edgeAccess = edgeAccess;
         this.baseGraph = baseGraph;
      }

      public final int getBaseNode() {
         return this.baseNode;
      }

      public final int getAdjNode() {
         return this.adjNode;
      }

      public final double getDistance() {
         return this.edgeAccess.getDist(this.edgePointer);
      }

      public final EdgeIteratorState setDistance(double dist) {
         this.edgeAccess.setDist(this.edgePointer, dist);
         return this;
      }

      final long getDirectFlags() {
         if(!this.freshFlags) {
            this.cachedFlags = this.edgeAccess.getFlags_(this.edgePointer, this.reverse);
            this.freshFlags = true;
         }

         return this.cachedFlags;
      }

      public long getFlags() {
         return this.getDirectFlags();
      }

      public final EdgeIteratorState setFlags(long fl) {
         this.edgeAccess.setFlags_(this.edgePointer, this.reverse, fl);
         this.cachedFlags = fl;
         this.freshFlags = true;
         return this;
      }

      public final int getAdditionalField() {
         return this.baseGraph.edges.getInt(this.edgePointer + (long)this.baseGraph.E_ADDITIONAL);
      }

      public final EdgeIteratorState setAdditionalField(int value) {
         this.baseGraph.setAdditionalEdgeField(this.edgePointer, value);
         return this;
      }

      public final EdgeIteratorState copyPropertiesTo(EdgeIteratorState edge) {
         return this.baseGraph.copyProperties(this, edge);
      }

      public boolean isForward(FlagEncoder encoder) {
         return encoder.isForward(this.getDirectFlags());
      }

      public boolean isBackward(FlagEncoder encoder) {
         return encoder.isBackward(this.getDirectFlags());
      }

      public EdgeIteratorState setWayGeometry(PointList pillarNodes) {
         BaseGraph.access$100(this.baseGraph, pillarNodes, this.edgePointer, this.reverse);
         return this;
      }

      public PointList fetchWayGeometry(int mode) {
         return BaseGraph.access$200(this.baseGraph, this.edgePointer, this.reverse, mode, this.getBaseNode(), this.getAdjNode());
      }

      public int getEdge() {
         return this.edgeId;
      }

      public String getName() {
         int nameIndexRef = this.baseGraph.edges.getInt(this.edgePointer + (long)this.baseGraph.E_NAME);
         return this.baseGraph.nameIndex.get((long)nameIndexRef);
      }

      public EdgeIteratorState setName(String name) {
         BaseGraph.access$300(this.baseGraph, this.edgePointer, name);
         return this;
      }

      public final boolean getBoolean(int key, boolean reverse, boolean _default) {
         return _default;
      }

      public final String toString() {
         return this.getEdge() + " " + this.getBaseNode() + "-" + this.getAdjNode();
      }
   }

   protected static class AllEdgeIterator extends BaseGraph.CommonEdgeIterator implements AllEdgesIterator {
      public AllEdgeIterator(BaseGraph baseGraph) {
         this(baseGraph, baseGraph.edgeAccess);
      }

      private AllEdgeIterator(BaseGraph baseGraph, EdgeAccess edgeAccess) {
         super(-1L, edgeAccess, baseGraph);
      }

      public int getMaxId() {
         return this.baseGraph.edgeCount;
      }

      public boolean next() {
         do {
            ++this.edgeId;
            this.edgePointer = (long)this.edgeId * (long)this.edgeAccess.getEntryBytes();
            if(!this.checkRange()) {
               return false;
            }

            this.baseNode = this.edgeAccess.edges.getInt(this.edgePointer + (long)this.edgeAccess.E_NODEA);
         } while(this.baseNode == -1);

         this.freshFlags = false;
         this.adjNode = this.edgeAccess.edges.getInt(this.edgePointer + (long)this.edgeAccess.E_NODEB);
         this.reverse = false;
         return true;
      }

      protected boolean checkRange() {
         return this.edgeId < this.baseGraph.edgeCount;
      }

      public final EdgeIteratorState detach(boolean reverseArg) {
         if(this.edgePointer < 0L) {
            throw new IllegalStateException("call next before detaching");
         } else {
            BaseGraph.AllEdgeIterator iter = new BaseGraph.AllEdgeIterator(this.baseGraph, this.edgeAccess);
            iter.edgeId = this.edgeId;
            iter.edgePointer = this.edgePointer;
            if(reverseArg) {
               iter.reverse = !this.reverse;
               iter.baseNode = this.adjNode;
               iter.adjNode = this.baseNode;
            } else {
               iter.reverse = this.reverse;
               iter.baseNode = this.baseNode;
               iter.adjNode = this.adjNode;
            }

            return iter;
         }
      }

      // $FF: synthetic method
      AllEdgeIterator(BaseGraph x0, EdgeAccess x1, Object x2) {
         this(x0, x1);
      }
   }

   protected static class EdgeIterable extends BaseGraph.CommonEdgeIterator implements EdgeExplorer, EdgeIterator {
      final EdgeFilter filter;
      int nextEdgeId;

      public EdgeIterable(BaseGraph baseGraph, EdgeAccess edgeAccess, EdgeFilter filter) {
         super(-1L, edgeAccess, baseGraph);
         if(filter == null) {
            throw new IllegalArgumentException("Instead null filter use EdgeFilter.ALL_EDGES");
         } else {
            this.filter = filter;
         }
      }

      final void setEdgeId(int edgeId) {
         this.nextEdgeId = this.edgeId = edgeId;
      }

      final boolean init(int tmpEdgeId, int expectedAdjNode) {
         this.setEdgeId(tmpEdgeId);
         if(tmpEdgeId != -1) {
            this.selectEdgeAccess();
            this.edgePointer = this.edgeAccess.toPointer(tmpEdgeId);
         }

         this.baseNode = this.edgeAccess.edges.getInt(this.edgePointer + (long)this.edgeAccess.E_NODEA);
         if(this.baseNode == -1) {
            throw new IllegalStateException("content of edgeId " + this.edgeId + " is marked as invalid - ie. the edge is already removed!");
         } else {
            this.adjNode = this.edgeAccess.edges.getInt(this.edgePointer + (long)this.edgeAccess.E_NODEB);
            this.nextEdgeId = -1;
            if(expectedAdjNode != this.adjNode && expectedAdjNode != Integer.MIN_VALUE) {
               if(expectedAdjNode == this.baseNode) {
                  this.reverse = true;
                  this.baseNode = this.adjNode;
                  this.adjNode = expectedAdjNode;
                  return true;
               } else {
                  return false;
               }
            } else {
               this.reverse = false;
               return true;
            }
         }
      }

      final void _setBaseNode(int baseNode) {
         this.baseNode = baseNode;
      }

      public EdgeIterator setBaseNode(int baseNode) {
         this.setEdgeId(this.baseGraph.edgeAccess.getEdgeRef(baseNode));
         this._setBaseNode(baseNode);
         return this;
      }

      protected void selectEdgeAccess() {
      }

      public final boolean next() {
         do {
            if(this.nextEdgeId == -1) {
               return false;
            }

            this.selectEdgeAccess();
            this.edgePointer = this.edgeAccess.toPointer(this.nextEdgeId);
            this.edgeId = this.nextEdgeId;
            this.adjNode = this.edgeAccess.getOtherNode(this.baseNode, this.edgePointer);
            this.reverse = this.baseNode > this.adjNode;
            this.freshFlags = false;
            this.nextEdgeId = this.edgeAccess.getEdgeRef(this.baseNode, this.adjNode, this.edgePointer);

            assert this.nextEdgeId != this.edgeId : "endless loop detected for base node: " + this.baseNode + ", adj node: " + this.adjNode + ", edge pointer: " + this.edgePointer + ", edge: " + this.edgeId;
         } while(!this.filter.accept(this));

         return true;
      }

      public EdgeIteratorState detach(boolean reverseArg) {
         if(this.edgeId != this.nextEdgeId && this.edgeId != -1) {
            BaseGraph.EdgeIterable iter = this.edgeAccess.createSingleEdge(this.filter);
            boolean ret;
            if(reverseArg) {
               ret = iter.init(this.edgeId, this.baseNode);
               iter.reverse = !this.reverse;
            } else {
               ret = iter.init(this.edgeId, this.adjNode);
            }

            assert ret;

            return iter;
         } else {
            throw new IllegalStateException("call next before detaching or setEdgeId (edgeId:" + this.edgeId + " vs. next " + this.nextEdgeId + ")");
         }
      }
   }
}
