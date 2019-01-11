/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.util;

import org.junit.Assert;
import org.junit.Test;
import org.openmrs.api.APIException;

/**
 * Tests methods on the {@link OpenmrsSecurityManager} class
 */
public class OpenmrsSecurityManagerTest {
	
	/**
	 * @see OpenmrsSecurityManager#getCallerClass(int)
	 */
	@Test
	public void getCallerClass_shouldGetTheMostRecentlyCalledMethod() {
		OpenmrsSecurityManager openmrsSecurityManager = new OpenmrsSecurityManager();
		Class<?> callerClass = openmrsSecurityManager.getCallerClass(0);
		Assert.assertTrue("Oops, didn't get a junit type of class: " + callerClass, callerClass.getPackage().getName()
		        .contains("junit"));
	}
	
	/**
	 * @see OpenmrsSecurityManager#getCallerClass(int)
	 */
	@Test(expected = APIException.class)
	public void getCallerClass_shouldThrowAnErrorIfGivenASubzeroCallStackLevel() {
		new OpenmrsSecurityManager().getCallerClass(-1);
	}
}
