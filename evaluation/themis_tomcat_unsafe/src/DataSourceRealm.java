/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
//package org.apache.catalina.realm;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
//import java.util.Queue;

import org.apache.catalina.realm.GenericPrincipal;
import org.apache.catalina.realm.RealmBase;
//import ConcurrentMessageDigest;
import org.apache.catalina.LifecycleException;
import org.apache.tomcat.util.codec.binary.Base64;
import org.apache.tomcat.util.buf.HexUtils;
import java.sql.DriverManager; 
import java.sql.Statement;
//import java.security.MessageDigest;

import java.util.Random;

/**
*
* Implementation of <b>Realm</b> that works with any JDBC JNDI DataSource.
* See the JDBCRealm.howto for more details on how to set up the database and
* for configuration options.
*
* @author Glenn L. Nielsen
* @author Craig R. McClanahan
* @author Carson McDonald
* @author Ignacio Ortega
*/
/*
 * Shirin: 
 * http://www.vogella.com/tutorials/MySQLJava/article.html
 * https://tomcat.apache.org/tomcat-3.3-doc/JDBCRealm-howto.html
 * install h2:
 * create a db.
 * configure tomcat
 * 
 */
public class DataSourceRealm extends RealmBase {


    // ----------------------------------------------------- Instance Variables


    /**
     * The generated string for the roles PreparedStatement
     */
    private String preparedRoles = null;


    /**
     * The generated string for the credentials PreparedStatement
     */
    private String preparedCredentials = null;


    /**
     * The name of the JNDI JDBC DataSource
     */
    protected String dataSourceName = null;


    /**
     * Context local datasource.
     */
//    protected boolean localDataSource = false;
    /* Shirin: The realm will by default always look for a global datasource 
     * (configured in Tomcat side in /conf/server.xml), 
     * however we want to define a local datasource. 
     * For the realm being able to find the datasource, 
     * I set the localDataSource attribute to true.
     */
    protected boolean localDataSource = true;


    /**
     * Descriptive information about this Realm implementation.
     */
    protected static final String name = "DataSourceRealm";


    /**
     * The column in the user role table that names a role
     */
    protected String roleNameCol = null;


    /**
     * The column in the user table that holds the user's credentials
     */
    protected String userCredCol = null;


    /**
     * The column in the user table that holds the user's name
     */
    protected String userNameCol = null;


    /**
     * The table that holds the relation between user's and roles
     */
    protected String userRoleTable = null;


    /**
     * The table that holds user data.
     */
    protected String userTable = null;


    private Charset encoding = StandardCharsets.UTF_8;
    private String algorithm = null;
    
    // ------------------------------------------------------------- Properties
    public String getAlgorithm() {
        return algorithm;
    }
    public void setAlgorithm(String algorithm) throws NoSuchAlgorithmException {
        ConcurrentMessageDigest.init(algorithm);
        this.algorithm = algorithm;
    }

    /**
     * @return the name of the JNDI JDBC DataSource.
     */
    public String getDataSourceName() {
        return dataSourceName;
    }

    /**
     * Set the name of the JNDI JDBC DataSource.
     *
     * @param dataSourceName the name of the JNDI JDBC DataSource
     */
    public void setDataSourceName( String dataSourceName) {
      this.dataSourceName = dataSourceName;
    }

    /**
     * @return if the datasource will be looked up in the webapp JNDI Context.
     */
    public boolean getLocalDataSource() {
        return localDataSource;
    }

    /**
     * Set to true to cause the datasource to be looked up in the webapp JNDI
     * Context.
     *
     * @param localDataSource the new flag value
     */
    public void setLocalDataSource(boolean localDataSource) {
      this.localDataSource = localDataSource;
    }

    /**
     * @return the column in the user role table that names a role.
     */
    public String getRoleNameCol() {
        return roleNameCol;
    }

    /**
     * Set the column in the user role table that names a role.
     *
     * @param roleNameCol The column name
     */
    public void setRoleNameCol( String roleNameCol ) {
        this.roleNameCol = roleNameCol;
    }

    /**
     * @return the column in the user table that holds the user's credentials.
     */
    public String getUserCredCol() {
        return userCredCol;
    }

    /**
     * Set the column in the user table that holds the user's credentials.
     *
     * @param userCredCol The column name
     */
    public void setUserCredCol( String userCredCol ) {
       this.userCredCol = userCredCol;
    }

