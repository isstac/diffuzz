import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;

import com.acciente.oacc.PasswordCredentials;

import edu.cmu.sv.kelinci.Kelinci;
import edu.cmu.sv.kelinci.Mem;

public class Driver {

    public static final int MAX_PASSWORD_LENGTH = 16; // characters

    static char[] validCharacters = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F',
            'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a',
            'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v',
            'w', 'x', 'y', 'z', '{', '}', '(', ')', '[', ']', '#', ':', ';', '^', '!', '|', '&', '_', '~', '@', '$',
            '%', '/' };

    public static String mapToLetterOrDigest(String stringValue) {
        char[] newCharValues = new char[stringValue.length()];
        for (int i = 0; i < newCharValues.length; i++) {
            newCharValues[i] = validCharacters[stringValue.charAt(i) % validCharacters.length];
        }
        return new String(newCharValues);
    }

    public static void main(String[] args) {

        if (args.length != 1) {
            System.out.println("Expects file name as parameter");
            return;
        }

        /* Read input. */
        PasswordCredentials public_credentials;
        PasswordCredentials secret1;
        PasswordCredentials secret2;

        try (FileInputStream fis = new FileInputStream(args[0])) {

            int minNumBytesToRead = 3 * 1;
            int maxNumBytesToRead = 3 * MAX_PASSWORD_LENGTH;

            int totalNumberOfBytesInFile = Math.toIntExact(fis.getChannel().size());

            if (totalNumberOfBytesInFile < minNumBytesToRead) {
                throw new RuntimeException("not enough data!");
            }

            int usedNumberOfBytes = Math.min(totalNumberOfBytesInFile, maxNumBytesToRead);
            byte[] allBytes = new byte[usedNumberOfBytes];
            fis.read(allBytes);

            int eachPasswordLength = usedNumberOfBytes / 3;

            /* Read public. */
            int index = 0;
            byte[] temp = Arrays.copyOfRange(allBytes, index, index + eachPasswordLength);
            // public_credentials = PasswordCredentials.newInstance(mapToLetterOrDigest(new
            // String(temp)).toCharArray());
            public_credentials = PasswordCredentials.newInstance(new String(temp).toCharArray());

            /* Read secret1. */
            index += eachPasswordLength;
            temp = Arrays.copyOfRange(allBytes, index, index + eachPasswordLength);
            // secret1 = PasswordCredentials.newInstance(mapToLetterOrDigest(new String(temp)).toCharArray());
            secret1 = PasswordCredentials.newInstance(new String(temp).toCharArray());

            /* Read secret2. */
            index += eachPasswordLength;
            temp = Arrays.copyOfRange(allBytes, index, index + eachPasswordLength);
            // secret2 = PasswordCredentials.newInstance(mapToLetterOrDigest(new String(temp)).toCharArray());
            secret2 = PasswordCredentials.newInstance(new String(temp).toCharArray());

        } catch (IOException e) {
            System.err.println("Error reading input");
            e.printStackTrace();
            return;
        }

        System.out.println("public (password): " + Arrays.toString(public_credentials.getPassword()));
        System.out.println("secret1: " + Arrays.toString(secret1.getPassword()));
        System.out.println("secret2: " + Arrays.toString(secret2.getPassword()));

        Mem.clear();
        boolean valid1 = public_credentials.equals(secret1);
        long cost1 = Mem.instrCost;
        System.out.println("valid1=" + valid1);
        System.out.println("cost1=" + cost1);

        Mem.clear();
        boolean valid2 = public_credentials.equals(secret2);
        long cost2 = Mem.instrCost;
        System.out.println("valid2=" + valid2);
        System.out.println("cost2=" + cost2);

        long diff = Math.abs(cost1 - cost2);
        Kelinci.addCost(diff);
        System.out.println("diff=" + diff);

        System.out.println("Done.");
    }

}
