import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import edu.cmu.sv.kelinci.Kelinci;
import edu.cmu.sv.kelinci.Mem;

public class Tomcat_FuzzDriver {
/*
 * Created a H2 database called Tomcat with USERSm ROLES, and USERs_ROLES tables.
 * Check http://www.h2database.com/html/tutorial.html for creating a db.
 *  To start db: cd h2/bin, and type: java -jar h2*.jar
 *
 */
	public static void create_an_example() {
         String secret_user1="Shirin";
         String secret_user2="Yannic";
		 String public_user="Shirin";

		try {

			 OutputStream os = new FileOutputStream("/Users/tempadmin/Desktop/exps/tomcat/in_dir/example.txt");
			 os.write(secret_user1.getBytes());
			 os.write(secret_user2.getBytes());
			 os.write(public_user.getBytes());
		     os.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
//	public static void main(String[] args) {
//		create_an_example();
//	}
    public static void main(String[] args) {

    	 	if (args.length != 1) {
             System.out.println("Expects file name as parameter");
             return;
         }
    	 	System.out.println("input file= " + args[0]);

         int n = 3;
         String public_user;
         String secret_user1;
         String secret_user2;
         String pw = "1234";

      // Read all inputs.
         List<Character> values = new ArrayList<>();
         try (FileInputStream fis = new FileInputStream(args[0])) {
             byte[] bytes = new byte[Character.BYTES];
             while (((fis.read(bytes)) != -1)) {
                 char value = ByteBuffer.wrap(bytes).getChar();
                 values.add(value);
             }
         } catch (IOException e) {
             System.err.println("Error reading input");
             e.printStackTrace();
             return;
         }
         if (values.size() < n) {
             throw new RuntimeException("Too less data...");
         }

         int m = values.size() / n;
         System.out.println("m=" + m);

         // Read secret1.
         char[] secret1_arr = new char[m];
         for (int i = 0; i < m; i++) {
             secret1_arr[i] = values.get(i);
         }
         secret_user1 = new String(secret1_arr);

         // Read secret2.
         char[] secret2_arr = new char[m];
         for (int i = 0; i < m; i++) {
             secret2_arr[i] = values.get(i + m);
         }
         secret_user2= new String(secret2_arr);

         // Read public.
         char[] public_arr = new char[m];
         for (int i = 0; i < m; i++) {
             public_arr[i] = values.get(i + 2 * m);
         }
         public_user = new String(public_arr);

         System.out.println("secret1_username=" + secret_user1);
         System.out.println("secret2_username=" + secret_user2);
         System.out.println("public_username=" + public_user);

    		DataSourceRealm DSR = new DataSourceRealm();
        /* Create Connection do database. */
        Connection dbConnection = null;

        // Ensure that we have an open database connection
        dbConnection = DSR.open();
        if (dbConnection == null) {
            // If the db connection open fails, return "not authenticated"
        		System.out.println("DB connection failed...");
        }
        /* Prepare database. */
        Statement st;
        try {
	        st = dbConnection.createStatement();
	        st.execute("delete from users;");
	        st.execute("insert into users (user_name, user_pass) values ('" + public_user + "', '" + pw + "');");
        }catch(Exception e){
        		System.out.println("Could not insert user in the table...");
        		throw new RuntimeException(e);
        }
        Mem.clear();
        DSR.authenticate_unsafe(dbConnection, secret_user1, pw);
        long cost1 = Mem.instrCost;
        System.out.println("cost1=" + cost1);

        Mem.clear();
        DSR.authenticate_unsafe(dbConnection, secret_user2, pw);
        long cost2 = Mem.instrCost;
        System.out.println("cost2=" + cost2);

        Kelinci.addCost(Math.abs(cost1 - cost2));
        System.out.println("|cost1-cost2|=" + Math.abs(cost2-cost1));

        /* Clean database. */
        try {
        		st.execute("delete from users;");
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
        		DSR.close(dbConnection);
        }

        System.out.println("Done.");
    }

}
