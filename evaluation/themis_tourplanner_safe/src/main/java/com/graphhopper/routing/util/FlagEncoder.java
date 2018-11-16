package com.graphhopper.routing.util;

import com.graphhopper.routing.util.TurnCostEncoder;
import com.graphhopper.util.InstructionAnnotation;
import com.graphhopper.util.Translation;

public interface FlagEncoder extends TurnCostEncoder {
   int K_ROUNDABOUT = 2;

   int getVersion();

   double getMaxSpeed();

   double getSpeed(long var1);

   long setSpeed(long var1, double var3);

   double getReverseSpeed(long var1);

   long setReverseSpeed(long var1, double var3);

   long setAccess(long var1, boolean var3, boolean var4);

   long setProperties(double var1, boolean var3, boolean var4);

   boolean isForward(long var1);

   boolean isBackward(long var1);

   boolean isBool(long var1, int var3);

   long setBool(long var1, int var3, boolean var4);

   long getLong(long var1, int var3);

   long setLong(long var1, int var3, long var4);

   double getDouble(long var1, int var3);

   long setDouble(long var1, int var3, double var4);

   boolean supports(Class var1);

   InstructionAnnotation getAnnotation(long var1, Translation var3);

   boolean isRegistered();
}
