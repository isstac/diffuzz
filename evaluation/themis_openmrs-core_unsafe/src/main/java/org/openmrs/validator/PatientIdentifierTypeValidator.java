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

import org.openmrs.PatientIdentifierType;
import org.openmrs.annotation.Handler;
import org.openmrs.api.context.Context;
import org.openmrs.util.OpenmrsUtil;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

/**
 * Validates attributes on the {@link PatientIdentifierType} object.
 * 
 * @since 1.5
 */
@Handler(supports = { PatientIdentifierType.class }, order = 50)
public class PatientIdentifierTypeValidator implements Validator {
	
	/**
	 * Determines if the command object being submitted is a valid type
	 * 
	 * @see org.springframework.validation.Validator#supports(java.lang.Class)
	 */
	@Override
	public boolean supports(Class<?> c) {
		return c.equals(PatientIdentifierType.class);
	}
	
	/**
	 * Checks the form object for any inconsistencies/errors
	 * 
	 * @see org.springframework.validation.Validator#validate(java.lang.Object,
	 *      org.springframework.validation.Errors)
	 * @should fail validation if name is null or empty or whitespace
	 * @should pass validation if description is null or empty or whitespace
	 * @should pass validation if all required fields have proper values
	 * @should pass validation if regEx field length is not too long
	 * @should fail validation if regEx field length is too long
	 * @should fail validation if name field length is too long
	 * @should fail validation if name is already exist in non retired identifier types
	 * @should pass validation if field lengths are correct
	 * @should fail validation if field lengths are not correct
	 */
	@Override
	public void validate(Object obj, Errors errors) {
		PatientIdentifierType identifierType = (PatientIdentifierType) obj;
		if (identifierType == null) {
			errors.rejectValue("identifierType", "error.general");
		} else {
			ValidationUtils.rejectIfEmptyOrWhitespace(errors, "name", "error.name");
			ValidateUtil.validateFieldLengths(errors, identifierType.getClass(), "name", "format", "formatDescription",
			    "validator", "retireReason");
			PatientIdentifierType exist = Context.getPatientService().getPatientIdentifierTypeByName(
			    identifierType.getName());
			if (exist != null && !exist.getRetired()
			        && !OpenmrsUtil.nullSafeEquals(identifierType.getUuid(), exist.getUuid())) {
				errors.rejectValue("name", "identifierType.duplicate.name");
			}
		}
	}
}
