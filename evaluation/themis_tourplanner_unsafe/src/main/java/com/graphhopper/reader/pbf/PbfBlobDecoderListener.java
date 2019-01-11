package com.graphhopper.reader.pbf;

import java.util.List;

public interface PbfBlobDecoderListener {
   void complete(List var1);

   void error(Exception var1);
}
