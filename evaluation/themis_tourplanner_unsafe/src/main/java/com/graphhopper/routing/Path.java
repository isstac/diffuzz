package com.graphhopper.routing;

import com.graphhopper.routing.util.DefaultEdgeFilter;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.storage.EdgeEntry;
import com.graphhopper.storage.Graph;
import com.graphhopper.storage.NodeAccess;
import com.graphhopper.util.AngleCalc;
import com.graphhopper.util.EdgeExplorer;
import com.graphhopper.util.EdgeIterator;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.FinishInstruction;
import com.graphhopper.util.Instruction;
import com.graphhopper.util.InstructionAnnotation;
import com.graphhopper.util.InstructionList;
import com.graphhopper.util.PointList;
import com.graphhopper.util.RoundaboutInstruction;
import com.graphhopper.util.StopWatch;
import com.graphhopper.util.Translation;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import java.util.ArrayList;
import java.util.List;

public class Path {
   private static final AngleCalc ac = new AngleCalc();
   protected Graph graph;
   private FlagEncoder encoder;
   protected double distance;
   protected boolean reverseOrder;
   protected long time;
   private boolean found;
   protected EdgeEntry edgeEntry;
   final StopWatch extractSW;
   private int fromNode;
   protected int endNode;
   private TIntList edgeIds;
   private double weight;
   private NodeAccess nodeAccess;

   public Path(Graph graph, FlagEncoder encoder) {
      this.reverseOrder = true;
      this.extractSW = new StopWatch("extract");
      this.fromNode = -1;
      this.endNode = -1;
      this.weight = Double.MAX_VALUE;
      this.graph = graph;
      this.nodeAccess = graph.getNodeAccess();
      this.encoder = encoder;
      this.edgeIds = new TIntArrayList();
   }

   Path(Path p) {
      this(p.graph, p.encoder);
      this.weight = p.weight;
      this.edgeIds = new TIntArrayList(p.edgeIds);
      this.edgeEntry = p.edgeEntry;
   }

   public Path setEdgeEntry(EdgeEntry edgeEntry) {
      this.edgeEntry = edgeEntry;
      return this;
   }

   protected void addEdge(int edge) {
      this.edgeIds.add(edge);
   }

   protected Path setEndNode(int end) {
      this.endNode = end;
      return this;
   }

   protected Path setFromNode(int from) {
      this.fromNode = from;
      return this;
   }

   private int getFromNode() {
      if(this.fromNode < 0) {
         throw new IllegalStateException("Call extract() before retrieving fromNode");
      } else {
         return this.fromNode;
      }
   }

   public boolean isFound() {
      return this.found;
   }

   public Path setFound(boolean found) {
      this.found = found;
      return this;
   }

   void reverseOrder() {
      if(!this.reverseOrder) {
         throw new IllegalStateException("Switching order multiple times is not supported");
      } else {
         this.reverseOrder = false;
         this.edgeIds.reverse();
      }
   }

   public double getDistance() {
      return this.distance;
   }

   /** @deprecated */
   public long getMillis() {
      return this.time;
   }

   public long getTime() {
      return this.time;
   }

   public double getWeight() {
      return this.weight;
   }

   public Path setWeight(double w) {
      this.weight = w;
      return this;
   }

   public Path extract() {
      if(this.isFound()) {
         throw new IllegalStateException("Extract can only be called once");
      } else {
         this.extractSW.start();
         EdgeEntry goalEdge = this.edgeEntry;
         this.setEndNode(goalEdge.adjNode);

         while(EdgeIterator.Edge.isValid(goalEdge.edge)) {
            this.processEdge(goalEdge.edge, goalEdge.adjNode);
            goalEdge = goalEdge.parent;
         }

         this.setFromNode(goalEdge.adjNode);
         this.reverseOrder();
         this.extractSW.stop();
         return this.setFound(true);
      }
   }

   public EdgeIteratorState getFinalEdge() {
      return this.graph.getEdgeIteratorState(this.edgeIds.get(this.edgeIds.size() - 1), this.endNode);
   }

   public long getExtractTime() {
      return this.extractSW.getNanos();
   }

   public String getDebugInfo() {
      return this.extractSW.toString();
   }

