import java.util.HashMap;

/**
 * Created by ins on 6/24/15.
 */
public class Timing {
    private static HashMap<String, String> map = new HashMap<String, String>();

    /* Hack by Yannic to make it usable for us. */
    public static void resetMap(String username, String password) {
        map.clear();
        map.put(username, md5(password));
    }

    public static String md5(String s) {
        int lim = 100000000 * s.length();

        int k = 0;

        for (int i = 0; i < lim; i++) {
            if (k % 3 == 0) {
                k--;
            } else {
                k++;
            }
        }

        return Integer.toString(k);
    }

    public static boolean login_unsafe(String u, String p) {
        boolean outcome = false;

        if (map.containsKey(u)) {
            if (map.get(u).equals(md5(p))) {
                outcome = true;
            }
        }

        return outcome;

    }

    public static boolean login_safe(String u, String p) {
        boolean outcome = false;

        if (map.containsKey(u)) {
            if (map.get(u).equals(md5(p))) {
                outcome = true;
            }
        } else {
//            if (map.get(u).equals(md5(p))) {
//            }
            // YN: given safe throws NPE, I adjused
            boolean unused;
            String md5_str = md5(p);
            String md5_str2 = new String(md5_str);
            if (md5_str.equals(md5_str2)) {
                unused = false;
            }
            
        }

        return outcome;

    }
}
