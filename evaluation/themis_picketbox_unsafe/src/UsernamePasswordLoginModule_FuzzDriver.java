import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import edu.cmu.sv.kelinci.Kelinci;
import edu.cmu.sv.kelinci.Mem;

public class UsernamePasswordLoginModule_FuzzDriver {
    public static void main(String[] args) {

        if (args.length != 1) {
            System.out.println("Expects file name as parameter");
            return;
        }

        String secret1_expected;
        String secret2_expected;
        String public_actual;

        int n = 3;
        int maxM = Integer.MAX_VALUE;

       // Read all inputs.
        List<Character> values = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(args[0])) {
            byte[] bytes = new byte[Character.BYTES];
            int i = 0;
            while (((fis.read(bytes)) != -1) && (i < maxM * n)) {
                char value = ByteBuffer.wrap(bytes).getChar();
                values.add(value);
                i++;
            }
        } catch (IOException e) {
            System.err.println("Error reading input");
            e.printStackTrace();
            return;
        }

        if (values.size() < 3) {
            throw new RuntimeException("Too less data...");
        }

        int m = values.size() / 3;
        System.out.println("m=" + m);

        // Read secret1.
        char[] secret1_arr = new char[m];
        for (int i = 0; i < m; i++) {
            secret1_arr[i] = values.get(i);
        }
        secret1_expected = new String(secret1_arr);

        // Read secret2.
        char[] secret2_arr = new char[m];
        for (int i = 0; i < m; i++) {
            secret2_arr[i] = values.get(i + m);
        }
        secret2_expected = new String(secret2_arr);

        // Read public.
        char[] public_arr = new char[m];
        for (int i = 0; i < m; i++) {
            public_arr[i] = values.get(i + 2 * m);
        }
        public_actual = new String(public_arr);

        System.out.println("secret1_expected=" + secret1_expected);
        System.out.println("secret2_expected=" + secret2_expected);
        System.out.println("public_actual=" + public_actual);


        Mem.clear();
        boolean result1 = UsernamePasswordLoginModule.validatePassword_unsafe(secret1_expected, public_actual);
        System.out.println("Answer1: " + result1);
        long cost1 = Mem.instrCost;
        System.out.println("cost1=" + cost1);
        Mem.clear();

        boolean result2 = UsernamePasswordLoginModule.validatePassword_unsafe(secret2_expected, public_actual);
        System.out.println("Answer2: " + result2);
        long cost2 = Mem.instrCost;
        System.out.println("cost2=" + cost2);
        System.out.println("|cost1-cost2|=" + Math.abs(cost1 - cost2));

        Kelinci.addCost(Math.abs(cost1 - cost2));

        System.out.println("Done.");
    }



        // Shirin: Divide the input to two random parts: one partition will be divided to two equal parts
        // thus, eventually we have 3 partitions, two of which have with equal size.
        // Random rand = new Random();
        // int m = rand.nextInt(values.size()-1);
        // int halfm = m/2;
        // int publicLength = values.size()-2*halfm;
        // // Read secret1.
        // char[] secret1_arr = new char[halfm];
        // for (int i = 0; i < halfm; i++) {
        //     secret1_arr[i] = values.get(i);
        // }
        // secret1_expected = new String(secret1_arr);

        // // Read secret2.
        // char[] secret2_arr = new char[halfm];
        // for (int i = 0; i < halfm; i++) {
        //     secret2_arr[i] = values.get(i + halfm);
        // }
        // secret2_expected = new String(secret2_arr);

        // // Read public.
        // char[] public_arr = new char[publicLength];
        // for (int i = 0; i < publicLength; i++) {
        //     public_arr[i] = values.get(i + 2 * halfm);
        // }
        // public_actual = new String(public_arr);

    // private static int[] randSum(int n, int min, int m) {
    //     Random rand = new Random();
    //     int[] nums = new int[n];
    //     int max = m - min*n;
    //     if(max <= 0)
    //         throw new IllegalArgumentException();
    //     for(int i=1; i<nums.length; i++) {
    //         nums[i] = rand.nextInt(max);
    //     }
    //     Arrays.sort(nums, 1, nums.length);
    //     for(int i=1; i<nums.length; i++) {
    //         nums[i-1] = nums[i]-nums[i-1]+min;
    //     }
    //     nums[nums.length-1] = max-nums[nums.length-1]+min;
    //     return nums;
    // }

}
