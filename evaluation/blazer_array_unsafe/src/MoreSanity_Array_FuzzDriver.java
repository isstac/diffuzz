import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.cmu.sv.kelinci.Kelinci;
import edu.cmu.sv.kelinci.Mem;

public class MoreSanity_Array_FuzzDriver {

    public static void main(String[] args) {

        if (args.length != 1) {
            System.out.println("Expects file name as parameter");
            return;
        }

        int[] public_a;
        int secret1_taint = 0;
        int secret2_taint = 0;

        int n = 3; // how many variables
        int maxM = 64;

        List<Integer> highValues = new ArrayList<>();

        // Read all inputs.
        try (FileInputStream fis = new FileInputStream(args[0])) {
            byte[] bytes = new byte[Integer.BYTES];
            int i = 0;
            int maximumIterations = maxM * 1 + 2;
            while ((fis.read(bytes) != -1) && (i < maximumIterations)) {
                if (i == 0) {
                    secret1_taint = ByteBuffer.wrap(bytes).getInt();
                } if (i == 1) {
                    secret2_taint = ByteBuffer.wrap(bytes).getInt();
                }else {
                    highValues.add(ByteBuffer.wrap(bytes).getInt());
                }
                i++;
            }
        } catch (IOException e) {
            System.err.println("Error reading input");
            e.printStackTrace();
            return;
        }
        if (highValues.size() < 2) {
            throw new RuntimeException("Too less data!");
        }

        public_a = new int[highValues.size()];
        for (int i = 0; i < highValues.size(); i++) {
            public_a[i] = highValues.get(i);
        }

        System.out.println("secret1=" + secret1_taint);
        System.out.println("secret2=" + secret2_taint);
        System.out.println("public=" + Arrays.toString(public_a));

        boolean answer1 = MoreSanity.array_unsafe(public_a, secret1_taint);
        System.out.println("Answer1: " + answer1);

        long cost1 = Mem.instrCost;
        Mem.clear();

        boolean answer2 = MoreSanity.array_unsafe(public_a, secret2_taint);
        System.out.println("Answer2: " + answer2);

        long cost2 = Mem.instrCost;

        Kelinci.addCost(Math.abs(cost1 - cost2));
        System.out.println("|cost1 - cost2|= " + Math.abs(cost1 - cost2));


        System.out.println("Done.");
    }

}
