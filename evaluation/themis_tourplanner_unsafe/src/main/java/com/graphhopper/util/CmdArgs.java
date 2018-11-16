package com.graphhopper.util;

import com.graphhopper.util.Helper;
import com.graphhopper.util.PMap;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

public class CmdArgs extends PMap {
   public CmdArgs() {
   }

   public CmdArgs(Map map) {
      super(map);
   }

   public CmdArgs put(String key, Object str) {
      super.put(key, str);
      return this;
   }

   public static CmdArgs readFromConfig(String fileStr, String systemProperty) throws IOException {
      if(systemProperty.startsWith("-D")) {
         systemProperty = systemProperty.substring(2);
      }

      String configLocation = System.getProperty(systemProperty);
      if(Helper.isEmpty(configLocation)) {
         configLocation = fileStr;
      }

      LinkedHashMap map = new LinkedHashMap();
      Helper.loadProperties(map, new InputStreamReader(new FileInputStream((new File(configLocation)).getAbsoluteFile()), Helper.UTF_CS));
      CmdArgs args = new CmdArgs();
      args.merge(map);
      Properties props = System.getProperties();
      Iterator i$ = props.entrySet().iterator();

      while(i$.hasNext()) {
         Entry e = (Entry)i$.next();
         String k = (String)e.getKey();
         String v = (String)e.getValue();
         if(k.startsWith("graphhopper.")) {
            k = k.substring("graphhopper.".length());
            args.put(k, v);
         }
      }

      return args;
   }

   public static CmdArgs read(String[] args) {
      LinkedHashMap map = new LinkedHashMap();
      String[] arr$ = args;
      int len$ = args.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         String arg = arr$[i$];
         int index = arg.indexOf("=");
         if(index > 0) {
            String key = arg.substring(0, index);
            if(key.startsWith("-")) {
               key = key.substring(1);
            }

            if(key.startsWith("-")) {
               key = key.substring(1);
            }

            String value = arg.substring(index + 1);
            map.put(key.toLowerCase(), value);
         }
      }

      return new CmdArgs(map);
   }

   public static CmdArgs readFromConfigAndMerge(CmdArgs args, String configKey, String configSysAttr) {
      String configVal = args.get(configKey, "");
      if(!Helper.isEmpty(configVal)) {
         try {
            CmdArgs ex = readFromConfig(configVal, configSysAttr);
            ex.merge(args);
            return ex;
         } catch (Exception var5) {
            throw new RuntimeException(var5);
         }
      } else {
         return args;
      }
   }
}
