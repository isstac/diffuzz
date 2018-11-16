import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.openmrs.util.Security;

import edu.cmu.sv.kelinci.Kelinci;
import edu.cmu.sv.kelinci.Mem;

public class Driver {

    public static final int MAX_PASSWORD_LENGTH = 16; // characters

    public static void main(String[] args) {

        if (args.length != 1) {
            System.out.println("Expects file name as parameter");
            return;
        }

        /* Read input. */
        String validPassword_public;
        String storedPassword_secret1;
        String storedPassword_secret2;

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
            /* input must be non-empty */
            if (i < 3) {
                throw new RuntimeException("not enough data!");
            }

            int eachSize = values.size() / 3;
            if (eachSize % Character.BYTES == 1) {
                eachSize--;
            }

            char[] tmp_array = new char[eachSize];
            for (i = 0; i < eachSize; i++) {
                tmp_array[i] = values.get(i);
            }
            validPassword_public = new String(tmp_array);

            tmp_array = new char[eachSize];
            for (i = 0; i < eachSize; i++) {
                tmp_array[i] = values.get(i + eachSize);
            }
            storedPassword_secret1 = Security.encodeString(new String(tmp_array));

            tmp_array = new char[eachSize];
            for (i = 0; i < eachSize; i++) {
                tmp_array[i] = values.get(i + 2 * eachSize);
            }
            storedPassword_secret2 = Security.encodeString(new String(tmp_array));

        } catch (IOException e) {
            System.err.println("Error reading input");
            e.printStackTrace();
            return;
        }

        System.out.println("password=" + validPassword_public);
        System.out.println("valid hash=" + Security.encodeString(validPassword_public));
        System.out.println("secret1=" + storedPassword_secret1);
        System.out.println("secret2=" + storedPassword_secret2);

        Mem.clear();
        boolean valid1 = Security.hashMatches(storedPassword_secret1, validPassword_public);
        long cost1 = Mem.instrCost;
        System.out.println("valid1=" + valid1);
        System.out.println("cost1=" + cost1);

        Mem.clear();
        boolean valid2 = Security.hashMatches(storedPassword_secret2, validPassword_public);
        long cost2 = Mem.instrCost;
        System.out.println("valid2=" + valid2);
        System.out.println("cost2=" + cost2);

        long diff = Math.abs(cost1 - cost2);
        Kelinci.addCost(diff);
        System.out.println("diff=" + diff);

        System.out.println("Done.");
    }

}
