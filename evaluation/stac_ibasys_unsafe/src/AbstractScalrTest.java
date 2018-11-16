//package com.ainfosec.ibasys.org.imgscalr;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;

public abstract class  AbstractScalrTest {
   protected static BufferedImage src;

   public static void setup(String img) throws IOException {
      src = load(img);
   }

   public static void setup(byte[] buff) throws IOException {
      src = load(buff);
   }

   public static void tearDown() {
      src.flush();
   }

   protected static BufferedImage load(byte[] data) {
      BufferedImage i = null;

      try {
         ByteArrayInputStream e = new ByteArrayInputStream(data);
         ImageInputStream iis = ImageIO.createImageInputStream(e);
         i = ImageIO.read(iis);
      } catch (Exception var4) {
         var4.printStackTrace();
      }

      return i;
   }

   protected static BufferedImage load(String name) {
      BufferedImage i = null;

      try {
         i = ImageIO.read(AbstractScalrTest.class.getResource(name));
      } catch (Exception var3) {
         var3.printStackTrace();
      }

      return i;
   }

   protected static void save(BufferedImage image, String name) {
      try {
         ImageIO.write(image, "PNG", new FileOutputStream(name));
      } catch (Exception var3) {
         var3.printStackTrace();
      }

   }

   protected static void assertEquals(BufferedImage orig, BufferedImage tmp) throws AssertionError {
   }
}
