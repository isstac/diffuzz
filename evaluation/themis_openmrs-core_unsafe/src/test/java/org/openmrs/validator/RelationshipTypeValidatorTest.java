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
import org.openmrs.RelationshipType;
import org.openmrs.test.BaseContextSensitiveTest;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;

/**
 *Tests methods on the {@link RelationshipTypeValidator} class.
 *
 * @since 1.10
 */
public class RelationshipTypeValidatorTest extends BaseContextSensitiveTest {
	
	/**
	 * @see RelationshipTypeValidator#validate(Object,Errors)
	 */
	@Test
	public void validate_shouldFailValidationIfaIsToBIsNullOrEmptyOrWhitespace() {
		RelationshipType type = new RelationshipType();
		
		Errors errors = new BindException(type, "type");
		new RelationshipTypeValidator().validate(type, errors);
		Assert.assertTrue(errors.hasFieldErrors("aIsToB"));
		
		type.setaIsToB("");
		errors = new BindException(type, "type");
		new RelationshipTypeValidator().validate(type, errors);
		Assert.assertTrue(errors.hasFieldErrors("aIsToB"));
		
		type.setaIsToB(" ");
		errors = new BindException(type, "type");
		new RelationshipTypeValidator().validate(type, errors);
		Assert.assertTrue(errors.hasFieldErrors("aIsToB"));
	}
	
	/**
	 * @see RelationshipTypeValidator#validate(Object,Errors)
	 */
	@Test
	public void validate_shouldFailValidationIfbIsToAIsNullOrEmptyOrWhitespace() {
		RelationshipType type = new RelationshipType();
		
		Errors errors = new BindException(type, "type");
		new RelationshipTypeValidator().validate(type, errors);
		Assert.assertTrue(errors.hasFieldErrors("bIsToA"));
		
		type.setbIsToA("");
		errors = new BindException(type, "type");
		new RelationshipTypeValidator().validate(type, errors);
		Assert.assertTrue(errors.hasFieldErrors("bIsToA"));
		
		type.setbIsToA(" ");
		errors = new BindException(type, "type");
		new RelationshipTypeValidator().validate(type, errors);
		Assert.assertTrue(errors.hasFieldErrors("bIsToA"));
	}
	
	/**
	 * @see RelationshipTypeValidator#validate(Object,Errors)
	 */
	@Test
	public void validate_shouldFailValidationIfDescriptionIsNullOrEmptyOrWhitespace() {
		RelationshipType type = new RelationshipType();
		
		Errors errors = new BindException(type, "type");
		new RelationshipTypeValidator().validate(type, errors);
		Assert.assertTrue(errors.hasFieldErrors("description"));
		
		type.setDescription("");
		errors = new BindException(type, "type");
		new RelationshipTypeValidator().validate(type, errors);
		Assert.assertTrue(errors.hasFieldErrors("description"));
		
		type.setDescription(" ");
		errors = new BindException(type, "type");
		new RelationshipTypeValidator().validate(type, errors);
		Assert.assertTrue(errors.hasFieldErrors("description"));
	}
	
	/**
	 * Test for all the field being set to some values
	 * @see RelationshipTypeValidator#validate(Object,Errors)
	 */
	@Test
	public void validate_shouldPassValidationIfAllRequiredFieldsAreSet() {
		RelationshipType type = new RelationshipType();
		type.setaIsToB("A is To B");
		type.setbIsToA("B is To A");
		type.setDescription("Description");
		Errors errors = new BindException(type, "type");
		new RelationshipTypeValidator().validate(type, errors);
		Assert.assertFalse(errors.hasErrors());
	}
	
	/**
	 * @see org.openmrs.validator.RelationshipTypeValidator#validate(Object, Errors)
	 */
	@Test
	public void validate_shouldPassEditingEncounterTypeName() {
		RelationshipType type = new RelationshipType();
		type.setaIsToB("Doctor");
		type.setbIsToA("Patient");
		
		Errors errors = new BindException(type, "type");
		new RelationshipTypeValidator().validate(type, errors);
		Assert.assertTrue(errors.hasErrors());
	}
	
	/**
	 * Test for all the field being set to some values
	 * @see RelationshipTypeValidator#validate(Object,Errors)
	 */
	@Test
	public void validate_shouldPassValidationIfFieldLengthsAreCorrect() {
		RelationshipType type = new RelationshipType();
		type.setaIsToB("A is To B");
		type.setbIsToA("B is To A");
		type.setDescription("description");
		type.setRetireReason("retireReason");
		Errors errors = new BindException(type, "type");
		new RelationshipTypeValidator().validate(type, errors);
		Assert.assertFalse(errors.hasErrors());
	}
	
	/**
	 * Test for all the field being set to some values
	 * @see RelationshipTypeValidator#validate(Object,Errors)
	 */
	@Test
	public void validate_shouldFailValidationIfFieldLengthsAreNotCorrect() {
		RelationshipType type = new RelationshipType();
		type
		        .setaIsToB("too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text");
		type
		        .setbIsToA("too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text");
		type
		        .setDescription("too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text");
		type
		        .setRetireReason("too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text");
		Errors errors = new BindException(type, "type");
		new RelationshipTypeValidator().validate(type, errors);
		Assert.assertTrue(errors.hasFieldErrors("aIsToB"));
		Assert.assertTrue(errors.hasFieldErrors("bIsToA"));
		Assert.assertTrue(errors.hasFieldErrors("description"));
		Assert.assertTrue(errors.hasFieldErrors("retireReason"));
	}
}