    /**
     * @return the column in the user table that holds the user's name.
     */
    public String getUserNameCol() {
        return userNameCol;
    }

    /**
     * Set the column in the user table that holds the user's name.
     *
     * @param userNameCol The column name
     */
    public void setUserNameCol( String userNameCol ) {
       this.userNameCol = userNameCol;
    }

    /**
     * @return the table that holds the relation between user's and roles.
     */
    public String getUserRoleTable() {
        return userRoleTable;
    }

    /**
     * Set the table that holds the relation between user's and roles.
     *
     * @param userRoleTable The table name
     */
    public void setUserRoleTable( String userRoleTable ) {
        this.userRoleTable = userRoleTable;
    }

    /**
     * @return the table that holds user data..
     */
    public String getUserTable() {
        return userTable;
    }

    /**
     * Set the table that holds user data.
     *
     * @param userTable The table name
     */
    public void setUserTable( String userTable ) {
      this.userTable = userTable;
    }


    // --------------------------------------------------------- Public Methods


    // -------------------------------------------------------- Package Methods


    // ------------------------------------------------------ Protected Methods


    /**
     * Return the Principal associated with the specified username and
     * credentials, if there is one; otherwise return <code>null</code>.
     *
     * @param dbConnection The database connection to be used
     * @param username Username of the Principal to look up
     * @param credentials Password or other credentials to use in
     *  authenticating this username
     * @return the associated principal, or <code>null</code> if there is none.
     */
    protected Boolean authenticate_safe(Connection dbConnection,
                                     String username,
                                     String credentials) {
        // No user or no credentials
        // Can't possibly authenticate, don't bother the database then
        if (username == null || credentials == null) {
            if (containerLog.isTraceEnabled())
                containerLog.trace(sm.getString("dataSourceRealm.authenticateFailure",
                                                username));
            return null;
        }

        // Look up the user's credentials
        String dbCredentials = getPassword(dbConnection, username);

        if(dbCredentials == null) {
            // User was not found in the database.
            // Waste a bit of time as not to reveal that the user does not exist.
//            getCredentialHandler().mutate(credentials);
        		mutate(credentials);
            return false;
        }

        // Validate the user's credentials
//        boolean validated = getCredentialHandler().matches(credentials, dbCredentials);
//        boolean validated = credentials.matches(dbCredentials);
        boolean validated = matches(credentials, dbCredentials);

        if (!validated) {
        		System.out.println("User not validated ...");
            return false;
        }
        // Create and return a suitable Principal for this user
        return true;
    }   
    /* 
     * FROM DigestCredentialHandlerBase class
     */
    public String mutate(String userCredential) {
    		Random random = new Random();
        byte[] salt = null;
        int iterations = 20000; //  public static final int DEFAULT_ITERATIONS = 20000;
        int saltLength = 32; //public static final int DEFAULT_SALT_LENGTH = 32
        if (saltLength == 0) {
            salt = new byte[0];
        } else if (saltLength > 0) {
            salt = new byte[saltLength];
            // Concurrent use of this random is unlikely to be a performance
            // issue as it is only used during stored password generation.
            random.nextBytes(salt);
        }

        String serverCredential = mutate(userCredential, salt, iterations);

        // Failed to generate server credential from user credential. Points to
        // a configuration issue. The root cause should have been logged in the
        // mutate() method.
        if (serverCredential == null) {
            return null;
        }

        if (saltLength == 0 && iterations == 1) {
            // Output the simple/old format for backwards compatibility
            return serverCredential;
        } else {
            StringBuilder result =
                    new StringBuilder((saltLength << 1) + 10 + serverCredential.length() + 2);
            result.append(HexUtils.toHexString(salt));
            result.append('$');
            result.append(iterations);
            result.append('$');
            result.append(serverCredential);

            return result.toString();
        }
    }

    
    /*
     * Got implementation of matches and mutate from here:
     * https://apache.googlesource.com/tomcat/+/TOMCAT_9_0_0_M9/java/org/apache/catalina/realm/MessageDigestCredentialHandler.java
     */
    public boolean matches(String inputCredentials, String storedCredentials) {
        if (inputCredentials == null || storedCredentials == null) {
            return false;
        }

        // Some directories and databases prefix the password with the hash
        // type. The string is in a format compatible with Base64.encode not
        // the normal hex encoding of the digest
        if (storedCredentials.startsWith("{MD5}") ||
                storedCredentials.startsWith("{SHA}")) {
            // Server is storing digested passwords with a prefix indicating
            // the digest type
            String serverDigest = storedCredentials.substring(5);
            String userDigest = Base64.encodeBase64String(ConcurrentMessageDigest.digest(
                    getAlgorithm(), inputCredentials.getBytes(StandardCharsets.ISO_8859_1)));
            return userDigest.equals(serverDigest);
        } else if (storedCredentials.startsWith("{SSHA}")) {
            // Server is storing digested passwords with a prefix indicating
            // the digest type and the salt used when creating that digest
            String serverDigestPlusSalt = storedCredentials.substring(6);
            // Need to convert the salt to bytes to apply it to the user's
            // digested password.
            byte[] serverDigestPlusSaltBytes =
                    Base64.decodeBase64(serverDigestPlusSalt);
            final int saltPos = 20;
            byte[] serverDigestBytes = new byte[saltPos];
            System.arraycopy(serverDigestPlusSaltBytes, 0,
                    serverDigestBytes, 0, saltPos);
            final int saltLength = serverDigestPlusSaltBytes.length - saltPos;
            byte[] serverSaltBytes = new byte[saltLength];
            System.arraycopy(serverDigestPlusSaltBytes, saltPos,
                    serverSaltBytes, 0, saltLength);
            // Generate the digested form of the user provided password
            // using the salt
            byte[] userDigestBytes = ConcurrentMessageDigest.digest(getAlgorithm(),
                    inputCredentials.getBytes(StandardCharsets.ISO_8859_1),
                    serverSaltBytes);
            return Arrays.equals(userDigestBytes, serverDigestBytes);
        } 
//        else if (storedCredentials.indexOf('$') > -1) {
//            return matchesSaltIterationsEncoded(inputCredentials, storedCredentials);
//        } 
        else {
            // Hex hashes should be compared case-insensitively
            String userDigest = mutate(inputCredentials, null, 1);
//            return storedCredentials.equalsIgnoreCase(userDigest);
            return equalsIgnoreCase(userDigest, storedCredentials);
        }
    }
    
