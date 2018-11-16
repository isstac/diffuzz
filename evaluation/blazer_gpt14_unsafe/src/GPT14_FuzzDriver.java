import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;

import edu.cmu.sv.kelinci.Kelinci;
import edu.cmu.sv.kelinci.Mem;

public class GPT14_FuzzDriver {

    public static void main(String[] args) {

         if (args.length != 1) {
            System.out.println("Expects file name as parameter");
            return;
        }

        BigInteger secret1_b = null;
        BigInteger secret2_b = null;
        BigInteger public_a = null;
        BigInteger public_p = null;

        int n = 4; // how many BigInteger variables

        byte[] secret1_bytes = null;
        byte[] secret2_bytes = null;
        byte[] public_base_bytes = null;
        byte[] public_modulus_bytes = null;

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
        public_base_bytes = Arrays.copyOfRange(bytes, 2 * m, 3 * m);
        public_modulus_bytes = Arrays.copyOfRange(bytes, 3 * m, 4 * m);

         /* Use only positive values, first value determines the signum. */
        if (secret1_bytes[0] < 0) {
            secret1_bytes[0] = (byte) (secret1_bytes[0] * (-1) - 1);
        }
        if (secret2_bytes[0] < 0) {
            secret2_bytes[0] = (byte) (secret2_bytes[0] * (-1) - 1);
        }
        if (public_base_bytes[0] < 0) {
            public_base_bytes[0] = (byte) (public_base_bytes[0] * (-1) - 1);
        }
        if (public_modulus_bytes[0] < 0) {
            public_modulus_bytes[0] = (byte) (public_modulus_bytes[0] * (-1) - 1);
        }

        /* Ensure secret1 and secret2 has same bit length */
        secret1_b = new BigInteger(secret1_bytes);
        secret2_b = new BigInteger(secret2_bytes);
        // fix to 1 if number is zero, BigInteger will return 0 which is wrong, might be a bug in JDK.
        int bitLength1 = (secret1_b.equals(BigInteger.ZERO) ? 1 : secret1_b.bitLength());
        int bitLength2 = (secret2_b.equals(BigInteger.ZERO) ? 1 : secret2_b.bitLength());
        if (bitLength1 != bitLength2) {

            /*
             * Trim bigger number to smaller bit length and ensure there is the 1 in the beginning of the bit
             * representation, otherwise the zero would be trimmed again by the BigInteger constructor and hence it
             * would have a smaller bit length.
             */
            if (bitLength1 > bitLength2) {
                String bitStr1 = secret1_b.toString(2);
                bitStr1 = "1" + bitStr1.substring(bitLength1 - bitLength2 + 1);
                secret1_b = new BigInteger(bitStr1, 2);
            } else {
                String bitStr2 = secret2_b.toString(2);
                bitStr2 = "1" + bitStr2.substring(bitLength2 - bitLength1 + 1);
                secret2_b = new BigInteger(bitStr2, 2);
            }
        }
        bitLength1 = (secret1_b.equals(BigInteger.ZERO) ? 1 : secret1_b.bitLength());
        bitLength2 = (secret2_b.equals(BigInteger.ZERO) ? 1 : secret2_b.bitLength());

        /* We do not care about the bit length of the public values. */
        public_a = new BigInteger(public_base_bytes);
        public_p = new BigInteger(public_modulus_bytes); // TODO may fix the modulus value here
        // Ensure that modulus is not zero.
        if (public_p.equals(BigInteger.ZERO)) {
            public_p = BigInteger.ONE;
        }

        System.out.println("secret1_b=" + secret1_b);
        System.out.println("secret1_b.bitlength=" + secret1_b.bitLength());
        System.out.println("secret1_b=" + secret1_b.toString(2));
        System.out.println("secret2_b=" + secret2_b);
        System.out.println("secret2_b.bitlength=" + secret2_b.bitLength());
        System.out.println("secret2_b=" + secret2_b.toString(2));
        System.out.println("public_a=" + public_a);
        System.out.println("public_a.bitlength=" + public_a.bitLength());
        System.out.println("public_p=" + public_p);
        System.out.println("public_p.bitlength=" + public_p.bitLength());


        BigInteger result1 = GPT14.modular_exponentiation_inline_unsafe(public_a, secret1_b, public_p);
        System.out.println("Answer1: " + result1);
        long cost1 = Mem.instrCost;
        System.out.println("cost1=" + cost1);
        Mem.clear();

        BigInteger result2 = GPT14.modular_exponentiation_inline_unsafe(public_a, secret2_b, public_p);
        System.out.println("Answer2: " + result2);
        long cost2 = Mem.instrCost;
        System.out.println("cost2=" + cost2);

        Kelinci.addCost(Math.abs(cost1 - cost2));

        System.out.println("Done.");
    }

}
