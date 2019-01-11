import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.cmu.sv.kelinci.Kelinci;
import edu.cmu.sv.kelinci.Mem;

public class Driver {

    public final static boolean SAFE_MODE = true;

    public static void main(String[] args) {

        if (args.length != 1) {
            System.out.println("Expects file name as parameter");
            return;
        }

        byte[] secret1_pw;
        byte[] secret2_pw;
        byte[] public_guess;

        int n = 3; // how many variables
        int maxM = 16;

        // Read all inputs.
        List<Byte> values = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(args[0])) {
            byte[] bytes = new byte[1];
            int i = 0;
            while ((fis.read(bytes) != -1) && (i < maxM * n)) {
                values.add(bytes[0]);
                i++;
            }
        } catch (IOException e) {
            System.err.println("Error reading input");
            e.printStackTrace();
            return;
        }

        if (values.size() < 3) {
            throw new RuntimeException("Not enough input data...");
        }

        int m = values.size() / 3;
        System.out.println("m=" + m);

        // Read secret1.
        secret1_pw = new byte[m];
        for (int i = 0; i < m; i++) {
            secret1_pw[i] = values.get(i);
        }

        // Read secret2.
        secret2_pw = new byte[m];
        for (int i = 0; i < m; i++) {
            secret2_pw[i] = values.get(i + m);
        }

        // Read public.
        public_guess = new byte[m];
        for (int i = 0; i < m; i++) {
            public_guess[i] = values.get(i + 2 * m);
        }

        System.out.println("secret1=" + Arrays.toString(secret1_pw));
        System.out.println("secret2=" + Arrays.toString(secret2_pw));
        System.out.println("public_guess=" + Arrays.toString(public_guess));

        boolean answer1;
        long cost1;
        if (SAFE_MODE) {
            Mem.clear();
            answer1 = PWCheck.pwcheck3_safe(public_guess, secret1_pw);
            cost1 = Mem.instrCost;
        } else {
            Mem.clear();
            answer1 = PWCheck.pwcheck1_unsafe(public_guess, secret1_pw);
            cost1 = Mem.instrCost;
        }
        System.out.println("answer1: " + answer1);
        System.out.println("cost1: " + cost1);

        boolean answer2;
        long cost2;
        if (SAFE_MODE) {
            Mem.clear();
            answer2 = PWCheck.pwcheck3_safe(public_guess, secret2_pw);
            cost2 = Mem.instrCost;
        } else {
            Mem.clear();
            answer2 = PWCheck.pwcheck1_unsafe(public_guess, secret2_pw);
            cost2 = Mem.instrCost;
        }
        System.out.println("answer2: " + answer2);
        System.out.println("cost2: " + cost2);


        Kelinci.addCost(Math.abs(cost1 - cost2));
        System.out.println("|cost1 - cost2|= " + Math.abs(cost1 - cost2));
        System.out.println("Done.");
    }

}