    public boolean equalsIgnoreCase(String anotherString, String thisString ) {
        return (thisString == anotherString) ? true
                : (anotherString != null)
                && (anotherString.length() == thisString.length())
                && regionMatches(true, 0, anotherString, 0, thisString.length(), thisString);
    }
    
    public boolean regionMatches(boolean ignoreCase, int toffset,
            String other, int ooffset, int len, String thisString) {
        char ta[] = thisString.toCharArray();
        int to = toffset;
        char pa[] = other.toCharArray();
        int po = ooffset;
        // Note: toffset, ooffset, or len might be near -1>>>1.
        if ((ooffset < 0) || (toffset < 0)
                || (toffset > (long)thisString.length() - len)
                || (ooffset > (long)other.length() - len)) {
            return false;
        }
        while (len-- > 0) {
            char c1 = ta[to++];
            char c2 = pa[po++];
            if (c1 == c2) {
                continue;
            }
            if (ignoreCase) {
                // If characters don't match but case may be ignored,
                // try converting both characters to uppercase.
                // If the results match, then the comparison scan should
                // continue.
                char u1 = Character.toUpperCase(c1);
                char u2 = Character.toUpperCase(c2);
                if (u1 == u2) {
                    continue;
                }
                // Unfortunately, conversion to uppercase does not work properly
                // for the Georgian alphabet, which has strange rules about case
                // conversion.  So we need to make one last check before
                // exiting.
                if (Character.toLowerCase(u1) == Character.toLowerCase(u2)) {
                    continue;
                }
            }
            return false;
        }
        return true;
    }
    
//    public String mutate(String inputCredentials, String credentialHandlers) {
//        if (credentialHandlers.isEmpty()) {
//            return null;
//        }
//        return credentialHandlers.mutate(inputCredentials);
//    }
    

    
    protected String mutate(String inputCredentials, byte[] salt, int iterations) {
        if (algorithm == null) {
            return inputCredentials;
        } else {
            byte[] userDigest;
            if (salt == null) {
                userDigest = ConcurrentMessageDigest.digest(algorithm, iterations,
                        inputCredentials.getBytes(encoding));
            } else {
                userDigest = ConcurrentMessageDigest.digest(algorithm, iterations,
                        salt, inputCredentials.getBytes(encoding));
            }
            return HexUtils.toHexString(userDigest);
        }
    }

//    public static byte[] digest(String algorithm, int rounds, byte[]... input) {
//        Queue<MessageDigest> queue = queues.get(algorithm);
//        if (queue == null) {
//            throw new IllegalStateException("Must call init() first");
//        }
//        MessageDigest md = queue.poll();
//        if (md == null) {
//            try {
//                md = MessageDigest.getInstance(algorithm);
//            } catch (NoSuchAlgorithmException e) {
//                // Ignore. Impossible if init() has been successfully called
//                // first.
//                throw new IllegalStateException("Must call init() first");
//            }
//        }
//        // Round 1
//        for (byte[] bytes : input) {
//            md.update(bytes);
//        }
//        byte[] result = md.digest();
//        // Subsequent rounds
//        if (rounds > 1) {
//            for (int i = 1; i < rounds; i++) {
//                md.update(result);
//                result = md.digest();
//            }
//        }
//        queue.add(md);
//        return result;
//    }
//    
    protected Boolean authenticate_unsafe(Connection dbConnection,
                                     String username,
                                     String credentials) {
        // No user or no credentials
        // Can't possibly authenticate, don't bother the database then
        if (username == null || credentials == null) {
            return null;
        }

        System.out.println("Looking up the user's credentials ...");
        String dbCredentials = getPassword(dbConnection, username);

        if(dbCredentials == null) {
            // Timing channel!!!!
        		System.out.println("User not found ...");
            return false;
        }

        // Validate the user's credentials
        boolean validated = matches(credentials, dbCredentials);
//        boolean validated = getCredentialHandler().matches(credentials, dbCredentials);
        if (! validated) {
        		System.out.println("User not validated...");
            return false;
        }
        System.out.println("User is validated...");
        return true;
    }


