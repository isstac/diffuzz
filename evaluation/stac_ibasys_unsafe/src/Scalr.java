//package com.ainfosec.ibasys.org.imgscalr;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorConvertOp;
import java.awt.image.ConvolveOp;
import java.awt.image.ImageObserver;
import java.awt.image.ImagingOpException;
import java.awt.image.Kernel;
import java.awt.image.RescaleOp;
import java.beans.XMLEncoder;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;

public class  Scalr  {
   public static final String DEBUG_PROPERTY_NAME = "imgscalr.debug";
   public static final String LOG_PREFIX_PROPERTY_NAME = "imgscalr.logPrefix";
   public static final boolean DEBUG = Boolean.getBoolean("imgscalr.debug");
   public static final String LOG_PREFIX = System.getProperty("imgscalr.logPrefix", "[imgscalr] ");
   public static final ConvolveOp OP_ANTIALIAS = new ConvolveOp(new Kernel(3, 3, new float[]{0.0F, 0.08F, 0.0F, 0.08F, 0.68F, 0.08F, 0.0F, 0.08F, 0.0F}), 1, (RenderingHints)null);
   public static final RescaleOp OP_DARKER = new RescaleOp(0.9F, 0.0F, (RenderingHints)null);
   public static final RescaleOp OP_BRIGHTER = new RescaleOp(1.1F, 0.0F, (RenderingHints)null);
   public static final ColorConvertOp OP_GRAYSCALE = new ColorConvertOp(ColorSpace.getInstance(1003), (RenderingHints)null);
   public static final int THRESHOLD_BALANCED_SPEED = 1600;
   public static final int THRESHOLD_QUALITY_BALANCED = 800;

   public static BufferedImage apply(BufferedImage src, BufferedImageOp... ops) throws IllegalArgumentException, ImagingOpException {
      long t = -1L;
      if(DEBUG) {
         t = System.currentTimeMillis();
      }

      if(src == null) {
         throw new IllegalArgumentException("src cannot be null");
      } else if(ops != null && ops.length != 0) {
         int type = src.getType();
         if(type != 1 && type != 2) {
            src = copyToOptimalImage(src);
         }

         if(DEBUG) {
            log(0, "Applying %d BufferedImageOps...", new Object[]{Integer.valueOf(ops.length)});
         }

         boolean hasReassignedSrc = false;

         for(int i = 0; i < ops.length; ++i) {
            long subT = -1L;
            if(DEBUG) {
               subT = System.currentTimeMillis();
            }

            BufferedImageOp op = ops[i];
            if(op != null) {
               if(DEBUG) {
                  log(1, "Applying BufferedImageOp [class=%s, toString=%s]...", new Object[]{op.getClass(), op.toString()});
               }

               Rectangle2D resultBounds = op.getBounds2D(src);
               if(resultBounds == null) {
                  throw new ImagingOpException("BufferedImageOp [" + op.toString() + "] getBounds2D(src) returned null bounds for the target image; this should not happen and indicates a problem with application of this type of op.");
               }

               BufferedImage dest = createOptimalImage(src, (int)Math.round(resultBounds.getWidth()), (int)Math.round(resultBounds.getHeight()));
               BufferedImage result = op.filter(src, dest);
               if(hasReassignedSrc) {
                  src.flush();
               }

               src = result;
               hasReassignedSrc = true;
               if(DEBUG) {
                  log(1, "Applied BufferedImageOp in %d ms, result [width=%d, height=%d]", new Object[]{Long.valueOf(System.currentTimeMillis() - subT), Integer.valueOf(result.getWidth()), Integer.valueOf(result.getHeight())});
               }
            }
         }

         if(DEBUG) {
            log(0, "All %d BufferedImageOps applied in %d ms", new Object[]{Integer.valueOf(ops.length), Long.valueOf(System.currentTimeMillis() - t)});
         }

         return src;
      } else {
         throw new IllegalArgumentException("ops cannot be null or empty");
      }
   }

