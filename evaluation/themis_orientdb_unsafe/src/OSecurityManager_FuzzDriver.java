import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import edu.cmu.sv.kelinci.Kelinci;
import edu.cmu.sv.kelinci.Mem;

public class OSecurityManager_FuzzDriver {
    public static void main(String[] args) {

    	 if (args.length != 1) {
             System.out.println("Expects file name as parameter");
             return;
         }
         int n = 3;
         String public_ihash;
         String secret_iPassword1;
         String secret_iPassword2;

      // Read all inputs.
         List<Character> values = new ArrayList<>();
         try (FileInputStream fis = new FileInputStream(args[0])) {
             byte[] bytes = new byte[Character.BYTES];
             int i = 0;
             while (((fis.read(bytes)) != -1)) {
                 char value = ByteBuffer.wrap(bytes).getChar();
                 values.add(value);
                 i++;
             }
         } catch (IOException e) {
             System.err.println("Error reading input");
             e.printStackTrace();
             return;
         }
         if (values.size() < n) {
             throw new RuntimeException("Too less data...");
         }

         int m = values.size() / n;
         System.out.println("m=" + m);

         // Read secret1.
         char[] secret1_arr = new char[m];
         for (int i = 0; i < m; i++) {
             secret1_arr[i] = values.get(i);
         }
         secret_iPassword1 = new String(secret1_arr);

         // Read secret2.
         char[] secret2_arr = new char[m];
         for (int i = 0; i < m; i++) {
             secret2_arr[i] = values.get(i + m);
         }
         secret_iPassword2= new String(secret2_arr);

         // Read public.
         char[] public_arr = new char[m];
         for (int i = 0; i < m; i++) {
             public_arr[i] = values.get(i + 2 * m);
         }
         public_ihash = new String(public_arr);

         System.out.println("secret_iPassword1=" + secret_iPassword1);
         System.out.println("secret_iPassword2=" + secret_iPassword2);
         System.out.println("public_ihash=" + public_ihash);

        Mem.clear();
        OSecurityManager manager = new OSecurityManager();
        manager.checkPassword_unsafe(secret_iPassword1, public_ihash);

        long cost1 = Mem.instrCost;
        System.out.println("cost1=" + cost1);

        Mem.clear();
        manager.checkPassword_unsafe(secret_iPassword2, public_ihash);
        
        long cost2 = Mem.instrCost;
        System.out.println("cost2=" + cost2);

        Kelinci.addCost(Math.abs(cost1 - cost2));
        System.out.println("|cost1-cost2|=" + Math.abs(cost2-cost1));

        System.out.println("Done.");
    }

}
