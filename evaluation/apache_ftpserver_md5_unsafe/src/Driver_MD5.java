import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.ftpserver.usermanager.Md5PasswordEncryptor;
import org.apache.ftpserver.usermanager.PasswordEncryptor;

import edu.cmu.sv.kelinci.Kelinci;
import edu.cmu.sv.kelinci.Mem;

public class Driver_MD5 {

    public static final int MAX_PASSWORD_LENGTH = 16; // characters

    public static final boolean SAFE_MODE = false;

    public static void main(String[] args) {

        if (args.length != 1) {
            System.out.println("Expects file name as parameter");
            return;
        }

        /* Read input. */
        char[] validPassword_userInputArray_public;
        String validPassword_public;
        String storedPassword_valid_secret1;
        char[] storedPassword_array_invalid_secret2;
        String storedPassword_invalid_secret2;

        PasswordEncryptor pe = new Md5PasswordEncryptor(SAFE_MODE);

        List<Character> values = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(args[0])) {

            int i = 0;
            int value;
            while (((value = fis.read()) != -1) && (i < MAX_PASSWORD_LENGTH)) {
                /* each char value must be between 0 and 127 and a printable character */
                value = value % 127;
                char charValue = (char) value;
                if (Character.isAlphabetic(charValue) || Character.isDigit(charValue)) {
                    values.add(charValue);
                    i++;
                }
            }

            /* input must be non-empty */
            if (i == 0) {
                throw new RuntimeException("not enough data!");
            }

            validPassword_userInputArray_public = new char[values.size()];
            for (i = 0; i < values.size(); i++) {
                validPassword_userInputArray_public[i] = values.get(i);
            }
            validPassword_public = new String(validPassword_userInputArray_public);

            /* use a new String object to not have the same object, which might result in an early return during comparison */
            storedPassword_valid_secret1 = new String(pe.encrypt(validPassword_public));

            /* ensure same length for secrets */
            storedPassword_array_invalid_secret2 = new char[storedPassword_valid_secret1.length()];

            i = 0;
            while (((value = fis.read()) != -1) && (i < storedPassword_array_invalid_secret2.length)) {
                /* each char value must be between 0 and 127 and a printable character */
                value = value % 127;
                char charValue = (char) value;
                if (Character.isAlphabetic(charValue) || Character.isDigit(charValue)) {
                    storedPassword_array_invalid_secret2[i] = charValue;
                    i++;
                }
            }

            /* storedPassword_array_invalid_secret2 must be completed */
            if (i != storedPassword_array_invalid_secret2.length) {
                throw new RuntimeException("not enough data!");
            }

            storedPassword_invalid_secret2 = new String(storedPassword_array_invalid_secret2);

        } catch (IOException e) {
            System.err.println("Error reading input");
            e.printStackTrace();
            return;
        }

        System.out.println("public user input (password): " + validPassword_public);
        System.out.println("secret1 (valid stored password): " + storedPassword_valid_secret1);
        System.out.println("secret2 (invalid stored password): " + storedPassword_invalid_secret2);

        Mem.clear();
        boolean valid1 = pe.matches(validPassword_public, storedPassword_valid_secret1);
        long cost1 = Mem.instrCost;
        System.out.println("valid1=" + valid1);
        System.out.println("cost1=" + cost1);

        Mem.clear();
        boolean valid2 = pe.matches(validPassword_public, storedPassword_invalid_secret2);
        long cost2 = Mem.instrCost;
        System.out.println("valid2=" + valid2);
        System.out.println("cost2=" + cost2);

        long diff = Math.abs(cost1 - cost2);
        Kelinci.addCost(diff);
        System.out.println("diff=" + diff);

        System.out.println("Done.");
    }

}
