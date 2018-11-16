import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;

import edu.cmu.sv.kelinci.Kelinci;
import edu.cmu.sv.kelinci.Mem;

public class K96_FuzzDriver {

    public static void main(String[] args) {

        if (args.length != 1) {
            System.out.println("Expects file name as parameter");
            return;
        }

        BigInteger secret1_x = null;
        BigInteger secret2_x = null;
        BigInteger public_y = null;
        BigInteger public_n = null;

        int n = 4; // how many BigInteger variables

        byte[] secret1_bytes = null;
        byte[] secret2_bytes = null;
        byte[] public_y_bytes = null;
        byte[] public_n_bytes = null;

        // Read all inputs.
        byte[] bytes;
        try (FileInputStream fis = new FileInputStream(args[0])) {
            // Determine size of byte array.
            int fileSize = Math.toIntExact(fis.getChannel().size());
            bytes = new byte[fileSize];

            if (bytes.length < n) {
                throw new RuntimeException("too less data");
            } else {
                fis.read(bytes);
            }
        } catch (IOException e) {
            System.err.println("Error reading input");
            e.printStackTrace();
            return;
        }
        int m = bytes.length / n;

        secret1_bytes = Arrays.copyOfRange(bytes, 0, m);
        secret2_bytes = Arrays.copyOfRange(bytes, m, 2 * m);
        public_y_bytes = Arrays.copyOfRange(bytes, 2 * m, 3 * m);
        public_n_bytes = Arrays.copyOfRange(bytes, 3 * m, 4 * m);

         /* Use only positive values, first value determines the signum. */
        if (secret1_bytes[0] < 0) {
            secret1_bytes[0] = (byte) (secret1_bytes[0] * (-1) - 1);
        }
        if (secret2_bytes[0] < 0) {
            secret2_bytes[0] = (byte) (secret2_bytes[0] * (-1) - 1);
        }
        if (public_y_bytes[0] < 0) {
            public_y_bytes[0] = (byte) (public_y_bytes[0] * (-1) - 1);
        }
        if (public_n_bytes[0] < 0) {
            public_n_bytes[0] = (byte) (public_n_bytes[0] * (-1) - 1);
        }

        /* Ensure secret1 and secret2 has same bit length */
        secret1_x = new BigInteger(secret1_bytes);
        secret2_x = new BigInteger(secret2_bytes);
        // fix to 1 if number is zero, BigInteger will return 0 which is wrong, might be a bug in JDK.
        int bitLength1 = (secret1_x.equals(BigInteger.ZERO) ? 1 : secret1_x.bitLength());
        int bitLength2 = (secret2_x.equals(BigInteger.ZERO) ? 1 : secret2_x.bitLength());
        if (bitLength1 != bitLength2) {

            /*
             * Trim bigger number to smaller bit length and ensure there is the 1 in the beginning of the bit
             * representation, otherwise the zero would be trimmed again by the BigInteger constructor and hence it
             * would have a smaller bit length.
             */
            if (bitLength1 > bitLength2) {
                String bitStr1 = secret1_x.toString(2);
                bitStr1 = "1" + bitStr1.substring(bitLength1 - bitLength2 + 1);
                secret1_x = new BigInteger(bitStr1, 2);
            } else {
                String bitStr2 = secret2_x.toString(2);
                bitStr2 = "1" + bitStr2.substring(bitLength2 - bitLength1 + 1);
                secret2_x = new BigInteger(bitStr2, 2);
            }
        }
        bitLength1 = (secret1_x.equals(BigInteger.ZERO) ? 1 : secret1_x.bitLength());
        bitLength2 = (secret2_x.equals(BigInteger.ZERO) ? 1 : secret2_x.bitLength());

        /* We do not care about the bit length of the public values. */
        public_y = new BigInteger(public_y_bytes);
        public_n = new BigInteger(public_n_bytes); // TODO may fix the modulus value here
        // Ensure that modulus is not zero.
        if (public_n.equals(BigInteger.ZERO)) {
            public_n = BigInteger.ONE;
        }

        System.out.println("secret1_x=" + secret1_x);
        System.out.println("secret1_x.bitlength=" + secret1_x.bitLength());
        // System.out.println("secret1_x=" + secret1_x.toString(2));
        System.out.println("secret2_x=" + secret2_x);
        System.out.println("secret2_x.bitlength=" + secret2_x.bitLength());
        // System.out.println("secret2_x=" + secret2_x.toString(2));
        System.out.println("public_y=" + public_y);
        System.out.println("public_y.bitlength=" + public_y.bitLength());
        System.out.println("public_n=" + public_n);
        System.out.println("public_n.bitlength=" + public_n.bitLength());

        Mem.clear();
        BigInteger result1 = K96.modular_exponentiation_unsafe(public_y, secret1_x, public_n, bitLength1);
        System.out.println("Answer1: " + result1);
        long cost1 = Mem.instrCost;
        System.out.println("cost1=" + cost1);
        Mem.clear();

         BigInteger result2 = K96.modular_exponentiation_unsafe(public_y, secret2_x, public_n, bitLength2);
        System.out.println("Answer2: " + result2);
        long cost2 = Mem.instrCost;
        System.out.println("cost2=" + cost2);
        System.out.println("|cost1-cost2|=" + Math.abs(cost1 - cost2));
        
        Kelinci.addCost(Math.abs(cost1 - cost2));

        System.out.println("Done.");
    }

}
