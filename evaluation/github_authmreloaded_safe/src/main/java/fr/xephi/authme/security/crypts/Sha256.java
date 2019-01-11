package fr.xephi.authme.security.crypts;

import fr.xephi.authme.security.crypts.description.Recommendation;
import fr.xephi.authme.security.crypts.description.Usage;

import static fr.xephi.authme.security.HashUtils.sha256;

@Recommendation(Usage.RECOMMENDED)
public class Sha256 extends HexSaltedMethod {

    @Override
    public String computeHash(String password, String salt, String name) {
        return "$SHA$" + salt + "$" + sha256(sha256(password) + salt);
    }

    @Override
    public boolean comparePassword(String password, HashedPassword hashedPassword, String playerName) {
        String hash = hashedPassword.getHash();
        String[] line = hash.split("\\$");
        // return line.length == 4 && hash.equals(computeHash(password, line[2], ""));
        return line.length == 4 && isEqual_unsafe(hash, computeHash(password, line[2], ""));
    }

    @Override
    public int getSaltLength() {
        return 16;
    }

    /* YN inline equals method of String class */
    public boolean isEqual_unsafe(String thisObject, Object otherObject) {
        if (thisObject == otherObject) {
            return true;
        }
        if (otherObject instanceof String) {
            String anotherString = (String) otherObject;
            int n = thisObject.length();
            if (n == anotherString.length()) {
                char v1[] = thisObject.toCharArray();
                char v2[] = anotherString.toCharArray();
                int i = 0;
                while (n-- != 0) {
                    if (v1[i] != v2[i])
                        return false;
                    i++;
                }
                return true;
            }
        }
        return false;
    }

}
