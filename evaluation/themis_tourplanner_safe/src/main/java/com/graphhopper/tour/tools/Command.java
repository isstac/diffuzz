package com.graphhopper.tour.tools;

import com.graphhopper.util.CmdArgs;
import java.util.ArrayList;
import java.util.List;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

public abstract class Command {
//   protected final Logger logger = LoggerFactory.getLogger(this.getClass());
   protected CmdArgs cmdArgs;
   protected List ownArgs;

   public Command parseArgs(String[] args) {
      ArrayList ghArgs = new ArrayList();
      this.ownArgs = new ArrayList();
      String[] arr$ = args;
      int len$ = args.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         String arg = arr$[i$];
         if(arg.contains("=")) {
            ghArgs.add(arg);
         } else {
            this.ownArgs.add(arg);
         }
      }

      this.cmdArgs = CmdArgs.read((String[])ghArgs.toArray(new String[ghArgs.size()]));
      this.checkArgs();
      return this;
   }

   public abstract void run() throws Exception;

   protected void checkArgs() {
   }
}
