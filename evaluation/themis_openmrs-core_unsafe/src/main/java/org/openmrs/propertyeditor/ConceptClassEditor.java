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

import org.openmrs.ConceptClass;
import org.openmrs.api.context.Context;

/**
 * Allows for serializing/deserializing an object to a string so that Spring knows how to pass an
 * object back and forth through an html form or other medium. <br>
 * <br>
 * In version 1.9, added ability for this to also retrieve objects by uuid
 * 
 * @see ConceptClass
 */
public class ConceptClassEditor extends OpenmrsPropertyEditor<ConceptClass> {
	
	public ConceptClassEditor() {
	}
	
	@Override
	protected ConceptClass getObjectById(Integer id) {
		return Context.getConceptService().getConceptClass(id);
	}
	
	@Override
	protected ConceptClass getObjectByUuid(String uuid) {
		return Context.getConceptService().getConceptClassByUuid(uuid);
	}
}
