package com.graphhopper.tour.util;

import com.graphhopper.tour.util.Edge;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Graph {
   private final Set vertices = new HashSet();
   private final Set edges = new HashSet();
   private final Map edgesFrom = new HashMap();

   public int size() {
      return this.vertices.size();
   }

   public boolean contains(Object v) {
      return this.vertices.contains(v);
   }

   public boolean contains(Edge e) {
      return this.edges.contains(e);
   }

   public Set vertices() {
      return Collections.unmodifiableSet(this.vertices);
   }

   public Set edges() {
      return Collections.unmodifiableSet(this.edges);
   }

   public Graph add(Object v) {
      this.vertices.add(v);
      return this;
   }

   public Graph add(Edge e) {
      this.vertices.add(e.from);
      this.vertices.add(e.to);
      this.edges.add(e);
      Object el = (List)this.edgesFrom.get(e.from);
      if(el == null) {
         el = new ArrayList();
         this.edgesFrom.put(e.from, el);
      }

      ((List)el).add(e);
      return this;
   }

   public List edgesFrom(Object from) {
      List el = (List)this.edgesFrom.get(from);
      return el == null?el:Collections.unmodifiableList(el);
   }

   public List depthFirstWalk(Object root) {
      final ArrayList result = new ArrayList();
      this.depthFirstWalk(root, new Graph.Visitor() {
         public void visit(Object vertex) {
            result.add(vertex);
         }
      });
      return result;
   }

   public void depthFirstWalk(Object root, Graph.Visitor visitor) {
      HashSet visited = new HashSet();
      this.depthFirstWalk(root, visitor, visited);
   }

   private void depthFirstWalk(Object from, Graph.Visitor visitor, Set visited) {
      visitor.visit(from);
      visited.add(from);
      List el = (List)this.edgesFrom.get(from);
      if(el != null) {
         Iterator i$ = el.iterator();

         while(i$.hasNext()) {
            Edge e = (Edge)i$.next();
            if(!visited.contains(e.to)) {
               this.depthFirstWalk(e.to, visitor, visited);
            }
         }

      }
   }

   public interface Visitor {
      void visit(Object var1);
   }
}
