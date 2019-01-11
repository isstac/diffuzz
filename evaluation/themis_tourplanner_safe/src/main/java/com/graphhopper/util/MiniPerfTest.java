package com.graphhopper.util;

import java.text.DecimalFormat;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

public abstract class MiniPerfTest {
//   protected Logger logger = LoggerFactory.getLogger(this.getClass());
   private int counts = 100;
   private double fullTime = 0.0D;
   private double max;
   private double min = Double.MAX_VALUE;
   private int dummySum;

   public MiniPerfTest start() {
      int warmupCount = Math.max(1, this.counts / 3);

      for(int startFull = 0; startFull < warmupCount; ++startFull) {
         this.dummySum += this.doCalc(true, startFull);
      }

      long var9 = System.nanoTime();

      for(int i = 0; i < this.counts; ++i) {
         long start = System.nanoTime();
         this.dummySum += this.doCalc(false, i);
         long time = System.nanoTime() - start;
         if((double)time < this.min) {
            this.min = (double)time;
         }

         if((double)time > this.max) {
            this.max = (double)time;
         }
      }

      this.fullTime = (double)(System.nanoTime() - var9);
//      this.logger.info("dummySum:" + this.dummySum);
      return this;
   }

   public MiniPerfTest setIterations(int counts) {
      this.counts = counts;
      return this;
   }

   public double getMin() {
      return this.min / 1000000.0D;
   }

   public double getMax() {
      return this.max / 1000000.0D;
   }

   public double getSum() {
      return this.fullTime / 1000000.0D;
   }

   public double getMean() {
      return this.getSum() / (double)this.counts;
   }

   public String getReport() {
      return "sum:" + this.nf(Double.valueOf(this.getSum() / 1000.0D)) + "s, time/call:" + this.nf(Double.valueOf(this.getMean() / 1000.0D)) + "s";
   }

   public String nf(Number num) {
      return (new DecimalFormat("#.#")).format(num);
   }

   public abstract int doCalc(boolean var1, int var2);
}
