import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;

import edu.cmu.sv.kelinci.Kelinci;
import edu.cmu.sv.kelinci.Mem;

public class CRIME_Driver {

//    static int SIZE = 10;
    public static void main(String[] args) throws IOException {
        byte[] h1;// = new byte[SIZE]; // secret
        byte[] h2;// = new byte[SIZE]; // secret
        byte[] l;// = new byte[SIZE]; // public

       // Read all inputs.
        byte[] bytes;
        try (FileInputStream fis = new FileInputStream(args[0])) {
            // Determine size of byte array.
            int fileSize = Math.toIntExact(fis.getChannel().size());
            bytes = new byte[fileSize];
            if (bytes.length < 3) {
                throw new RuntimeException("too less data");
            } else {
                fis.read(bytes);
            }
        } catch (IOException e) {
            System.err.println("Error reading input");
            e.printStackTrace();
            return;
        }
        int SIZE = bytes.length / 3;
        
        h1 = Arrays.copyOfRange(bytes, 0, SIZE);
        h2 = Arrays.copyOfRange(bytes, SIZE, 2*SIZE);
        l = Arrays.copyOfRange(bytes, 2*SIZE, 3*SIZE);
        
        System.out.println("h1=" + h1);
        System.out.println("h2=" + h2);
        System.out.println("l=" + l);

        byte[] cookie = { 'c', 'o', 'o', 'k', 'i', 'e' };
        byte[] h1_cookie = Arrays.copyOf(h1, h1.length + cookie.length);
        byte[] l_cookie = Arrays.copyOf(l, l.length + cookie.length);
        System.arraycopy(cookie, 0, h1_cookie, h1.length, cookie.length);
        System.arraycopy(cookie, 0, l_cookie, l.length, cookie.length);

        final byte[] all1 = Arrays.copyOf(h1_cookie, h1_cookie.length + l_cookie.length);
        System.arraycopy(l_cookie, 0, all1, h1_cookie.length, l_cookie.length);

        final byte[] compressed1 = LZ77T.compress(all1);
        long cost1 = compressed1.length;
        System.out.println("cost1=" + cost1);
        System.out.println("observable1: " + cost1);
        
        byte[] h2_cookie = Arrays.copyOf(h2, h2.length + cookie.length);
        System.arraycopy(cookie, 0, h2_cookie, h1.length, cookie.length);
        System.arraycopy(cookie, 0, l_cookie, l.length, cookie.length);

        final byte[] all2 = Arrays.copyOf(h2_cookie, h2_cookie.length
                + l_cookie.length);
        System.arraycopy(l_cookie, 0, all2, h2_cookie.length, l_cookie.length);

        final byte[] compressed2 = LZ77T.compress(all2);
        long cost2 = compressed2.length;
        System.out.println("observable2: "+cost2);
        
        Kelinci.addCost(Math.abs(cost1 - cost2));
        System.out.println("|cost1-cost2|=" + Math.abs(cost2-cost1));

        System.out.println("Done.");
    }

}