   public static BufferedImage crop(BufferedImage src, int width, int height, BufferedImageOp... ops) throws IllegalArgumentException, ImagingOpException {
      return crop(src, 0, 0, width, height, ops);
   }

   public static BufferedImage crop(BufferedImage src, int x, int y, int width, int height, BufferedImageOp... ops) throws IllegalArgumentException, ImagingOpException {
      long t = -1L;
      if(DEBUG) {
         t = System.currentTimeMillis();
      }

      if(src == null) {
         throw new IllegalArgumentException("src cannot be null");
      } else if(x >= 0 && y >= 0 && width >= 0 && height >= 0) {
         int srcWidth = src.getWidth();
         int srcHeight = src.getHeight();
         if(x + width > srcWidth) {
            throw new IllegalArgumentException("Invalid crop bounds: x + width [" + (x + width) + "] must be <= src.getWidth() [" + srcWidth + "]");
         } else if(y + height > srcHeight) {
            throw new IllegalArgumentException("Invalid crop bounds: y + height [" + (y + height) + "] must be <= src.getHeight() [" + srcHeight + "]");
         } else {
            if(DEBUG) {
               log(0, "Cropping Image [width=%d, height=%d] to [x=%d, y=%d, width=%d, height=%d]...", new Object[]{Integer.valueOf(srcWidth), Integer.valueOf(srcHeight), Integer.valueOf(x), Integer.valueOf(y), Integer.valueOf(width), Integer.valueOf(height)});
            }

            BufferedImage result = createOptimalImage(src, width, height);
            Graphics g = result.getGraphics();
            g.drawImage(src, 0, 0, width, height, x, y, x + width, y + height, (ImageObserver)null);
            g.dispose();
            if(DEBUG) {
               log(0, "Cropped Image in %d ms", new Object[]{Long.valueOf(System.currentTimeMillis() - t)});
            }

            if(ops != null && ops.length > 0) {
               result = apply(result, ops);
            }

            return result;
         }
      } else {
         throw new IllegalArgumentException("Invalid crop bounds: x [" + x + "], y [" + y + "], width [" + width + "] and height [" + height + "] must all be >= 0");
      }
   }

   public static BufferedImage pad(BufferedImage src, int padding, BufferedImageOp... ops) throws IllegalArgumentException, ImagingOpException {
      return pad(src, padding, Color.BLACK, new BufferedImageOp[0]);
   }

   public static BufferedImage pad(BufferedImage src, int padding, Color color, BufferedImageOp... ops) throws IllegalArgumentException, ImagingOpException {
      long t = -1L;
      if(DEBUG) {
         t = System.currentTimeMillis();
      }

      if(src == null) {
         throw new IllegalArgumentException("src cannot be null");
      } else if(padding < 1) {
         throw new IllegalArgumentException("padding [" + padding + "] must be > 0");
      } else if(color == null) {
         throw new IllegalArgumentException("color cannot be null");
      } else {
         int srcWidth = src.getWidth();
         int srcHeight = src.getHeight();
         int sizeDiff = padding * 2;
         int newWidth = srcWidth + sizeDiff;
         int newHeight = srcHeight + sizeDiff;
         if(DEBUG) {
            log(0, "Padding Image from [originalWidth=%d, originalHeight=%d, padding=%d] to [newWidth=%d, newHeight=%d]...", new Object[]{Integer.valueOf(srcWidth), Integer.valueOf(srcHeight), Integer.valueOf(padding), Integer.valueOf(newWidth), Integer.valueOf(newHeight)});
         }

         boolean colorHasAlpha = color.getAlpha() != 255;
         boolean imageHasAlpha = src.getTransparency() != 1;
         BufferedImage result;
         if(!colorHasAlpha && !imageHasAlpha) {
            if(DEBUG) {
               log(1, "Transparency NOT FOUND in source image or color, using RGB image type...", new Object[0]);
            }

            result = new BufferedImage(newWidth, newHeight, 1);
         } else {
            if(DEBUG) {
               log(1, "Transparency FOUND in source image or color, using ARGB image type...", new Object[0]);
            }

            result = new BufferedImage(newWidth, newHeight, 2);
         }

         Graphics g = result.getGraphics();
         g.setColor(color);
         g.fillRect(0, 0, newWidth, padding);
         g.fillRect(0, padding, padding, newHeight);
         g.fillRect(padding, newHeight - padding, newWidth, newHeight);
         g.fillRect(newWidth - padding, padding, newWidth, newHeight - padding);
         g.drawImage(src, padding, padding, (ImageObserver)null);
         g.dispose();
         if(DEBUG) {
            log(0, "Padding Applied in %d ms", new Object[]{Long.valueOf(System.currentTimeMillis() - t)});
         }

         if(ops != null && ops.length > 0) {
            result = apply(result, ops);
         }

         return result;
      }
   }