   protected void processEdge(int edgeId, int adjNode) {
      EdgeIteratorState iter = this.graph.getEdgeIteratorState(edgeId, adjNode);
      double dist = iter.getDistance();
      this.distance += dist;
      this.time += this.calcMillis(dist, iter.getFlags(), false);
      this.addEdge(edgeId);
   }

   protected long calcMillis(double distance, long flags, boolean revert) {
      if((!revert || this.encoder.isBackward(flags)) && (revert || this.encoder.isForward(flags))) {
         double speed = revert?this.encoder.getReverseSpeed(flags):this.encoder.getSpeed(flags);
         if(!Double.isInfinite(speed) && !Double.isNaN(speed) && speed >= 0.0D) {
            if(speed == 0.0D) {
               throw new IllegalStateException("Speed cannot be 0 for unblocked edge, use access properties to mark edge blocked! Should only occur for shortest path calculation. See #242.");
            } else {
               return (long)(distance * 3600.0D / speed);
            }
         } else {
            throw new IllegalStateException("Invalid speed stored in edge! " + speed);
         }
      } else {
         throw new IllegalStateException("Calculating time should not require to read speed from edge in wrong direction. Reverse:" + revert + ", fwd:" + this.encoder.isForward(flags) + ", bwd:" + this.encoder.isBackward(flags));
      }
   }

   private void forEveryEdge(Path.EdgeVisitor visitor) {
      int tmpNode = this.getFromNode();
      int len = this.edgeIds.size();

      for(int i = 0; i < len; ++i) {
         EdgeIteratorState edgeBase = this.graph.getEdgeIteratorState(this.edgeIds.get(i), tmpNode);
         if(edgeBase == null) {
            throw new IllegalStateException("Edge " + this.edgeIds.get(i) + " was empty when requested with node " + tmpNode + ", array index:" + i + ", edges:" + this.edgeIds.size());
         }

         tmpNode = edgeBase.getBaseNode();
         edgeBase = this.graph.getEdgeIteratorState(edgeBase.getEdge(), tmpNode);
         visitor.next(edgeBase, i);
      }

   }

   public List calcEdges() {
      final ArrayList edges = new ArrayList(this.edgeIds.size());
      if(this.edgeIds.isEmpty()) {
         return edges;
      } else {
         this.forEveryEdge(new Path.EdgeVisitor() {
            public void next(EdgeIteratorState eb, int i) {
               edges.add(eb);
            }
         });
         return edges;
      }
   }

   public TIntList calcNodes() {
      final TIntArrayList nodes = new TIntArrayList(this.edgeIds.size() + 1);
      if(this.edgeIds.isEmpty()) {
         if(this.isFound()) {
            nodes.add(this.endNode);
         }

         return nodes;
      } else {
         int tmpNode = this.getFromNode();
         nodes.add(tmpNode);
         this.forEveryEdge(new Path.EdgeVisitor() {
            public void next(EdgeIteratorState eb, int i) {
               nodes.add(eb.getAdjNode());
            }
         });
         return nodes;
      }
   }

   public PointList calcPoints() {
      final PointList points = new PointList(this.edgeIds.size() + 1, this.nodeAccess.is3D());
      if(this.edgeIds.isEmpty()) {
         if(this.isFound()) {
            points.add(this.graph.getNodeAccess(), this.endNode);
         }

         return points;
      } else {
         int tmpNode = this.getFromNode();
         points.add(this.nodeAccess, tmpNode);
         this.forEveryEdge(new Path.EdgeVisitor() {
            public void next(EdgeIteratorState eb, int index) {
               PointList pl = eb.fetchWayGeometry(2);

               for(int j = 0; j < pl.getSize(); ++j) {
                  points.add(pl, j);
               }

            }
         });
         return points;
      }
   }