    /**
     * Close the specified database connection.
     *
     * @param dbConnection The connection to be closed
     */
    protected void close(Connection dbConnection) {

        // Do nothing if the database connection is already closed
        if (dbConnection == null)
            return;

        // Commit if not auto committed
        try {
            if (!dbConnection.getAutoCommit()) {
                dbConnection.commit();
            }
        } catch (SQLException e) {
            containerLog.error("Exception committing connection before closing:", e);
        }

        // Close this database connection, and log any errors
        try {
            dbConnection.close();
        } catch (SQLException e) {
            containerLog.error(sm.getString("dataSourceRealm.close"), e); // Just log it here
        }

    }
    protected Connection open() {
    	// Shirin: replaced with our database connection 
        try {
        		System.out.println("Connecting to database..."); 
        		Connection conn = DriverManager.getConnection("jdbc:h2:~/tomcat", "sa", "");  
        		return conn;
        } catch (Exception e) {
        		e.printStackTrace();
        }
        return null;
    }

    /**
     * Return a short name for this Realm implementation.
     */
    protected String getName() {

        return (name);

    }

    /**
     * @return the password associated with the given principal's user name.
     */
    @Override
    protected String getPassword(String username) {

        Connection dbConnection = null;

        // Ensure that we have an open database connection
        dbConnection = open();
        if (dbConnection == null) {
            return null;
        }

        try {
            return getPassword(dbConnection, username);
        } finally {
            close(dbConnection);
        }
    }

    /**
     * Return the password associated with the given principal's user name.
     * @param dbConnection The database connection to be used
     * @param username Username for which password should be retrieved
     * @return the password for the specified user
     */
    protected String getPassword(Connection dbConnection,
                                 String username) {

        String dbCredentials = null;
        try {
            Statement st = dbConnection.createStatement(); 
            String sql =  "SELECT * FROM users where user_name='" + username + "';";
            ResultSet rs = st.executeQuery(sql);
            if (rs.next()) {
                dbCredentials = rs.getString(2);
            }
            return (dbCredentials != null) ? dbCredentials.trim() : null;

        } catch (SQLException e) {
//        		e.printStackTrace();  
            return null;
        }
    }


