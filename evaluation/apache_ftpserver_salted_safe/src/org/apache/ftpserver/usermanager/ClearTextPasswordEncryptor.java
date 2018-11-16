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

import java.io.UnsupportedEncodingException;

/**
 * Password encryptor that does no encryption, that is, keps the password in clear text
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class ClearTextPasswordEncryptor implements PasswordEncryptor {

    /* YN */
    private boolean safeMode;

    public ClearTextPasswordEncryptor() {
        this.safeMode = false;
    }

    public ClearTextPasswordEncryptor(boolean safeMode) {
        this.safeMode = safeMode;
    }

    /**
     * Returns the clear text password
     */
    public String encrypt(String password) {
        return password;
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

        // return passwordToCheck.equals(storedPassword);
        if (safeMode) {
//            return isEqual_safe(passwordToCheck, storedPassword);
            return PasswordUtil.secureCompare(passwordToCheck, storedPassword, 1024);
        } else {
            return isEqual_unsafe(passwordToCheck, storedPassword);
        }

    }

    /* inline equals method of String class */
    public boolean isEqual_unsafe(String thisObject, Object otherObject) {
        if (thisObject == otherObject) {
            return true;
        }
        if (otherObject instanceof String) {
            String anotherString = (String) otherObject;
            int n = thisObject.length();
            if (n == anotherString.length()) {
                char v1[] = thisObject.toCharArray();
                char v2[] = anotherString.toCharArray();
                int i = 0;
                while (n-- != 0) {
                    if (v1[i] != v2[i])
                        return false;
                    i++;
                }
                return true;
            }
        }
        return false;
    }

    /* YN: added safe version of isEquals */
    public boolean isEqual_safe(String a, String b) {
        // if (a == b) {
        // return true;
        // }
        // char a_value[] = a.toCharArray();
        // char b_value[] = b.toCharArray();
        // if (a_value.length != b_value.length) {
        // return false;
        // }
        // boolean match = true;
        // for (int i = 0; i < a_value.length; i++) {
        // // match &= a_value[i] == b_value[i];
        // boolean local_match = a_value[i] == b_value[i];
        // match &= local_match;
        // }
        // return match;
        // byte a_bytes[];
        // byte b_bytes[];
        // try {
        // a_bytes = a.getBytes("UTF-8");
        // b_bytes = b.getBytes("UTF-8");
        // } catch (UnsupportedEncodingException uee) {
        // return false;
        // }
        // if (a_bytes.length != b_bytes.length)
        // return false;
        // byte ret = 0;
        // for (int i = 0; i < b_bytes.length; i++)
        // ret |= a_bytes[i] ^ b_bytes[i];
        // return ret == 0;

        char a_value[] = a.toCharArray();
        char b_value[] = b.toCharArray();
        boolean unused;
        boolean matches = true;
        for (int i = 0; i < a_value.length; i++) {
            if (i < b_value.length) {
                if (a_value[i] != b_value[i]) {
                    matches = false;
                } else {
                    unused = true;
                }
            } else {
                unused = false;
                unused = true;
            }
        }
        return matches;
    }

}
