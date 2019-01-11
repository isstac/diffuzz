/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ftpserver.usermanager;

import org.apache.ftpserver.util.EncryptUtils;

/**
 * Password encryptor that hashes the password using MD5. Please note that this form of encryption is sensitive to
 * lookup attacks.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class Md5PasswordEncryptor implements PasswordEncryptor {
    
    /* YN */
    private boolean safeMode;
    
    public Md5PasswordEncryptor() {
        this.safeMode = false;
    }
    
    public Md5PasswordEncryptor(boolean safeMode) {
        this.safeMode = safeMode;
    }

    /**
     * Hashes the password using MD5
     */
    public String encrypt(String password) {
        return EncryptUtils.encryptMD5(password);
    }

    /**
     * {@inheritDoc}
     */
    public boolean matches(String passwordToCheck, String storedPassword) {
        if (storedPassword == null) {
            throw new NullPointerException("storedPassword can not be null");
        }
        if (passwordToCheck == null) {
            throw new NullPointerException("passwordToCheck can not be null");
        }

        // return encrypt(passwordToCheck).equalsIgnoreCase(storedPassword);
        if (safeMode) {
            return PasswordUtil.secureCompare(encrypt(passwordToCheck), storedPassword, 1024);
        } else {
            return equalsIgnoreCase(encrypt(passwordToCheck), (storedPassword));    
        }
    }

    /* YN inline implementations */

    public boolean equalsIgnoreCase(String thisString, String anotherString) {
        return (thisString == anotherString) ? true
                : (anotherString != null) && (anotherString.length() == thisString.length())
                        && regionMatches(thisString, true, 0, anotherString, 0, thisString.length());
    }

    public boolean regionMatches(String thisValue, boolean ignoreCase, int toffset, String other, int ooffset,
            int len) {
        char ta[] = thisValue.toCharArray();
        int to = toffset;
        char pa[] = other.toCharArray();
        int po = ooffset;
        // Note: toffset, ooffset, or len might be near -1>>>1.
        if ((ooffset < 0) || (toffset < 0) || (toffset > (long) thisValue.length() - len)
                || (ooffset > (long) other.length() - len)) {
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
                // conversion. So we need to make one last check before
                // exiting.
                if (Character.toLowerCase(u1) == Character.toLowerCase(u2)) {
                    continue;
                }
            }
            return false;
        }
        return true;
    }

}
