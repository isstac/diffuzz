import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import edu.cmu.sv.kelinci.Kelinci;
import edu.cmu.sv.kelinci.Mem;

/* Side-Channel not for password but to check whether username exists in system. */
public class Timing_FuzzDriver {

    public static final int MAX_PASSWORD_LENGTH = 16; // bytes

    public static void main(String[] args) {

        if (args.length != 1) {
            System.out.println("Expects file name as parameter");
            return;
        }

        int n = 6;

        String username;
        String password;
        String username_secret1;
        String password_secret1;
        String username_secret2;
        String password_secret2;

        try (FileInputStream fis = new FileInputStream(args[0])) {

            /* Read all data. */
            int i = 0;
            int value;
            List<Character> values = new ArrayList<>();
            while (((value = fis.read()) != -1) && (i < 3 * MAX_PASSWORD_LENGTH)) {
                /* each char value must be between 0 and 127 and a printable character */
                value = value % 127;
                char charValue = (char) value;
                if (Character.isAlphabetic(charValue) || Character.isDigit(charValue)) {
                    values.add(charValue);
                    i++;
                }
            }
            int eachSize = values.size() / n;
            if (eachSize % Character.BYTES == 1) {
                eachSize--;
            }
            /* input must be non-empty */
            if (eachSize < Character.BYTES) {
                throw new RuntimeException("not enough data!");
            }

            char[] temp_arr = new char[eachSize];
            for (i = 0; i < eachSize; i++) {
                temp_arr[i] = values.get(i);
            }
            username = new String(temp_arr);

            temp_arr = new char[eachSize];
            for (i = 0; i < eachSize; i++) {
                temp_arr[i] = values.get(i + eachSize);
            }
            password = new String(temp_arr);

            temp_arr = new char[eachSize];
            for (i = 0; i < eachSize; i++) {
                temp_arr[i] = values.get(i + 2 * eachSize);
            }
            username_secret1 = new String(temp_arr);

            temp_arr = new char[eachSize];
            for (i = 0; i < eachSize; i++) {
                temp_arr[i] = values.get(i + 3 * eachSize);
            }
            password_secret1 = new String(temp_arr);

            temp_arr = new char[eachSize];
            for (i = 0; i < eachSize; i++) {
                temp_arr[i] = values.get(i + 4 * eachSize);
            }
            username_secret2 = new String(temp_arr);

            temp_arr = new char[eachSize];
            for (i = 0; i < eachSize; i++) {
                temp_arr[i] = values.get(i + 5 * eachSize);
            }
            password_secret2 = new String(temp_arr);

        } catch (IOException e) {
            System.err.println("Error reading input");
            e.printStackTrace();
            return;
        }

        System.out.println("username=" + username);
        System.out.println("password=" + password);
        System.out.println("username_secret1=" + username_secret1);
        System.out.println("password_secret1=" + password_secret1);
        System.out.println("username_secret2=" + username_secret2);
        System.out.println("password_secret2=" + password_secret2);

        Timing.resetMap(username_secret1, password_secret1);
        Mem.clear();
        boolean answer1 = Timing.login_unsafe(username, password);
        long cost1 = Mem.instrCost;
        System.out.println("Answer1: " + answer1);
        System.out.println("cost1=" + cost1);

        Timing.resetMap(username_secret2, password_secret2);
        Mem.clear();
        boolean answer2 = Timing.login_unsafe(username, password);
        long cost2 = Mem.instrCost;
        System.out.println("Answer2: " + answer2);
        System.out.println("cost2=" + cost2);

        long diff = Math.abs(cost1 - cost2);
        Kelinci.addCost(diff);
        System.out.println("diff=" + diff);

        System.out.println("Done.");

    }

}
