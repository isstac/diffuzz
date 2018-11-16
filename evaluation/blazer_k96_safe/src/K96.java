import java.math.BigInteger;

public class K96 {
  
/*
 * this example was extracted from P. Kocher. "Timing attacks on implementations of Diffie-Hellman, RSA, DSS, and other systems". CRYPTO '96.
 */
    
    public static BigInteger modular_exponentiation_unsafe(BigInteger y, BigInteger x, BigInteger n, int w) {
        BigInteger s = BigInteger.ONE;
        // int w = x.bitLength();
        BigInteger r = BigInteger.ZERO;
        for(int k = 0; k < w; k++) {
            if(x.testBit(k)) {
                // r = s.multiply(y).mod(n);
                r = standardMultiply(s,y).mod(n);
            } else {
                r = s;
            }
            // s = r.multiply(r).mod(n);
            s = standardMultiply(r,r).mod(n);
        }
        return r;
    }
    
    public static BigInteger modular_exponentiation_safe(BigInteger y, BigInteger x, BigInteger n, int w) {
        BigInteger s = BigInteger.ONE;
        // int w = x.bitLength();
        BigInteger r = BigInteger.ZERO;
        for(int k = 0; k < w; k++) {
            if(x.testBit(k)) {
                r = s;
                // r = s.multiply(y).mod(n);
                r = standardMultiply(s,y).mod(n);
            } else {
                // r = s.multiply(y).mod(n);
                r = standardMultiply(s,y).mod(n);
                r = s;
            }
            s = r.multiply(r).mod(n);
        }
        return r;
    }

    public static BigInteger standardMultiply(BigInteger x, BigInteger y) {
        BigInteger ret = BigInteger.ZERO;
        for (int i = 0; i < y.bitLength(); i++) {
            if (y.testBit(i)) {
                ret = ret.add(x.shiftLeft(i));
            }
        }
        return ret;
    }
}
