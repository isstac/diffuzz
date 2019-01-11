import java.io.FileInputStream;
import java.io.IOException;

import edu.cmu.sv.kelinci.Kelinci;
import edu.cmu.sv.kelinci.Mem;

public class Driver {

    private static final int MAX_INT_VALUE = 100;

    public static void main(String[] args) {

        if (args.length != 1) {
            System.out.println("Expects file name as parameter");
            return;
        }

        int startRow, rowsCount; // public
        int usersLength1, usersLength2; // secret

        /* Read input file. */
        try (FileInputStream fis = new FileInputStream(args[0])) {
            int value;

            if ((value = fis.read()) == -1) {
                throw new RuntimeException("Not enough data!");
            }
            startRow = Math.abs(value) % MAX_INT_VALUE;

            if ((value = fis.read()) == -1) {
                throw new RuntimeException("Not enough data!");
            }
            rowsCount = Math.abs(value) % MAX_INT_VALUE;

            if ((value = fis.read()) == -1) {
                throw new RuntimeException("Not enough data!");
            }
            usersLength1 = Math.abs(value) % MAX_INT_VALUE;

            if ((value = fis.read()) == -1) {
                throw new RuntimeException("Not enough data!");
            }
            usersLength2 = Math.abs(value) % MAX_INT_VALUE;

        } catch (IOException e) {
            System.err.println("Error reading input");
            e.printStackTrace();
            return;
        }

        System.out.println("startRow=" + startRow);
        System.out.println("rowsCount=" + rowsCount);
        System.out.println("usersLength1=" + usersLength1);
        System.out.println("usersLength2=" + usersLength2);

        UsersTableModelServiceImpl ser1 = new UsersTableModelServiceImpl(usersLength1);
        long cost1 = 0L;
        Mem.clear();
        try {
            String[][] res1 = ser1.getRows(startRow, rowsCount, null, null, true);
            // cost1 = Mem.instrCost;
            cost1 = res1.length;
        } catch (IndexOutOfBoundsException e) {
            // ignore;
        }
        System.out.println("cost1=" + cost1);

        UsersTableModelServiceImpl ser2 = new UsersTableModelServiceImpl(usersLength2);
        long cost2 = 0L;
        Mem.clear();
        try {
            String[][] res2 = ser2.getRows(startRow, rowsCount, null, null, true);
            // cost2 = Mem.instrCost;
            cost2 = res2.length;
        } catch (IndexOutOfBoundsException e) {
            // ignore;
        }
        System.out.println("cost2=" + cost2);

        long diffCost = Math.abs(cost1 - cost2);
        Kelinci.addCost(diffCost);
        System.out.println("diff=" + diffCost);
        System.out.println("Done.");

    }

}