   public InstructionList calcInstructions(final Translation tr) {
      final InstructionList ways = new InstructionList(this.edgeIds.size() / 4, tr);
      if(this.edgeIds.isEmpty()) {
         if(this.isFound()) {
            ways.add(new FinishInstruction(this.nodeAccess, this.endNode));
         }

         return ways;
      } else {
         final int tmpNode = this.getFromNode();
         this.forEveryEdge(new Path.EdgeVisitor() {
            private double prevLat;
            private double prevLon;
            private double doublePrevLat;
            private double doublePrevLong;
            private int prevNode;
            private double prevOrientation;
            private Instruction prevInstruction;
            private boolean prevInRoundabout;
            private String name;
            private String prevName;
            private InstructionAnnotation annotation;
            private InstructionAnnotation prevAnnotation;
            private EdgeExplorer outEdgeExplorer;

            {
               this.prevLat = Path.this.nodeAccess.getLatitude(tmpNode);
               this.prevLon = Path.this.nodeAccess.getLongitude(tmpNode);
               this.prevNode = -1;
               this.prevInRoundabout = false;
               this.prevName = null;
               this.outEdgeExplorer = Path.this.graph.createEdgeExplorer(new DefaultEdgeFilter(Path.this.encoder, false, true));
            }

            public void next(EdgeIteratorState edge, int index) {
               int adjNode = edge.getAdjNode();
               int baseNode = edge.getBaseNode();
               long flags = edge.getFlags();
               double adjLat = Path.this.nodeAccess.getLatitude(adjNode);
               double adjLon = Path.this.nodeAccess.getLongitude(adjNode);
               PointList wayGeo = edge.fetchWayGeometry(3);
               boolean isRoundabout = Path.this.encoder.isBool(flags, 2);
               double latitude;
               double longitude;
               if(wayGeo.getSize() <= 2) {
                  latitude = adjLat;
                  longitude = adjLon;
               } else {
                  latitude = wayGeo.getLatitude(1);
                  longitude = wayGeo.getLongitude(1);

                  assert Double.compare(this.prevLat, Path.this.nodeAccess.getLatitude(baseNode)) == 0;

                  assert Double.compare(this.prevLon, Path.this.nodeAccess.getLongitude(baseNode)) == 0;
               }

               this.name = edge.getName();
               this.annotation = Path.this.encoder.getAnnotation(flags, tr);
               byte lastEdge;
               double delta1;
               if(this.prevName == null && !isRoundabout) {
                  lastEdge = 0;
                  this.prevInstruction = new Instruction(lastEdge, this.name, this.annotation, new PointList(10, Path.this.nodeAccess.is3D()));
                  ways.add(this.prevInstruction);
                  this.prevName = this.name;
                  this.prevAnnotation = this.annotation;
               } else if(isRoundabout) {
                  if(!this.prevInRoundabout) {
                     lastEdge = 6;
                     RoundaboutInstruction orientation = new RoundaboutInstruction(lastEdge, this.name, this.annotation, new PointList(10, Path.this.nodeAccess.is3D()));
                     if(this.prevName != null) {
                        EdgeIterator delta = this.outEdgeExplorer.setBaseNode(baseNode);

                        while(delta.next()) {
                           if(delta.getAdjNode() != this.prevNode && !Path.this.encoder.isBool(delta.getFlags(), 2)) {
                              orientation.increaseExitNumber();
                              break;
                           }
                        }

                        this.prevOrientation = Path.ac.calcOrientation(this.doublePrevLat, this.doublePrevLong, this.prevLat, this.prevLon);
                        delta1 = Path.ac.calcOrientation(this.prevLat, this.prevLon, latitude, longitude);
                        delta1 = Path.ac.alignOrientation(this.prevOrientation, delta1);
                        double delta2 = delta1 - this.prevOrientation;
                        orientation.setDirOfRotation(delta2);
                     } else {
                        this.prevOrientation = Path.ac.calcOrientation(this.prevLat, this.prevLon, latitude, longitude);
                        this.prevName = this.name;
                        this.prevAnnotation = this.annotation;
                     }

                     this.prevInstruction = orientation;
                     ways.add(this.prevInstruction);
                  }

                  EdgeIterator lastEdge1 = this.outEdgeExplorer.setBaseNode(adjNode);

                  while(lastEdge1.next()) {
                     if(!Path.this.encoder.isBool(lastEdge1.getFlags(), 2)) {
                        ((RoundaboutInstruction)this.prevInstruction).increaseExitNumber();
                        break;
                     }
                  }
               } else {
                  double absDelta;
                  double lastEdge2;
                  double delta3;
                  if(this.prevInRoundabout) {
                     this.prevInstruction.setName(this.name);
                     lastEdge2 = Path.ac.calcOrientation(this.prevLat, this.prevLon, latitude, longitude);
                     lastEdge2 = Path.ac.alignOrientation(this.prevOrientation, lastEdge2);
                     delta3 = lastEdge2 - this.prevOrientation;
                     absDelta = Path.ac.calcOrientation(this.doublePrevLat, this.doublePrevLong, this.prevLat, this.prevLon);
                     lastEdge2 = Path.ac.alignOrientation(absDelta, lastEdge2);
                     double sign = lastEdge2 - absDelta;
                     this.prevInstruction = ((RoundaboutInstruction)this.prevInstruction).setRadian(delta3).setDirOfRotation(sign).setExited();
                     this.prevName = this.name;
                     this.prevAnnotation = this.annotation;
                  } else if(!this.name.equals(this.prevName) || !this.annotation.equals(this.prevAnnotation)) {
                     this.prevOrientation = Path.ac.calcOrientation(this.doublePrevLat, this.doublePrevLong, this.prevLat, this.prevLon);
                     lastEdge2 = Path.ac.calcOrientation(this.prevLat, this.prevLon, latitude, longitude);
                     lastEdge2 = Path.ac.alignOrientation(this.prevOrientation, lastEdge2);
                     delta3 = lastEdge2 - this.prevOrientation;
                     absDelta = Math.abs(delta3);
                     byte sign1;
                     if(absDelta < 0.2D) {
                        sign1 = 0;
                     } else if(absDelta < 0.8D) {
                        if(delta3 > 0.0D) {
                           sign1 = -1;
                        } else {
                           sign1 = 1;
                        }
                     } else if(absDelta < 1.8D) {
                        if(delta3 > 0.0D) {
                           sign1 = -2;
                        } else {
                           sign1 = 2;
                        }
                     } else if(delta3 > 0.0D) {
                        sign1 = -3;
                     } else {
                        sign1 = 3;
                     }

                     this.prevInstruction = new Instruction(sign1, this.name, this.annotation, new PointList(10, Path.this.nodeAccess.is3D()));
                     ways.add(this.prevInstruction);
                     this.prevName = this.name;
                     this.prevAnnotation = this.annotation;
                  }
               }

               this.updatePointsAndInstruction(edge, wayGeo);
               if(wayGeo.getSize() <= 2) {
                  this.doublePrevLat = this.prevLat;
                  this.doublePrevLong = this.prevLon;
               } else {
                  int lastEdge3 = wayGeo.getSize() - 2;
                  this.doublePrevLat = wayGeo.getLatitude(lastEdge3);
                  this.doublePrevLong = wayGeo.getLongitude(lastEdge3);
               }

               this.prevInRoundabout = isRoundabout;
               this.prevNode = baseNode;
               this.prevLat = adjLat;
               this.prevLon = adjLon;
               boolean lastEdge4 = index == Path.this.edgeIds.size() - 1;
               if(lastEdge4) {
                  if(isRoundabout) {
                     double orientation1 = Path.ac.calcOrientation(this.doublePrevLat, this.doublePrevLong, this.prevLat, this.prevLon);
                     orientation1 = Path.ac.alignOrientation(this.prevOrientation, orientation1);
                     delta1 = orientation1 - this.prevOrientation;
                     ((RoundaboutInstruction)this.prevInstruction).setRadian(delta1);
                  }

                  ways.add(new FinishInstruction(Path.this.nodeAccess, adjNode));
               }

            }

            private void updatePointsAndInstruction(EdgeIteratorState edge, PointList pl) {
               int len = pl.size() - 1;

               for(int newDist = 0; newDist < len; ++newDist) {
                  this.prevInstruction.getPoints().add(pl, newDist);
               }

               double var8 = edge.getDistance();
               this.prevInstruction.setDistance(var8 + this.prevInstruction.getDistance());
               long flags = edge.getFlags();
               this.prevInstruction.setTime(Path.this.calcMillis(var8, flags, false) + this.prevInstruction.getTime());
            }
         });
         return ways;
      }
   }

   public String toString() {
      return "distance:" + this.getDistance() + ", edges:" + this.edgeIds.size();
   }

   public String toDetailsString() {
      String str = "";

      for(int i = 0; i < this.edgeIds.size(); ++i) {
         if(i > 0) {
            str = str + "->";
         }

         str = str + this.edgeIds.get(i);
      }

      return this.toString() + ", found:" + this.isFound() + ", " + str;
   }

   private interface EdgeVisitor {
      void next(EdgeIteratorState var1, int var2);
   }
}
