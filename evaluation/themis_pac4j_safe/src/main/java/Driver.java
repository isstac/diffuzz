import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.h2.jdbcx.JdbcConnectionPool;
import org.pac4j.core.credentials.UsernamePasswordCredentials;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;

import edu.cmu.sv.kelinci.Kelinci;
import edu.cmu.sv.kelinci.Mem;

/*
* Username might be in database or not Encoding/Hashing of given password is expensive. The unsafe version does
* encoding only if username is in database.
*
* Password is actually not relevant here, but we would use the same password for both executions.
*/
public class Driver {

    private static final int USERNAME_MAX_LENGTH = 5; // # of characters
    private static final int PASSWORD_MAX_LENGTH = 20;

    private static final boolean RUN_UNSAFE_VERSION = false; // true=unsafe, false=safe version

    public static void main(String[] args) {

        if (args.length != 1) {
            System.out.println("Expects file name as parameter");
            return;
        }

        boolean pwCorrect;
        String user = "";
        String pw = "";

        int minNumberOfBytes = Byte.BYTES + 2 * Character.BYTES;
        int maxNumberOfBytes = Byte.BYTES + USERNAME_MAX_LENGTH * Character.BYTES
                + PASSWORD_MAX_LENGTH * Character.BYTES;

        List<Byte> valueList = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(args[0])) {

            /* Read all bytes up to the specified maximum. */
            byte[] values = new byte[1];
            int i = 0;
            while (fis.read(values) != -1 && i < maxNumberOfBytes) {
                valueList.add(values[0]);
                i++;
            }
            if (i < minNumberOfBytes) {
                throw new RuntimeException("Not enough data!");
            }

            /* Determine boolean value for password correctness. */
            pwCorrect = valueList.get(0) > 0 ? true : false;

            /* Find available sizes for strings. */
            int remainingSize = valueList.size() - 1;
            int usernameSize;
            int pwSize;
            if ((remainingSize / 2) >= (USERNAME_MAX_LENGTH * Character.BYTES)) {
                usernameSize = USERNAME_MAX_LENGTH * Character.BYTES;
            } else {
                usernameSize = remainingSize / 2;
            }
            if (usernameSize % Character.BYTES == 1) {
                usernameSize--;
            }
            pwSize = Math.min(remainingSize - usernameSize, PASSWORD_MAX_LENGTH * Character.BYTES);
            if (pwSize % Character.BYTES == 1) {
                pwSize--;
            }

            /* Assign bytes to the remaining strings. */
            for (int j = 1; j < 1 + usernameSize; j += Character.BYTES) {
                byte[] bytes = new byte[Character.BYTES];
                for (int k = 0; k < Character.BYTES; k++) {
                    bytes[k] = valueList.get(j + k);
                }
                user += ByteBuffer.wrap(bytes).getChar();
            }
            for (int j = 1 + usernameSize; j < 1 + usernameSize + pwSize; j += Character.BYTES) {
                byte[] bytes = new byte[Character.BYTES];
                for (int k = 0; k < Character.BYTES; k++) {
                    bytes[k] = valueList.get(j + k);
                }
                pw += ByteBuffer.wrap(bytes).getChar();
            }

            System.out.println("user=" + user);
            System.out.println("pw=" + pw);

        } catch (IOException e) {
            System.err.println("Error reading input");
            e.printStackTrace();
            return;
        }

        UsernamePasswordCredentials cred = new UsernamePasswordCredentials(user, pw, ""); // public info

        /* Create Connection to database. */
        DataSource ds = JdbcConnectionPool.create("jdbc:h2:~/pac4j-fuzz", "sa", "");
        DBI dbi = new DBI(ds);

        DbAuthenticator dbAuth = new DbAuthenticator();
        dbAuth.dbi = dbi;

        /* Prepare database. */
        Handle h = dbi.open();
        try {
            String processedPW = dbAuth.getPasswordEncoder().encode(pw);
            if (!pwCorrect) {
                processedPW = processedPW.substring(0, processedPW.length() - 1)
                        + (char) (processedPW.charAt(processedPW.length() - 1) + 1);
            }
            h.execute("insert into users (id, username, password) values (1, ?, ?)", user, processedPW); // add user
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            h.close();
        }

        Mem.clear();
        boolean authenticated1 = false;
        try {
            if (RUN_UNSAFE_VERSION) {
                dbAuth.validate_unsafe(cred);
            } else {
                dbAuth.validate_safe(cred);
            }
            if (cred.getUserProfile() != null) {
                authenticated1 = true;
            }
        } catch (Exception e) {
        }
        long cost1 = Mem.instrCost;
        System.out.println("authenticated1: " + authenticated1);
        System.out.println("cost1=" + cost1);

        /* Prepare database. */
        h = dbi.open();
        try {
            h.execute("delete from users where id = 1"); // remove user
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            h.close();
        }

        Mem.clear();
        boolean authenticated2 = false;
        try {
            if (RUN_UNSAFE_VERSION) {
                dbAuth.validate_unsafe(cred);
            } else {
                dbAuth.validate_safe(cred);
            }
            if (cred.getUserProfile() != null) {
                authenticated2 = true;
            }
        } catch (Exception e) {
        }
        long cost2 = Mem.instrCost;
        System.out.println("authenticated2: " + authenticated2);
        System.out.println("cost2=" + cost2);

        Kelinci.addCost(Math.abs(cost1 - cost2));

        /* Clean database. */
        h = dbi.open();
        try {
            h.execute("delete from users");
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            h.close();
        }

        System.out.println("Done.");
    }

}
