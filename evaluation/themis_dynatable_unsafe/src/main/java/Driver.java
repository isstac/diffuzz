import java.io.FileInputStream;
import java.io.IOException;

import com.google.gwt.sample.dynatable.client.Person;
import com.google.gwt.sample.dynatable.client.SchoolCalendarService;

import edu.cmu.sv.kelinci.Kelinci;
import edu.cmu.sv.kelinci.Mem;

public class Driver {

    private static final int MAX_INT_VALUE = 100;

    public static void main(String[] args) {

        if (args.length != 1) {
            System.out.println("Expects file name as parameter");
            return;
        }

        int startIndex, maxCount; // public
        int peopleLength1, peopleLength2; // secret

        /* Read input file. */
        try (FileInputStream fis = new FileInputStream(args[0])) {
            int value;

            if ((value = fis.read()) == -1) {
                throw new RuntimeException("Not enough data!");
            }
            startIndex = Math.abs(value) % MAX_INT_VALUE;

            if ((value = fis.read()) == -1) {
                throw new RuntimeException("Not enough data!");
            }
            maxCount = Math.abs(value) % MAX_INT_VALUE;

            if ((value = fis.read()) == -1) {
                throw new RuntimeException("Not enough data!");
            }
            peopleLength1 = Math.abs(value) % MAX_INT_VALUE;

            if ((value = fis.read()) == -1) {
                throw new RuntimeException("Not enough data!");
            }
            peopleLength2 = Math.abs(value) % MAX_INT_VALUE;

        } catch (IOException e) {
            System.err.println("Error reading input");
            e.printStackTrace();
            return;
        }

        System.out.println("startIndex=" + startIndex);
        System.out.println("maxCount=" + maxCount);
        System.out.println("peopleLength1=" + peopleLength1);
        System.out.println("peopleLength2=" + peopleLength2);

        SchoolCalendarService ser1 = new SchoolCalendarServiceImpl(peopleLength1);
        Mem.clear();
        Person[] res1 = ser1.getPeople(startIndex, maxCount);
        // long cost1 = Mem.instrCost;
        long cost1 = res1.length;
        System.out.println("cost1=" + cost1);

        SchoolCalendarService ser2 = new SchoolCalendarServiceImpl(peopleLength2);
        Mem.clear();
        Person[] res2 = ser2.getPeople(startIndex, maxCount);
        // long cost2 = Mem.instrCost;
        long cost2 = res2.length;
        System.out.println("cost2=" + cost2);

        long diffCost = Math.abs(cost1 - cost2);
        Kelinci.addCost(diffCost);
        System.out.println("diff=" + diffCost);
        System.out.println("Done.");

    }

}
