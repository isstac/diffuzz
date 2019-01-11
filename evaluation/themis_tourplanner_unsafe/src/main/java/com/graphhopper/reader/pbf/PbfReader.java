package com.graphhopper.reader.pbf;

import com.graphhopper.reader.pbf.PbfDecoder;
import com.graphhopper.reader.pbf.PbfStreamSplitter;
import com.graphhopper.reader.pbf.Sink;
import java.io.DataInputStream;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PbfReader implements Runnable {
   private InputStream inputStream;
   private Sink sink;
   private int workers;

   public PbfReader(InputStream in, Sink sink, int workers) {
      this.inputStream = in;
      this.sink = sink;
      this.workers = workers;
   }

   public void run() {
      ExecutorService executorService = Executors.newFixedThreadPool(this.workers);

      try {
         PbfStreamSplitter e = new PbfStreamSplitter(new DataInputStream(this.inputStream));
         PbfDecoder pbfDecoder = new PbfDecoder(e, executorService, this.workers + 1, this.sink);
         pbfDecoder.run();
      } catch (Exception var7) {
         throw new RuntimeException("Unable to read PBF file.", var7);
      } finally {
         this.sink.complete();
         executorService.shutdownNow();
      }

   }
}
