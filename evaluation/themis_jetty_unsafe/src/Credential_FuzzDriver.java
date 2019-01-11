import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import edu.cmu.sv.kelinci.Kelinci;
import edu.cmu.sv.kelinci.Mem;

public class Credential_FuzzDriver {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Expects file name as parameter");
            return;
        }

        String secret1;
        String secret2;
        String publicCred;

        // Read all inputs.
        List<Character> values = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(args[0])) {
            byte[] bytes = new byte[Character.BYTES];
            while (((fis.read(bytes)) != -1)) {
                char value = ByteBuffer.wrap(bytes).getChar();
                values.add(value);
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
        secret1 = new String(secret1_arr);

        // Read secret2.
        char[] secret2_arr = new char[m];
        for (int i = 0; i < m; i++) {
            secret2_arr[i] = values.get(i + m);
        }
        secret2 = new String(secret2_arr);

        // Read public.
        char[] public_arr = new char[m];
        for (int i = 0; i < m; i++) {
            public_arr[i] = values.get(i + 2 * m);
        }
        publicCred = new String(public_arr);

        System.out.println("secret1_expected=" + secret1);
        System.out.println("secret2_expected=" + secret2);
        System.out.println("public_actual=" + publicCred);

        Mem.clear();
        boolean result1 = Credential.stringEquals_original(publicCred, secret1);
        long cost1 = Mem.instrCost;
	      System.out.println("Answer1: " + result1);
        System.out.println("cost1=" + cost1);

	      Mem.clear();
        boolean result2 = Credential.stringEquals_original(publicCred, secret2);
        long cost2 = Mem.instrCost;
        System.out.println("Answer2: " + result2);
	      System.out.println("cost2=" + cost2);

        System.out.println("|cost1-cost2|=" + Math.abs(cost1 - cost2));
        Kelinci.addCost(Math.abs(cost1 - cost2));

        System.out.println("Done.");
    }

}
