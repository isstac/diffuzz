import java.math.BigInteger;

public class ModPow1 {

    /*
     * this example is from STAC engagement proram: snapbuddy_2
     */

    // top-level modPow method: replaced the fastMultiply with BigInteger standard multiply
    public static BigInteger modPow1_safe(BigInteger base, BigInteger exponent, BigInteger modulus, int width) {
        BigInteger s = BigInteger.valueOf(1);
        // int width = exponent.bitLength(); // use width parameter because bitlength is wrong for 0
        for (int i = 0; i < width; i++) {
            s = s.multiply(s).mod(modulus); // needs to stay original, otherwise we get wrong result
            if (exponent.testBit(width - i - 1)) {
                // s = s.multiply(base).mod(modulus); // replaced to be able to instrument multiplication
                s = standardMultiply(s, base).mod(modulus);
            } else {
                // s.multiply(base).mod(modulus); // replaced to be able to instrument multiplication
                standardMultiply(s, base).mod(modulus);
            }
        }
        return s;
    }

    // YN added instead on inline implementation
    public static BigInteger modPow1_unsafe(BigInteger base, BigInteger exponent, BigInteger modulus, int width) {
        BigInteger s = BigInteger.valueOf(1);
        // int width = exponent.bitLength(); // use width parameter because bitlength is wrong for 0
        for (int i = 0; i < width; i++) {
            s = s.multiply(s).mod(modulus); // needs to stay original, otherwise we get wrong result
            if (exponent.testBit(width - i - 1)) {
                // s = OptimizedMultiplier.fastMultiply(s, base).mod(modulus);
                // s = s.multiply(base).mod(modulus); // replaced to be able to instrument multiplication
                s = standardMultiply(s, base).mod(modulus);
            }
        }
        return s;
    }

    // top-level modPow method: inline the custom implementation of fastMultiply
    /*
     * YN: is exponent the secret? base and modulus the public?
     */
    public static BigInteger modPow1_unsafe_inline(BigInteger base, BigInteger exponent, BigInteger modulus) {
        BigInteger s = BigInteger.valueOf(1);
        int width = exponent.bitLength();
        for (int i = 0; i < width; i++) {
            s = s.multiply(s).mod(modulus);
            if (exponent.testBit(width - i - 1)) {
                BigInteger x = s;
                BigInteger y = base;
                int xLen = x.bitLength();
                int yLen = y.bitLength();
                if (x.equals(BigInteger.ONE)) {
                    return y;
                }
                if (y.equals(BigInteger.ONE)) {
                    return x;
                }
                BigInteger ret = BigInteger.ZERO;
                int N = Math.max(xLen, yLen);
                if (N <= 800) {
                    ret = x.multiply(y);
                } else if (Math.abs(xLen - yLen) >= 32) {
                    ret = BigInteger.ZERO;
                    for (int j = 0; j < y.bitLength(); j++) {
                        if (y.testBit(j)) {
                            ret = ret.add(x.shiftLeft(j));
                        }
                    }
                } else {
                    // Number of bits/2 rounding up
                    N = (N / 2) + (N % 2);
                    // x = a + 2^N*b, y = c + 2^N*d
                    BigInteger b = x.shiftRight(N);
                    BigInteger a = x.subtract(b.shiftLeft(N));
                    BigInteger d = y.shiftRight(N);
                    BigInteger c = y.subtract(d.shiftLeft(N));
                    // Compute intermediate values
                    BigInteger ac = fastMultiply_1(a, c);
                    BigInteger bd = fastMultiply_1(b, d);
                    BigInteger crossterms = fastMultiply_1(a.add(b), c.add(d));
                    ret = ac.add(crossterms.subtract(ac).subtract(bd).shiftLeft(N)).add(bd.shiftLeft(2 * N));
                }
                s = ret.mod(modulus);
                // s = fastMultiply(s, base).mod(modulus);
            }
        }
        return s;
    }

    // fastMultiply method: replace standardMuliply call with BigInteger libarary implementation of multiply
    public static BigInteger fastMultiply_1(BigInteger x, BigInteger y) {
        int xLen = x.bitLength();
        int yLen = y.bitLength();
        if (x.equals(BigInteger.ONE)) {
            return y;
        }
        if (y.equals(BigInteger.ONE)) {
            return x;
        }
        BigInteger ret = BigInteger.ZERO;
        int N = Math.max(xLen, yLen);
        if (N <= 800) {
            ret = x.multiply(y);
        } else if (Math.abs(xLen - yLen) >= 32) {
            // ret = standardMultiply(x, y);
            ret = x.multiply(y);
        } else {
            // Number of bits/2 rounding up
            N = (N / 2) + (N % 2);
            // x = a + 2^N*b, y = c + 2^N*d
            BigInteger b = x.shiftRight(N);
            BigInteger a = x.subtract(b.shiftLeft(N));
            BigInteger d = y.shiftRight(N);
            BigInteger c = y.subtract(d.shiftLeft(N));
            // Compute intermediate values
            BigInteger ac = fastMultiply_1(a, c);
            BigInteger bd = fastMultiply_1(b, d);
            BigInteger crossterms = fastMultiply_1(a.add(b), c.add(d));
            ret = ac.add(crossterms.subtract(ac).subtract(bd).shiftLeft(N)).add(bd.shiftLeft(2 * N));
        }
        return ret;
    }

    // fastMultiply method: inlined standardMultiply implementation
    public static BigInteger fastMultiply_inline(BigInteger x, BigInteger y) {
        int xLen = x.bitLength();
        int yLen = y.bitLength();
        if (x.equals(BigInteger.ONE)) {
            return y;
        }
        if (y.equals(BigInteger.ONE)) {
            return x;
        }
        BigInteger ret = BigInteger.ZERO;
        int N = Math.max(xLen, yLen);
        if (N <= 800) {
            ret = x.multiply(y);
        } else if (Math.abs(xLen - yLen) >= 32) {
            ret = BigInteger.ZERO;
            for (int i = 0; i < y.bitLength(); i++) {
                if (y.testBit(i)) {
                    ret = ret.add(x.shiftLeft(i));
                }
            }
        } else {
            // Number of bits/2 rounding up
            N = (N / 2) + (N % 2);
            // x = a + 2^N*b, y = c + 2^N*d
            BigInteger b = x.shiftRight(N);
            BigInteger a = x.subtract(b.shiftLeft(N));
            BigInteger d = y.shiftRight(N);
            BigInteger c = y.subtract(d.shiftLeft(N));
            // Compute intermediate values
            BigInteger ac = fastMultiply_1(a, c);
            BigInteger bd = fastMultiply_1(b, d);
            BigInteger crossterms = fastMultiply_1(a.add(b), c.add(d));
            ret = ac.add(crossterms.subtract(ac).subtract(bd).shiftLeft(N)).add(bd.shiftLeft(2 * N));
        }
        return ret;
    }

    // standardMultiply method
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
