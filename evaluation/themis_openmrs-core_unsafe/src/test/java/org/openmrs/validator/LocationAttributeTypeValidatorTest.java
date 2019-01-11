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
import org.junit.Before;
import org.junit.Test;
import org.openmrs.LocationAttributeType;
import org.openmrs.api.context.Context;
import org.openmrs.test.BaseContextSensitiveTest;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;

/**
 * Tests methods on the {@link LocationAttributeTypeValidator} class.
 */
public class LocationAttributeTypeValidatorTest extends BaseContextSensitiveTest {
	
	protected static final String LOC_ATTRIBUTE_DATA_XML = "org/openmrs/api/include/LocationServiceTest-attributes.xml";
	
	/**
	 * Run this before each unit test in this class. This adds a bit more data to the base data that
	 * is done in the "@Before" method in {@link BaseContextSensitiveTest} (which is run right
	 * before this method).
	 *
	 * @throws Exception
	 */
	@Before
	public void runBeforeEachTest() {
		executeDataSet(LOC_ATTRIBUTE_DATA_XML);
	}
	
	/**
	 * @see LocationAttributeTypeValidator#validate(Object,Errors)
	 */
	@Test
	public void validate_shouldFailValidationIfNameIsNullOrEmptyOrWhitespace() {
		LocationAttributeType type = new LocationAttributeType();
		type.setName(null);
		type.setDescription("description");
		
		Errors errors = new BindException(type, "type");
		new LocationAttributeTypeValidator().validate(type, errors);
		Assert.assertTrue(errors.hasFieldErrors("name"));
		
		type.setName("");
		errors = new BindException(type, "type");
		new LocationAttributeTypeValidator().validate(type, errors);
		Assert.assertTrue(errors.hasFieldErrors("name"));
		
		type.setName(" ");
		errors = new BindException(type, "type");
		new LocationAttributeTypeValidator().validate(type, errors);
		Assert.assertTrue(errors.hasFieldErrors("name"));
	}
	
	/**
	 * @see LocationAttributeTypeValidator#validate(Object,Errors)
	 */
	@Test
	public void validate_shouldPassValidationIfAllRequiredFieldsHaveProperValues() {
		LocationAttributeType type = new LocationAttributeType();
		type.setName("name");
		type.setDatatypeClassname("org.openmrs.customdatatype.datatype.FreeTextDatatype");
		
		Errors errors = new BindException(type, "type");
		new LocationAttributeTypeValidator().validate(type, errors);
		
		Assert.assertFalse(errors.hasErrors());
	}
	
	/**
	 * @see LocationAttributeTypeValidator#validate(Object, Errors)
	 */
	@Test
	public void validate_shouldFailIfLocationAttributeTypeNameIsDuplicate() {
		
		Assert.assertNotNull(Context.getLocationService().getLocationAttributeTypeByName("Audit Date"));
		
		LocationAttributeType type = new LocationAttributeType();
		type.setName("Audit Date");
		type.setDatatypeClassname("org.openmrs.customdatatype.datatype.FreeTextDatatype");
		Errors errors = new BindException(type, "locationAttributeType");
		new LocationAttributeTypeValidator().validate(type, errors);
		Assert.assertTrue(errors.hasFieldErrors("name"));
		
	}
	
	/**
	 * @see LocationAttributeTypeValidator#validate(Object, Errors)
	 */
	@Test
	public void validate_shouldPassEditingLocationAttributeTypeName() {
		
		LocationAttributeType et = Context.getLocationService().getLocationAttributeTypeByName("Audit Date");
		Assert.assertNotNull(et);
		Errors errors = new BindException(et, "locationAttributeType");
		new LocationAttributeTypeValidator().validate(et, errors);
		Assert.assertFalse(errors.hasErrors());
		
	}
	
	/**
	 * @see LocationAttributeTypeValidator#validate(Object,Errors)
	 */
	@Test
	public void validate_shouldPassValidationIfFieldLengthsAreCorrect() {
		LocationAttributeType type = new LocationAttributeType();
		type.setName("name");
		type.setDatatypeClassname("org.openmrs.customdatatype.datatype.FreeTextDatatype");
		type.setDescription("description");
		type.setRetireReason("retireReason");
		
		Errors errors = new BindException(type, "type");
		new LocationAttributeTypeValidator().validate(type, errors);
		
		Assert.assertFalse(errors.hasErrors());
	}
	
	/**
	 * @see LocationAttributeTypeValidator#validate(Object,Errors)
	 */
	@Test
	public void validate_shouldFailValidationIfFieldLengthsAreNotCorrect() {
		LocationAttributeType type = new LocationAttributeType();
		type
		        .setName("too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text");
		type
		        .setDatatypeClassname("too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text");
		type
		        .setDescription("too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text");
		type
		        .setPreferredHandlerClassname("too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text");
		type
		        .setRetireReason("too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text");
		
		Errors errors = new BindException(type, "type");
		new LocationAttributeTypeValidator().validate(type, errors);
		
		Assert.assertTrue(errors.hasFieldErrors("name"));
		Assert.assertTrue(errors.hasFieldErrors("datatypeClassname"));
		Assert.assertTrue(errors.hasFieldErrors("description"));
		Assert.assertTrue(errors.hasFieldErrors("preferredHandlerClassname"));
		Assert.assertTrue(errors.hasFieldErrors("retireReason"));
	}
}
