package org.apache.ftpserver.usermanager;
public class PasswordUtil {
    /**
     * Securely compares two strings up to a maximum number of characters in a way that obscures the password length
     * from timing attacks
     * 
     * @param input
     *            user input
     * @param password
     *            correct password
     * @param limit
     *            number of characters to compare; must be larger than password length
     * 
     * @throws IllegalArgumentException
     *             when the limit is less than the password length
     * 
     * @return true if the passwords match
     */
    public static boolean secureCompare(String input, String password, int limit) {
        if (limit < password.length()) {
            throw new IllegalArgumentException("limit must be equal or greater than the password length");
        }
        /*
         * Set the default result based on the string lengths; if the lengths do not match then we know that this
         * comparison should always fail.
         */
        int result = (input.length() == password.length()) ? 0 : 1;
        /*
         * Cycle through all of the characters up to the limit value
         * 
         * Important to note that this loop may return a false positive comparison if the target string is a repeating
         * set of characters in direct multiples of the input string. This design fallacy is corrected by the original
         * length comparison above. The use of modulo this way is intended to prevent compiler and memory optimizations
         * for retrieving the same char position in sequence.
         */
        for (int i = 0; i < limit; i++) {
            result |= (input.charAt(i % input.length()) ^ password.charAt(i % password.length()));
        }

        return (result == 0);
    }
}