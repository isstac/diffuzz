//package com.ainfosec.ibasys.org.imgscalr;

//import com.ainfosec.ibasys.org.imgscalr.AbstractScalrTest;
//import com.ainfosec.ibasys.org.imgscalr.Scalr;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;

public class  ScalrApplyTest extends AbstractScalrTest {
   public void testApplyEX() {
      try {
          Scalr.apply(src, (BufferedImageOp[])null);
      } catch (Exception var2) {
         ;
      }

   }

   public BufferedImage testApply1() {
      BufferedImage resize = Scalr.crop(src, 100, 100, new BufferedImageOp[0]);
      return resize;
   }

   public void testApply4() {
   }
}
