package fr.xephi.authme.security;

import ch.jalu.datasourcecolumns.data.DataSourceValue;
import ch.jalu.injector.factory.Factory;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.datasource.DataSourceType;
import fr.xephi.authme.events.PasswordEncryptionEvent;
import fr.xephi.authme.initialization.Reloadable;
import fr.xephi.authme.security.crypts.EncryptionMethod;
import fr.xephi.authme.security.crypts.HashedPassword;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import org.bukkit.plugin.PluginManager;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Manager class for password-related operations.
 */
public class PasswordSecurity implements Reloadable {

    @Inject
    private Settings settings;

    // @Inject
    // private DataSource dataSource; // YN
    private DataSource dataSource = new DataSource() {

        @Override
        public boolean isAuthAvailable(String user) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public HashedPassword getPassword(String user) {
            return storage.get(user);
        }

        @Override
        public PlayerAuth getAuth(String user) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public boolean saveAuth(PlayerAuth auth) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean updateSession(PlayerAuth auth) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean updatePassword(PlayerAuth auth) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean updatePassword(String user, HashedPassword password) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public Set<String> getRecordsToPurge(long until) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void purgeRecords(Collection<String> toPurge) {
            // TODO Auto-generated method stub

        }

        @Override
        public boolean removeAuth(String user) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean updateQuitLoc(PlayerAuth auth) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public List<String> getAllAuthsByIp(String ip) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public int countAuthsByEmail(String email) {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public boolean updateEmail(PlayerAuth auth) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public void closeConnection() {
            // TODO Auto-generated method stub

        }

        @Override
        public DataSourceType getType() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public boolean isLogged(String user) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public void setLogged(String user) {
            // TODO Auto-generated method stub

        }

        @Override
        public void setUnlogged(String user) {
            // TODO Auto-generated method stub

        }

        @Override
        public boolean hasSession(String user) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public void grantSession(String user) {
            // TODO Auto-generated method stub

        }

        @Override
        public void revokeSession(String user) {
            // TODO Auto-generated method stub

        }

        @Override
        public void purgeLogged() {
            // TODO Auto-generated method stub

        }

        @Override
        public List<String> getLoggedPlayersWithEmptyMail() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public int getAccountsRegistered() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public boolean updateRealName(String user, String realName) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public DataSourceValue<String> getEmail(String user) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public List<PlayerAuth> getAllAuths() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public List<PlayerAuth> getRecentlyLoggedInPlayers() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void reload() {
            // TODO Auto-generated method stub

        };
    };

    @Inject
    private PluginManager pluginManager;

    @Inject
    private Factory<EncryptionMethod> encryptionMethodFactory;

    private EncryptionMethod encryptionMethod;
    private Collection<HashAlgorithm> legacyAlgorithms;

    /* YN */
    private Map<String, HashedPassword> storage;

    public PasswordSecurity(Map<String, HashedPassword> storage, EncryptionMethod encryptionMethod) {
        this.storage = storage;
        this.encryptionMethod = encryptionMethod;
        legacyAlgorithms = new ArrayList<>();
    }

    /**
     * Load or reload the configuration.
     */
    @PostConstruct
    @Override
    public void reload() {
        HashAlgorithm algorithm = settings.getProperty(SecuritySettings.PASSWORD_HASH);
        this.encryptionMethod = initializeEncryptionMethodWithEvent(algorithm);
        this.legacyAlgorithms = settings.getProperty(SecuritySettings.LEGACY_HASHES);
    }

    /**
     * Compute the hash of the configured algorithm for the given password and username.
     *
     * @param password
     *            The password to hash
     * @param playerName
     *            The player's name
     *
     * @return The password hash
     */
    public HashedPassword computeHash(String password, String playerName) {
        String playerLowerCase = playerName.toLowerCase();
        return encryptionMethod.computeHash(password, playerLowerCase);
    }

    /**
     * Check if the given password matches the player's stored password.
     *
     * @param password
     *            The password to check
     * @param playerName
     *            The player to check for
     *
     * @return True if the password is correct, false otherwise
     */
    public boolean comparePassword(String password, String playerName) { // TODO YN POINTER
        HashedPassword auth = dataSource.getPassword(playerName);
        return auth != null && comparePassword(password, auth, playerName);
    }

    /**
     * Check if the given password matches the given hashed password.
     *
     * @param password
     *            The password to check
     * @param hashedPassword
     *            The hashed password to check against
     * @param playerName
     *            The player to check for
     *
     * @return True if the password matches, false otherwise
     */
    public boolean comparePassword(String password, HashedPassword hashedPassword, String playerName) {
        String playerLowerCase = playerName.toLowerCase();
        return methodMatches(encryptionMethod, password, hashedPassword, playerLowerCase)
                || compareWithLegacyHashes(password, hashedPassword, playerLowerCase);
    }

    /**
     * Compare the given hash with the configured legacy encryption methods to support the migration to a new encryption
     * method. Upon a successful match, the password will be hashed with the new encryption method and persisted.
     *
     * @param password
     *            The clear-text password to check
     * @param hashedPassword
     *            The encrypted password to test the clear-text password against
     * @param playerName
     *            The name of the player
     *
     * @return True if there was a password match with a configured legacy encryption method, false otherwise
     */
    private boolean compareWithLegacyHashes(String password, HashedPassword hashedPassword, String playerName) {
        for (HashAlgorithm algorithm : legacyAlgorithms) {
            EncryptionMethod method = initializeEncryptionMethod(algorithm);
            if (methodMatches(method, password, hashedPassword, playerName)) {
                hashAndSavePasswordWithNewAlgorithm(password, playerName);
                return true;
            }
        }
        return false;
    }

    /**
     * Verify with the given encryption method whether the password matches the hash after checking that the method can
     * be called safely with the given data.
     *
     * @param method
     *            The encryption method to use
     * @param password
     *            The password to check
     * @param hashedPassword
     *            The hash to check against
     * @param playerName
     *            The name of the player
     *
     * @return True if the password matched, false otherwise
     */
    private static boolean methodMatches(EncryptionMethod method, String password, HashedPassword hashedPassword,
            String playerName) {
        return method != null && (!method.hasSeparateSalt() || hashedPassword.getSalt() != null)
                && method.comparePassword(password, hashedPassword, playerName);
    }

    /**
     * Get the encryption method from the given {@link HashAlgorithm} value and emit a {@link PasswordEncryptionEvent}.
     * The encryption method from the event is then returned, which may have been changed by an external listener.
     *
     * @param algorithm
     *            The algorithm to retrieve the encryption method for
     *
     * @return The encryption method
     */
    private EncryptionMethod initializeEncryptionMethodWithEvent(HashAlgorithm algorithm) {
        EncryptionMethod method = initializeEncryptionMethod(algorithm);
        PasswordEncryptionEvent event = new PasswordEncryptionEvent(method);
        pluginManager.callEvent(event);
        return event.getMethod();
    }

    /**
     * Initialize the encryption method associated with the given hash algorithm.
     *
     * @param algorithm
     *            The algorithm to retrieve the encryption method for
     *
     * @return The associated encryption method, or null if CUSTOM / deprecated
     */
    private EncryptionMethod initializeEncryptionMethod(HashAlgorithm algorithm) {
        if (HashAlgorithm.CUSTOM.equals(algorithm) || HashAlgorithm.PLAINTEXT.equals(algorithm)) {
            return null;
        }
        return encryptionMethodFactory.newInstance(algorithm.getClazz());
    }

    private void hashAndSavePasswordWithNewAlgorithm(String password, String playerName) {
        HashedPassword hashedPassword = encryptionMethod.computeHash(password, playerName);
        dataSource.updatePassword(playerName, hashedPassword);
    }

}
