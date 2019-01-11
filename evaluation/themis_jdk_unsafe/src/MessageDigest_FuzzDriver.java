import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.cmu.sv.kelinci.Kelinci;
import edu.cmu.sv.kelinci.Mem;

public class MessageDigest_FuzzDriver {

    public static void main(String[] args) {

        if (args.length != 1) {
            System.out.println("Expects file name as parameter");
            return;
        }

        int n =3;
        // int maxM= 64;
        // System.out.println("maxM=" + maxM);

        byte[] secret1_digesta;
        byte[] secret2_digesta;
        byte[] public_digestb;

        // Read all inputs.
        List<Byte> values = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(args[0])) {
            byte[] bytes = new byte[1];
            int i = 0;
            while ((fis.read(bytes) != -1) ) {//&& (i < maxM * n)
                values.add(bytes[0]);
                i++;
            }
        } catch (IOException e) {
            System.err.println("Error reading input...");
            e.printStackTrace();
            return;
        }

        if (values.size() < 3) {
            throw new RuntimeException("Too Less Data...");
        }

        int m = values.size() / 3;
        System.out.println("m=" + m);

        // Read secret 1.
        secret1_digesta = new byte[m];
        for (int i = 0; i < m; i++) {
            secret1_digesta[i] = values.get(i);
        }

        // Read user secret 2.
        secret2_digesta = new byte[m];
        for (int i = 0; i < m; i++) {
            secret2_digesta[i] = values.get(i + m);
        }

        // Read user public.
        public_digestb = new byte[m];
        for (int i = 0; i < m; i++) {
            public_digestb[i] = values.get(i + 2 * m);
        }

        System.out.println("secret1_digesta=" + Arrays.toString(secret1_digesta));
        System.out.println("secret2_digesta=" + Arrays.toString(secret2_digesta));
        System.out.println("public_digestb=" + Arrays.toString(public_digestb));

        Mem.clear();
        boolean answer1 = MessageDigest.isEqual_unsafe(secret1_digesta, public_digestb);
        System.out.println("Answer1: " + answer1);

        long cost1 = Mem.instrCost;
        System.out.println("cost1= " + cost1);
        Mem.clear();

        boolean answer2 = MessageDigest.isEqual_unsafe(secret2_digesta, public_digestb);
        System.out.println("Answer2: " + answer2);

        long cost2 = Mem.instrCost;
        System.out.println("cost2= " + cost2);
        System.out.println("|cost1 - cost2|= " + Math.abs(cost1 - cost2));

        Kelinci.addCost(Math.abs(cost1 - cost2));

        System.out.println("Done.");
    }

}
