/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.validator;

import org.junit.Assert;
import org.junit.Test;
import org.openmrs.Privilege;
import org.openmrs.test.BaseContextSensitiveTest;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;

/**
 * Tests methods on the {@link PrivilegeValidator} class.
 */
public class PrivilegeValidatorTest extends BaseContextSensitiveTest {
	
	/**
	 * @see PrivilegeValidator#validate(Object,Errors)
	 */
	@Test
	public void validate_shouldFailValidationIfPrivilegeIsNullOrEmptyOrWhitespace() {
		Privilege priv = new Privilege();
		priv.setPrivilege(null);
		priv.setDescription("some text");
		
		Errors errors = new BindException(priv, "priv");
		new PrivilegeValidator().validate(priv, errors);
		Assert.assertTrue(errors.hasFieldErrors("privilege"));
		
		priv.setPrivilege("");
		errors = new BindException(priv, "priv");
		new PrivilegeValidator().validate(priv, errors);
		Assert.assertTrue(errors.hasFieldErrors("privilege"));
		
		priv.setPrivilege(" ");
		errors = new BindException(priv, "priv");
		new PrivilegeValidator().validate(priv, errors);
		Assert.assertTrue(errors.hasFieldErrors("privilege"));
	}
	
	/**
	 * @see PrivilegeValidator#validate(Object,Errors)
	 */
	@Test
	public void validate_shouldPassValidationIfDescriptionIsNullOrEmptyOrWhitespace() {
		Privilege priv = new Privilege();
		priv.setPrivilege("Wallhacking");
		priv.setDescription(null);
		
		Errors errors = new BindException(priv, "priv");
		new PrivilegeValidator().validate(priv, errors);
		Assert.assertFalse(errors.hasFieldErrors("description"));
		
		priv.setDescription("");
		errors = new BindException(priv, "priv");
		new PrivilegeValidator().validate(priv, errors);
		Assert.assertFalse(errors.hasFieldErrors("description"));
		
		priv.setDescription(" ");
		errors = new BindException(priv, "priv");
		new PrivilegeValidator().validate(priv, errors);
		Assert.assertFalse(errors.hasFieldErrors("description"));
	}
	
	/**
	 * @see PrivilegeValidator#validate(Object,Errors)
	 */
	@Test
	public void validate_shouldPassValidationIfAllRequiredFieldsHaveProperValues() {
		Privilege priv = new Privilege();
		priv.setPrivilege("Wallhacking");
		priv.setDescription("idspispopd");
		
		Errors errors = new BindException(priv, "priv");
		new PrivilegeValidator().validate(priv, errors);
		
		Assert.assertFalse(errors.hasErrors());
		Assert.assertNotNull(priv.getName());
		Assert.assertEquals(priv.getPrivilege(), "Wallhacking");
	}
	
	/**
	 * @see PrivilegeValidator#validate(Object,Errors)
	 */
	@Test
	public void validate_shouldPassValidationIfFieldLengthsAreCorrect() {
		Privilege priv = new Privilege();
		priv.setPrivilege("Wallhacking");
		priv.setDescription("description");
		
		Errors errors = new BindException(priv, "priv");
		new PrivilegeValidator().validate(priv, errors);
		
		Assert.assertFalse(errors.hasErrors());
	}
	
	/**
	 * @see PrivilegeValidator#validate(Object,Errors)
	 */
	@Test
	public void validate_shouldFailValidationIfFieldLengthsAreNotCorrect() {
		Privilege priv = new Privilege();
		priv
		        .setPrivilege("too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text");
		priv
		        .setDescription("too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text");
		
		Errors errors = new BindException(priv, "priv");
		new PrivilegeValidator().validate(priv, errors);
		
		Assert.assertTrue(errors.hasFieldErrors("privilege"));
		Assert.assertTrue(errors.hasFieldErrors("description"));
	}
}
