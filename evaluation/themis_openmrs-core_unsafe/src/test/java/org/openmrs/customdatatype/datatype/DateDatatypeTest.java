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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class DateDatatypeTest {
	
	DateDatatype datatype;
	
	@Before
	public void before() {
		datatype = new DateDatatype();
	}
	
	/**
	 * @throws ParseException
	 * @see Date#deserialize(String)
	 */
	@Test
	public void deserialize_shouldReconstructADateSerializedByThisHandler() throws ParseException {
		Date date = new SimpleDateFormat("yyyy-MM-dd").parse("2011-04-25");
		Assert.assertEquals(date, datatype.deserialize(datatype.serialize(date)));
	}
	
	/**
	 * @throws ParseException
	 * @see Date#serialize(java.util.Date)
	 */
	@Test
	public void serialize_shouldConvertADateIntoAYmdStringRepresentation() throws ParseException {
		Date date = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse("2011-04-25 01:02:03");
		Assert.assertEquals("2011-04-25", datatype.serialize(date));
	}
}
