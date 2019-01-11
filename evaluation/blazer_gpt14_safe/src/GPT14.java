import java.math.BigInteger;

public class GPT14 {

/*
 * this example was extracted from D. Genkin, I. Pipman, and E. J. Tromer. "Get your hands off my laptop: physical side-channel key-extraction attacks on PCs". 2014
 */

    public static BigInteger modular_exponentiation_safe(BigInteger a, BigInteger b, BigInteger p) {
        BigInteger m = BigInteger.valueOf(1);
        int n = b.bitLength();
        for(int i = 0; i < n; i++) {
            m = m.multiply(m).mod(p);
            BigInteger t = m.multiply(a).mod(p);
            if(b.testBit(i)) {
                m = t;
            }
        }
        return a;
    }

    public static BigInteger sqr_basecase(BigInteger a) {
        BigInteger p;
        if(a.testBit(0)) {
            p = a;
        } else {
            p = BigInteger.valueOf(0);
        }
        int n = a.bitLength();
        for(int i = 1; i < n; i++) {
            if(a.testBit(i))
                p = p.add(a);
        }
        return p;
    }

    public static BigInteger modular_exponentiation_inline_unsafe(BigInteger a, BigInteger b, BigInteger p) {
        BigInteger m = BigInteger.valueOf(1);
        int n = b.bitLength();
        for(int i = 0; i < n; i++) {

            //starting of the inlining
            BigInteger p1;
            if(m.testBit(0)) {
                p1 = m;
            } else {
                p1 = BigInteger.valueOf(0);
            }
            int n1 = m.bitLength();
            for(int j = 1; j < n1; j++) {
                if(m.testBit(j))
                    p = p.add(m);
            }

            BigInteger t = m.multiply(a).mod(p);

            if(b.testBit(i)) {
                m = t;
            }
        }
        return a;
    }

}
