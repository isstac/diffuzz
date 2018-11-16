import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import javax.imageio.ImageIO;
//
import edu.cmu.sv.kelinci.Kelinci;
import edu.cmu.sv.kelinci.Mem;

public class ImageMatcher_FuzzDriver {
	public static byte[] extractBytes (String ImageName) {
		try {
			byte[] imageInByte;
			BufferedImage originalImage = ImageIO.read(new File(ImageName));

			// convert BufferedImage to byte array
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(originalImage, "jpg", baos);
			baos.flush();
			imageInByte = baos.toByteArray();
			baos.close();
			return(imageInByte) ;
		} catch (IOException e) {
			e.printStackTrace();
			return(null);
		}
	}

	public static byte[] createImagePasscode(byte[] image) {
	     try {
	           System.out.println("Creating passcode");
	           ScalrApplyTest b = new ScalrApplyTest();
	           ScalrApplyTest.setup(image);
	           BufferedImage p = b.testApply1();
	           int r = p.getWidth();
	           int h = p.getHeight();
	           int[] imageDataBuff = p.getRGB(0, 0, r, h, (int[])null, 0, r);
	           ByteBuffer byteBuffer = ByteBuffer.allocate(imageDataBuff.length * 4);
	           IntBuffer intBuffer = byteBuffer.asIntBuffer();
	           intBuffer.put(imageDataBuff);
	           ByteArrayOutputStream baos = new ByteArrayOutputStream();
	           baos.write(byteBuffer.array());
	           baos.flush();
	           baos.close();
	           System.out.println("Image Done");
	           ScalrApplyTest.tearDown();
	           byte[] pcodetest = new byte[128];
	           int csize = imageDataBuff.length / 128;
	           int ii = 0;

	           for(int i1 = 0; i1 < csize * 128; i1 += csize) {
	              pcodetest[ii] = (byte)(imageDataBuff[i1] % 2);
	              ++ii;
	           }
	           return(pcodetest);
	        } catch (Exception var15) {
	           System.out.println("worker ended, error: " + var15.getMessage());
	           return(null);
	        }
	}

	public static void firstInput (String ImageName) {
			byte[] image = extractBytes(ImageName);

			byte[] passcode = new byte[128];
			new Random().nextBytes(passcode); //createImagePasscode(image);
			byte[] passcode2 = new byte[128];
			new Random().nextBytes(passcode2);
			try (FileOutputStream fos = new FileOutputStream("in_dir/example.txt")) {
				fos.write(passcode);
				fos.write(passcode2);
				fos.write(image);
			} catch (IOException e) {
				e.printStackTrace();
			}
	}
//	public static void main(String[] args) {
//		firstInput("image.jpeg");
//	}
    public static void main(String[] args) {

        if (args.length != 1) {
            System.out.println("Expects file name as parameter");
            return;
        }

        byte[] secret1_pw;
        byte[] secret2_pw;
        byte[] public_guess;

        int pcode_length = 128;
        int n = 3; // how many variables

        // Read all inputs.
        List<Byte> values = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(args[0])) {
            byte[] bytes = new byte[1];
            int i = 0;
            while ((fis.read(bytes) != -1)) {
                values.add(bytes[0]);
                i++;
            }
        } catch (IOException e) {
            System.err.println("Error reading input");
            e.printStackTrace();
            return;
        }

        if (values.size() < pcode_length * 2 + 1) {
            throw new RuntimeException("Not enough input data...");
        }

        // Read secret1.
        secret1_pw = new byte[pcode_length];
        for (int i = 0; i < pcode_length; i++) {
            secret1_pw[i] = values.get(i);
        }

        // Read secret2.
        secret2_pw = new byte[pcode_length];
        for (int i = 0; i < pcode_length; i++) {
            secret2_pw[i] = values.get(i + pcode_length);
        }

        // Read public.
        int m = values.size() - (2 * pcode_length);
        System.out.println("m=" + m);

        public_guess = new byte[m];
        for (int i = 0; i < m; i++) {
            public_guess[i] = values.get(i + 2 * pcode_length);
        }

        System.out.println("secret1=" + Arrays.toString(secret1_pw));
        System.out.println("secret2=" + Arrays.toString(secret2_pw));
        System.out.println("public_guess=" + Arrays.toString(public_guess));

        ImageMatcherWorker.test(public_guess, secret1_pw);
        long cost1 = Mem.instrCost;
        System.out.println("cost1: " + cost1);
        Mem.clear();
        ImageMatcherWorker.test(public_guess, secret2_pw);

        long cost2 = Mem.instrCost;
        System.out.println("cost2: " + cost2);

        Kelinci.addCost(Math.abs(cost1 - cost2));
        System.out.println("|cost1 - cost2|= " + Math.abs(cost1 - cost2));

        System.out.println("Done.");
    }

}
