import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import org.apache.ftpserver.usermanager.SaltedPasswordEncryptor;

import edu.cmu.sv.kelinci.Kelinci;
import edu.cmu.sv.kelinci.Mem;

public class Driver_Salted {

    public static final int MAX_PASSWORD_LENGTH = 16; // characters

    public static final boolean SAFE_MODE = true;

    /* retrieved from application */
    private static final int SALT_LENGTH = 1; /* Integer number */
    private static final int MAXIMUM_SALT_VALUE = 99999999;
    private static final int HASH_LENGTH = 32; /* characters */

    static char[] validCharacters = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F',
            'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z' };

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
        String password_public;
        String storedPassword_secret1;
        String storedPassword_secret2;

        SaltedPasswordEncryptor pe = new SaltedPasswordEncryptor(SAFE_MODE);

        try (FileInputStream fis = new FileInputStream(args[0])) {

            int minNumBytesToRead = 1 + 2 * SALT_LENGTH * Integer.BYTES + 2 * HASH_LENGTH;
            int maxNumBytesToRead = MAX_PASSWORD_LENGTH + 2 * SALT_LENGTH * Integer.BYTES + 2 * HASH_LENGTH;

            int totalNumberOfBytesInFile = Math.toIntExact(fis.getChannel().size());

            if (totalNumberOfBytesInFile < minNumBytesToRead) {
                throw new RuntimeException("not enough data!");
            }

            int usedNumberOfBytes = Math.min(totalNumberOfBytesInFile, maxNumBytesToRead);
            byte[] allBytes = new byte[usedNumberOfBytes];
            fis.read(allBytes);

            /* Read public. */
            int passwordByteLength = usedNumberOfBytes - (2 * SALT_LENGTH * Integer.BYTES + 2 * HASH_LENGTH);
            int index = 0;
            byte[] temp = Arrays.copyOfRange(allBytes, index, index + passwordByteLength);
            password_public = new String(temp);

            /* Read salt1. */
            index += passwordByteLength;
            temp = Arrays.copyOfRange(allBytes, index, index + SALT_LENGTH * Integer.BYTES);
            int salt1 = Math.abs(ByteBuffer.wrap(temp).getInt() % (MAXIMUM_SALT_VALUE));

            /* Read hash1. */
            index += SALT_LENGTH * Integer.BYTES;
            temp = Arrays.copyOfRange(allBytes, index, index + HASH_LENGTH);
            String hash1 = mapToLetterOrDigest(new String(temp));
            storedPassword_secret1 = salt1 + ":" + hash1;

            /* Read salt2. */
            index += passwordByteLength;
            temp = Arrays.copyOfRange(allBytes, index, index + SALT_LENGTH * Integer.BYTES);
            int salt2 = Math.abs(ByteBuffer.wrap(temp).getInt() % (MAXIMUM_SALT_VALUE));

            /* Read hash2. */
            index += SALT_LENGTH * Integer.BYTES;
            temp = Arrays.copyOfRange(allBytes, index, index + HASH_LENGTH);
            String hash2 = mapToLetterOrDigest(new String(temp));
            storedPassword_secret2 = salt2 + ":" + hash2;

        } catch (IOException e) {
            System.err.println("Error reading input");
            e.printStackTrace();
            return;
        }

        System.out.println("public (password): " + password_public);
        System.out.println("secret1: " + storedPassword_secret1);
        System.out.println("secret2: " + storedPassword_secret2);

        Mem.clear();
        boolean valid1 = pe.matches(password_public, storedPassword_secret1);
        long cost1 = Mem.instrCost;
        System.out.println("valid1=" + valid1);
        System.out.println("cost1=" + cost1);

        Mem.clear();
        boolean valid2 = pe.matches(password_public, storedPassword_secret2);
        long cost2 = Mem.instrCost;
        System.out.println("valid2=" + valid2);
        System.out.println("cost2=" + cost2);

        long diff = Math.abs(cost1 - cost2);
        Kelinci.addCost(diff);
        System.out.println("diff=" + diff);

        System.out.println("Done.");
    }

}