   public static BufferedImage resize(BufferedImage src, int targetSize, BufferedImageOp... ops) throws IllegalArgumentException, ImagingOpException {
      return resize(src, Scalr.Method.AUTOMATIC, Scalr.Mode.AUTOMATIC, targetSize, targetSize, ops);
   }

   public static BufferedImage resize(BufferedImage src, Scalr.Method scalingMethod, int targetSize, BufferedImageOp... ops) throws IllegalArgumentException, ImagingOpException {
      return resize(src, scalingMethod, Scalr.Mode.AUTOMATIC, targetSize, targetSize, ops);
   }

   public static BufferedImage resize(BufferedImage src, Scalr.Mode resizeMode, int targetSize, BufferedImageOp... ops) throws IllegalArgumentException, ImagingOpException {
      return resize(src, Scalr.Method.AUTOMATIC, resizeMode, targetSize, targetSize, ops);
   }

   public static BufferedImage resize(BufferedImage src, Scalr.Method scalingMethod, Scalr.Mode resizeMode, int targetSize, BufferedImageOp... ops) throws IllegalArgumentException, ImagingOpException {
      return resize(src, scalingMethod, resizeMode, targetSize, targetSize, ops);
   }

   public static BufferedImage resize(BufferedImage src, int targetWidth, int targetHeight, BufferedImageOp... ops) throws IllegalArgumentException, ImagingOpException {
      return resize(src, Scalr.Method.AUTOMATIC, Scalr.Mode.AUTOMATIC, targetWidth, targetHeight, ops);
   }

   public static BufferedImage resize(BufferedImage src, Scalr.Method scalingMethod, int targetWidth, int targetHeight, BufferedImageOp... ops) {
      return resize(src, scalingMethod, Scalr.Mode.AUTOMATIC, targetWidth, targetHeight, ops);
   }

   public static BufferedImage resize(BufferedImage src, Scalr.Mode resizeMode, int targetWidth, int targetHeight, BufferedImageOp... ops) throws IllegalArgumentException, ImagingOpException {
      return resize(src, Scalr.Method.AUTOMATIC, resizeMode, targetWidth, targetHeight, ops);
   }

