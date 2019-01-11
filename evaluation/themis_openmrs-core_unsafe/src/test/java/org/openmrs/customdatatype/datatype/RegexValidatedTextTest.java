/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.customdatatype.datatype;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.customdatatype.InvalidCustomValueException;

public class RegexValidatedTextTest {
	
	RegexValidatedTextDatatype datatype;
	
	@Before
	public void before() {
		datatype = new RegexValidatedTextDatatype();
		datatype.setConfiguration("[a-z]+");
	}
	
	/**
	 * @see RegexValidatedTextDatatype#validate(String)
	 */
	@Test
	public void validate_shouldAcceptAStringThatMatchesTheRegex() {
		datatype.validate("thisisgood");
	}
	
	/**
	 * @see RegexValidatedTextDatatype#validate(String)
	 */
	@Test(expected = InvalidCustomValueException.class)
	public void validate_shouldFailIfTheStringDoesNotMatchTheRegex() {
		datatype.validate("spaces not allowed");
	}
	
	/**
	 * @see RegexValidatedTextDatatype#save(String, String))
	 */
	@Test(expected = InvalidCustomValueException.class)
	public void toPersistentString_shouldFailIfTheStringDoesNotMatchTheRegex() {
		datatype.save("spaces not allowed", null);
	}
}