    /**
     * Return the Principal associated with the given user name.
     * @param username the user name
     * @return the principal object
     */
    @Override
    protected Principal getPrincipal(String username) {
        Connection dbConnection = open();
        if (dbConnection == null) {
            return new GenericPrincipal(username, null, null);
        }
        try {
            return (new GenericPrincipal(username,
                    getPassword(dbConnection, username),
                    getRoles(dbConnection, username)));
        } finally {
            close(dbConnection);
        }

    }

    /**
     * Return the roles associated with the given user name.
     * @param username User name for which roles should be retrieved
     * @return an array list of the role names
     */
    protected ArrayList<String> getRoles(String username) {

        Connection dbConnection = null;

        // Ensure that we have an open database connection
        dbConnection = open();
        if (dbConnection == null) {
            return null;
        }

        try {
            return getRoles(dbConnection, username);
        } finally {
            close(dbConnection);
        }
    }

    /**
     * Return the roles associated with the given user name
     * @param dbConnection The database connection to be used
     * @param username User name for which roles should be retrieved
     * @return an array list of the role names
     */
    protected ArrayList<String> getRoles(Connection dbConnection,
                                     String username) {

        if (allRolesMode != AllRolesMode.STRICT_MODE && !isRoleStoreDefined()) {
            // Using an authentication only configuration and no role store has
            // been defined so don't spend cycles looking
            return null;
        }

        ArrayList<String> list = null;

        try (PreparedStatement stmt = roles(dbConnection, username);
                ResultSet rs = stmt.executeQuery()) {
            list = new ArrayList<>();

            while (rs.next()) {
                String role = rs.getString(1);
                if (role != null) {
                    list.add(role.trim());
                }
            }
            return list;
        } catch(SQLException e) {
            containerLog.error(
                sm.getString("dataSourceRealm.getRoles.exception", username), e);
        }

        return null;
    }

    /**
     * Return a PreparedStatement configured to perform the SELECT required
     * to retrieve user credentials for the specified username.
     *
     * @param dbConnection The database connection to be used
     * @param username User name for which credentials should be retrieved
     * @return the prepared statement
     * @exception SQLException if a database error occurs
     */
    private PreparedStatement credentials(Connection dbConnection, String username)
        throws SQLException {

        PreparedStatement credentials =
            dbConnection.prepareStatement(preparedCredentials);

        credentials.setString(1, username);
        return (credentials);

    }

    /**
     * Return a PreparedStatement configured to perform the SELECT required
     * to retrieve user roles for the specified username.
     *
     * @param dbConnection The database connection to be used
     * @param username User name for which roles should be retrieved
     * @return the prepared statement
     * @exception SQLException if a database error occurs
     */
    private PreparedStatement roles(Connection dbConnection, String username)
        throws SQLException {

        PreparedStatement roles =
            dbConnection.prepareStatement(preparedRoles);

        roles.setString(1, username);
        return (roles);

    }


    private boolean isRoleStoreDefined() {
        return userRoleTable != null || roleNameCol != null;
    }


    // ------------------------------------------------------ Lifecycle Methods

    /**
     * Prepare for the beginning of active use of the public methods of this
     * component and implement the requirements of
     * {@link org.apache.catalina.util.LifecycleBase#startInternal()}.
     *
     * @exception LifecycleException if this component detects a fatal error
     *  that prevents this component from being used
     */
    @Override
    protected void startInternal() throws LifecycleException {

        // Create the roles PreparedStatement string
        StringBuilder temp = new StringBuilder("SELECT ");
        temp.append(roleNameCol);
        temp.append(" FROM ");
        temp.append(userRoleTable);
        temp.append(" WHERE ");
        temp.append(userNameCol);
        temp.append(" = ?");
        preparedRoles = temp.toString();

        // Create the credentials PreparedStatement string
        temp = new StringBuilder("SELECT ");
        temp.append(userCredCol);
        temp.append(" FROM ");
        temp.append(userTable);
        temp.append(" WHERE ");
        temp.append(userNameCol);
        temp.append(" = ?");
        preparedCredentials = temp.toString();

        super.startInternal();
    }
    
//    public static void main(String[] args) {
//	    	DataSourceRealm DSR = new DataSourceRealm();
//	    	DSR.authenticate("shirin", "123");
//    }
}


