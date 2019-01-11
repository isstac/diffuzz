package com.graphhopper.util;

import com.graphhopper.util.Instruction;
import com.graphhopper.util.InstructionAnnotation;
import com.graphhopper.util.PointList;
import com.graphhopper.util.Translation;

public class ViaInstruction extends Instruction {
   private int viaPosition;

   public ViaInstruction(String name, InstructionAnnotation ia, PointList pl) {
      super(5, name, ia, pl);
      this.viaPosition = -1;
   }

   public ViaInstruction(Instruction instr) {
      this(instr.getName(), instr.getAnnotation(), instr.getPoints());
      this.setDistance(instr.getDistance());
      this.setTime(instr.getTime());
   }

   public void setViaCount(int count) {
      this.viaPosition = count;
   }

   public int getViaCount() {
      if(this.viaPosition < 0) {
         throw new IllegalStateException("Uninitialized via count in instruction " + this.getName());
      } else {
         return this.viaPosition;
      }
   }

   public String getTurnDescription(Translation tr) {
      return this.rawName?this.getName():tr.tr("stopover", new Object[]{Integer.valueOf(this.viaPosition)});
   }
}
