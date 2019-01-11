import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import edu.cmu.sv.kelinci.Kelinci;
import edu.cmu.sv.kelinci.Mem;

public class PasswordEncoderUtils_FuzzDriver {
    public static void main(String[] args) {

        if (args.length != 1) {
            System.out.println("Expects file name as parameter");
            return;
        }

        String secret1_expected;
        String secret2_expected;
        String public_actual;

        int n = 3;
        int maxM = 16;

       // Read all inputs.
        List<Character> values = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(args[0])) {
            byte[] bytes = new byte[Character.BYTES];
            int i = 0;
            while (((fis.read(bytes)) != -1) && (i < maxM * n)) {
                char value = ByteBuffer.wrap(bytes).getChar();
                values.add(value);
                i++;
            }
        } catch (IOException e) {
            System.err.println("Error reading input");
            e.printStackTrace();
            return;
        }

        if (values.size() < 3) {
            throw new RuntimeException("Too less data...");
        }

        int m = values.size() / 3;
        System.out.println("m=" + m);

        // Read secret1.
        char[] secret1_arr = new char[m];
        for (int i = 0; i < m; i++) {
            secret1_arr[i] = values.get(i);
        }
        secret1_expected = new String(secret1_arr);

        // Read secret2.
        char[] secret2_arr = new char[m];
        for (int i = 0; i < m; i++) {
            secret2_arr[i] = values.get(i + m);
        }
        secret2_expected = new String(secret2_arr);

        // Read public.
        char[] public_arr = new char[m];
        for (int i = 0; i < m; i++) {
            public_arr[i] = values.get(i + 2 * m);
        }
        public_actual = new String(public_arr);

        System.out.println("secret1_expected=" + secret1_expected);
        System.out.println("secret2_expected=" + secret2_expected);
        System.out.println("public_actual=" + public_actual);


        Mem.clear();
        boolean result1 = PasswordEncoderUtils.equals_unsafe(secret1_expected, public_actual);
        System.out.println("Answer1: " + result1);
        long cost1 = Mem.instrCost;
        System.out.println("cost1=" + cost1);
        Mem.clear();

         boolean result2 = PasswordEncoderUtils.equals_unsafe(secret2_expected, public_actual);
        System.out.println("Answer2: " + result2);
        long cost2 = Mem.instrCost;
        System.out.println("cost2=" + cost2);
        System.out.println("|cost1-cost2|=" + Math.abs(cost1 - cost2));

        Kelinci.addCost(Math.abs(cost1 - cost2));

        System.out.println("Done.");
    }

}
