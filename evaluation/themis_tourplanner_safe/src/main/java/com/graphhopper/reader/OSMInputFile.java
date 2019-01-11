package com.graphhopper.reader;

import com.graphhopper.reader.OSMElement;
import com.graphhopper.reader.OSMNode;
import com.graphhopper.reader.OSMRelation;
import com.graphhopper.reader.OSMWay;
import com.graphhopper.reader.pbf.PbfReader;
import com.graphhopper.reader.pbf.Sink;
import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

public class OSMInputFile implements Sink, Closeable {
   private boolean eof;
   private InputStream bis;
   private XMLStreamReader parser;
   private boolean binary = false;
   private final BlockingQueue itemQueue;
   private boolean hasIncomingData;
   private int workerThreads = -1;
   Thread pbfReaderThread;

   public OSMInputFile(File file) throws IOException {
      this.bis = this.decode(file);
      this.itemQueue = new LinkedBlockingQueue(50000);
   }

   public OSMInputFile open() throws XMLStreamException {
      if(this.binary) {
         this.openPBFReader(this.bis);
      } else {
         this.openXMLStream(this.bis);
      }

      return this;
   }

   public OSMInputFile setWorkerThreads(int num) {
      this.workerThreads = num;
      return this;
   }

   private InputStream decode(File file) throws IOException {
      String name = file.getName();
      BufferedInputStream ips = null;

      try {
         ips = new BufferedInputStream(new FileInputStream(file), 50000);
      } catch (FileNotFoundException var9) {
         throw new RuntimeException(var9);
      }

      ips.mark(10);
      byte[] header = new byte[6];
      ips.read(header);
      if(header[0] == 31 && header[1] == -117) {
         ips.reset();
         return new GZIPInputStream(ips, 50000);
      } else if(header[0] == 0 && header[1] == 0 && header[2] == 0 && header[4] == 10 && header[5] == 9 && (header[3] == 13 || header[3] == 14)) {
         ips.reset();
         this.binary = true;
         return ips;
      } else if(header[0] == 80 && header[1] == 75) {
         ips.reset();
         ZipInputStream clName1 = new ZipInputStream(ips);
         clName1.getNextEntry();
         return clName1;
      } else if(!name.endsWith(".osm") && !name.endsWith(".xml")) {
         if(!name.endsWith(".bz2") && !name.endsWith(".bzip2")) {
            throw new IllegalArgumentException("Input file is not of valid type " + file.getPath());
         } else {
            String clName = "org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream";

            try {
               Class e = Class.forName(clName);
               ips.reset();
               Constructor ctor = e.getConstructor(new Class[]{InputStream.class, Boolean.TYPE});
               return (InputStream)ctor.newInstance(new Object[]{ips, Boolean.valueOf(true)});
            } catch (Exception var8) {
               throw new IllegalArgumentException("Cannot instantiate " + clName, var8);
            }
         }
      } else {
         ips.reset();
         return ips;
      }
   }

   private void openXMLStream(InputStream in) throws XMLStreamException {
      XMLInputFactory factory = XMLInputFactory.newInstance();
      this.parser = factory.createXMLStreamReader(this.bis, "UTF-8");
      int event = this.parser.next();
      if(event == 1 && this.parser.getLocalName().equalsIgnoreCase("osm")) {
         this.eof = false;
      } else {
         throw new IllegalArgumentException("File is not a valid OSM stream");
      }
   }

   public OSMElement getNext() throws XMLStreamException {
      if(this.eof) {
         throw new IllegalStateException("EOF reached");
      } else {
         OSMElement item;
         if(this.binary) {
            item = this.getNextPBF();
         } else {
            item = this.getNextXML();
         }

         if(item != null) {
            return item;
         } else {
            this.eof = true;
            return null;
         }
      }
   }

   private OSMElement getNextXML() throws XMLStreamException {
      for(int event = this.parser.next(); event != 8; event = this.parser.next()) {
         if(event == 1) {
            String idStr = this.parser.getAttributeValue((String)null, "id");
            if(idStr != null) {
               String name = this.parser.getLocalName();
               long id = 0L;
               switch(name.charAt(0)) {
               case 'n':
                  if("node".equals(name)) {
                     id = Long.parseLong(idStr);
                     return OSMNode.create(id, this.parser);
                  }
                  break;
               case 'r':
                  id = Long.parseLong(idStr);
                  return OSMRelation.create(id, this.parser);
               case 'w':
                  id = Long.parseLong(idStr);
                  return OSMWay.create(id, this.parser);
               }
            }
         }
      }

      this.parser.close();
      return null;
   }

   public boolean isEOF() {
      return this.eof;
   }

   public void close() throws IOException {
      try {
         if(!this.binary) {
            this.parser.close();
         }
      } catch (XMLStreamException var5) {
         throw new IOException(var5);
      } finally {
         this.eof = true;
         this.bis.close();
         if(this.pbfReaderThread != null && this.pbfReaderThread.isAlive()) {
            this.pbfReaderThread.interrupt();
         }

      }

   }

   private void openPBFReader(InputStream stream) {
      this.hasIncomingData = true;
      if(this.workerThreads <= 0) {
         this.workerThreads = 2;
      }

      PbfReader reader = new PbfReader(stream, this, this.workerThreads);
      this.pbfReaderThread = new Thread(reader, "PBF Reader");
      this.pbfReaderThread.start();
   }

   public void process(OSMElement item) {
      try {
         this.itemQueue.put(item);
      } catch (InterruptedException var3) {
         throw new RuntimeException(var3);
      }
   }

   public void complete() {
      this.hasIncomingData = false;
   }

   private OSMElement getNextPBF() {
      OSMElement next = null;

      while(next == null) {
         if(!this.hasIncomingData && this.itemQueue.isEmpty()) {
            this.eof = true;
            break;
         }

         try {
            next = (OSMElement)this.itemQueue.poll(10L, TimeUnit.MILLISECONDS);
         } catch (InterruptedException var3) {
            this.eof = true;
            break;
         }
      }

      return next;
   }
}
