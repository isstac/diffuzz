package com.graphhopper.storage;

import com.graphhopper.routing.util.AllEdgesIterator;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.util.Weighting;
import com.graphhopper.storage.BaseGraph;
import com.graphhopper.storage.CHGraph;
import com.graphhopper.storage.CHGraphImpl;
import com.graphhopper.storage.Directory;
import com.graphhopper.storage.Graph;
import com.graphhopper.storage.GraphExtension;
import com.graphhopper.storage.GraphStorage;
import com.graphhopper.storage.InternalGraphEventListener;
import com.graphhopper.storage.NodeAccess;
import com.graphhopper.storage.StorableProperties;
import com.graphhopper.util.EdgeExplorer;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.shapes.BBox;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public final class GraphHopperStorage implements GraphStorage, Graph {
   private final Directory dir;
   private EncodingManager encodingManager;
   private final StorableProperties properties;
   private final BaseGraph baseGraph;
   private final Collection chGraphs;

   public GraphHopperStorage(Directory dir, EncodingManager encodingManager, boolean withElevation, GraphExtension extendedStorage) {
      this(Collections.emptyList(), dir, encodingManager, withElevation, extendedStorage);
   }

   public GraphHopperStorage(List chWeightings, Directory dir, EncodingManager encodingManager, boolean withElevation, GraphExtension extendedStorage) {
	   this.chGraphs = new ArrayList(5);
      if(extendedStorage == null) {
         throw new IllegalArgumentException("GraphExtension cannot be null, use NoOpExtension");
      } else {
         this.encodingManager = encodingManager;
         this.dir = dir;
         this.properties = new StorableProperties(dir);
         InternalGraphEventListener listener = new InternalGraphEventListener() {
            public void initStorage() {
               Iterator i$ = GraphHopperStorage.this.chGraphs.iterator();

               while(i$.hasNext()) {
                  CHGraphImpl cg = (CHGraphImpl)i$.next();
                  cg.initStorage();
               }

            }

            public void freeze() {
               Iterator i$ = GraphHopperStorage.this.chGraphs.iterator();

               while(i$.hasNext()) {
                  CHGraphImpl cg = (CHGraphImpl)i$.next();
                  cg._freeze();
               }

            }
         };
         
         this.baseGraph = new BaseGraph(dir, encodingManager, withElevation, listener, extendedStorage);
         Iterator i$ = chWeightings.iterator();
         while(i$.hasNext()) {
            Weighting w = (Weighting)i$.next();
            this.chGraphs.add(new CHGraphImpl(w, dir, this.baseGraph));
         }

      }
   }

   public Graph getGraph(Class clazz, Weighting weighting) {
      if(clazz.equals(Graph.class)) {
         return this.baseGraph;
      } else if(this.chGraphs.isEmpty()) {
         throw new IllegalStateException("Cannot find graph implementation for " + clazz);
      } else if(weighting == null) {
         throw new IllegalStateException("Cannot find CHGraph with null weighting");
      } else {
         ArrayList existing = new ArrayList();
         Iterator i$ = this.chGraphs.iterator();

         while(i$.hasNext()) {
            CHGraphImpl cg = (CHGraphImpl)i$.next();
            if(cg.getWeighting() == weighting) {
               return cg;
            }

            existing.add(cg.getWeighting());
         }

         throw new IllegalStateException("Cannot find CHGraph for specified weighting: " + weighting + ", existing:" + existing);
      }
   }

   public Graph getGraph(Class clazz) {
      if(clazz.equals(Graph.class)) {
         return this.baseGraph;
      } else if(this.chGraphs.isEmpty()) {
         throw new IllegalStateException("Cannot find graph implementation for " + clazz);
      } else {
         CHGraph cg = (CHGraph)this.chGraphs.iterator().next();
         return cg;
      }
   }

   public boolean isCHPossible() {
      return !this.chGraphs.isEmpty();
   }

   public List getCHWeightings() {
      ArrayList list = new ArrayList(this.chGraphs.size());
      Iterator i$ = this.chGraphs.iterator();

      while(i$.hasNext()) {
         CHGraphImpl cg = (CHGraphImpl)i$.next();
         list.add(cg.getWeighting());
      }

      return list;
   }

   public Directory getDirectory() {
      return this.dir;
   }

   public void setSegmentSize(int bytes) {
      this.baseGraph.setSegmentSize(bytes);
      Iterator i$ = this.chGraphs.iterator();

      while(i$.hasNext()) {
         CHGraphImpl cg = (CHGraphImpl)i$.next();
         cg.setSegmentSize(bytes);
      }

   }

   public GraphHopperStorage create(long byteCount) {
      this.baseGraph.checkInit();
      if(this.encodingManager == null) {
         throw new IllegalStateException("EncodingManager can only be null if you call loadExisting");
      } else {
         long initSize = Math.max(byteCount, 100L);
         this.properties.create(100L);
         this.properties.put("graph.bytesForFlags", (Object)Integer.valueOf(this.encodingManager.getBytesForFlags()));
         this.properties.put("graph.flagEncoders", this.encodingManager.toDetailsString());
         this.properties.put("graph.byteOrder", (Object)this.dir.getByteOrder());
         this.properties.put("graph.dimension", (Object)Integer.valueOf(this.baseGraph.nodeAccess.getDimension()));
         this.properties.putCurrentVersions();
         this.baseGraph.create(initSize);
         Iterator i$ = this.chGraphs.iterator();

         while(i$.hasNext()) {
            CHGraphImpl cg = (CHGraphImpl)i$.next();
            cg.create(byteCount);
         }

         this.properties.put("graph.chWeightings", this.getCHWeightings().toString());
         return this;
      }
   }

   public EncodingManager getEncodingManager() {
      return this.encodingManager;
   }

   public StorableProperties getProperties() {
      return this.properties;
   }

   public void setAdditionalEdgeField(long edgePointer, int value) {
      this.baseGraph.setAdditionalEdgeField(edgePointer, value);
   }

   public void markNodeRemoved(int index) {
      this.baseGraph.getRemovedNodes().add(index);
   }

   public boolean isNodeRemoved(int index) {
      return this.baseGraph.getRemovedNodes().contains(index);
   }

   public void optimize() {
      if(this.isFrozen()) {
         throw new IllegalStateException("do not optimize after graph was frozen");
      } else {
         int delNodes = this.baseGraph.getRemovedNodes().getCardinality();
         if(delNodes > 0) {
            this.baseGraph.inPlaceNodeRemove(delNodes);
            this.baseGraph.trimToSize();
         }
      }
   }

   public boolean loadExisting() {
      this.baseGraph.checkInit();
      if(this.properties.loadExisting()) {
         this.properties.checkVersions(false);
         String acceptStr = this.properties.get("graph.flagEncoders");
         if(this.encodingManager == null) {
            if(acceptStr.isEmpty()) {
               throw new IllegalStateException("No EncodingManager was configured. And no one was found in the graph: " + this.dir.getLocation());
            }

            byte byteOrder = 4;
            if("8".equals(this.properties.get("graph.bytesForFlags"))) {
               byteOrder = 8;
            }

            this.encodingManager = new EncodingManager(acceptStr, byteOrder);
         } else if(!acceptStr.isEmpty() && !this.encodingManager.toDetailsString().equalsIgnoreCase(acceptStr)) {
            throw new IllegalStateException("Encoding does not match:\nGraphhopper config: " + this.encodingManager.toDetailsString() + "\nGraph: " + acceptStr + ", dir:" + this.dir.getLocation());
         }

         String byteOrder1 = this.properties.get("graph.byteOrder");
         if(!byteOrder1.equalsIgnoreCase("" + this.dir.getByteOrder())) {
        	 System.out.println("Configured byteOrder (" + byteOrder1 + ") is not equal to byteOrder of loaded graph (" + this.dir.getByteOrder() + ")");
            throw new IllegalStateException("Configured byteOrder (" + byteOrder1 + ") is not equal to byteOrder of loaded graph (" + this.dir.getByteOrder() + ")");
         } else {
            String dim = this.properties.get("graph.dimension");
            this.baseGraph.loadExisting(dim);
            String loadedCHWeightings = this.properties.get("graph.chWeightings");
            String configuredCHWeightings = this.getCHWeightings().toString();
            if(!loadedCHWeightings.equals(configuredCHWeightings)) {
            	System.out.println("Configured graph.chWeightings: " + configuredCHWeightings + " is not equal to loaded " + loadedCHWeightings);
               throw new IllegalStateException("Configured graph.chWeightings: " + configuredCHWeightings + " is not equal to loaded " + loadedCHWeightings);
            } else {
               Iterator i$ = this.chGraphs.iterator();

               CHGraphImpl cg;
               do {
                  if(!i$.hasNext()) {
                     return true;
                  }

                  cg = (CHGraphImpl)i$.next();
               } while(cg.loadExisting());

               System.out.println("IllegalStateException");
               
               throw new IllegalStateException("Cannot load " + cg);
            }
         }
      } else {
         return false;
      }
   }

   public void flush() {
      Iterator i$ = this.chGraphs.iterator();

      while(i$.hasNext()) {
         CHGraphImpl cg = (CHGraphImpl)i$.next();
         cg.setEdgesHeader();
         cg.flush();
      }

      this.baseGraph.flush();
      this.properties.flush();
   }

   public void close() {
      this.properties.close();
      this.baseGraph.close();
      Iterator i$ = this.chGraphs.iterator();

      while(i$.hasNext()) {
         CHGraphImpl cg = (CHGraphImpl)i$.next();
         cg.close();
      }

   }

   public boolean isClosed() {
      return this.baseGraph.nodes.isClosed();
   }

   public long getCapacity() {
      long cnt = this.baseGraph.getCapacity() + this.properties.getCapacity();

      CHGraphImpl cg;
      for(Iterator i$ = this.chGraphs.iterator(); i$.hasNext(); cnt += cg.getCapacity()) {
         cg = (CHGraphImpl)i$.next();
      }

      return cnt;
   }

   public void freeze() {
      if(!this.baseGraph.isFrozen()) {
         this.baseGraph.freeze();
      }

   }

   boolean isFrozen() {
      return this.baseGraph.isFrozen();
   }

   public String toDetailsString() {
      String str = this.baseGraph.toDetailsString();

      CHGraphImpl cg;
      for(Iterator i$ = this.chGraphs.iterator(); i$.hasNext(); str = str + ", " + cg.toDetailsString()) {
         cg = (CHGraphImpl)i$.next();
      }

      return str;
   }

   public String toString() {
      return (this.isCHPossible()?"CH|":"") + this.encodingManager + "|" + this.getDirectory().getDefaultType() + "|" + this.baseGraph.nodeAccess.getDimension() + "D" + "|" + this.baseGraph.extStorage + "|" + this.getProperties().versionsToString();
   }

   public Graph getBaseGraph() {
      return this.baseGraph;
   }

   public final int getNodes() {
      return this.baseGraph.getNodes();
   }

   public final NodeAccess getNodeAccess() {
      return this.baseGraph.getNodeAccess();
   }

   public final BBox getBounds() {
      return this.baseGraph.getBounds();
   }

   public final EdgeIteratorState edge(int a, int b) {
      return this.baseGraph.edge(a, b);
   }

   public final EdgeIteratorState edge(int a, int b, double distance, boolean bothDirections) {
      return this.baseGraph.edge(a, b, distance, bothDirections);
   }

   public final EdgeIteratorState getEdgeIteratorState(int edgeId, int adjNode) {
      return this.baseGraph.getEdgeIteratorState(edgeId, adjNode);
   }

   public final AllEdgesIterator getAllEdges() {
      return this.baseGraph.getAllEdges();
   }

   public final EdgeExplorer createEdgeExplorer(EdgeFilter filter) {
      return this.baseGraph.createEdgeExplorer(filter);
   }

   public final EdgeExplorer createEdgeExplorer() {
      return this.baseGraph.createEdgeExplorer();
   }

   public final Graph copyTo(Graph g) {
      return this.baseGraph.copyTo(g);
   }

   public final GraphExtension getExtension() {
      return this.baseGraph.getExtension();
   }
}
