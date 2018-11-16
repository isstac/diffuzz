package com.graphhopper.tour;

import com.graphhopper.reader.OSMElement;
import com.graphhopper.reader.OSMInputFile;
import com.graphhopper.reader.OSMNode;
import com.graphhopper.util.CmdArgs;
import com.graphhopper.util.Helper;
import com.graphhopper.util.shapes.GHPlace;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLStreamException;
import org.apache.commons.lang3.StringUtils;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

public class Places {
//   private static final Logger logger = LoggerFactory.getLogger(Places.class);

   public static List names(List places) {
      ArrayList names = new ArrayList(places.size());
      Iterator i$ = places.iterator();

      while(i$.hasNext()) {
         GHPlace p = (GHPlace)i$.next();
         names.add(p.getName());
      }

      return names;
   }

   public static Map nameIndex(List places) {
      HashMap index = new HashMap();
      Iterator i$ = places.iterator();

      while(i$.hasNext()) {
         GHPlace p = (GHPlace)i$.next();
         index.put(p.getName(), p);
      }

      return index;
   }

   public static List selectByName(Map index, List names) {
      ArrayList filtered = new ArrayList(names.size());
      Iterator i$ = names.iterator();

      while(i$.hasNext()) {
         String name = (String)i$.next();
         GHPlace place = (GHPlace)index.get(name);
         if(place == null) {
            throw new IllegalArgumentException("Could not find place \"" + name + "\"");
         }

         filtered.add(place);
      }

      return filtered;
   }

   public static List selectByName(List places, List names) {
      return selectByName(nameIndex(places), names);
   }

   public static List selectByName(Map index, File namesFile) throws IOException {
      return selectByName(index, readLines(namesFile));
   }

   public static List selectByName(List places, File namesFile) throws IOException {
      return selectByName(places, readLines(namesFile));
   }

   public static List load(CmdArgs args) throws IOException, XMLStreamException {
      String osmFile = args.get("places.osm", "");
      String csvFile = args.get("places.csv", "");
      if(Helper.isEmpty(osmFile) && Helper.isEmpty(csvFile)) {
         throw new IllegalArgumentException("You must specify a places file (places.osm=FILE or places.csv=FILE).");
      } else if(!Helper.isEmpty(osmFile) && !Helper.isEmpty(csvFile)) {
         throw new IllegalArgumentException("Either places.osm or places.csv must be specified, not both.");
      } else {
         return !Helper.isEmpty(osmFile)?readOsm(new File(osmFile)):readCsv(new File(csvFile));
      }
   }

   public static List readOsm(File osmFile) throws IOException, XMLStreamException {
      if(!osmFile.exists()) {
         throw new IllegalStateException("Places file does not exist: " + osmFile.getAbsolutePath());
      } else {
//         logger.info("Reading places file " + osmFile.getAbsolutePath());
         OSMInputFile in = (new OSMInputFile(osmFile)).open();
         Throwable var2 = null;

         List var3;
         try {
            var3 = readOsm(in);
         } catch (XMLStreamException var12) {
            var2 = var12;
            throw var12;
         } finally {
            if(in != null) {
               if(var2 != null) {
                  try {
                     in.close();
                  } catch (Throwable var11) {
                     var2.addSuppressed(var11);
                  }
               } else {
                  in.close();
               }
            }

         }

         return var3;
      }
   }

   private static List readOsm(OSMInputFile in) throws XMLStreamException {
      ArrayList places = new ArrayList();

      OSMElement item;
      while((item = in.getNext()) != null) {
         if(item.isType(0)) {
            OSMNode node = (OSMNode)item;
            if(node.hasTag("name", new String[0]) && node.hasTag("place", new String[0])) {
               String name = node.getTag("name");
               GHPlace place = (new GHPlace(node.getLat(), node.getLon())).setName(name);
               places.add(place);
            }
         }
      }

//      logger.info("Read " + places.size() + " places");
      return places;
   }

   public static List readCsv(File csvFile) throws IOException {
      if(!csvFile.exists()) {
         throw new IllegalStateException("Places file does not exist: " + csvFile.getAbsolutePath());
      } else {
//         logger.info("Reading places file " + csvFile.getAbsolutePath());
         FileReader in = new FileReader(csvFile);
         Throwable var2 = null;

         List var3;
         try {
            var3 = readCsv(new BufferedReader(in));
         } catch (IOException var12) {
            var2 = var12;
            throw var12;
         } finally {
            if(in != null) {
               if(var2 != null) {
                  try {
                     in.close();
                  } catch (Throwable var11) {
                     var2.addSuppressed(var11);
                  }
               } else {
                  in.close();
               }
            }

         }

         return var3;
      }
   }

   public static List readCsv(BufferedReader in) throws IOException {
      ArrayList places = new ArrayList();
      String expected = "Name,Lat,Lon";
      String line = in.readLine();
      if(line != null && StringUtils.strip(line).equals(expected)) {
         while((line = in.readLine()) != null) {
            line = StringUtils.strip(line);
            if(line.equals("")) {
               break;
            }

            places.add(parseCsv(line));
         }

//         logger.info("Read " + places.size() + " places");
         return places;
      } else {
         throw new IllegalArgumentException("Expected header row, got " + line);
      }
   }

   public static void writeCsv(List places, File csvFile) throws IOException {
      PrintStream out = new PrintStream(csvFile);
      Throwable var3 = null;

      try {
         writeCsv(places, out);
      } catch (IOException var12) {
         var3 = var12;
         throw var12;
      } finally {
         if(out != null) {
            if(var3 != null) {
               try {
                  out.close();
               } catch (Throwable var11) {
                  var3.addSuppressed(var11);
               }
            } else {
               out.close();
            }
         }

      }

   }

   public static void writeCsv(List places, PrintStream out) throws IOException {
      out.println("Name,Lat,Lon");
      Iterator i$ = places.iterator();

      while(i$.hasNext()) {
         GHPlace p = (GHPlace)i$.next();
         out.println(toCsv(p));
      }

   }

   private static String toCsv(GHPlace p) {
      return p.getName() + "," + p.getLat() + "," + p.getLon();
   }

   private static GHPlace parseCsv(String s) {
      String[] cols = StringUtils.split(StringUtils.strip(s), ',');
      if(cols.length != 3) {
         throw new IllegalArgumentException("Expected 3 CSV elements, got " + cols.length + ": " + s);
      } else {
         String name = cols[0];
         double lat = Double.parseDouble(cols[1]);
         double lon = Double.parseDouble(cols[2]);
         return (new GHPlace(lat, lon)).setName(name);
      }
   }

   private static List readLines(File file) throws IOException {
//      logger.info("Reading place names from file " + file.getPath());
      ArrayList lines = new ArrayList();
      BufferedReader br = new BufferedReader(new FileReader(file));
      Throwable var3 = null;

      try {
         for(String x2 = br.readLine(); x2 != null; x2 = br.readLine()) {
            lines.add(x2);
         }
      } catch (IOException var12) {
         var3 = var12;
         throw var12;
      } finally {
         if(br != null) {
            if(var3 != null) {
               try {
                  br.close();
               } catch (Throwable var11) {
                  var3.addSuppressed(var11);
               }
            } else {
               br.close();
            }
         }

      }

      return lines;
   }
}
