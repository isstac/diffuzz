import edu.cmu.sv.kelinci.Mem;

class Sanity {

    public static boolean notaint_unsafe(int sec[], int taint) {
	if(sec[0] > 0) {
	    int i=0; while (i<sec[0]) i++;
	}
	return true;
    }
    public static boolean nosecret_safe(int sec[], int taint) {
	if (taint>0) {
	    int i=0; while (i<taint) i++;
	}
	return true;
    }

    public static boolean straightline_unsafe(int a, int b) {
	int x=a, y=b;
	if(a>0 && b>0) {
	    x = 2;
	} else {
	    x=1+y; y=2+x; x=3+y; y=4+x; x=5+y; y=6+x; x=7+y; y=8+x; x=9+y; y=10+x; x=1+y; y=2+x; x=3+y; y=4+x; x=5+y; y=6+x; x=7+y; y=8+x; x=9+y; y=10+x; x=1+y; y=2+x; x=3+y; y=4+x; x=5+y; y=6+x; x=7+y; y=8+x; x=9+y; y=10+x;
	    x=1+y; y=2+x; x=3+y; y=4+x; x=5+y; y=6+x; x=7+y; y=8+x; x=9+y; y=10+x; x=1+y; y=2+x; x=3+y; y=4+x; x=5+y; y=6+x; x=7+y; y=8+x; x=9+y; y=10+x; x=1+y; y=2+x; x=3+y; y=4+x; x=5+y; y=6+x; x=7+y; y=8+x; x=9+y; y=10+x;
	    x=1+y; y=2+x; x=3+y; y=4+x; x=5+y; y=6+x; x=7+y; y=8+x; x=9+y; y=10+x; x=1+y; y=2+x; x=3+y; y=4+x; x=5+y; y=6+x; x=7+y; y=8+x; x=9+y; y=10+x; x=1+y; y=2+x; x=3+y; y=4+x; x=5+y; y=6+x; x=7+y; y=8+x; x=9+y; y=10+x;
	    x=1+y; y=2+x; x=3+y; y=4+x; x=5+y; y=6+x; x=7+y; y=8+x; x=9+y; y=10+x; x=1+y; y=2+x; x=3+y; y=4+x; x=5+y; y=6+x; x=7+y; y=8+x; x=9+y; y=10+x; x=1+y; y=2+x; x=3+y; y=4+x; x=5+y; y=6+x; x=7+y; y=8+x; x=9+y; y=10+x;
	    x=1+y; y=2+x; x=3+y; y=4+x; x=5+y; y=6+x; x=7+y; y=8+x; x=9+y; y=10+x; x=1+y; y=2+x; x=3+y; y=4+x; x=5+y; y=6+x; x=7+y; y=8+x; x=9+y; y=10+x; x=1+y; y=2+x; x=3+y; y=4+x; x=5+y; y=6+x; x=7+y; y=8+x; x=9+y; y=10+x;
	    x=1+y; y=2+x; x=3+y; y=4+x; x=5+y; y=6+x; x=7+y; y=8+x; x=9+y; y=10+x; x=1+y; y=2+x; x=3+y; y=4+x; x=5+y; y=6+x; x=7+y; y=8+x; x=9+y; y=10+x; x=1+y; y=2+x; x=3+y; y=4+x; x=5+y; y=6+x; x=7+y; y=8+x; x=9+y; y=10+x;
	    x=1+y; y=2+x; x=3+y; y=4+x; x=5+y; y=6+x; x=7+y; y=8+x; x=9+y; y=10+x; x=1+y; y=2+x; x=3+y; y=4+x; x=5+y; y=6+x; x=7+y; y=8+x; x=9+y; y=10+x; x=1+y; y=2+x; x=3+y; y=4+x; x=5+y; y=6+x; x=7+y; y=8+x; x=9+y; y=10+x;
	    x=1+y; y=2+x; x=3+y; y=4+x; x=5+y; y=6+x; x=7+y; y=8+x; x=9+y; y=10+x; x=1+y; y=2+x; x=3+y; y=4+x; x=5+y; y=6+x; x=7+y; y=8+x; x=9+y; y=10+x; x=1+y; y=2+x; x=3+y; y=4+x; x=5+y; y=6+x; x=7+y; y=8+x; x=9+y; y=10+x;
	    x=1+y; y=2+x; x=3+y; y=4+x; x=5+y; y=6+x; x=7+y; y=8+x; x=9+y; y=10+x; x=1+y; y=2+x; x=3+y; y=4+x; x=5+y; y=6+x; x=7+y; y=8+x; x=9+y; y=10+x; x=1+y; y=2+x; x=3+y; y=4+x; x=5+y; y=6+x; x=7+y; y=8+x; x=9+y; y=10+x;
	}
	return false;
    }
    public static boolean straightline_safe(int a, int b) {
	int x=a, y=b;
	if(a>0 && b>0) {
	    x=1+y; y=2+x; x=3+y; y=4+x; x=5+y; y=6+x; x=7+y; y=8+x; x=9+y; y=10+x;
	    x=1+y; y=2+x; x=3+y; y=4+x; x=5+y; y=6+x; x=7+y; y=8+x; x=9+y; y=10+x;
	    x=1+y; y=2+x; x=3+y; y=4+x; x=5+y; y=6+x; x=7+y; y=8+x; x=9+y; y=10+x;
	} else {
	    x=1+y; y=2+x; x=3+y; y=4+x; x=5+y; y=6+x; x=7+y; y=8+x; x=9+y; y=10+x;
	    x=1+y; y=2+x; x=3+y; y=4+x; x=5+y; y=6+x; x=7+y; y=8+x; x=9+y; y=10+x;
	    x=1+y; y=2+x; x=3+y; y=4+x; x=5+y; y=6+x; x=7+y; y=8+x; x=9+y; y=10+x;
	}
	return false;
    }

    public static boolean sanity_safe(int a, int b) {
    System.out.println("a: " + a + ";b: " + b);
	int i = b;
	int j = b;
	if (b<0) return false;

	if (a>0) {
	    while (i > 0) {
		i--;
	    }
	} else {
	    while (j > 0) {
		j--;
	    }
	    
	}
        return false;
    }

    public static boolean sanity_unsafe(int a, int b) {
	int i = b;
	int j = b;
	if (b<0) return false;

	if (a<0) {
	    return true;
	} else {
	    while (i > 0) {
		i--;
	    }
	}
        return false;
    }
}
