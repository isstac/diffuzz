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

import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class OrderGroupTest {

    @Test
    public void addOrder_shouldSetTheOrderGroupOnTheAddedOrder() {
        
        OrderGroup orderGroup = new OrderGroup();

        Order firstOrder = new Order();
        Order secondOrder = new Order();

        orderGroup.addOrder(firstOrder);
        orderGroup.addOrder(secondOrder);

        List<Order> orders = orderGroup.getOrders();

        Assert.assertNotNull("should have orderGroup in order", orders.get(0).getOrderGroup());
        Assert.assertNotNull("should have orderGroup in order", orders.get(1).getOrderGroup());
    }
}