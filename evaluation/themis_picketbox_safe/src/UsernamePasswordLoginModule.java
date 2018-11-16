/*
* JBoss, Home of Professional Open Source
* Copyright 2005, JBoss Inc., and individual contributors as indicated
* by the @authors tag. See the copyright.txt in the distribution for a
* full listing of individual contributors.
*
* This is free software; you can redistribute it and/or modify it
* under the terms of the GNU Lesser General Public License as
* published by the Free Software Foundation; either version 2.1 of
* the License, or (at your option) any later version.
*
* This software is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this software; if not, write to the Free
* Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
* 02110-1301 USA, or see the FSF site: http://www.fsf.org.
*/
//package org.jboss.security.auth.spi;

//import org.jboss.crypto.digest.DigestCallback;
//import org.jboss.security.PicketBoxLogger;
//import org.jboss.security.PicketBoxMessages;
//import org.jboss.security.plugins.ClassLoaderLocatorFactory;
//import org.jboss.security.vault.SecurityVaultException;
//import org.jboss.security.vault.SecurityVaultUtil;
//
//import javax.security.auth.Subject;
//import javax.security.auth.callback.*;
//import javax.security.auth.login.FailedLoginException;
//import javax.security.auth.login.LoginException;
//
//import java.io.IOException;
//import java.io.InputStream;
//import java.security.Principal;
//import java.util.HashMap;
//import java.util.Map;


/** An abstract subclass of AbstractServerLoginModule that imposes
 * an identity == String username, credentials == String password view on
 * the login process.
 * <p>
 * Subclasses override the <code>getUsersPassword()</code>
 * and <code>getRoleSets()</code> methods to return the expected password and roles
 * for the user.
 *
 * @see #getUsername()
 * @see #getUsersPassword()
 * @see #getRoleSets()
 * @see #createIdentity(String)
 
 @author Scott.Stark@jboss.org
 @version $Revision$
 */
public abstract class UsernamePasswordLoginModule// extends AbstractServerLoginModule
{

   /** A hook that allows subclasses to change the validation of the input
    password against the expected password. This version checks that
    neither inputPassword or expectedPassword are null that that
    inputPassword.equals(expectedPassword) is true;
    @return true if the inputPassword is valid, false otherwise.
    */
   protected static boolean validatePassword_safe(String inputPassword, String expectedPassword)
   {
      if( inputPassword == null || expectedPassword == null )
         return false;
      boolean valid = false;
//      if( ignorePasswordCase == true )
//         valid = inputPassword.equalsIgnoreCase(expectedPassword);
//      else
         valid = slowEquals(inputPassword, expectedPassword);
      return valid;
   }

   protected static boolean validatePassword_unsafe(String inputPassword, String expectedPassword)
   {
      if( inputPassword == null || expectedPassword == null )
         return false;
      boolean valid = false;
//      if( ignorePasswordCase == true )
//         valid = inputPassword.equalsIgnoreCase(expectedPassword);
//      else
//         valid = inputPassword.equals(expectedPassword);
      valid = equals(inputPassword, expectedPassword);
      return valid;
   }


   /**
    * Compares two strings in length-constant time. This comparison method
    * is used so that passwords or password hashes cannot be extracted
    * using a timing attack.
    *
    * @param stinga the first string
    * @param stringb the second string
    * @return {@code true} if both byte strings are the equal, {@code false} if not
    * @see java.security.MessageDigest#isEqual(byte[], byte[])
    */
   private static boolean slowEquals(String stinga, String stringb)
   {
       int aLength = stinga.length();
       int bLength = stringb.length();
       int diff = aLength ^ bLength;
       int lenght = Math.min(aLength, bLength);
       for(int i = 0; i < lenght; i++)
       {
           diff |= stinga.charAt(i) ^ stringb.charAt(i);
       }
       return diff == 0;
   }
   public static boolean equals(String a, String b) {
      if (a == b) return true;
        int n = a.length();
        if (n == b.length()) {
            char v1[] = a.toCharArray();
            char v2[] = b.toCharArray();
            int i = 0;
            int j = 0;
            while (n-- != 0) {
               if (v1[i++] != v2[j++])
                   return false;
            }
            return true;
        }
      return false;
    }

}
