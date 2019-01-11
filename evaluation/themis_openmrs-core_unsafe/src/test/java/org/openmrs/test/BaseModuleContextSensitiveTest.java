/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.test;

import org.springframework.test.context.ContextConfiguration;

/**
 * Modules using the unit test framework should use this class instead of the general
 * {@link BaseContextSensitiveTest} one. Developers just need to make sure their modules are on the
 * classpath. The TestingApplicationContext.xml file tells spring/hibernate to look for and load all
 * modules found on the classpath. The ContextConfiguration annotation adds in the module
 * application context files to the config locations and the test application context (so that the
 * module services are loaded from the system classloader)
 */
@ContextConfiguration(locations = { "classpath:applicationContext-service.xml", "classpath*:TestingApplicationContext.xml",
        "classpath*:moduleApplicationContext.xml" }, inheritLocations = false)
public abstract class BaseModuleContextSensitiveTest extends BaseContextSensitiveTest {

}
