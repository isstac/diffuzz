import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.DatatypeConverter;

import com.jdriven.stateless.security.User;

import edu.cmu.sv.kelinci.Kelinci;
import edu.cmu.sv.kelinci.Mem;

public class Driver {

    public static final boolean SAFE_MODE = false;
    public static final int MAX_USERNAME_LENGTH = 5; // characters

    public static void main(String[] args) {

        /* Read input. */
        int invalidCharacterIndex;
        String userBytesString;

        if (args.length != 1) {
            System.out.println("Expects file name as parameter");
            return;
        }

        List<Character> values = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(args[0])) {

            invalidCharacterIndex = fis.read();
            if (invalidCharacterIndex == -1) {
                throw new RuntimeException("not enough data!");
            }

            byte[] bytes = new byte[Character.BYTES];
            int i = 0;
            while ((fis.read(bytes) != -1) && (i < MAX_USERNAME_LENGTH)) {
                char value = ByteBuffer.wrap(bytes).getChar();
                /* each char value must be between 0 and 127 and a printable character */
                value = (char) ((int) value % 127);
                if (Character.isAlphabetic(value) || Character.isDigit(value)) {
                    values.add(value);
                    i++;
                }
            }

            /* input must be non-empty */
            if (i == 0) {
                throw new RuntimeException("not enough data!");
            }

            char[] charArr = new char[values.size()];
            for (int j = 0; j < values.size(); j++) {
                charArr[j] = values.get(j);
            }
            userBytesString = new String(charArr);

        } catch (IOException e) {
            System.err.println("Error reading input");
            e.printStackTrace();
            return;
        }

        System.out.println("userBytesString: " + userBytesString);
        System.out.println("invalidCharacterIndex: " + invalidCharacterIndex);

        byte[] secretKey = { 15, 23, -12, 17, 3 }; // YN just random, but fixed for all experiments
        TokenHandler th = new TokenHandler(secretKey, SAFE_MODE);

        byte[] validHash = th.hmac.doFinal(DatatypeConverter.parseBase64Binary(userBytesString));
        String hashByteStringValid = DatatypeConverter.printBase64Binary(validHash);
        String userTokenValid = userBytesString + TokenHandler.SEPARATOR + hashByteStringValid;

        /* Generate a hash with same size but the wrong content. */
        invalidCharacterIndex = invalidCharacterIndex % validHash.length;
        byte[] invalidHash = new byte[validHash.length];
        for (int i = 0; i < invalidHash.length; i++) {
            if (i == invalidCharacterIndex) {
                invalidHash[i] = (byte) ((validHash[i] == 42) ? 21 : 42);
            } else {
                invalidHash[i] = validHash[i];
            }
        }
        String hashByteStringInvalid = DatatypeConverter.printBase64Binary(invalidHash);
        String userTokenInvalid = userBytesString + TokenHandler.SEPARATOR + hashByteStringInvalid;

        Mem.clear();
        User userValid = th.parseUserFromToken(userTokenValid);
        long cost1 = Mem.instrCost;
        System.out.println("auth1: " + (userValid == TokenHandler.VALID_USER));
        System.out.println("cost1: " + cost1);

        Mem.clear();
        User userInvalid = th.parseUserFromToken(userTokenInvalid);
        long cost2 = Mem.instrCost;
        System.out.println("auth2: " + (userInvalid == TokenHandler.VALID_USER));
        System.out.println("cost2: " + cost2);

        long diffCost = Math.abs(cost1 - cost2);
        Kelinci.addCost(diffCost);
        System.out.println("diffCost: " + diffCost);

        System.out.println("Done.");

    }

}
