/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs;

import java.util.Date;

/**
 * In OpenMRS, we distinguish between data and metadata within our data model. Metadata represent
 * system and descriptive data such as data types &mdash; a relationship type or encounter type.
 * Metadata are generally referenced by clinical data but don't represent patient-specific data
 * themselves. OpenMRS objects that represent metadata should implement this interface.
 * 
 * @see OpenmrsData
 * @see BaseChangeableOpenmrsMetadata
 * @since 1.5
 */
public interface OpenmrsMetadata extends Auditable, Retireable {
	
	/**
	 * @return the name
	 */
	public String getName();
	
	/**
	 * @param name the name to set
	 */
	public void setName(String name);
	
	/**
	 * @return the description
	 */
	public String getDescription();
	
	/**
	 * @param description the description to set
	 */
	public void setDescription(String description);
	
	/**
	 * @deprecated As of version 2.2 OpenmrsMetadata is immutable by default, it's up to the
	 *             subclasses to make themselves mutable by extending BaseChangeableOpenmrsMetadata,
	 *             this method will be removed in 2.3
	 */
	@Override
	@Deprecated
	User getChangedBy();
	
	/**
	 * @deprecated As of version 2.2 OpenmrsMetadata is immutable by default, it's up to the
	 *             subclasses to make themselves mutable by extending BaseChangeableOpenmrsMetadata,
	 *             this method will be removed in 2.3
	 */
	@Override
	@Deprecated
	void setChangedBy(User changedBy);
	
	/**
	 * @deprecated As of version 2.2 OpenmrsMetadata is immutable by default, it's up to the
	 *             subclasses to make themselves mutable by extending BaseChangeableOpenmrsMetadata,
	 *             this method will be removed in 2.3
	 */
	@Override
	@Deprecated
	Date getDateChanged();
	
	/**
	 * @deprecated As of version 2.2 OpenmrsMetadata is immutable by default, it's up to the
	 *             subclasses to make themselves mutable by extending BaseChangeableOpenmrsMetadata,
	 *             this method will be removed in 2.3
	 */
	@Override
	@Deprecated
	void setDateChanged(Date dateChanged);
}
