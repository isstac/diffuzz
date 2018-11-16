import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import edu.cmu.sv.kelinci.Kelinci;
import edu.cmu.sv.kelinci.Mem;

public class Sanity_FuzzDriver {

    public static void main(String[] args) {

        if (args.length != 1) {
            System.out.println("Expects file name as parameter");
            return;
        }

        int secret1_a = 0;
        int secret2_a = 0;
        int public_b = 0;

        int n = 3; // how many variables

        // Read all inputs.
        try (FileInputStream fis = new FileInputStream(args[0])) {
            byte[] bytes = new byte[Integer.BYTES];
            int i = 0;
            while ((fis.read(bytes) != -1) && (i < n)) {
                switch (i) {
                case 0:
                    secret1_a = ByteBuffer.wrap(bytes).getInt();
                    break;
                case 1:
                    secret2_a = ByteBuffer.wrap(bytes).getInt();
                    break;
                case 2:
                    public_b = ByteBuffer.wrap(bytes).getInt();
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

        System.out.println("secret1=" + secret1_a);
        System.out.println("secret2=" + secret2_a);
        System.out.println("public=" + public_b);

        boolean answer1 = Sanity.straightline_safe(secret1_a, public_b);
        long cost1 = Mem.instrCost;
        System.out.println("Answer1: " + answer1);
        System.out.println("cost1= " + cost1);

        Mem.clear();

        boolean answer2 = Sanity.straightline_safe(secret2_a, public_b);
        long cost2 = Mem.instrCost;
        System.out.println("Answer2: " + answer2);
        System.out.println("cost2= " + cost2);
        System.out.println("|cost1-cost2|= " + Math.abs(cost1 - cost2));

        Kelinci.addCost(Math.abs(cost1 - cost2));

        System.out.println("Done.");
    }

}
