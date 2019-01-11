/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.obs;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.openmrs.Obs;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.obs.handler.AbstractHandler;
import org.openmrs.obs.handler.BinaryStreamHandler;
import org.openmrs.util.OpenmrsUtil;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ AbstractHandler.class, OpenmrsUtil.class, Context.class })

public class BinaryStreamHandlerTest {
	
	private final String FILENAME = "TestingComplexObsSaving";

	private final String TESTBYTES = "Teststring";
	
	private byte[] CONTENT = TESTBYTES.getBytes();
	
	private String filepath;
	
	@Mock
	private AdministrationService administrationService;
	
    @Test
    public void shouldReturnSupportedViews() {
        BinaryStreamHandler handler = new BinaryStreamHandler();
        String[] actualViews = handler.getSupportedViews();
        String[] expectedViews = { ComplexObsHandler.RAW_VIEW };

        assertArrayEquals(actualViews, expectedViews);
    }

    @Test
    public void shouldSupportRawView() {
        BinaryStreamHandler handler = new BinaryStreamHandler();

        assertTrue(handler.supportsView(ComplexObsHandler.RAW_VIEW));
    }

    @Test
    public void shouldNotSupportOtherViews() {
        BinaryStreamHandler handler = new BinaryStreamHandler();

        assertFalse(handler.supportsView(ComplexObsHandler.HTML_VIEW));
        assertFalse(handler.supportsView(ComplexObsHandler.PREVIEW_VIEW));
        assertFalse(handler.supportsView(ComplexObsHandler.TEXT_VIEW));
        assertFalse(handler.supportsView(ComplexObsHandler.TITLE_VIEW));
        assertFalse(handler.supportsView(ComplexObsHandler.URI_VIEW));
        assertFalse(handler.supportsView(""));
        assertFalse(handler.supportsView((String) null));
    }

	/** This method sets up the test data's filepath for the mime type tests  **/
	@Before
	public void initFilepathForMimetypeTests() {
		filepath = new File("target" + File.separator + "test-classes").getAbsolutePath();
	}
	
	@Test
	public void shouldRetrieveCorrectMimetype() {
		final String mimetype = "application/octet-stream";
		
		ByteArrayInputStream byteIn = new ByteArrayInputStream(CONTENT);
		
		ComplexData complexData = new ComplexData(FILENAME, byteIn);
		
		// Construct 2 Obs to also cover the case where the filename exists already
		Obs obs1 = new Obs();
		obs1.setComplexData(complexData);
		
		Obs obs2 = new Obs();
		obs2.setComplexData(complexData);
		
		// Mocked methods
		mockStatic(Context.class);
		when(Context.getAdministrationService()).thenReturn(administrationService);
		when(administrationService.getGlobalProperty(any())).thenReturn(filepath);
		
		BinaryStreamHandler handler = new BinaryStreamHandler();
		
		// Execute save
		handler.saveObs(obs1);
		handler.saveObs(obs2);
		
		// Get observation
		Obs complexObs = handler.getObs(obs1, "RAW_VIEW");
		Obs complexObs2 = handler.getObs(obs2, "RAW_VIEW");
		
		assertTrue(complexObs.getComplexData().getMimeType().equals(mimetype));
		assertTrue(complexObs2.getComplexData().getMimeType().equals(mimetype));
		
		// Delete created files to avoid cluttering
		File obsFile1 = BinaryStreamHandler.getComplexDataFile(obs1);
		File obsFile2 = BinaryStreamHandler.getComplexDataFile(obs2);
		obsFile1.delete();
		obsFile2.delete();
	}
	
}
