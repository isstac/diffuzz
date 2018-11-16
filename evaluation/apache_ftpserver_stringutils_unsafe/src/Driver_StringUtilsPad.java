import java.io.FileInputStream;
import java.io.IOException;

import org.apache.ftpserver.util.StringUtils;

import edu.cmu.sv.kelinci.Kelinci;
import edu.cmu.sv.kelinci.Mem;

public class Driver_StringUtilsPad {

    public static final int MAX_USERNAME_LENGTH = 16; // characters

    public static final boolean SAFE_MODE = false;

    public static void main(String[] args) {

        if (args.length != 1) {
            System.out.println("Expects file name as parameter");
            return;
        }

        /* Read input. */
        int userNameLength1;
        int userNameLength2;
        String userName1 = "";
        String userName2 = "";

        try (FileInputStream fis = new FileInputStream(args[0])) {

            userNameLength1 = fis.read();
            if (userNameLength1 == -1) {
                throw new RuntimeException("not enough data!");
            }

            userNameLength2 = fis.read();
            if (userNameLength2 == -1) {
                throw new RuntimeException("not enough data!");
            }

            userNameLength1 = userNameLength1 % (MAX_USERNAME_LENGTH+1);
            userNameLength2 = userNameLength2 % (MAX_USERNAME_LENGTH+1);

            for (int i=0; i<userNameLength1; i++) {
                userName1 += "X";
            }

            for (int i=0; i<userNameLength2; i++) {
                userName2 += "X";
            }


        } catch (IOException e) {
            System.err.println("Error reading input");
            e.printStackTrace();
            return;
        }

        System.out.println("userName1Length=" + userNameLength1);
        System.out.println("userName1=" + userName1);
        System.out.println("userName2Length=" + userNameLength2);
        System.out.println("userName2=" + userName2);

        StringUtils.safeMode = SAFE_MODE;

        Mem.clear();
        StringUtils.pad(userName1, ' ', true, MAX_USERNAME_LENGTH);
        long cost1 = Mem.instrCost;
        System.out.println("cost1=" + cost1);

        Mem.clear();
        StringUtils.pad(userName2, ' ', true, MAX_USERNAME_LENGTH);
        long cost2 = Mem.instrCost;
        System.out.println("cost2=" + cost2);

        long diff = Math.abs(cost1 - cost2);
        Kelinci.addCost(diff);
        System.out.println("diff=" + diff);

        System.out.println("Done.");
    }

}
