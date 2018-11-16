import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;

import com.graphhopper.GraphHopper;
import com.graphhopper.tour.Matrix;

import edu.cmu.sv.kelinci.Kelinci;

public class Driver {

    public static boolean safe_mode = false;

    /* 25 valid points/cities */
    public static String[] validPointNames = { "Boston", "Worcester", "Taunton", "Lowell", "Brockton", "Revere",
            "Peabody", "Weymouth", "Springfield", "Newton", "Quincy", "Lynn", "Somerville", "Plymouth", "Fall River",
            "Malden", "Waltham", "Brookline", "New Bedford", "Lawrence", "Cambridge", "Framingham", "Medford",
            "Chicopee", "Haverhill" };
    public static int numberOfCities = validPointNames.length;
    public static Map<String, String> validPoints = new HashMap<>();
    static {
        validPoints.put("Boston", "<42.3604823,-71.0595678>");
        validPoints.put("Worcester", "<42.2625932,-71.8022934>");
        validPoints.put("Springfield", "<42.1014831,-72.589811>");
        validPoints.put("Lowell", "<42.6334247,-71.3161718>");
        validPoints.put("Cambridge", "<42.3750997,-71.1056157>");
        validPoints.put("New Bedford", "<41.6362152,-70.934205>");
        validPoints.put("Brockton", "<42.0834335,-71.0183787>");
        validPoints.put("Quincy", "<42.2528772,-71.0022705>");
        validPoints.put("Lynn", "<42.466763,-70.9494939>");
        validPoints.put("Fall River", "<41.7010642,-71.1546367>");
        validPoints.put("Newton", "<42.3370414,-71.2092214>");
        validPoints.put("Lawrence", "<42.7070354,-71.1631137>");
        validPoints.put("Somerville", "<42.3875968,-71.0994968>");
        validPoints.put("Framingham", "<42.2792625,-71.416172>");
        validPoints.put("Haverhill", "<42.7777829,-71.0767724>");
        validPoints.put("Waltham", "<42.3756401,-71.2358004>");
        validPoints.put("Malden", "<42.4250964,-71.066163>");
        validPoints.put("Brookline", "<42.3317642,-71.1211635>");
        validPoints.put("Plymouth", "<41.9584367,-70.6672577>");
        validPoints.put("Medford", "<42.4184296,-71.1061639>");
        validPoints.put("Taunton", "<41.900101,-71.0897675>");
        validPoints.put("Chicopee", "<42.1487043,-72.6078672>");
        validPoints.put("Weymouth", "<42.2212188,-70.9391625>");
        validPoints.put("Revere", "<42.4084302,-71.0119948>");
        validPoints.put("Peabody", "<42.5278731,-70.9286609>");
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Expects file name as parameter");
            return;
        }

        String[] points1;
        String[] points2;

        /* at least two points must be specified */

        try (FileInputStream fis = new FileInputStream(args[0])) {
            byte[] values = new byte[1];

            /* Read number of cities for 1st run. */
            if (fis.read(values) == -1) {
                throw new RuntimeException("Not enough data!");
            }
            int n1 = values[0] % numberOfCities;
            n1 = (n1 < 0) ? n1 + numberOfCities : n1;
            System.out.println("n1=" + n1);

            /* Read number of cities for 2nd run. */
            if (fis.read(values) == -1) {
                throw new RuntimeException("Not enough data!");
            }
            int n2 = values[0] % numberOfCities;
            n2 = (n2 < 0) ? n2 + numberOfCities : n2;
            System.out.println("n2=" + n2);

            /* Read cities for 1st request. */
            points1 = new String[n1];
            int i = 0;
            while (fis.read(values) != -1 && i < n1) {
                int cityId = values[0] % numberOfCities;
                cityId = (cityId < 0) ? cityId + numberOfCities : cityId;
                points1[i] = validPoints.get(validPointNames[cityId]);
                i++;
            }
            if (i < n1) {
                throw new RuntimeException("Not enough data!");
            }

            /* Read cities for 2nd request. */
            points2 = new String[n2];
            i = 0;
            while (fis.read(values) != -1 && i < n2) {
                int cityId = values[0] % numberOfCities;
                cityId = (cityId < 0) ? cityId + numberOfCities : cityId;
                points2[i] = validPoints.get(validPointNames[cityId]);
                i++;
            }
            if (i < n2) {
                throw new RuntimeException("Not enough data!");
            }
        } catch (IOException e) {
            System.err.println("Error reading input");
            e.printStackTrace();
            return;
        }

        DummyRequest req1 = new DummyRequest();
        req1.setAttribute("point", points1);

        DummyRequest req2 = new DummyRequest();
        req2.setAttribute("point", points2);

        // Prepare application.
        String matrixLocation = "./data/matrix.csv";
        String graphHopperDirectory = "./data/massachusetts-latest.osm-gh";
        Matrix matrix = null;
        try {
            matrix = Matrix.readCsv(new File(matrixLocation));
        } catch (IOException e1) {
            e1.printStackTrace();
            return;
        }
        GraphHopper gh = new GraphHopper();
        gh.load(graphHopperDirectory);
        TourServlet ts = new TourServlet(matrix, gh, safe_mode);

        // Call app and measure cost.
        try {
            DummyResponse res1 = new DummyResponse();
             ts.doGet(req1, res1);
            int cost1 = res1.getResponseLength();
            System.out.println("length1: " + cost1);
            res1.printConsole();

            DummyResponse res2 = new DummyResponse();
             ts.doGet(req2, res2);
            int cost2 = res2.getResponseLength();
            System.out.println("length2: " + cost2);
            res2.printConsole();

            int diffCost = Math.abs(cost1 - cost2);
            System.out.println("diff: " + diffCost);

            Kelinci.addCost(diffCost);
        } catch (ServletException e1) {
            e1.printStackTrace();
        } catch (IOException e2) {
            e2.printStackTrace();
        }

    }

}
