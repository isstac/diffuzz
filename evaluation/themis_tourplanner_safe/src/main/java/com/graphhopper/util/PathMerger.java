package com.graphhopper.util;

import com.graphhopper.GHResponse;
import com.graphhopper.routing.Path;
import com.graphhopper.util.DouglasPeucker;
import com.graphhopper.util.Instruction;
import com.graphhopper.util.InstructionList;
import com.graphhopper.util.PointList;
import com.graphhopper.util.Translation;
import com.graphhopper.util.ViaInstruction;
import java.util.Iterator;
import java.util.List;

public class PathMerger {
   private boolean enableInstructions = true;
   private boolean simplifyResponse = true;
   private DouglasPeucker douglasPeucker;
   private boolean calcPoints = true;

   public void doWork(GHResponse rsp, List paths, Translation tr) {
      int origPoints = 0;
      long fullTimeInMillis = 0L;
      double fullWeight = 0.0D;
      double fullDistance = 0.0D;
      boolean allFound = true;
      InstructionList fullInstructions = new InstructionList(tr);
      PointList fullPoints = PointList.EMPTY;

      for(int debug = 0; debug < paths.size(); ++debug) {
         Path path = (Path)paths.get(debug);
         fullTimeInMillis += path.getTime();
         fullDistance += path.getDistance();
         fullWeight += path.getWeight();
         if(!this.enableInstructions) {
            if(this.calcPoints) {
               PointList var20 = path.calcPoints();
               if(fullPoints.isEmpty()) {
                  fullPoints = new PointList(var20.size(), var20.is3D());
               }

               if(this.simplifyResponse) {
                  origPoints = var20.getSize();
                  this.douglasPeucker.simplify(var20);
               }

               fullPoints.add(var20);
            }
         } else {
            InstructionList tmpPoints = path.calcInstructions(tr);
            if(!tmpPoints.isEmpty()) {
               if(fullPoints.isEmpty()) {
                  PointList newInstr = tmpPoints.get(0).getPoints();
                  fullPoints = new PointList(tmpPoints.size() * Math.min(10, newInstr.size()), newInstr.is3D());
               }

               Iterator var21 = tmpPoints.iterator();

               while(var21.hasNext()) {
                  Instruction i = (Instruction)var21.next();
                  if(this.simplifyResponse) {
                     origPoints += i.getPoints().size();
                     this.douglasPeucker.simplify(i.getPoints());
                  }

                  fullInstructions.add(i);
                  fullPoints.add(i.getPoints());
               }

               if(debug + 1 < paths.size()) {
                  ViaInstruction var22 = new ViaInstruction(fullInstructions.get(fullInstructions.size() - 1));
                  var22.setViaCount(debug + 1);
                  fullInstructions.replaceLast(var22);
               }
            }
         }

         allFound = allFound && path.isFound();
      }

      if(!fullPoints.isEmpty()) {
         String var19 = rsp.getDebugInfo() + ", simplify (" + origPoints + "->" + fullPoints.getSize() + ")";
         rsp.setDebugInfo(var19);
      }

      if(this.enableInstructions) {
         rsp.setInstructions(fullInstructions);
      }

      if(!allFound) {
         rsp.addError(new RuntimeException("Connection between locations not found"));
      }

      rsp.setPoints(fullPoints).setRouteWeight(fullWeight).setDistance(fullDistance).setTime(fullTimeInMillis);
   }

   public PathMerger setCalcPoints(boolean calcPoints) {
      this.calcPoints = calcPoints;
      return this;
   }

   public PathMerger setDouglasPeucker(DouglasPeucker douglasPeucker) {
      this.douglasPeucker = douglasPeucker;
      return this;
   }

   public PathMerger setSimplifyResponse(boolean simplifyRes) {
      this.simplifyResponse = simplifyRes;
      return this;
   }

   public PathMerger setEnableInstructions(boolean enableInstructions) {
      this.enableInstructions = enableInstructions;
      return this;
   }
}
