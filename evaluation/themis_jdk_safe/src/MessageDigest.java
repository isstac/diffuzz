import java.security.MessageDigestSpi;

public abstract class MessageDigest extends MessageDigestSpi {
    /**
     * Compares two digests for equality. Does a simple byte compare.
     *
     * @param digesta one of the digests to compare.
     *
     * @param digestb the other digest to compare.
     *
     * @return true if the digests are equal, false otherwise.
     */
    public static boolean isEqual_unsafe(byte digesta[], byte digestb[]) {
        if (digesta.length != digestb.length)
            return false;

        for (int i = 0; i < digesta.length; i++) {
            if (digesta[i] != digestb[i]) {
                return false;
            }
        }
        return true;
    }

    public static boolean isEqual_safe(byte[] digesta, byte[] digestb) {
        if (digesta.length != digestb.length) {
            return false;
        }

        int result = 0;
        // time-constant comparison
        for (int i = 0; i < digesta.length; i++) {
            result |= digesta[i] ^ digestb[i];
        }
        return result == 0;
    }


}
