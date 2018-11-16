package com.graphhopper.util;

import com.graphhopper.util.Helper;
import com.graphhopper.util.ProgressListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Unzipper {
   public void unzip(String from, boolean remove) throws IOException {
      String to = Helper.pruneFileEnd(from);
      this.unzip(from, to, remove);
   }

   public boolean unzip(String fromStr, String toStr, boolean remove) throws IOException {
      File from = new File(fromStr);
      if(from.exists() && !fromStr.equals(toStr)) {
         this.unzip(new FileInputStream(from), new File(toStr), (ProgressListener)null);
         if(remove) {
            Helper.removeDir(from);
         }

         return true;
      } else {
         return false;
      }
   }

   public void unzip(InputStream fromIs, File toFolder, ProgressListener progressListener) throws IOException {
      if(!toFolder.exists()) {
         toFolder.mkdirs();
      }

      long sumBytes = 0L;
      ZipInputStream zis = new ZipInputStream(fromIs);

      try {
         ZipEntry ze = zis.getNextEntry();

         for(byte[] buffer = new byte[8192]; ze != null; ze = zis.getNextEntry()) {
            if(ze.isDirectory()) {
               (new File(toFolder, ze.getName())).mkdir();
            } else {
               double factor = 1.0D;
               if(ze.getCompressedSize() > 0L && ze.getSize() > 0L) {
                  factor = (double)ze.getCompressedSize() / (double)ze.getSize();
               }

               File newFile = new File(toFolder, ze.getName());
               FileOutputStream fos = new FileOutputStream(newFile);

               int len;
               try {
                  while((len = zis.read(buffer)) > 0) {
                     fos.write(buffer, 0, len);
                     sumBytes = (long)((double)sumBytes + (double)len * factor);
                     if(progressListener != null) {
                        progressListener.update(sumBytes);
                     }
                  }
               } finally {
                  fos.close();
               }
            }
         }

         zis.closeEntry();
      } finally {
         zis.close();
      }

   }
}
