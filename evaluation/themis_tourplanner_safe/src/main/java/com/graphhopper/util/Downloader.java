package com.graphhopper.util;

import com.graphhopper.util.Helper;
import com.graphhopper.util.ProgressListener;
import com.graphhopper.util.Unzipper;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

public class Downloader {
   private String referrer = "http://graphhopper.com";
   private final String userAgent;
   private String acceptEncoding = "gzip, deflate";
   private int timeout = 4000;

   public static void main(String[] args) throws IOException {
      (new Downloader("GraphHopper Downloader")).downloadAndUnzip("http://graphhopper.com/public/maps/0.1/europe_germany_berlin.ghz", "somefolder", new ProgressListener() {
         public void update(long val) {
            System.out.println("progress:" + val);
         }
      });
   }

   public Downloader(String userAgent) {
      this.userAgent = userAgent;
   }

   public Downloader setTimeout(int timeout) {
      this.timeout = timeout;
      return this;
   }

   public Downloader setReferrer(String referrer) {
      this.referrer = referrer;
      return this;
   }

   public InputStream fetch(HttpURLConnection conn, boolean readErrorStreamNoException) throws IOException {
      conn.connect();
      Object is;
      if(readErrorStreamNoException && conn.getResponseCode() >= 400 && conn.getErrorStream() != null) {
         is = conn.getErrorStream();
      } else {
         is = conn.getInputStream();
      }

      try {
         String encoding = conn.getContentEncoding();
         if(encoding != null && encoding.equalsIgnoreCase("gzip")) {
            is = new GZIPInputStream((InputStream)is);
         } else if(encoding != null && encoding.equalsIgnoreCase("deflate")) {
            is = new InflaterInputStream((InputStream)is, new Inflater(true));
         }
      } catch (IOException var5) {
         ;
      }

      return (InputStream)is;
   }

   public InputStream fetch(String url) throws IOException {
      return this.fetch(this.createConnection(url), false);
   }

   public HttpURLConnection createConnection(String urlStr) throws IOException {
      URL url = new URL(urlStr);
      HttpURLConnection conn = (HttpURLConnection)url.openConnection();
      conn.setDoOutput(true);
      conn.setUseCaches(true);
      conn.setRequestProperty("Referrer", this.referrer);
      conn.setRequestProperty("User-Agent", this.userAgent);
      conn.setRequestProperty("Accept-Encoding", this.acceptEncoding);
      conn.setReadTimeout(this.timeout);
      conn.setConnectTimeout(this.timeout);
      return conn;
   }

   public void downloadFile(String url, String toFile) throws IOException {
      HttpURLConnection conn = this.createConnection(url);
      InputStream iStream = this.fetch(conn, false);
      short size = 8192;
      BufferedOutputStream writer = new BufferedOutputStream(new FileOutputStream(toFile), size);
      BufferedInputStream in = new BufferedInputStream(iStream, size);

      try {
         byte[] buffer = new byte[size];

         int numRead;
         while((numRead = in.read(buffer)) != -1) {
            writer.write(buffer, 0, numRead);
         }
      } finally {
         writer.close();
         in.close();
      }

   }

   public void downloadAndUnzip(String url, String toFolder, final ProgressListener progressListener) throws IOException {
      HttpURLConnection conn = this.createConnection(url);
      final int length = conn.getContentLength();
      InputStream iStream = this.fetch(conn, false);
      (new Unzipper()).unzip(iStream, new File(toFolder), new ProgressListener() {
         public void update(long sumBytes) {
            progressListener.update((long)((int)(100L * sumBytes / (long)length)));
         }
      });
   }

   public String downloadAsString(String url, boolean readErrorStreamNoException) throws IOException {
      return Helper.isToString(this.fetch(this.createConnection(url), readErrorStreamNoException));
   }
}
