import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import edu.cmu.sv.kelinci.Kelinci;
import edu.cmu.sv.kelinci.Mem;

public class User_FuzzDriver {

    public final static boolean SAFE_MODE = false;

    public static void main(String[] args) {

        if (args.length != 1) {
            System.out.println("Expects file name as parameter");
            return;
        }

        int n = 3;
        int maxLen = 16;

        String secret1;
        String secret2;
        String publicVal;

        List<Character> values = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(args[0])) {
            byte[] bytes = new byte[Character.BYTES];
            int i = 0;
            while (((fis.read(bytes)) != -1) && (i < n * maxLen * Character.BYTES)) {
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
            throw new RuntimeException("");
        }

        int m = values.size() / 3;
        if (m % 2 == 1) {
            m--;
        }
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
        publicVal = new String(public_arr);

        System.out.println("secret1=" + secret1);
        System.out.println("secret2=" + secret2);
        System.out.println("public=" + publicVal);

        boolean answer1;
        long cost1;
        if (SAFE_MODE) {
            Mem.clear();
            answer1 = User.passwordsEqual_safe(secret1, publicVal);
            cost1 = Mem.instrCost;
        } else {
            Mem.clear();
            answer1 = User.passwordsEqual_unsafe(secret1, publicVal);
            cost1 = Mem.instrCost;
        }
        System.out.println("answer1: " + answer1);
        System.out.println("cost1: " + cost1);

        boolean answer2;
        long cost2;
        if (SAFE_MODE) {
            Mem.clear();
            answer2 = User.passwordsEqual_safe(secret2, publicVal);
            cost2 = Mem.instrCost;
        } else {
            Mem.clear();
            answer2 = User.passwordsEqual_unsafe(secret2, publicVal);
            cost2 = Mem.instrCost;
        }
        System.out.println("answer2: " + answer2);
        System.out.println("cost2: " + cost2);

        long diffCost = Math.abs(cost1 - cost2);
        Kelinci.addCost(diffCost);
        System.out.println("diffCost: " + diffCost);

        System.out.println("Done.");

    }

}
