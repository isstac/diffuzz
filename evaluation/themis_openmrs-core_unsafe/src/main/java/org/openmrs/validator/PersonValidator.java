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

import java.util.Calendar;
import java.util.Date;

import org.openmrs.Person;
import org.openmrs.PersonAddress;
import org.openmrs.PersonName;
import org.openmrs.annotation.Handler;
import org.openmrs.util.OpenmrsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

/**
 * This class validates a Person object.
 *
 * @since 1.9
 */
@Handler(supports = { Person.class }, order = 50)
public class PersonValidator implements Validator {
	
	private static final Logger log = LoggerFactory.getLogger(PersonValidator.class);
	
	@Autowired
	private PersonNameValidator personNameValidator;
	
	@Autowired
	private PersonAddressValidator personAddressValidator;
	
	@Override
	public boolean supports(Class<?> clazz) {
		return Person.class.isAssignableFrom(clazz);
	}
	
	/**
	 * @see org.springframework.validation.Validator#validate(java.lang.Object,
	 *      org.springframework.validation.Errors)
	 * @should fail validation if birthdate makes patient older that 120 years old
	 * @should fail validation if birthdate is a future date
	 * @should fail validation if deathdate is a future date
	 * @should fail validation if birthdate is after death date
	 * @should fail validation if voidReason is blank when patient is voided
	 * @should fail validation if causeOfDeath is blank when patient is dead
	 * @should pass validation if gender is blank for Persons
	 * @should pass validation if field lengths are correct
	 * @should fail validation if field lengths are not correct
	 */
	@Override
	public void validate(Object target, Errors errors) {
		log.debug("{}.validate...", this.getClass().getName());
		
		if (target == null) {
			return;
		}
		
		Person person = (Person) target;
		
		int index = 0;
		boolean atLeastOneNonVoidPersonNameLeft = false;
		for (PersonName personName : person.getNames()) {
			errors.pushNestedPath("names[" + index + "]");
			personNameValidator.validate(personName, errors);
			if (!personName.getVoided()) {
				atLeastOneNonVoidPersonNameLeft = true;
			}
			errors.popNestedPath();
			index++;
		}
		if (!person.getVoided() && !atLeastOneNonVoidPersonNameLeft) {
			errors.rejectValue("names", "Person.shouldHaveAtleastOneNonVoidedName");
		}
		
		// validate the personAddress
		index = 0;
		for (PersonAddress address : person.getAddresses()) {
			try {
				errors.pushNestedPath("addresses[" + index + "]");
				ValidationUtils.invokeValidator(personAddressValidator, address, errors);
			}
			finally {
				errors.popNestedPath();
				index++;
			}
		}
		
		validateBirthDate(errors, person.getBirthdate());
		validateDeathDate(errors, person.getDeathDate(), person.getBirthdate());
		
		if (person.getVoided()) {
			ValidationUtils.rejectIfEmptyOrWhitespace(errors, "voidReason", "error.null");
		}
		if (person.getDead()) {
			ValidationUtils.rejectIfEmpty(errors, "causeOfDeath", "Person.dead.causeOfDeathNull");
		}
		
		ValidateUtil.validateFieldLengths(errors, Person.class, "gender", "personVoidReason");
	}
	
	/**
	 * Checks if the birth date specified is in the future or older than 120 years old..
	 *
	 * @param birthDate The birthdate to validate.
	 * @param errors Stores information about errors encountered during validation.
	 */
	private void validateBirthDate(Errors errors, Date birthDate) {
		if (birthDate == null) {
			return;
		}
		rejectIfFutureDate(errors, birthDate, "birthdate");
		rejectDateIfBefore120YearsAgo(errors, birthDate, "birthdate");
	}
	
	/**
	 * Checks if the death date is in the future.
	 * 
	 * @param errors Stores information about errors encountered during validation
	 * @param deathDate the deathdate to validate
	 */
	private void validateDeathDate(Errors errors, Date deathDate, Date birthDate) {
		if (deathDate == null) {
			return;
		}
		rejectIfFutureDate(errors, deathDate, "deathDate");
		if (birthDate == null) {
			return;
		}
		rejectDeathDateIfBeforeBirthDate(errors, deathDate, birthDate);
	}
	
	/**
	 * Rejects a date if it is in the future.
	 * 
	 * @param errors the error object
	 * @param date the date to check
	 * @param dateField the name of the field
	 */
	private void rejectIfFutureDate(Errors errors, Date date, String dateField) {
		if (OpenmrsUtil.compare(date, new Date()) > 0) {
			errors.rejectValue(dateField, "error.date.future");
		}
	}
	
	/**
	 * Rejects a date if it is before 120 years ago.
	 * 
	 * @param errors the error object
	 * @param date the date to check
	 * @param dateField the name of the field
	 */
	private void rejectDateIfBefore120YearsAgo(Errors errors, Date date, String dateField) {
		Calendar c = Calendar.getInstance();
		c.setTime(new Date());
		c.add(Calendar.YEAR, -120);
		if (OpenmrsUtil.compare(date, c.getTime()) < 0) {
			errors.rejectValue(dateField, "error.date.nonsensical");
		}
	}
	
	/**
	 * Rejects a death date if it is before birth date
	 * 
	 * @param errors the error object
	 * @param deathdate the date to check
	 * @param birthdate to check with
	 */
	private void rejectDeathDateIfBeforeBirthDate(Errors errors, Date deathdate, Date birthdate) {
		if (OpenmrsUtil.compare(deathdate, birthdate) < 0) {
			errors.rejectValue("deathDate", "error.deathdate.before.birthdate");
		}
	}
	
}
