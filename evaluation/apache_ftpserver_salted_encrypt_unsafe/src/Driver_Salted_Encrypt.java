import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import org.apache.ftpserver.usermanager.SaltedPasswordEncryptor;

import edu.cmu.sv.kelinci.Kelinci;
import edu.cmu.sv.kelinci.Mem;

public class Driver_Salted_Encrypt {

    public static final int MAX_PASSWORD_LENGTH = 16; // characters

    public static final boolean SAFE_MODE = false;

    /* retrieved from application */
    private static final int SALT_LENGTH = 1; /* Integer number */
    private static final int MAXIMUM_SALT_VALUE = 99999999;

    public static void main(String[] args) {

        if (args.length != 1) {
            System.out.println("Expects file name as parameter");
            return;
        }

        /* Read input. */
        String password_public;
        String salt1;
        String salt2;

        SaltedPasswordEncryptor pe = new SaltedPasswordEncryptor(SAFE_MODE);

        try (FileInputStream fis = new FileInputStream(args[0])) {

            int minNumBytesToRead = 1 + 2 * SALT_LENGTH * Integer.BYTES;
            int maxNumBytesToRead = MAX_PASSWORD_LENGTH + 2 * SALT_LENGTH * Integer.BYTES;

            int totalNumberOfBytesInFile = Math.toIntExact(fis.getChannel().size());

            if (totalNumberOfBytesInFile < minNumBytesToRead) {
                throw new RuntimeException("not enough data!");
            }

            int usedNumberOfBytes = Math.min(totalNumberOfBytesInFile, maxNumBytesToRead);
            byte[] allBytes = new byte[usedNumberOfBytes];
            fis.read(allBytes);

            /* Read public. */
            int passwordByteLength = usedNumberOfBytes - (2 * SALT_LENGTH * Integer.BYTES);
            int index = 0;
            byte[] temp = Arrays.copyOfRange(allBytes, index, index + passwordByteLength);
            password_public = new String(temp);

            /* Read salt1. */
            index += passwordByteLength;
            temp = Arrays.copyOfRange(allBytes, index, index + SALT_LENGTH * Integer.BYTES);
            int salt1_int = Math.abs(ByteBuffer.wrap(temp).getInt() % (MAXIMUM_SALT_VALUE));
            salt1 = Integer.toString(salt1_int);

            /* Read salt2. */
            index += SALT_LENGTH * Integer.BYTES;
            temp = Arrays.copyOfRange(allBytes, index, index + SALT_LENGTH * Integer.BYTES);
            int salt2_int = Math.abs(ByteBuffer.wrap(temp).getInt() % (MAXIMUM_SALT_VALUE));
            salt2 = Integer.toString(salt2_int);

        } catch (IOException e) {
            System.err.println("Error reading input");
            e.printStackTrace();
            return;
        }

        System.out.println("public (password): " + password_public);
        System.out.println("secret1: " + salt1);
        System.out.println("secret2: " + salt2);

        Mem.clear();
        String encrypted1 = pe.encrypt(password_public, salt1);
        long cost1 = Mem.instrCost;
        System.out.println("encrypted1=" + encrypted1);
        System.out.println("cost1=" + cost1);

        Mem.clear();
        String encrypted2 = pe.encrypt(password_public, salt2);
        long cost2 = Mem.instrCost;
        System.out.println("encrypted1=" + encrypted2);
        System.out.println("cost2=" + cost2);

        long diff = Math.abs(cost1 - cost2);
        Kelinci.addCost(diff);
        System.out.println("diff=" + diff);

        System.out.println("Done.");
    }

}
