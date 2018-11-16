import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import edu.cmu.sv.kelinci.Kelinci;
import edu.cmu.sv.kelinci.Mem;

public class MoreSanity_LoopAndBranch_FuzzDriver {

    public static void main(String[] args) {

        if (args.length != 1) {
            System.out.println("Expects file name as parameter");
            return;
        }

        int secret1_taint = 0;
        int secret2_taint = 0;
        int public_a = 0;

        int n = 3; // how many variables

        // Read all inputs.
        try (FileInputStream fis = new FileInputStream(args[0])) {
            byte[] bytes = new byte[Integer.BYTES];
            int i = 0;
            while ((fis.read(bytes) != -1) && (i < n)) {
                switch (i) {
                case 0:
                    secret1_taint = ByteBuffer.wrap(bytes).getInt();
                    break;
                case 1:
                    secret2_taint = ByteBuffer.wrap(bytes).getInt();
                    break;
                case 2:
                    public_a = ByteBuffer.wrap(bytes).getInt();
                    break;
                default:
                    throw new RuntimeException("unreachable");
                }
                i++;
            }

            if (i != n) {
                throw new RuntimeException("reading imcomplete: too less data");
            }

        } catch (IOException e) {
            System.err.println("Error reading input");
            e.printStackTrace();
            return;
        }

        System.out.println("secret1=" + secret1_taint);
        System.out.println("secret2=" + secret1_taint);
        System.out.println("public=" + public_a);

        boolean answer1 = MoreSanity.loopAndbranch_safe(public_a, secret1_taint);
        System.out.println("Answer1: " + answer1);

        long cost1 = Mem.instrCost;
        Mem.clear();

        boolean answer2 = MoreSanity.loopAndbranch_safe(public_a, secret2_taint);
        System.out.println("Answer2: " + answer2);

        long cost2 = Mem.instrCost;

        Kelinci.addCost(Math.abs(cost1 - cost2));

        System.out.println("Done.");
    }

}
