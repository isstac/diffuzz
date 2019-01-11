import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import edu.cmu.sv.kelinci.Kelinci;
import edu.cmu.sv.kelinci.Mem;
import fr.xephi.authme.security.crypts.EncryptionMethod;
import fr.xephi.authme.security.crypts.HashedPassword;
import fr.xephi.authme.security.crypts.RoyalAuth;

public class Driver {

    public static final int MAX_INPUT_LENGTH = 16; // characters

    public static final boolean SAFE_MODE = false;

    public static void main(String[] args) {

        if (args.length != 1) {
            System.out.println("Expects file name as parameter");
            return;
        }

        EncryptionMethod encrMethod = new RoyalAuth(SAFE_MODE);

        HashedPassword storedPassword_valid_secret1;
        HashedPassword storedPassword_invalid_secret2;

        /* Read input. */
        String username_public;
        String password_public;

        List<Character> values = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(args[0])) {

            /* Read all data. */
            int i = 0;
            int value;
            while (((value = fis.read()) != -1) && (i < 3 * MAX_INPUT_LENGTH)) {
                /* each char value must be between 0 and 127 and a printable character */
                value = value % 127;
                char charValue = (char) value;
                if (Character.isAlphabetic(charValue) || Character.isDigit(charValue)) {
                    values.add(charValue);
                    i++;
                }
            }
            if (i < 3) {
                throw new RuntimeException("not enough data!");
            }

            int eachSize = values.size() / 3;
            if (eachSize % Character.BYTES == 1) {
                eachSize--;
            }

            char[] temp_arr = new char[eachSize];
            for (i = 0; i < eachSize; i++) {
                temp_arr[i] = values.get(i);
            }
            username_public = new String(temp_arr);

            temp_arr = new char[eachSize];
            for (i = 0; i < eachSize; i++) {
                temp_arr[i] = values.get(i + eachSize);
            }
            password_public = new String(temp_arr);
            storedPassword_valid_secret1 = encrMethod.computeHash(password_public, username_public);

            temp_arr = new char[eachSize];
            for (i = 0; i < eachSize; i++) {
                temp_arr[i] = values.get(i + 2 * eachSize);
            }
            storedPassword_invalid_secret2 = encrMethod.computeHash(new String(temp_arr), username_public);

        } catch (IOException e) {
            System.err.println("Error reading input");
            e.printStackTrace();
            return;
        }

        System.out.println("username_public=" + username_public);
        System.out.println("password_public=" + password_public);
        System.out.println("storedPassword_valid_secret1=" + storedPassword_valid_secret1.getHash());
        System.out.println("storedPassword_invalid_secret2=" + storedPassword_invalid_secret2.getHash());

        Mem.clear();
        boolean valid1 = encrMethod.comparePassword(password_public, storedPassword_valid_secret1, username_public);
        long cost1 = Mem.instrCost;
        System.out.println("valid1=" + valid1);
        System.out.println("cost1=" + cost1);

        Mem.clear();
        boolean valid2 = encrMethod.comparePassword(password_public, storedPassword_invalid_secret2, username_public);
        long cost2 = Mem.instrCost;
        System.out.println("valid2=" + valid2);
        System.out.println("cost2=" + cost2);

        long diff = Math.abs(cost1 - cost2);
        Kelinci.addCost(diff);
        System.out.println("diff=" + diff);

    }

}
