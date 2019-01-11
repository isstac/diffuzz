package com.graphhopper.util;

import com.graphhopper.coll.GHBitSet;
import com.graphhopper.coll.GHBitSetImpl;
import com.graphhopper.routing.util.AllCHEdgesIterator;
import com.graphhopper.routing.util.AllEdgesIterator;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.storage.CHGraph;
import com.graphhopper.storage.Directory;
import com.graphhopper.storage.GHDirectory;
import com.graphhopper.storage.Graph;
import com.graphhopper.storage.GraphHopperStorage;
import com.graphhopper.storage.GraphStorage;
import com.graphhopper.storage.MMapDirectory;
import com.graphhopper.storage.NodeAccess;
import com.graphhopper.storage.RAMDirectory;
import com.graphhopper.util.BitUtil;
import com.graphhopper.util.BreadthFirstSearch;
import com.graphhopper.util.CHEdgeExplorer;
import com.graphhopper.util.CHEdgeIterator;
import com.graphhopper.util.CHEdgeIteratorState;
import com.graphhopper.util.DepthFirstSearch;
import com.graphhopper.util.EdgeExplorer;
import com.graphhopper.util.EdgeIterator;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.PointList;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class GHUtility {
   public static List getProblems(Graph g) {
      ArrayList problems = new ArrayList();
      int nodes = g.getNodes();
      int nodeIndex = 0;
      NodeAccess na = g.getNodeAccess();

      try {
         for(EdgeExplorer ex = g.createEdgeExplorer(); nodeIndex < nodes; ++nodeIndex) {
            double lat = na.getLatitude(nodeIndex);
            if(lat > 90.0D || lat < -90.0D) {
               problems.add("latitude is not within its bounds " + lat);
            }

            double lon = na.getLongitude(nodeIndex);
            if(lon > 180.0D || lon < -180.0D) {
               problems.add("longitude is not within its bounds " + lon);
            }

            EdgeIterator iter = ex.setBaseNode(nodeIndex);

            while(iter.next()) {
               if(iter.getAdjNode() >= nodes) {
                  problems.add("edge of " + nodeIndex + " has a node " + iter.getAdjNode() + " greater or equal to getNodes");
               }

               if(iter.getAdjNode() < 0) {
                  problems.add("edge of " + nodeIndex + " has a negative node " + iter.getAdjNode());
               }
            }
         }

         return problems;
      } catch (Exception var11) {
         throw new RuntimeException("problem with node " + nodeIndex, var11);
      }
   }

   public static int count(EdgeIterator iter) {
      int counter;
      for(counter = 0; iter.next(); ++counter) {
         ;
      }

      return counter;
   }

   public static Set asSet(int... values) {
      HashSet s = new HashSet();
      int[] arr$ = values;
      int len$ = values.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         int v = arr$[i$];
         s.add(Integer.valueOf(v));
      }

      return s;
   }

   public static Set getNeighbors(EdgeIterator iter) {
      LinkedHashSet list = new LinkedHashSet();

      while(iter.next()) {
         list.add(Integer.valueOf(iter.getAdjNode()));
      }

      return list;
   }

   public static List getEdgeIds(EdgeIterator iter) {
      ArrayList list = new ArrayList();

      while(iter.next()) {
         list.add(Integer.valueOf(iter.getEdge()));
      }

      return list;
   }

   public static void printEdgeInfo(Graph g, FlagEncoder encoder) {
      System.out.println("-- Graph n:" + g.getNodes() + " e:" + g.getAllEdges().getMaxId() + " ---");
      AllEdgesIterator iter = g.getAllEdges();

      while(iter.next()) {
         String sc = "";
         if(iter instanceof AllCHEdgesIterator) {
            AllCHEdgesIterator fwdStr = (AllCHEdgesIterator)iter;
            sc = fwdStr.isShortcut()?"sc":"  ";
         }

         String fwdStr1 = iter.isForward(encoder)?"fwd":"   ";
         String bckStr = iter.isBackward(encoder)?"bckwd":"";
         System.out.println(sc + " " + iter + " " + fwdStr1 + " " + bckStr);
      }

   }

   public static void printInfo(final Graph g, int startNode, final int counts, final EdgeFilter filter) {
      (new BreadthFirstSearch() {
         int counter = 0;

         protected boolean goFurther(int nodeId) {
            System.out.println(GHUtility.getNodeInfo(g, nodeId, filter));
            return this.counter++ <= counts;
         }
      }).start(g.createEdgeExplorer(), startNode);
   }

   public static String getNodeInfo(CHGraph g, int nodeId, EdgeFilter filter) {
      CHEdgeExplorer ex = g.createEdgeExplorer(filter);
      CHEdgeIterator iter = ex.setBaseNode(nodeId);
      NodeAccess na = g.getNodeAccess();

      String str;
      for(str = nodeId + ":" + na.getLatitude(nodeId) + "," + na.getLongitude(nodeId) + "\n"; iter.next(); str = str + "  ->" + iter.getAdjNode() + "(" + iter.getSkippedEdge1() + "," + iter.getSkippedEdge2() + ") " + iter.getEdge() + " \t" + BitUtil.BIG.toBitString(iter.getFlags(), 8) + "\n") {
         ;
      }

      return str;
   }

   public static String getNodeInfo(Graph g, int nodeId, EdgeFilter filter) {
      EdgeIterator iter = g.createEdgeExplorer(filter).setBaseNode(nodeId);
      NodeAccess na = g.getNodeAccess();

      String str;
      for(str = nodeId + ":" + na.getLatitude(nodeId) + "," + na.getLongitude(nodeId) + "\n"; iter.next(); str = str + "  ->" + iter.getAdjNode() + " (" + iter.getDistance() + ") pillars:" + iter.fetchWayGeometry(0).getSize() + ", edgeId:" + iter.getEdge() + "\t" + BitUtil.BIG.toBitString(iter.getFlags(), 8) + "\n") {
         ;
      }

      return str;
   }

   public static Graph shuffle(Graph g, Graph sortedGraph) {
      int len = g.getNodes();
      TIntArrayList list = new TIntArrayList(len, -1);
      list.fill(0, len, -1);

      for(int i = 0; i < len; ++i) {
         list.set(i, i);
      }

      list.shuffle(new Random());
      return createSortedGraph(g, sortedGraph, list);
   }

   public static Graph sortDFS(Graph g, Graph sortedGraph) {
      final TIntArrayList list = new TIntArrayList(g.getNodes(), -1);
      int nodes = g.getNodes();
      list.fill(0, nodes, -1);
      final GHBitSetImpl bitset = new GHBitSetImpl(nodes);
      final AtomicInteger ref = new AtomicInteger(-1);
      EdgeExplorer explorer = g.createEdgeExplorer();

      for(int startNode = 0; startNode >= 0 && startNode < nodes; startNode = bitset.nextClear(startNode + 1)) {
         (new DepthFirstSearch() {
            protected GHBitSet createBitSet() {
               return bitset;
            }

            protected boolean goFurther(int nodeId) {
               list.set(nodeId, ref.incrementAndGet());
               return super.goFurther(nodeId);
            }
         }).start(explorer, startNode);
      }

      return createSortedGraph(g, sortedGraph, list);
   }

   static Graph createSortedGraph(Graph fromGraph, Graph toSortedGraph, TIntList oldToNewNodeList) {
      AllEdgesIterator eIter = fromGraph.getAllEdges();

      int nodes;
      int old;
      while(eIter.next()) {
         nodes = eIter.getBaseNode();
         int na = oldToNewNodeList.get(nodes);
         int sna = eIter.getAdjNode();
         old = oldToNewNodeList.get(sna);
         if(na >= 0 && old >= 0) {
            eIter.copyPropertiesTo(toSortedGraph.edge(na, old));
         }
      }

      nodes = fromGraph.getNodes();
      NodeAccess var9 = fromGraph.getNodeAccess();
      NodeAccess var10 = toSortedGraph.getNodeAccess();

      for(old = 0; old < nodes; ++old) {
         int newIndex = oldToNewNodeList.get(old);
         if(var10.is3D()) {
            var10.setNode(newIndex, var9.getLatitude(old), var9.getLongitude(old), var9.getElevation(old));
         } else {
            var10.setNode(newIndex, var9.getLatitude(old), var9.getLongitude(old));
         }
      }

      return toSortedGraph;
   }

   public static Graph copyTo(Graph fromGraph, Graph toGraph) {
      AllEdgesIterator eIter = fromGraph.getAllEdges();

      while(eIter.next()) {
         int fna = eIter.getBaseNode();
         int tna = eIter.getAdjNode();
         eIter.copyPropertiesTo(toGraph.edge(fna, tna));
      }

      NodeAccess var7 = fromGraph.getNodeAccess();
      NodeAccess var8 = toGraph.getNodeAccess();
      int nodes = fromGraph.getNodes();

      for(int node = 0; node < nodes; ++node) {
         if(var8.is3D()) {
            var8.setNode(node, var7.getLatitude(node), var7.getLongitude(node), var7.getElevation(node));
         } else {
            var8.setNode(node, var7.getLatitude(node), var7.getLongitude(node));
         }
      }

      return toGraph;
   }

   static Directory guessDirectory(GraphStorage store) {
      String location = store.getDirectory().getLocation();
      if(store.getDirectory() instanceof MMapDirectory) {
         throw new IllegalStateException("not supported yet: mmap will overwrite existing storage at the same location");
      } else {
         boolean isStoring = ((GHDirectory)store.getDirectory()).isStoring();
         RAMDirectory outdir = new RAMDirectory(location, isStoring);
         return outdir;
      }
   }

   public static GraphHopperStorage newStorage(GraphHopperStorage store) {
      Directory outdir = guessDirectory(store);
      boolean is3D = store.getNodeAccess().is3D();
      return (new GraphHopperStorage(store.getCHWeightings(), outdir, store.getEncodingManager(), is3D, store.getExtension())).create((long)store.getNodes());
   }

   public static int getAdjNode(Graph g, int edge, int adjNode) {
      if(EdgeIterator.Edge.isValid(edge)) {
         EdgeIteratorState iterTo = g.getEdgeIteratorState(edge, adjNode);
         return iterTo.getAdjNode();
      } else {
         return adjNode;
      }
   }

   public static EdgeIteratorState getEdge(Graph graph, int base, int adj) {
      EdgeIterator iter = graph.createEdgeExplorer().setBaseNode(base);

      do {
         if(!iter.next()) {
            return null;
         }
      } while(iter.getAdjNode() != adj);

      return iter;
   }

   public static int createEdgeKey(int nodeA, int nodeB, int edgeId, boolean reverse) {
      edgeId <<= 1;
      return reverse?(nodeA > nodeB?edgeId:edgeId + 1):(nodeA > nodeB?edgeId + 1:edgeId);
   }

   public static boolean isSameEdgeKeys(int edgeKey1, int edgeKey2) {
      return edgeKey1 / 2 == edgeKey2 / 2;
   }

   public static int reverseEdgeKey(int edgeKey) {
      return edgeKey % 2 == 0?edgeKey + 1:edgeKey - 1;
   }

   public static class DisabledEdgeIterator implements CHEdgeIterator {
      public EdgeIterator detach(boolean reverse) {
         throw new UnsupportedOperationException("Not supported. Edge is empty.");
      }

      public EdgeIteratorState setDistance(double dist) {
         throw new UnsupportedOperationException("Not supported. Edge is empty.");
      }

      public EdgeIteratorState setFlags(long flags) {
         throw new UnsupportedOperationException("Not supported. Edge is empty.");
      }

      public boolean next() {
         throw new UnsupportedOperationException("Not supported. Edge is empty.");
      }

      public int getEdge() {
         throw new UnsupportedOperationException("Not supported. Edge is empty.");
      }

      public int getBaseNode() {
         throw new UnsupportedOperationException("Not supported. Edge is empty.");
      }

      public int getAdjNode() {
         throw new UnsupportedOperationException("Not supported. Edge is empty.");
      }

      public double getDistance() {
         throw new UnsupportedOperationException("Not supported. Edge is empty.");
      }

      public long getFlags() {
         throw new UnsupportedOperationException("Not supported. Edge is empty.");
      }

      public PointList fetchWayGeometry(int type) {
         throw new UnsupportedOperationException("Not supported. Edge is empty.");
      }

      public EdgeIteratorState setWayGeometry(PointList list) {
         throw new UnsupportedOperationException("Not supported. Edge is empty.");
      }

      public String getName() {
         throw new UnsupportedOperationException("Not supported. Edge is empty.");
      }

      public EdgeIteratorState setName(String name) {
         throw new UnsupportedOperationException("Not supported. Edge is empty.");
      }

      public boolean getBoolean(int key, boolean reverse, boolean _default) {
         throw new UnsupportedOperationException("Not supported. Edge is empty.");
      }

      public boolean isBackward(FlagEncoder encoder) {
         throw new UnsupportedOperationException("Not supported. Edge is empty.");
      }

      public boolean isForward(FlagEncoder encoder) {
         throw new UnsupportedOperationException("Not supported. Edge is empty.");
      }

      public int getAdditionalField() {
         throw new UnsupportedOperationException("Not supported. Edge is empty.");
      }

      public EdgeIteratorState setAdditionalField(int value) {
         throw new UnsupportedOperationException("Not supported. Edge is empty.");
      }

      public EdgeIteratorState copyPropertiesTo(EdgeIteratorState edge) {
         throw new UnsupportedOperationException("Not supported. Edge is empty.");
      }

      public boolean isShortcut() {
         return false;
      }

      public int getSkippedEdge1() {
         throw new UnsupportedOperationException("Not supported. Edge is empty.");
      }

      public int getSkippedEdge2() {
         throw new UnsupportedOperationException("Not supported. Edge is empty.");
      }

      public void setSkippedEdges(int edge1, int edge2) {
         throw new UnsupportedOperationException("Not supported. Edge is empty.");
      }

      public double getWeight() {
         throw new UnsupportedOperationException("Not supported. Edge is empty.");
      }

      public CHEdgeIteratorState setWeight(double weight) {
         throw new UnsupportedOperationException("Not supported. Edge is empty.");
      }

      public boolean canBeOverwritten(long flags) {
         throw new UnsupportedOperationException("Not supported. Edge is empty.");
      }
   }
}
