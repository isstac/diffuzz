package com.graphhopper.storage;

import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.util.Weighting;
import com.graphhopper.storage.CHGraph;
import com.graphhopper.storage.Directory;
import com.graphhopper.storage.GraphExtension;
import com.graphhopper.storage.GraphHopperStorage;
import com.graphhopper.storage.MMapDirectory;
import com.graphhopper.storage.RAMDirectory;
import com.graphhopper.storage.TurnCostExtension;
import java.util.Arrays;

public class GraphBuilder {
   private final EncodingManager encodingManager;
   private String location;
   private boolean mmap;
   private boolean store;
   private boolean elevation;
   private long byteCapacity = 100L;
   private Weighting singleCHWeighting;

   public GraphBuilder(EncodingManager encodingManager) {
      this.encodingManager = encodingManager;
   }

   public GraphBuilder setCHGraph(Weighting singleCHWeighting) {
      this.singleCHWeighting = singleCHWeighting;
      return this;
   }

   public GraphBuilder setLocation(String location) {
      this.location = location;
      return this;
   }

   public GraphBuilder setStore(boolean store) {
      this.store = store;
      return this;
   }

   public GraphBuilder setMmap(boolean mmap) {
      this.mmap = mmap;
      return this;
   }

   public GraphBuilder setExpectedSize(byte cap) {
      this.byteCapacity = (long)cap;
      return this;
   }

   public GraphBuilder set3D(boolean withElevation) {
      this.elevation = withElevation;
      return this;
   }

   public boolean hasElevation() {
      return this.elevation;
   }

   public CHGraph chGraphCreate(Weighting singleCHWeighting) {
      return (CHGraph)this.setCHGraph(singleCHWeighting).create().getGraph(CHGraph.class, singleCHWeighting);
   }

   public GraphHopperStorage build() {
      Object dir;
      if(this.mmap) {
         dir = new MMapDirectory(this.location);
      } else {
         dir = new RAMDirectory(this.location, this.store);
      }

      GraphHopperStorage graph;
      if(!this.encodingManager.needsTurnCostsSupport() && this.singleCHWeighting != null) {
         graph = new GraphHopperStorage(Arrays.asList(new Weighting[]{this.singleCHWeighting}), (Directory)dir, this.encodingManager, this.elevation, new GraphExtension.NoOpExtension());
      } else {
         graph = new GraphHopperStorage((Directory)dir, this.encodingManager, this.elevation, new TurnCostExtension());
      }

      return graph;
   }

   public GraphHopperStorage create() {
      return this.build().create(this.byteCapacity);
   }

   public GraphHopperStorage load() {
      GraphHopperStorage gs = this.build();
      if(!gs.loadExisting()) {
    	  System.out.println("Cannot load graph " + this.location);
         throw new IllegalStateException("Cannot load graph " + this.location);
      } else {
         return gs;
      }
   }
}