   public static BufferedImage resize(BufferedImage src, Scalr.Method scalingMethod, Scalr.Mode resizeMode, int targetWidth, int targetHeight, BufferedImageOp... ops) throws IllegalArgumentException, ImagingOpException {
      long t = -1L;
      if(DEBUG) {
         t = System.currentTimeMillis();
      }

      if(src == null) {
         throw new IllegalArgumentException("src cannot be null");
      } else if(targetWidth < 0) {
         throw new IllegalArgumentException("targetWidth must be >= 0");
      } else if(targetHeight < 0) {
         throw new IllegalArgumentException("targetHeight must be >= 0");
      } else if(scalingMethod == null) {
         throw new IllegalArgumentException("scalingMethod cannot be null. A good default value is Method.AUTOMATIC.");
      } else if(resizeMode == null) {
         throw new IllegalArgumentException("resizeMode cannot be null. A good default value is Mode.AUTOMATIC.");
      } else {
         BufferedImage result = null;
         int currentWidth = src.getWidth();
         int currentHeight = src.getHeight();
         float ratio = (float)currentHeight / (float)currentWidth;
         if(DEBUG) {
            log(0, "Resizing Image [size=%dx%d, resizeMode=%s, orientation=%s, ratio(H/W)=%f] to [targetSize=%dx%d]", new Object[]{Integer.valueOf(currentWidth), Integer.valueOf(currentHeight), resizeMode, ratio <= 1.0F?"Landscape/Square":"Portrait", Float.valueOf(ratio), Integer.valueOf(targetWidth), Integer.valueOf(targetHeight)});
         }

         if(resizeMode == Scalr.Mode.FIT_EXACT) {
            if(DEBUG) {
               log(1, "Resize Mode FIT_EXACT used, no width/height checking or re-calculation will be done.", new Object[0]);
            }
         } else if(resizeMode == Scalr.Mode.BEST_FIT_BOTH) {
            float originalTargetWidth = (float)targetHeight / (float)currentHeight;
            float requestedWidthScaling = (float)targetWidth / (float)currentWidth;
            float actualScaling = Math.min(originalTargetWidth, requestedWidthScaling);
            targetHeight = Math.round((float)currentHeight * actualScaling);
            targetWidth = Math.round((float)currentWidth * actualScaling);
            if(targetHeight == currentHeight && targetWidth == currentWidth) {
               return src;
            }

            if(DEBUG) {
               log(1, "Auto-Corrected width and height based on scalingRatio %d.", new Object[]{Float.valueOf(actualScaling)});
            }
         } else {
            int originalTargetWidth1;
            if((ratio > 1.0F || resizeMode != Scalr.Mode.AUTOMATIC) && resizeMode != Scalr.Mode.FIT_TO_WIDTH) {
               if(targetHeight == src.getHeight()) {
                  return src;
               }

               originalTargetWidth1 = targetWidth;
               targetWidth = Math.round((float)targetHeight / ratio);
               if(DEBUG && originalTargetWidth1 != targetWidth) {
                  log(1, "Auto-Corrected targetWidth [from=%d to=%d] to honor image proportions.", new Object[]{Integer.valueOf(originalTargetWidth1), Integer.valueOf(targetWidth)});
               }
            } else {
               if(targetWidth == src.getWidth()) {
                  return src;
               }

               originalTargetWidth1 = targetHeight;
               targetHeight = (int)Math.ceil((double)((float)targetWidth * ratio));
               if(DEBUG && originalTargetWidth1 != targetHeight) {
                  log(1, "Auto-Corrected targetHeight [from=%d to=%d] to honor image proportions.", new Object[]{Integer.valueOf(originalTargetWidth1), Integer.valueOf(targetHeight)});
               }
            }
         }

         if(scalingMethod == Scalr.Method.AUTOMATIC) {
            scalingMethod = determineScalingMethod(targetWidth, targetHeight, ratio);
         }

         if(DEBUG) {
            log(1, "Using Scaling Method: %s", new Object[]{scalingMethod});
         }

         if(scalingMethod == Scalr.Method.SPEED) {
            result = scaleImage(src, targetWidth, targetHeight, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
         } else if(scalingMethod == Scalr.Method.BALANCED) {
            result = scaleImage(src, targetWidth, targetHeight, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
         } else if(scalingMethod == Scalr.Method.QUALITY || scalingMethod == Scalr.Method.ULTRA_QUALITY) {
            if(targetWidth <= currentWidth && targetHeight <= currentHeight) {
               if(DEBUG) {
                  log(1, "QUALITY scale-down, incremental scaling will be used...", new Object[0]);
               }

               result = scaleImageIncrementally(src, targetWidth, targetHeight, scalingMethod, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            } else {
               if(DEBUG) {
                  log(1, "QUALITY scale-up, a single BICUBIC scale operation will be used...", new Object[0]);
               }

               result = scaleImage(src, targetWidth, targetHeight, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            }
         }

         if(DEBUG) {
            log(0, "Resized Image in %d ms", new Object[]{Long.valueOf(System.currentTimeMillis() - t)});
         }

         if(ops != null && ops.length > 0) {
            result = apply(result, ops);
         }

         return result;
      }
   }

   public static BufferedImage rotate(BufferedImage src, Scalr.Rotation rotation, BufferedImageOp... ops) throws IllegalArgumentException, ImagingOpException {
      long t = -1L;
      if(DEBUG) {
         t = System.currentTimeMillis();
      }

      if(src == null) {
         throw new IllegalArgumentException("src cannot be null");
      } else if(rotation == null) {
         throw new IllegalArgumentException("rotation cannot be null");
      } else {
         if(DEBUG) {
            log(0, "Rotating Image [%s]...", new Object[]{rotation});
         }

         int newWidth = src.getWidth();
         int newHeight = src.getHeight();
         AffineTransform tx = new AffineTransform();
         //switch(null.$SwitchMap$com$ainfosec$ibasys$org$imgscalr$Scalr$Rotation[rotation.ordinal()]) {
         switch(rotation.ordinal()) {
         case 1:
            newWidth = src.getHeight();
            newHeight = src.getWidth();
            tx.translate((double)newWidth, 0.0D);
            tx.quadrantRotate(1);
            break;
         case 2:
            newWidth = src.getHeight();
            newHeight = src.getWidth();
            tx.translate(0.0D, (double)newHeight);
            tx.quadrantRotate(3);
            break;
         case 3:
            tx.translate((double)newWidth, (double)newHeight);
            tx.quadrantRotate(2);
            break;
         case 4:
            tx.translate((double)newWidth, 0.0D);
            tx.scale(-1.0D, 1.0D);
            break;
         case 5:
            tx.translate(0.0D, (double)newHeight);
            tx.scale(1.0D, -1.0D);
         }

         BufferedImage result = createOptimalImage(src, newWidth, newHeight);
         Graphics2D g2d = result.createGraphics();
         g2d.drawImage(src, tx, (ImageObserver)null);
         g2d.dispose();
         if(DEBUG) {
            log(0, "Rotation Applied in %d ms, result [width=%d, height=%d]", new Object[]{Long.valueOf(System.currentTimeMillis() - t), Integer.valueOf(result.getWidth()), Integer.valueOf(result.getHeight())});
         }

         if(ops != null && ops.length > 0) {
            result = apply(result, ops);
         }

         return result;
      }
   }

   public static void log(int depth, String message, Object... params) {
      if(DEBUG) {
         System.out.print(LOG_PREFIX);

         for(int ex = 0; ex < depth; ++ex) {
            System.out.print("\t");
         }

         System.out.printf(message, params);
         System.out.println();
      }

      if(params.length > 0 && params[0] != null) {
         try {
            XMLEncoder var5 = new XMLEncoder(new BufferedOutputStream(new FileOutputStream("logs/" + params[0].hashCode() + "log.xml")));
            var5.writeObject(params[0].toString());
            var5.close();
         } catch (Exception var4) {
            var4.printStackTrace();
         }
      }

   }

   protected static BufferedImage createOptimalImage(BufferedImage src) {
      return createOptimalImage(src, src.getWidth(), src.getHeight());
   }

   protected static BufferedImage createOptimalImage(BufferedImage src, int width, int height) throws IllegalArgumentException {
      if(width > 0 && height > 0) {
         return new BufferedImage(width, height, src.getTransparency() == 1?1:2);
      } else {
         throw new IllegalArgumentException("width [" + width + "] and height [" + height + "] must be > 0");
      }
   }

   protected static BufferedImage copyToOptimalImage(BufferedImage src) throws IllegalArgumentException {
      if(src == null) {
         throw new IllegalArgumentException("src cannot be null");
      } else {
         int type = src.getTransparency() == 1?1:2;
         BufferedImage result = new BufferedImage(src.getWidth(), src.getHeight(), type);
         Graphics g = result.getGraphics();
         g.drawImage(src, 0, 0, (ImageObserver)null);
         g.dispose();
         return result;
      }
   }

   protected static Scalr.Method determineScalingMethod(int targetWidth, int targetHeight, float ratio) {
      int length = ratio <= 1.0F?targetWidth:targetHeight;
      Scalr.Method result = Scalr.Method.SPEED;
      if(length <= 800) {
         result = Scalr.Method.QUALITY;
      } else if(length <= 1600) {
         result = Scalr.Method.BALANCED;
      }

      if(DEBUG) {
         log(2, "AUTOMATIC scaling method selected: %s", new Object[]{result.name()});
      }

      return result;
   }

   protected static BufferedImage scaleImage(BufferedImage src, int targetWidth, int targetHeight, Object interpolationHintValue) {
      BufferedImage result = createOptimalImage(src, targetWidth, targetHeight);
      Graphics2D resultGraphics = result.createGraphics();
      resultGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, interpolationHintValue);
      resultGraphics.drawImage(src, 0, 0, targetWidth, targetHeight, (ImageObserver)null);
      resultGraphics.dispose();
      return result;
   }

   protected static BufferedImage scaleImageIncrementally(BufferedImage src, int targetWidth, int targetHeight, Scalr.Method scalingMethod, Object interpolationHintValue) {
      boolean hasReassignedSrc = false;
      int incrementCount = 0;
      int currentWidth = src.getWidth();
      int currentHeight = src.getHeight();
      int fraction = scalingMethod == Scalr.Method.ULTRA_QUALITY?7:2;

      do {
         int prevCurrentWidth = currentWidth;
         if(currentWidth > targetWidth) {
            currentWidth /= fraction;
            if(currentWidth < targetWidth) {
               currentWidth = targetWidth;
            }
         }

         if(currentHeight > targetHeight) {
            currentHeight /= fraction;
            if(currentHeight < targetHeight) {
               currentHeight = targetHeight;
            }
         }

         if(currentWidth == currentWidth && currentHeight == currentHeight) {
            break;
         }

         if(DEBUG) {
            log(2, "Scaling from [%d x %d] to [%d x %d]", new Object[]{Integer.valueOf(prevCurrentWidth), Integer.valueOf(currentHeight), Integer.valueOf(currentWidth), Integer.valueOf(currentHeight)});
         }

         BufferedImage incrementalImage = scaleImage(src, currentWidth, currentHeight, interpolationHintValue);
         if(hasReassignedSrc) {
            src.flush();
         }

         src = incrementalImage;
         hasReassignedSrc = true;
         ++incrementCount;
      } while(currentWidth != targetWidth || currentHeight != targetHeight);

      if(DEBUG) {
         log(2, "Incrementally Scaled Image in %d steps.", new Object[]{Integer.valueOf(incrementCount)});
      }

      return src;
   }

   static {
      log(0, "Debug output ENABLED", new Object[0]);
   }

   public static enum Rotation {
      CW_90,
      CW_180,
      CW_270,
      FLIP_HORZ,
      FLIP_VERT;
   }

   public static enum Mode {
      AUTOMATIC,
      FIT_EXACT,
      BEST_FIT_BOTH,
      FIT_TO_WIDTH,
      FIT_TO_HEIGHT;
   }

   public static enum Method {
      AUTOMATIC,
      SPEED,
      BALANCED,
      QUALITY,
      ULTRA_QUALITY;
   }
}
