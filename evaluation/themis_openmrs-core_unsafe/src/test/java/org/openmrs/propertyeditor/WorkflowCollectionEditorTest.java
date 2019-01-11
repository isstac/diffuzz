/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.propertyeditor;

import org.junit.Assert;
import org.junit.Test;
import org.openmrs.Program;
import org.openmrs.api.context.Context;
import org.openmrs.test.BaseContextSensitiveTest;

/**
 * Tests {@link WorkflowCollectionEditor}
 */
public class WorkflowCollectionEditorTest extends BaseContextSensitiveTest {
	
	/**
	 * @see WorkflowCollectionEditor#setAsText(String)
	 */
	@Test
	public void setAsText_shouldUpdateWorkflowsInProgram() {
		Program program = Context.getProgramWorkflowService().getProgram(1);
		WorkflowCollectionEditor editor = new WorkflowCollectionEditor();
		
		Assert.assertEquals(2, program.getWorkflows().size());
		
		editor.setAsText("1:3");
		
		Assert.assertEquals(1, program.getWorkflows().size());
		Assert.assertEquals(3, program.getWorkflows().iterator().next().getConcept().getConceptId().intValue());
		Assert.assertEquals(3, program.getAllWorkflows().size());
	}
	
}
