/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.api;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.openmrs.test.OpenmrsMatchers.hasId;
import static org.openmrs.test.TestUtil.containsId;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.openmrs.CareSetting;
import org.openmrs.Concept;
import org.openmrs.ConceptClass;
import org.openmrs.ConceptDatatype;
import org.openmrs.ConceptDescription;
import org.openmrs.ConceptName;
import org.openmrs.Drug;
import org.openmrs.DrugOrder;
import org.openmrs.Encounter;
import org.openmrs.FreeTextDosingInstructions;
import org.openmrs.GlobalProperty;
import org.openmrs.Obs;
import org.openmrs.Order;
import org.openmrs.Order.Action;
import org.openmrs.OrderFrequency;
import org.openmrs.OrderGroup;
import org.openmrs.OrderSet;
import org.openmrs.OrderType;
import org.openmrs.Patient;
import org.openmrs.Provider;
import org.openmrs.SimpleDosingInstructions;
import org.openmrs.TestOrder;
import org.openmrs.api.builder.OrderBuilder;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.OrderServiceImpl;
import org.openmrs.messagesource.MessageSourceService;
import org.openmrs.order.OrderUtil;
import org.openmrs.order.OrderUtilTest;
import org.openmrs.orders.TimestampOrderNumberGenerator;
import org.openmrs.test.BaseContextSensitiveTest;
import org.openmrs.test.TestUtil;
import org.openmrs.util.DateUtil;
import org.openmrs.util.OpenmrsConstants;
import org.openmrs.util.PrivilegeConstants;

/**
 * TODO clean up and test all methods in OrderService
 */
public class OrderServiceTest extends BaseContextSensitiveTest {
	
	private static final String OTHER_ORDER_FREQUENCIES_XML = "org/openmrs/api/include/OrderServiceTest-otherOrderFrequencies.xml";
	
	protected static final String ORDER_SET = "org/openmrs/api/include/OrderSetServiceTest-general.xml";
	
	private ConceptService conceptService;
	
	private OrderService orderService;
	
	private PatientService patientService;
	
	private EncounterService encounterService;
	
	private ProviderService providerService;
	
	private AdministrationService adminService;
	
	private OrderSetService orderSetService;
	
	private MessageSourceService mss;
	
	@Rule
	public ExpectedException expectedException = ExpectedException.none();
	
	private class SomeTestOrder extends TestOrder {}
	
	@Before
	public void setup() {
		if (orderService == null) {
			orderService = Context.getOrderService();
		}
		if (patientService == null) {
			patientService = Context.getPatientService();
		}
		
		if (conceptService == null) {
			conceptService = Context.getConceptService();
		}
		
		if (encounterService == null) {
			encounterService = Context.getEncounterService();
		}
		if (providerService == null) {
			providerService = Context.getProviderService();
		}
		if (adminService == null) {
			adminService = Context.getAdministrationService();
		}
		if (orderSetService == null) {
			orderSetService = Context.getOrderSetService();
		}
		if (mss == null) {
			mss = Context.getMessageSourceService();
		}
	}
	
	/**
	 * @see OrderService#saveOrder(org.openmrs.Order, OrderContext)
	 */
	@Test
	public void saveOrder_shouldNotSaveOrderIfOrderDoesntValidate() {
		Order order = new Order();
		order.setPatient(null);
		order.setOrderer(null);
		expectedException.expect(APIException.class);
		expectedException.expectMessage("failed to validate with reason:");
		orderService.saveOrder(order, null);
	}
	
	/**
	 * @see OrderService#getOrderByUuid(String)
	 */
	@Test
	public void getOrderByUuid_shouldFindObjectGivenValidUuid() {
		String uuid = "921de0a3-05c4-444a-be03-e01b4c4b9142";
		Order order = orderService.getOrderByUuid(uuid);
		Assert.assertEquals(1, (int) order.getOrderId());
	}
	
	/**
	 * @see OrderService#getOrderByUuid(String)
	 */
	@Test
	public void getOrderByUuid_shouldReturnNullIfNoObjectFoundWithGivenUuid() {
		Assert.assertNull(orderService.getOrderByUuid("some invalid uuid"));
	}
	
	/**
	 * @see OrderService#purgeOrder(org.openmrs.Order, boolean)
	 */
	@Test
	public void purgeOrder_shouldDeleteAnyObsAssociatedToTheOrderWhenCascadeIsTrue() {
		executeDataSet("org/openmrs/api/include/OrderServiceTest-deleteObsThatReference.xml");
		final String ordUuid = "0c96f25c-4949-4f72-9931-d808fbcdb612";
		final String obsUuid = "be3a4d7a-f9ab-47bb-aaad-bc0b452fcda4";
		ObsService os = Context.getObsService();
		
		Obs obs = os.getObsByUuid(obsUuid);
		Assert.assertNotNull(obs);
		
		Order order = orderService.getOrderByUuid(ordUuid);
		Assert.assertNotNull(order);
		
		//sanity check to ensure that the obs and order are actually related
		Assert.assertEquals(order, obs.getOrder());
		
		//Ensure that passing false does not delete the related obs
		orderService.purgeOrder(order, false);
		Assert.assertNotNull(os.getObsByUuid(obsUuid));
		
		orderService.purgeOrder(order, true);
		
		//Ensure that actually the order got purged
		Assert.assertNull(orderService.getOrderByUuid(ordUuid));
		
		//Ensure that the related obs got deleted
		Assert.assertNull(os.getObsByUuid(obsUuid));
	}
	
	/**
	 * @see OrderService#purgeOrder(org.openmrs.Order, boolean)
	 */
	@Test
	public void purgeOrder_shouldDeleteOrderFromTheDatabase() {
		final String uuid = "9c21e407-697b-11e3-bd76-0800271c1b75";
		Order order = orderService.getOrderByUuid(uuid);
		assertNotNull(order);
		orderService.purgeOrder(order);
		assertNull(orderService.getOrderByUuid(uuid));
	}
	
	/**
	 * @throws InterruptedException
	 * @see OrderNumberGenerator#getNewOrderNumber(OrderContext)
	 */
	@Test
	public void getNewOrderNumber_shouldAlwaysReturnUniqueOrderNumbersWhenCalledMultipleTimesWithoutSavingOrders()
	        throws InterruptedException {
		
		int N = 50;
		final Set<String> uniqueOrderNumbers = new HashSet<>(50);
		List<Thread> threads = new ArrayList<>();
		for (int i = 0; i < N; i++) {
			threads.add(new Thread(() -> {
				try {
					Context.openSession();
					Context.addProxyPrivilege(PrivilegeConstants.ADD_ORDERS);
					uniqueOrderNumbers.add(((OrderNumberGenerator) orderService).getNewOrderNumber(null));
				}
				finally {
					Context.removeProxyPrivilege(PrivilegeConstants.ADD_ORDERS);
					Context.closeSession();
				}
			}));
		}
		for (int i = 0; i < N; ++i) {
			threads.get(i).start();
		}
		for (int i = 0; i < N; ++i) {
			threads.get(i).join();
		}
		//since we used a set we should have the size as N indicating that there were no duplicates
		Assert.assertEquals(N, uniqueOrderNumbers.size());
	}
	
	/**
	 * @see OrderService#getOrderByOrderNumber(String)
	 */
	@Test
	public void getOrderByOrderNumber_shouldFindObjectGivenValidOrderNumber() {
		Order order = orderService.getOrderByOrderNumber("1");
		Assert.assertNotNull(order);
		Assert.assertEquals(1, (int) order.getOrderId());
	}
	
	/**
	 * @see OrderService#getOrderByOrderNumber(String)
	 */
	@Test
	public void getOrderByOrderNumber_shouldReturnNullIfNoObjectFoundWithGivenOrderNumber() {
		Assert.assertNull(orderService.getOrderByOrderNumber("some invalid order number"));
	}
	
	/**
	 * @see OrderService#getOrderHistoryByConcept(Patient,Concept)
	 */
	@Test
	public void getOrderHistoryByConcept_shouldReturnOrdersWithTheGivenConcept() {
		//We should have two orders with this concept.
		Concept concept = Context.getConceptService().getConcept(88);
		Patient patient = Context.getPatientService().getPatient(2);
		List<Order> orders = orderService.getOrderHistoryByConcept(patient, concept);
		
		//They must be sorted by dateActivated starting with the latest
		Assert.assertEquals(3, orders.size());
		Assert.assertEquals(444, orders.get(0).getOrderId().intValue());
		Assert.assertEquals(44, orders.get(1).getOrderId().intValue());
		Assert.assertEquals(4, orders.get(2).getOrderId().intValue());
		
		concept = Context.getConceptService().getConcept(792);
		orders = orderService.getOrderHistoryByConcept(patient, concept);
		
		//They must be sorted by dateActivated starting with the latest
		Assert.assertEquals(4, orders.size());
		Assert.assertEquals(3, orders.get(0).getOrderId().intValue());
		Assert.assertEquals(222, orders.get(1).getOrderId().intValue());
		Assert.assertEquals(22, orders.get(2).getOrderId().intValue());
		Assert.assertEquals(2, orders.get(3).getOrderId().intValue());
	}
	
	/**
	 * @see OrderService#getOrderHistoryByConcept(Patient, Concept)
	 */
	@Test
	public void getOrderHistoryByConcept_shouldReturnEmptyListForConceptWithoutOrders() {
		Concept concept = Context.getConceptService().getConcept(21);
		Patient patient = Context.getPatientService().getPatient(2);
		List<Order> orders = orderService.getOrderHistoryByConcept(patient, concept);
		Assert.assertEquals(0, orders.size());
	}
	
	/**
	 * @see OrderService#getOrderHistoryByConcept(org.openmrs.Patient, org.openmrs.Concept)
	 */
	@Test
	public void getOrderHistoryByConcept_shouldRejectANullConcept() {
		expectedException.expect(IllegalArgumentException.class);
		expectedException.expectMessage("patient and concept are required");
		orderService.getOrderHistoryByConcept(new Patient(), null);
	}
	
	/**
	 * @see OrderService#getOrderHistoryByConcept(org.openmrs.Patient, org.openmrs.Concept)
	 */
	@Test
	public void getOrderHistoryByConcept_shouldRejectANullPatient() {
		expectedException.expect(IllegalArgumentException.class);
		expectedException.expectMessage("patient and concept are required");
		orderService.getOrderHistoryByConcept(null, new Concept());
	}
	
	/**
	 * @see OrderService#getOrderHistoryByOrderNumber(String)
	 */
	@Test
	public void getOrderHistoryByOrderNumber_shouldReturnAllOrderHistoryForGivenOrderNumber() {
		List<Order> orders = orderService.getOrderHistoryByOrderNumber("111");
		assertEquals(2, orders.size());
		assertEquals(111, orders.get(0).getOrderId().intValue());
		assertEquals(1, orders.get(1).getOrderId().intValue());
	}
	
	/**
	 * @see OrderService#getOrderFrequency(Integer)
	 */
	@Test
	public void getOrderFrequency_shouldReturnTheOrderFrequencyThatMatchesTheSpecifiedId() {
		assertEquals("28090760-7c38-11e3-baa7-0800200c9a66", orderService.getOrderFrequency(1).getUuid());
	}
	
	/**
	 * @see OrderService#getOrderFrequencyByUuid(String)
	 */
	@Test
	public void getOrderFrequencyByUuid_shouldReturnTheOrderFrequencyThatMatchesTheSpecifiedUuid() {
		assertEquals(1, orderService.getOrderFrequencyByUuid("28090760-7c38-11e3-baa7-0800200c9a66").getOrderFrequencyId()
		        .intValue());
	}
	
	/**
	 * @see OrderService#getOrderFrequencyByConcept(org.openmrs.Concept)
	 */
	@Test
	public void getOrderFrequencyByConcept_shouldReturnTheOrderFrequencyThatMatchesTheSpecifiedConcept() {
		Concept concept = conceptService.getConcept(4);
		assertEquals(3, orderService.getOrderFrequencyByConcept(concept).getOrderFrequencyId().intValue());
	}
	
	/**
	 * @see OrderService#getOrderFrequencies(boolean)
	 */
	@Test
	public void getOrderFrequencies_shouldReturnOnlyNonRetiredOrderFrequenciesIfIncludeRetiredIsSetToFalse()
	{
		List<OrderFrequency> orderFrequencies = orderService.getOrderFrequencies(false);
		assertEquals(2, orderFrequencies.size());
		assertTrue(containsId(orderFrequencies, 1));
		assertTrue(containsId(orderFrequencies, 2));
	}
	
	/**
	 * @see OrderService#getOrderFrequencies(boolean)
	 */
	@Test
	public void getOrderFrequencies_shouldReturnAllTheOrderFrequenciesIfIncludeRetiredIsSetToTrue() {
		List<OrderFrequency> orderFrequencies = orderService.getOrderFrequencies(true);
		assertEquals(3, orderFrequencies.size());
		assertTrue(containsId(orderFrequencies, 1));
		assertTrue(containsId(orderFrequencies, 2));
		assertTrue(containsId(orderFrequencies, 3));
	}
	
	/**
	 * @see OrderService#getActiveOrders(org.openmrs.Patient, org.openmrs.OrderType,
	 *      org.openmrs.CareSetting, java.util.Date)
	 */
	@Test
	public void getActiveOrders_shouldReturnAllActiveOrdersForTheSpecifiedPatient() {
		Patient patient = Context.getPatientService().getPatient(2);
		List<Order> orders = orderService.getActiveOrders(patient, null, null, null);
		assertEquals(5, orders.size());
		Order[] expectedOrders = { orderService.getOrder(222), orderService.getOrder(3), orderService.getOrder(444),
		        orderService.getOrder(5), orderService.getOrder(7) };
		assertThat(orders, hasItems(expectedOrders));
		
		assertTrue(OrderUtilTest.isActiveOrder(orders.get(0), null));
		assertTrue(OrderUtilTest.isActiveOrder(orders.get(1), null));
		assertTrue(OrderUtilTest.isActiveOrder(orders.get(2), null));
		assertTrue(OrderUtilTest.isActiveOrder(orders.get(3), null));
		assertTrue(OrderUtilTest.isActiveOrder(orders.get(4), null));
	}
	
	/**
	 * @see OrderService#getActiveOrders(org.openmrs.Patient, org.openmrs.OrderType,
	 *      org.openmrs.CareSetting, java.util.Date)
	 */
	@Test
	public void getActiveOrders_shouldReturnAllActiveOrdersForTheSpecifiedPatientAndCareSetting() {
		Patient patient = patientService.getPatient(2);
		CareSetting careSetting = orderService.getCareSetting(1);
		List<Order> orders = orderService.getActiveOrders(patient, null, careSetting, null);
		assertEquals(4, orders.size());
		Order[] expectedOrders = { orderService.getOrder(3), orderService.getOrder(444), orderService.getOrder(5),
		        orderService.getOrder(7) };
		assertThat(orders, hasItems(expectedOrders));
	}
	
	/**
	 * @see OrderService#getActiveOrders(org.openmrs.Patient, org.openmrs.OrderType,
	 *      org.openmrs.CareSetting, java.util.Date)
	 */
	@Test
	public void getActiveOrders_shouldReturnAllActiveDrugOrdersForTheSpecifiedPatient() {
		Patient patient = patientService.getPatient(2);
		List<Order> orders = orderService.getActiveOrders(patient, orderService.getOrderType(1), null, null);
		assertEquals(4, orders.size());
		Order[] expectedOrders = { orderService.getOrder(222), orderService.getOrder(3), orderService.getOrder(444),
		        orderService.getOrder(5) };
		assertThat(orders, hasItems(expectedOrders));
	}
	
	/**
	 * @see OrderService#getActiveOrders(org.openmrs.Patient, org.openmrs.OrderType,
	 *      org.openmrs.CareSetting, java.util.Date)
	 */
	@Test
	public void getActiveOrders_shouldReturnAllActiveTestOrdersForTheSpecifiedPatient() {
		Patient patient = patientService.getPatient(2);
		List<Order> orders = orderService
		        .getActiveOrders(patient, orderService.getOrderTypeByName("Test order"), null, null);
		assertEquals(1, orders.size());
		assertEquals(orders.get(0), orderService.getOrder(7));
	}
	
	/**
	 * @see OrderService#getActiveOrders(org.openmrs.Patient, org.openmrs.OrderType,
	 *      org.openmrs.CareSetting, java.util.Date)
	 */
	@Test
	public void getActiveOrders_shouldFailIfPatientIsNull() {
		expectedException.expect(IllegalArgumentException.class);
		expectedException.expectMessage("Patient is required when fetching active orders");
		orderService.getActiveOrders(null, null, orderService.getCareSetting(1), null);
	}
	
	/**
	 * @throws ParseException
	 * @see OrderService#getActiveOrders(org.openmrs.Patient, org.openmrs.OrderType,
	 *      org.openmrs.CareSetting, java.util.Date)
	 */
	@Test
	public void getActiveOrders_shouldReturnActiveOrdersAsOfTheSpecifiedDate() throws ParseException {
		Patient patient = Context.getPatientService().getPatient(2);
		List<Order> orders = orderService.getAllOrdersByPatient(patient);
		assertEquals(12, orders.size());
		
		Date asOfDate = Context.getDateFormat().parse("10/12/2007");
		orders = orderService.getActiveOrders(patient, null, null, asOfDate);
		assertEquals(2, orders.size());
		assertFalse(orders.contains(orderService.getOrder(22)));//DC
		assertFalse(orders.contains(orderService.getOrder(44)));//DC
		assertFalse(orders.contains(orderService.getOrder(8)));//voided
		
		Order[] expectedOrders = { orderService.getOrder(9) };
		
		asOfDate = Context.getDateTimeFormat().parse("10/12/2007 00:01:00");
		orders = orderService.getActiveOrders(patient, null, null, asOfDate);
		assertEquals(1, orders.size());
		assertThat(orders, hasItems(expectedOrders));
		
		Order[] expectedOrders1 = { orderService.getOrder(3), orderService.getOrder(4), orderService.getOrder(222) };
		
		asOfDate = Context.getDateFormat().parse("10/04/2008");
		orders = orderService.getActiveOrders(patient, null, null, asOfDate);
		assertEquals(3, orders.size());
		assertThat(orders, hasItems(expectedOrders1));
		
		asOfDate = Context.getDateTimeFormat().parse("10/04/2008 00:01:00");
		orders = orderService.getActiveOrders(patient, null, null, asOfDate);
		assertEquals(2, orders.size());
		Order[] expectedOrders2 = { orderService.getOrder(222), orderService.getOrder(3) };
		assertThat(orders, hasItems(expectedOrders2));
		
		Order[] expectedOrders3 = { orderService.getOrder(222), orderService.getOrder(3), orderService.getOrder(444),
		        orderService.getOrder(5), orderService.getOrder(6) };
		asOfDate = Context.getDateTimeFormat().parse("26/09/2008 09:24:10");
		orders = orderService.getActiveOrders(patient, null, null, asOfDate);
		assertEquals(5, orders.size());
		assertThat(orders, hasItems(expectedOrders3));
		
		asOfDate = Context.getDateTimeFormat().parse("26/09/2008 09:25:10");
		orders = orderService.getActiveOrders(patient, null, null, asOfDate);
		assertEquals(4, orders.size());
		Order[] expectedOrders4 = { orderService.getOrder(222), orderService.getOrder(3), orderService.getOrder(444),
		        orderService.getOrder(5) };
		assertThat(orders, hasItems(expectedOrders4));
		
		asOfDate = Context.getDateFormat().parse("04/12/2008");
		orders = orderService.getActiveOrders(patient, null, null, asOfDate);
		assertEquals(5, orders.size());
		Order[] expectedOrders5 = { orderService.getOrder(222), orderService.getOrder(3), orderService.getOrder(444),
		        orderService.getOrder(5), orderService.getOrder(7) };
		assertThat(orders, hasItems(expectedOrders5));
		
		asOfDate = Context.getDateFormat().parse("06/12/2008");
		orders = orderService.getActiveOrders(patient, null, null, asOfDate);
		assertEquals(5, orders.size());
		assertThat(orders, hasItems(expectedOrders5));
	}
	
	/**
	 * @see OrderService#getActiveOrders(org.openmrs.Patient, org.openmrs.OrderType,
	 *      org.openmrs.CareSetting, java.util.Date)
	 */
	@Test
	public void getActiveOrders_shouldReturnAllOrdersIfNoOrderTypeIsSpecified() {
		Patient patient = Context.getPatientService().getPatient(2);
		List<Order> orders = orderService.getActiveOrders(patient, null, null, null);
		assertEquals(5, orders.size());
		Order[] expectedOrders = { orderService.getOrder(222), orderService.getOrder(3), orderService.getOrder(444),
		        orderService.getOrder(5), orderService.getOrder(7) };
		assertThat(orders, hasItems(expectedOrders));
	}
	
	/**
	 * @see OrderService#getActiveOrders(org.openmrs.Patient, org.openmrs.OrderType,
	 *      org.openmrs.CareSetting, java.util.Date)
	 */
	@Test
	public void getActiveOrders_shouldIncludeOrdersForSubTypesIfOrderTypeIsSpecified() {
		executeDataSet("org/openmrs/api/include/OrderServiceTest-otherOrders.xml");
		Patient patient = Context.getPatientService().getPatient(2);
		OrderType testOrderType = orderService.getOrderType(2);
		List<Order> orders = orderService.getActiveOrders(patient, testOrderType, null, null);
		assertEquals(5, orders.size());
		Order[] expectedOrder1 = { orderService.getOrder(7), orderService.getOrder(101), orderService.getOrder(102),
		        orderService.getOrder(103), orderService.getOrder(104) };
		assertThat(orders, hasItems(expectedOrder1));
		
		OrderType labTestOrderType = orderService.getOrderType(7);
		orders = orderService.getActiveOrders(patient, labTestOrderType, null, null);
		assertEquals(3, orders.size());
		Order[] expectedOrder2 = { orderService.getOrder(101), orderService.getOrder(103), orderService.getOrder(104) };
		assertThat(orders, hasItems(expectedOrder2));
	}
	
	/**
	 * @see OrderService#discontinueOrder(org.openmrs.Order, String, java.util.Date,
	 *      org.openmrs.Provider, org.openmrs.Encounter)
	 */
	@Test
	public void discontinueOrder_shouldPopulateCorrectAttributesOnTheDiscontinueAndDiscontinuedOrders() {
		Order order = orderService.getOrderByOrderNumber("111");
		Encounter encounter = encounterService.getEncounter(3);
		Provider orderer = providerService.getProvider(1);
		assertTrue(OrderUtilTest.isActiveOrder(order, null));
		Date discontinueDate = new Date();
		String discontinueReasonNonCoded = "Test if I can discontinue this";
		
		Order discontinueOrder = orderService.discontinueOrder(order, discontinueReasonNonCoded, discontinueDate, orderer,
		    encounter);
		
		Assert.assertEquals(order.getDateStopped(), discontinueDate);
		Assert.assertNotNull(discontinueOrder);
		Assert.assertNotNull(discontinueOrder.getId());
		Assert.assertEquals(discontinueOrder.getDateActivated(), discontinueOrder.getAutoExpireDate());
		Assert.assertEquals(discontinueOrder.getAction(), Action.DISCONTINUE);
		Assert.assertEquals(discontinueOrder.getOrderReasonNonCoded(), discontinueReasonNonCoded);
		Assert.assertEquals(discontinueOrder.getPreviousOrder(), order);
	}
	
	/**
	 * @see OrderService#discontinueOrder(Order,String,Date,Provider,Encounter)
	 */
	@Test
	public void discontinueOrder_shouldPassForAnActiveOrderWhichIsScheduledAndNotStartedAsOfDiscontinueDate()
	{
		Order order = new Order();
		order.setAction(Action.NEW);
		order.setPatient(Context.getPatientService().getPatient(7));
		order.setConcept(Context.getConceptService().getConcept(5497));
		order.setCareSetting(orderService.getCareSetting(1));
		order.setOrderer(orderService.getOrder(1).getOrderer());
		order.setEncounter(encounterService.getEncounter(3));
		order.setEncounter(encounterService.getEncounter(3));
		order.setOrderType(orderService.getOrderType(17));
		order.setDateActivated(new Date());
		order.setScheduledDate(DateUtils.addMonths(new Date(), 2));
		order.setUrgency(Order.Urgency.ON_SCHEDULED_DATE);
		order = orderService.saveOrder(order, null);
		
		assertTrue(OrderUtilTest.isActiveOrder(order, null));
		assertFalse(order.isStarted());
		
		Encounter encounter = encounterService.getEncounter(3);
		Provider orderer = providerService.getProvider(1);
		Date discontinueDate = new Date();
		String discontinueReasonNonCoded = "Test if I can discontinue this";
		
		Order discontinueOrder = orderService.discontinueOrder(order, discontinueReasonNonCoded, discontinueDate, orderer,
		    encounter);
		
		Assert.assertEquals(order.getDateStopped(), discontinueDate);
		Assert.assertNotNull(discontinueOrder);
		Assert.assertNotNull(discontinueOrder.getId());
		Assert.assertEquals(discontinueOrder.getDateActivated(), discontinueOrder.getAutoExpireDate());
		Assert.assertEquals(discontinueOrder.getAction(), Action.DISCONTINUE);
		Assert.assertEquals(discontinueOrder.getOrderReasonNonCoded(), discontinueReasonNonCoded);
		Assert.assertEquals(discontinueOrder.getPreviousOrder(), order);
	}
	
	/**
	 * @see OrderService#discontinueOrder(org.openmrs.Order, org.openmrs.Concept, java.util.Date,
	 *      org.openmrs.Provider, org.openmrs.Encounter)
	 */
	@Test
	public void discontinueOrder_shouldSetCorrectAttributesOnTheDiscontinueAndDiscontinuedOrders() {
		executeDataSet("org/openmrs/api/include/OrderServiceTest-discontinueReason.xml");
		
		Order order = orderService.getOrderByOrderNumber("111");
		Encounter encounter = encounterService.getEncounter(3);
		Provider orderer = providerService.getProvider(1);
		Date discontinueDate = new Date();
		Concept concept = Context.getConceptService().getConcept(1);
		
		Order discontinueOrder = orderService.discontinueOrder(order, concept, discontinueDate, orderer, encounter);
		
		Assert.assertEquals(order.getDateStopped(), discontinueDate);
		Assert.assertNotNull(discontinueOrder);
		Assert.assertNotNull(discontinueOrder.getId());
		Assert.assertEquals(discontinueOrder.getDateActivated(), discontinueOrder.getAutoExpireDate());
		Assert.assertEquals(discontinueOrder.getAction(), Action.DISCONTINUE);
		Assert.assertEquals(discontinueOrder.getOrderReason(), concept);
		Assert.assertEquals(discontinueOrder.getPreviousOrder(), order);
	}
	
	/**
	 * @see OrderService#discontinueOrder(Order,Concept,Date,Provider,Encounter)
	 */
	@Test
	public void discontinueOrder_shouldPassForAnActiveOrderWhichIsScheduledAndNotStartedAsOfDiscontinueDateWithParamConcept()
	{
		Order order = new Order();
		order.setAction(Action.NEW);
		order.setPatient(Context.getPatientService().getPatient(7));
		order.setConcept(Context.getConceptService().getConcept(5497));
		order.setCareSetting(orderService.getCareSetting(1));
		order.setOrderer(orderService.getOrder(1).getOrderer());
		order.setEncounter(encounterService.getEncounter(3));
		order.setEncounter(encounterService.getEncounter(3));
		order.setOrderType(orderService.getOrderType(17));
		order.setDateActivated(new Date());
		order.setScheduledDate(DateUtils.addMonths(new Date(), 2));
		order.setUrgency(Order.Urgency.ON_SCHEDULED_DATE);
		order = orderService.saveOrder(order, null);
		
		assertTrue(OrderUtilTest.isActiveOrder(order, null));
		assertFalse(order.isStarted());
		
		Encounter encounter = encounterService.getEncounter(3);
		Provider orderer = providerService.getProvider(1);
		Date discontinueDate = new Date();
		Concept concept = Context.getConceptService().getConcept(1);
		
		Order discontinueOrder = orderService.discontinueOrder(order, concept, discontinueDate, orderer, encounter);
		
		Assert.assertEquals(order.getDateStopped(), discontinueDate);
		Assert.assertNotNull(discontinueOrder);
		Assert.assertNotNull(discontinueOrder.getId());
		Assert.assertEquals(discontinueOrder.getDateActivated(), discontinueOrder.getAutoExpireDate());
		Assert.assertEquals(discontinueOrder.getAction(), Action.DISCONTINUE);
		Assert.assertEquals(discontinueOrder.getOrderReason(), concept);
		Assert.assertEquals(discontinueOrder.getPreviousOrder(), order);
	}
	
	/**
	 * @see OrderService#discontinueOrder(org.openmrs.Order, String, java.util.Date,
	 *      org.openmrs.Provider, org.openmrs.Encounter)
	 */
	@Test
	public void discontinueOrder_shouldFailForADiscontinuationOrder() {
		executeDataSet("org/openmrs/api/include/OrderServiceTest-discontinuedOrder.xml");
		Order discontinuationOrder = orderService.getOrder(26);
		assertEquals(Action.DISCONTINUE, discontinuationOrder.getAction());
		Encounter encounter = encounterService.getEncounter(3);
		expectedException.expect(CannotStopDiscontinuationOrderException.class);
		expectedException.expectMessage(mss.getMessage("Order.action.cannot.discontinue"));
		orderService.discontinueOrder(discontinuationOrder, "Test if I can discontinue this", null, null, encounter);
	}
	
	/**
	 * @see OrderService#discontinueOrder(org.openmrs.Order, org.openmrs.Concept, java.util.Date,
	 *      org.openmrs.Provider, org.openmrs.Encounter)
	 */
	@Test
	public void discontinueOrder_shouldNotPassForADiscontinuationOrder() {
		executeDataSet("org/openmrs/api/include/OrderServiceTest-discontinuedOrder.xml");
		executeDataSet("org/openmrs/api/include/OrderServiceTest-discontinueReason.xml");
		Order discontinuationOrder = orderService.getOrder(26);
		assertEquals(Action.DISCONTINUE, discontinuationOrder.getAction());
		Encounter encounter = encounterService.getEncounter(3);
		expectedException.expect(CannotStopDiscontinuationOrderException.class);
		expectedException.expectMessage(mss.getMessage("Order.action.cannot.discontinue"));
		orderService.discontinueOrder(discontinuationOrder, (Concept) null, null, null, encounter);
	}
	
	/**
	 * @see OrderService#discontinueOrder(org.openmrs.Order, String, java.util.Date,
	 *      org.openmrs.Provider, org.openmrs.Encounter)
	 */
	@Test
	public void discontinueOrder_shouldFailForADiscontinuedOrder() {
		Order discontinuationOrder = orderService.getOrder(2);
		assertFalse(discontinuationOrder.isActive());
		assertNotNull(discontinuationOrder.getDateStopped());
		Encounter encounter = encounterService.getEncounter(3);
		expectedException.expect(CannotStopInactiveOrderException.class);
		expectedException.expectMessage(mss.getMessage("Order.cannot.discontinue.inactive"));
		orderService.discontinueOrder(discontinuationOrder, "some reason", null, null, encounter);
	}
	
	/**
	 * @see OrderService#discontinueOrder(org.openmrs.Order, org.openmrs.Concept, java.util.Date,
	 *      org.openmrs.Provider, org.openmrs.Encounter)
	 */
	@Test
	public void discontinueOrder_shouldNotPassForADiscontinuedOrder() {
		Order discontinuationOrder = orderService.getOrder(2);
		assertFalse(discontinuationOrder.isActive());
		assertNotNull(discontinuationOrder.getDateStopped());
		Encounter encounter = encounterService.getEncounter(3);
		expectedException.expect(CannotStopInactiveOrderException.class);
		expectedException.expectMessage(mss.getMessage("Order.cannot.discontinue.inactive"));
		orderService.discontinueOrder(discontinuationOrder, (Concept) null, null, null, encounter);
	}
	
	/**
	 * @see OrderService#saveOrder(org.openmrs.Order, OrderContext)
	 */
	@Test
	public void saveOrder_shouldDiscontinueExistingActiveOrderIfNewOrderBeingSavedWithActionToDiscontinue() {
		DrugOrder order = new DrugOrder();
		order.setAction(Order.Action.DISCONTINUE);
		order.setOrderReasonNonCoded("Discontinue this");
		order.setDrug(conceptService.getDrug(3));
		order.setEncounter(encounterService.getEncounter(5));
		order.setPatient(patientService.getPatient(7));
		order.setOrderer(providerService.getProvider(1));
		order.setCareSetting(orderService.getCareSetting(1));
		order.setEncounter(encounterService.getEncounter(3));
		order.setOrderType(orderService.getOrderType(1));
		order.setDateActivated(new Date());
		order.setDosingType(SimpleDosingInstructions.class);
		order.setDose(500.0);
		order.setDoseUnits(conceptService.getConcept(50));
		order.setFrequency(orderService.getOrderFrequency(1));
		order.setRoute(conceptService.getConcept(22));
		order.setNumRefills(10);
		order.setQuantity(20.0);
		order.setQuantityUnits(conceptService.getConcept(51));
		
		//We are trying to discontinue order id 111 in standardTestDataset.xml
		Order expectedPreviousOrder = orderService.getOrder(111);
		Assert.assertNull(expectedPreviousOrder.getDateStopped());
		
		order = (DrugOrder) orderService.saveOrder(order, null);
		
		Assert.assertNotNull("should populate dateStopped in previous order", expectedPreviousOrder.getDateStopped());
		Assert.assertNotNull("should save discontinue order", order.getId());
		Assert.assertEquals(expectedPreviousOrder, order.getPreviousOrder());
		Assert.assertNotNull(expectedPreviousOrder.getDateStopped());
		Assert.assertEquals(order.getDateActivated(), order.getAutoExpireDate());
	}
	
	/**
	 * @see OrderService#saveOrder(org.openmrs.Order, OrderContext)
	 */
	@Test
	public void saveOrder_shouldDiscontinuePreviousOrderIfItIsNotAlreadyDiscontinued() {
		//We are trying to discontinue order id 111 in standardTestDataset.xml
		DrugOrder order = new DrugOrder();
		order.setAction(Order.Action.DISCONTINUE);
		order.setOrderReasonNonCoded("Discontinue this");
		order.setDrug(conceptService.getDrug(3));
		order.setEncounter(encounterService.getEncounter(5));
		order.setPatient(Context.getPatientService().getPatient(7));
		order.setOrderer(Context.getProviderService().getProvider(1));
		order.setCareSetting(orderService.getCareSetting(1));
		order.setEncounter(encounterService.getEncounter(3));
		order.setOrderType(orderService.getOrderType(1));
		order.setDateActivated(new Date());
		order.setDosingType(SimpleDosingInstructions.class);
		order.setDose(500.0);
		order.setDoseUnits(conceptService.getConcept(50));
		order.setFrequency(orderService.getOrderFrequency(1));
		order.setRoute(conceptService.getConcept(22));
		order.setNumRefills(10);
		order.setQuantity(20.0);
		order.setQuantityUnits(conceptService.getConcept(51));
		Order previousOrder = orderService.getOrder(111);
		assertTrue(OrderUtilTest.isActiveOrder(previousOrder, null));
		order.setPreviousOrder(previousOrder);
		
		orderService.saveOrder(order, null);
		Assert.assertEquals(order.getDateActivated(), order.getAutoExpireDate());
		Assert.assertNotNull("previous order should be discontinued", previousOrder.getDateStopped());
	}
	
	/**
	 * @see OrderService#saveOrder(org.openmrs.Order, OrderContext)
	 */
	@Test
	public void saveOrder_shouldFailIfConceptInPreviousOrderDoesNotMatchThisConcept() {
		Order previousOrder = orderService.getOrder(7);
		assertTrue(OrderUtilTest.isActiveOrder(previousOrder, null));
		Order order = previousOrder.cloneForDiscontinuing();
		order.setDateActivated(new Date());
		order.setOrderReasonNonCoded("Discontinue this");
		order.setEncounter(encounterService.getEncounter(6));
		order.setOrderer(providerService.getProvider(1));
		Concept newConcept = conceptService.getConcept(5089);
		assertFalse(previousOrder.getConcept().equals(newConcept));
		order.setConcept(newConcept);
		
		expectedException.expect(EditedOrderDoesNotMatchPreviousException.class);
		expectedException.expectMessage("The orderable of the previous order and the new one order don't match");
		orderService.saveOrder(order, null);
	}
	
	/**
	 * @see OrderService#discontinueOrder(org.openmrs.Order, org.openmrs.Concept, java.util.Date,
	 *      org.openmrs.Provider, org.openmrs.Encounter)
	 */
	@Test
	public void discontinueOrder_shouldRejectAFutureDiscontinueDate() {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.HOUR_OF_DAY, 1);
		Patient patient = Context.getPatientService().getPatient(2);
		CareSetting careSetting = orderService.getCareSetting(1);
		Order orderToDiscontinue = orderService.getActiveOrders(patient, null, careSetting, null).get(0);
		Encounter encounter = encounterService.getEncounter(3);
		expectedException.expect(IllegalArgumentException.class);
		expectedException.expectMessage("Discontinue date cannot be in the future");
		orderService.discontinueOrder(orderToDiscontinue, new Concept(), cal.getTime(), null, encounter);
	}
	
	/**
	 * @see OrderService#discontinueOrder(org.openmrs.Order, String, java.util.Date,
	 *      org.openmrs.Provider, org.openmrs.Encounter)
	 */
	@Test
	public void discontinueOrder_shouldFailIfDiscontinueDateIsInTheFuture() {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.HOUR_OF_DAY, 1);
		Order orderToDiscontinue = orderService.getActiveOrders(Context.getPatientService().getPatient(2), null,
		    orderService.getCareSetting(1), null).get(0);
		Encounter encounter = encounterService.getEncounter(3);
		expectedException.expect(IllegalArgumentException.class);
		expectedException.expectMessage("Discontinue date cannot be in the future");
		orderService.discontinueOrder(orderToDiscontinue, "Testing", cal.getTime(), null, encounter);
	}
	
	/**
	 * @see OrderService#saveOrder(org.openmrs.Order, OrderContext)
	 */
	@Test
	public void saveOrder_shouldPassIfTheExistingDrugOrderMatchesTheConceptAndDrugOfTheDCOrder() {
		final DrugOrder orderToDiscontinue = (DrugOrder) orderService.getOrder(444);
		assertTrue(OrderUtilTest.isActiveOrder(orderToDiscontinue, null));
		
		DrugOrder order = new DrugOrder();
		order.setDrug(orderToDiscontinue.getDrug());
		order.setOrderType(orderService.getOrderTypeByName("Drug order"));
		order.setAction(Order.Action.DISCONTINUE);
		order.setOrderReasonNonCoded("Discontinue this");
		order.setPatient(orderToDiscontinue.getPatient());
		order.setConcept(orderToDiscontinue.getConcept());
		order.setOrderer(orderToDiscontinue.getOrderer());
		order.setCareSetting(orderToDiscontinue.getCareSetting());
		order.setEncounter(encounterService.getEncounter(6));
		order.setDateActivated(new Date());
		order.setDosingType(SimpleDosingInstructions.class);
		order.setDose(orderToDiscontinue.getDose());
		order.setDoseUnits(orderToDiscontinue.getDoseUnits());
		order.setRoute(orderToDiscontinue.getRoute());
		order.setFrequency(orderToDiscontinue.getFrequency());
		order.setQuantity(orderToDiscontinue.getQuantity());
		order.setQuantityUnits(orderToDiscontinue.getQuantityUnits());
		order.setNumRefills(orderToDiscontinue.getNumRefills());
		
		orderService.saveOrder(order, null);
		
		Assert.assertNotNull("previous order should be discontinued", orderToDiscontinue.getDateStopped());
	}
	
	/**
	 * @see OrderService#saveOrder(org.openmrs.Order, OrderContext)
	 */
	@Test
	public void saveOrder_shouldFailIfTheExistingDrugOrderMatchesTheConceptAndNotDrugOfTheDCOrder() {
		final DrugOrder orderToDiscontinue = (DrugOrder) orderService.getOrder(5);
		assertTrue(OrderUtilTest.isActiveOrder(orderToDiscontinue, null));
		
		//create a different test drug
		Drug discontinuationOrderDrug = new Drug();
		discontinuationOrderDrug.setConcept(orderToDiscontinue.getConcept());
		discontinuationOrderDrug = conceptService.saveDrug(discontinuationOrderDrug);
		assertNotEquals(discontinuationOrderDrug, orderToDiscontinue.getDrug());
		assertNotNull(orderToDiscontinue.getDrug());
		
		DrugOrder order = orderToDiscontinue.cloneForRevision();
		order.setDateActivated(new Date());
		order.setOrderer(providerService.getProvider(1));
		order.setEncounter(encounterService.getEncounter(6));
		order.setDrug(discontinuationOrderDrug);
		order.setOrderReasonNonCoded("Discontinue this");
		
		expectedException.expect(EditedOrderDoesNotMatchPreviousException.class);
		expectedException.expectMessage("The orderable of the previous order and the new one order don't match");
		orderService.saveOrder(order, null);
	}
	
	/**
	 *           previous order
	 * @see OrderService#saveOrder(org.openmrs.Order, OrderContext)
	 */
	@Test
	public void saveOrder_shouldPassIfTheExistingDrugOrderMatchesTheConceptAndThereIsNoDrugOnThePreviousOrder()
	{
		DrugOrder orderToDiscontinue = new DrugOrder();
		orderToDiscontinue.setAction(Action.NEW);
		orderToDiscontinue.setPatient(Context.getPatientService().getPatient(7));
		orderToDiscontinue.setConcept(Context.getConceptService().getConcept(5497));
		orderToDiscontinue.setCareSetting(orderService.getCareSetting(1));
		orderToDiscontinue.setOrderer(orderService.getOrder(1).getOrderer());
		orderToDiscontinue.setEncounter(encounterService.getEncounter(3));
		orderToDiscontinue.setDateActivated(new Date());
		orderToDiscontinue.setScheduledDate(new Date());
		orderToDiscontinue.setUrgency(Order.Urgency.ON_SCHEDULED_DATE);
		orderToDiscontinue.setEncounter(encounterService.getEncounter(3));
		orderToDiscontinue.setOrderType(orderService.getOrderType(17));
		
		orderToDiscontinue.setDrug(null);
		orderToDiscontinue.setDosingType(FreeTextDosingInstructions.class);
		orderToDiscontinue.setDosingInstructions("instructions");
		orderToDiscontinue.setOrderer(providerService.getProvider(1));
		orderToDiscontinue.setDosingInstructions("2 for 5 days");
		orderToDiscontinue.setQuantity(10.0);
		orderToDiscontinue.setQuantityUnits(conceptService.getConcept(51));
		orderToDiscontinue.setNumRefills(2);
		
		orderService.saveOrder(orderToDiscontinue, null);
		assertTrue(OrderUtilTest.isActiveOrder(orderToDiscontinue, null));
		
		DrugOrder order = orderToDiscontinue.cloneForDiscontinuing();
		order.setDateActivated(new Date());
		order.setOrderer(providerService.getProvider(1));
		order.setEncounter(encounterService.getEncounter(3));
		order.setOrderReasonNonCoded("Discontinue this");
		
		orderService.saveOrder(order, null);
		
		Assert.assertNotNull("previous order should be discontinued", orderToDiscontinue.getDateStopped());
	}
	
	/**
	 * @see OrderService#discontinueOrder(org.openmrs.Order, org.openmrs.Concept, java.util.Date,
	 *      org.openmrs.Provider, org.openmrs.Encounter)
	 */
	@Test
	public void discontinueOrder_shouldFailForAStoppedOrder() {
		Order orderToDiscontinue = orderService.getOrder(1);
		Encounter encounter = encounterService.getEncounter(3);
		assertNotNull(orderToDiscontinue.getDateStopped());
		expectedException.expect(CannotStopInactiveOrderException.class);
		expectedException.expectMessage(mss.getMessage("Order.cannot.discontinue.inactive"));
		orderService.discontinueOrder(orderToDiscontinue, Context.getConceptService().getConcept(1), null, null, encounter);
	}
	
	/**
	 * @see OrderService#discontinueOrder(org.openmrs.Order, String, java.util.Date,
	 *      org.openmrs.Provider, org.openmrs.Encounter)
	 */
	@Test
	public void discontinueOrder_shouldFailForAVoidedOrder() {
		Order orderToDiscontinue = orderService.getOrder(8);
		Encounter encounter = encounterService.getEncounter(3);
		assertTrue(orderToDiscontinue.getVoided());
		expectedException.expect(CannotStopInactiveOrderException.class);
		expectedException.expectMessage(mss.getMessage("Order.cannot.discontinue.inactive"));
		orderService.discontinueOrder(orderToDiscontinue, "testing", null, null, encounter);
	}
	
	/**
	 * @see OrderService#discontinueOrder(org.openmrs.Order, org.openmrs.Concept, java.util.Date,
	 *      org.openmrs.Provider, org.openmrs.Encounter)
	 */
	@Test
	public void discontinueOrder_shouldFailForAnExpiredOrder() {
		Order orderToDiscontinue = orderService.getOrder(6);
		Encounter encounter = encounterService.getEncounter(3);
		assertNotNull(orderToDiscontinue.getAutoExpireDate());
		assertTrue(orderToDiscontinue.getAutoExpireDate().before(new Date()));
		expectedException.expect(CannotStopInactiveOrderException.class);
		expectedException.expectMessage(mss.getMessage("Order.cannot.discontinue.inactive"));
		orderService.discontinueOrder(orderToDiscontinue, Context.getConceptService().getConcept(1), null, null, encounter);
	}
	
	/**
	 * @see OrderService#saveOrder(org.openmrs.Order, OrderContext)
	 */
	@Test
	public void saveOrder_shouldNotAllowEditingAnExistingOrder() {
		final DrugOrder order = (DrugOrder) orderService.getOrder(5);
		expectedException.expect(UnchangeableObjectException.class);
		expectedException.expectMessage("Order.cannot.edit.existing");
		orderService.saveOrder(order, null);
	}
	
	/**
	 * @see OrderService#getCareSettingByUuid(String)
	 */
	@Test
	public void getCareSettingByUuid_shouldReturnTheCareSettingWithTheSpecifiedUuid() {
		CareSetting cs = orderService.getCareSettingByUuid("6f0c9a92-6f24-11e3-af88-005056821db0");
		assertEquals(1, cs.getId().intValue());
	}
	
	/**
	 * @see OrderService#getCareSettingByName(String)
	 */
	@Test
	public void getCareSettingByName_shouldReturnTheCareSettingWithTheSpecifiedName() {
		CareSetting cs = orderService.getCareSettingByName("INPATIENT");
		assertEquals(2, cs.getId().intValue());
		
		//should also be case insensitive
		cs = orderService.getCareSettingByName("inpatient");
		assertEquals(2, cs.getId().intValue());
	}
	
	/**
	 * @see OrderService#getCareSettings(boolean)
	 */
	@Test
	public void getCareSettings_shouldReturnOnlyUnRetiredCareSettingsIfIncludeRetiredIsSetToFalse() {
		List<CareSetting> careSettings = orderService.getCareSettings(false);
		assertEquals(2, careSettings.size());
		assertTrue(containsId(careSettings, 1));
		assertTrue(containsId(careSettings, 2));
	}
	
	/**
	 * @see OrderService#getCareSettings(boolean)
	 */
	@Test
	public void getCareSettings_shouldReturnRetiredCareSettingsIfIncludeRetiredIsSetToTrue() {
		CareSetting retiredCareSetting = orderService.getCareSetting(3);
		assertTrue(retiredCareSetting.getRetired());
		List<CareSetting> careSettings = orderService.getCareSettings(true);
		assertEquals(3, careSettings.size());
		assertTrue(containsId(careSettings, retiredCareSetting.getCareSettingId()));
	}
	
	/**
	 * @see OrderService#saveOrder(org.openmrs.Order, OrderContext)
	 */
	@Test
	public void saveOrder_shouldNotAllowRevisingAStoppedOrder() {
		Order originalOrder = orderService.getOrder(1);
		assertNotNull(originalOrder.getDateStopped());
		Order revisedOrder = originalOrder.cloneForRevision();
		revisedOrder.setEncounter(encounterService.getEncounter(4));
		revisedOrder.setInstructions("Take after a meal");
		revisedOrder.setOrderer(providerService.getProvider(1));
		revisedOrder.setDateActivated(new Date());
		expectedException.expect(CannotStopInactiveOrderException.class);
		expectedException.expectMessage(mss.getMessage("Order.cannot.discontinue.inactive"));
		orderService.saveOrder(revisedOrder, null);
	}
	
	/**
	 * @see OrderService#saveOrder(org.openmrs.Order, OrderContext)
	 */
	@Test
	public void saveOrder_shouldNotAllowRevisingAVoidedOrder() {
		Order originalOrder = orderService.getOrder(8);
		assertTrue(originalOrder.getVoided());
		Order revisedOrder = originalOrder.cloneForRevision();
		revisedOrder.setEncounter(encounterService.getEncounter(6));
		revisedOrder.setInstructions("Take after a meal");
		revisedOrder.setOrderer(providerService.getProvider(1));
		revisedOrder.setDateActivated(new Date());
		expectedException.expect(CannotStopInactiveOrderException.class);
		expectedException.expectMessage(mss.getMessage("Order.cannot.discontinue.inactive"));
		orderService.saveOrder(revisedOrder, null);
	}
	
	/**
	 * @see OrderService#saveOrder(org.openmrs.Order, OrderContext)
	 */
	@Test
	public void saveOrder_shouldNotAllowRevisingAnExpiredOrder() {
		Order originalOrder = orderService.getOrder(6);
		assertNotNull(originalOrder.getAutoExpireDate());
		assertTrue(originalOrder.getAutoExpireDate().before(new Date()));
		Order revisedOrder = originalOrder.cloneForRevision();
		revisedOrder.setEncounter(encounterService.getEncounter(6));
		revisedOrder.setInstructions("Take after a meal");
		revisedOrder.setOrderer(providerService.getProvider(1));
		revisedOrder.setDateActivated(new Date());
		revisedOrder.setAutoExpireDate(new Date());
		expectedException.expect(CannotStopInactiveOrderException.class);
		expectedException.expectMessage(mss.getMessage("Order.cannot.discontinue.inactive"));
		orderService.saveOrder(revisedOrder, null);
	}
	
	/**
	 * @see OrderService#saveOrder(org.openmrs.Order, OrderContext)
	 */
	@Test
	public void saveOrder_shouldNotAllowRevisingAnOrderWithNoPreviousOrder() {
		Order originalOrder = orderService.getOrder(111);
		assertTrue(originalOrder.isActive());
		Order revisedOrder = originalOrder.cloneForRevision();
		revisedOrder.setEncounter(encounterService.getEncounter(5));
		revisedOrder.setInstructions("Take after a meal");
		revisedOrder.setPreviousOrder(null);
		revisedOrder.setOrderer(providerService.getProvider(1));
		revisedOrder.setDateActivated(new Date());
		
		expectedException.expect(MissingRequiredPropertyException.class);
		expectedException.expectMessage(mss.getMessage("Order.previous.required"));
		orderService.saveOrder(revisedOrder, null);
	}
	
	/**
	 * @see OrderService#saveOrder(org.openmrs.Order, OrderContext)
	 */
	@Test
	public void saveOrder_shouldSaveARevisedOrder() {
		Order originalOrder = orderService.getOrder(111);
		assertTrue(originalOrder.isActive());
		final Patient patient = originalOrder.getPatient();
		List<Order> originalActiveOrders = orderService.getActiveOrders(patient, null, null, null);
		final int originalOrderCount = originalActiveOrders.size();
		assertTrue(originalActiveOrders.contains(originalOrder));
		Order revisedOrder = originalOrder.cloneForRevision();
		revisedOrder.setEncounter(encounterService.getEncounter(5));
		revisedOrder.setInstructions("Take after a meal");
		revisedOrder.setDateActivated(new Date());
		revisedOrder.setOrderer(providerService.getProvider(1));
		revisedOrder.setEncounter(encounterService.getEncounter(3));
		orderService.saveOrder(revisedOrder, null);
		
		List<Order> activeOrders = orderService.getActiveOrders(patient, null, null, null);
		assertEquals(originalOrderCount, activeOrders.size());
		assertEquals(revisedOrder.getDateActivated(), DateUtils.addSeconds(originalOrder.getDateStopped(), 1));
		assertFalse(originalOrder.isActive());
	}
	
	/**
	 * @see OrderService#saveOrder(Order,OrderContext)
	 */
	@Test
	public void saveOrder_shouldSaveARevisedOrderForAScheduledOrderWhichIsNotStarted() {
		Order originalOrder = new Order();
		originalOrder.setAction(Action.NEW);
		originalOrder.setPatient(Context.getPatientService().getPatient(7));
		originalOrder.setConcept(Context.getConceptService().getConcept(5497));
		originalOrder.setCareSetting(orderService.getCareSetting(1));
		originalOrder.setOrderer(orderService.getOrder(1).getOrderer());
		originalOrder.setEncounter(encounterService.getEncounter(3));
		originalOrder.setOrderType(orderService.getOrderType(17));
		originalOrder.setDateActivated(new Date());
		originalOrder.setScheduledDate(DateUtils.addMonths(new Date(), 2));
		originalOrder.setUrgency(Order.Urgency.ON_SCHEDULED_DATE);
		originalOrder = orderService.saveOrder(originalOrder, null);
		
		assertTrue(originalOrder.isActive());
		final Patient patient = originalOrder.getPatient();
		List<Order> originalActiveOrders = orderService.getActiveOrders(patient, null, null, null);
		final int originalOrderCount = originalActiveOrders.size();
		assertTrue(originalActiveOrders.contains(originalOrder));
		
		Order revisedOrder = originalOrder.cloneForRevision();
		revisedOrder.setEncounter(encounterService.getEncounter(5));
		revisedOrder.setInstructions("Take after a meal");
		revisedOrder.setDateActivated(new Date());
		revisedOrder.setOrderer(providerService.getProvider(1));
		revisedOrder.setEncounter(encounterService.getEncounter(3));
		orderService.saveOrder(revisedOrder, null);
		
		List<Order> activeOrders = orderService.getActiveOrders(patient, null, null, null);
		assertEquals(originalOrderCount, activeOrders.size());
		assertEquals(revisedOrder.getDateActivated(), DateUtils.addSeconds(originalOrder.getDateStopped(), 1));
		assertFalse(activeOrders.contains(originalOrder));
		assertFalse(originalOrder.isActive());
	}
	
	/**
	 * @see OrderService#getOrderFrequencies(String, java.util.Locale, boolean, boolean)
	 */
	@Test
	public void getOrderFrequencies_shouldGetNonRetiredFrequenciesWithNamesMatchingThePhraseIfIncludeRetiredIsFalse()
	{
		executeDataSet("org/openmrs/api/include/OrderServiceTest-otherOrderFrequencies.xml");
		List<OrderFrequency> orderFrequencies = orderService.getOrderFrequencies("once", Locale.US, false, false);
		assertEquals(2, orderFrequencies.size());
		assertTrue(containsId(orderFrequencies, 100));
		assertTrue(containsId(orderFrequencies, 102));
		
		//should match anywhere in the concept name
		orderFrequencies = orderService.getOrderFrequencies("nce", Locale.US, false, false);
		assertEquals(2, orderFrequencies.size());
		assertTrue(containsId(orderFrequencies, 100));
		assertTrue(containsId(orderFrequencies, 102));
	}
	
	/**
	 * @see OrderService#getOrderFrequencies(String, java.util.Locale, boolean, boolean)
	 */
	@Test
	public void getOrderFrequencies_shouldIncludeRetiredFrequenciesIfIncludeRetiredIsSetToTrue() {
		executeDataSet("org/openmrs/api/include/OrderServiceTest-otherOrderFrequencies.xml");
		List<OrderFrequency> orderFrequencies = orderService.getOrderFrequencies("ce", Locale.US, false, true);
		assertEquals(4, orderFrequencies.size());
		assertTrue(containsId(orderFrequencies, 100));
		assertTrue(containsId(orderFrequencies, 101));
		assertTrue(containsId(orderFrequencies, 102));
		assertTrue(containsId(orderFrequencies, 103));
	}
	
	/**
	 * @see OrderService#getOrderFrequencies(String, java.util.Locale, boolean, boolean)
	 */
	@Test
	public void getOrderFrequencies_shouldGetFrequenciesWithNamesThatMatchThePhraseAndLocalesIfExactLocaleIsFalse()
	{
		executeDataSet("org/openmrs/api/include/OrderServiceTest-otherOrderFrequencies.xml");
		List<OrderFrequency> orderFrequencies = orderService.getOrderFrequencies("ce", Locale.US, false, false);
		assertEquals(3, orderFrequencies.size());
		assertTrue(containsId(orderFrequencies, 100));
		assertTrue(containsId(orderFrequencies, 101));
		assertTrue(containsId(orderFrequencies, 102));
	}
	
	/**
	 * @see OrderService#getOrderFrequencies(String, java.util.Locale, boolean, boolean)
	 */
	@Test
	public void getOrderFrequencies_shouldGetFrequenciesWithNamesThatMatchThePhraseAndLocaleIfExactLocaleIsTrue()
	{
		executeDataSet("org/openmrs/api/include/OrderServiceTest-otherOrderFrequencies.xml");
		List<OrderFrequency> orderFrequencies = orderService.getOrderFrequencies("ce", Locale.US, true, false);
		assertEquals(1, orderFrequencies.size());
		assertEquals(102, orderFrequencies.get(0).getOrderFrequencyId().intValue());
		
		orderFrequencies = orderService.getOrderFrequencies("ce", Locale.ENGLISH, true, false);
		assertEquals(2, orderFrequencies.size());
		assertTrue(containsId(orderFrequencies, 100));
		assertTrue(containsId(orderFrequencies, 101));
	}
	
	/**
	 * @see OrderService#getOrderFrequencies(String, java.util.Locale, boolean, boolean)
	 */
	@Test
	public void getOrderFrequencies_shouldReturnUniqueFrequencies() {
		executeDataSet("org/openmrs/api/include/OrderServiceTest-otherOrderFrequencies.xml");
		final String searchPhrase = "once";
		final Locale locale = Locale.ENGLISH;
		List<OrderFrequency> orderFrequencies = orderService.getOrderFrequencies(searchPhrase, locale, true, false);
		assertEquals(1, orderFrequencies.size());
		final OrderFrequency expectedOrderFrequency = orderService.getOrderFrequency(100);
		assertEquals(expectedOrderFrequency, orderFrequencies.get(0));
		
		//Add a new name to the frequency concept so that our search phrase matches on 2
		//concept names for the same frequency concept
		Concept frequencyConcept = expectedOrderFrequency.getConcept();
		final String newConceptName = searchPhrase + " A Day";
		frequencyConcept.addName(new ConceptName(newConceptName, locale));
		frequencyConcept.addDescription(new ConceptDescription("some description", null));
		conceptService.saveConcept(frequencyConcept);
		
		orderFrequencies = orderService.getOrderFrequencies(searchPhrase, locale, true, false);
		assertEquals(1, orderFrequencies.size());
		assertEquals(expectedOrderFrequency, orderFrequencies.get(0));
	}
	
	/**
	 * @see OrderService#getOrderFrequencies(String, java.util.Locale, boolean, boolean)
	 */
	@Test
	public void getOrderFrequencies_shouldRejectANullSearchPhrase() {
		expectedException.expect(IllegalArgumentException.class);
		expectedException.expectMessage("searchPhrase is required");
		orderService.getOrderFrequencies(null, Locale.ENGLISH, false, false);
	}
	
	@Test
	public void retireOrderFrequency_shouldRetireGivenOrderFrequency() {
		OrderFrequency orderFrequency = orderService.getOrderFrequency(1);
		assertNotNull(orderFrequency);
		Assert.assertFalse(orderFrequency.getRetired());
		Assert.assertNull(orderFrequency.getRetireReason());
		Assert.assertNull(orderFrequency.getDateRetired());
		
		orderService.retireOrderFrequency(orderFrequency, "retire reason");
		
		orderFrequency = orderService.getOrderFrequency(1);
		assertNotNull(orderFrequency);
		assertTrue(orderFrequency.getRetired());
		assertEquals("retire reason", orderFrequency.getRetireReason());
		assertNotNull(orderFrequency.getDateRetired());
		
		//Should not change the number of order frequencies.
		assertEquals(3, orderService.getOrderFrequencies(true).size());
	}
	
	@Test
	public void unretireOrderFrequency_shouldUnretireGivenOrderFrequency() {
		executeDataSet("org/openmrs/api/include/OrderServiceTest-otherOrderFrequencies.xml");
		OrderFrequency orderFrequency = orderService.getOrderFrequency(103);
		assertNotNull(orderFrequency);
		assertTrue(orderFrequency.getRetired());
		assertNotNull(orderFrequency.getRetireReason());
		assertNotNull(orderFrequency.getDateRetired());

		orderService.unretireOrderFrequency(orderFrequency);
		
		orderFrequency = orderService.getOrderFrequency(103);
		assertNotNull(orderFrequency);
		assertFalse(orderFrequency.getRetired());
		assertNull(orderFrequency.getRetireReason());
		assertNull(orderFrequency.getDateRetired());

		//Should not change the number of order frequencies.
		assertEquals(7, orderService.getOrderFrequencies(true).size());
	}
	
	@Test
	public void purgeOrderFrequency_shouldDeleteGivenOrderFrequency() {
		OrderFrequency orderFrequency = orderService.getOrderFrequency(3);
		assertNotNull(orderFrequency);
		
		orderService.purgeOrderFrequency(orderFrequency);
		
		orderFrequency = orderService.getOrderFrequency(3);
		Assert.assertNull(orderFrequency);
		
		//Should reduce the existing number of order frequencies.
		assertEquals(2, orderService.getOrderFrequencies(true).size());
	}
	
	/**
	 * @see OrderService#saveOrderFrequency(OrderFrequency)
	 */
	@Test
	public void saveOrderFrequency_shouldAddANewOrderFrequencyToTheDatabase() {
		Concept concept = new Concept();
		concept.addName(new ConceptName("new name", Context.getLocale()));
		concept.addDescription(new ConceptDescription("some description", null));
		concept.setDatatype(new ConceptDatatype(1));
		concept.setConceptClass(conceptService.getConceptClassByName("Frequency"));
		concept = conceptService.saveConcept(concept);
		Integer originalSize = orderService.getOrderFrequencies(true).size();
		OrderFrequency orderFrequency = new OrderFrequency();
		orderFrequency.setConcept(concept);
		orderFrequency.setFrequencyPerDay(2d);
		
		orderFrequency = orderService.saveOrderFrequency(orderFrequency);
		
		assertNotNull(orderFrequency.getId());
		assertNotNull(orderFrequency.getUuid());
		assertNotNull(orderFrequency.getCreator());
		assertNotNull(orderFrequency.getDateCreated());
		assertEquals(originalSize + 1, orderService.getOrderFrequencies(true).size());
	}
	
	/**
	 * @see OrderService#saveOrderFrequency(OrderFrequency)
	 */
	@Test
	public void saveOrderFrequency_shouldEditAnExistingOrderFrequencyThatIsNotInUse() {
		executeDataSet(OTHER_ORDER_FREQUENCIES_XML);
		OrderFrequency orderFrequency = orderService.getOrderFrequency(100);
		assertNotNull(orderFrequency);
		
		orderFrequency.setFrequencyPerDay(4d);
		orderService.saveOrderFrequency(orderFrequency);
	}
	
	/**
	 * @see OrderService#saveOrderFrequency(OrderFrequency)
	 */
	@Test
	public void saveOrderFrequency_shouldNotAllowEditingAnExistingOrderFrequencyThatIsInUse() {
		OrderFrequency orderFrequency = orderService.getOrderFrequency(1);
		assertNotNull(orderFrequency);
		
		orderFrequency.setFrequencyPerDay(4d);
		expectedException.expect(CannotUpdateObjectInUseException.class);
		expectedException.expectMessage("Order.frequency.cannot.edit");
		orderService.saveOrderFrequency(orderFrequency);
	}
	
	/**
	 * @see OrderService#purgeOrderFrequency(OrderFrequency)
	 */
	@Test
	public void purgeOrderFrequency_shouldNotAllowDeletingAnOrderFrequencyThatIsInUse() {
		OrderFrequency orderFrequency = orderService.getOrderFrequency(1);
		assertNotNull(orderFrequency);
		
		expectedException.expect(CannotDeleteObjectInUseException.class);
		expectedException.expectMessage(mss.getMessage("Order.frequency.cannot.delete"));
		orderService.purgeOrderFrequency(orderFrequency);
	}
	
	@Test
	public void saveOrderWithScheduledDate_shouldAddANewOrderWithScheduledDateToTheDatabase() {
		Date scheduledDate = new Date();
		Order order = new Order();
		order.setAction(Action.NEW);
		order.setPatient(Context.getPatientService().getPatient(7));
		order.setConcept(Context.getConceptService().getConcept(5497));
		order.setCareSetting(orderService.getCareSetting(1));
		order.setOrderer(orderService.getOrder(1).getOrderer());
		order.setEncounter(encounterService.getEncounter(3));
		order.setDateActivated(new Date());
		order.setScheduledDate(scheduledDate);
		order.setUrgency(Order.Urgency.ON_SCHEDULED_DATE);
		order.setEncounter(encounterService.getEncounter(3));
		order.setOrderType(orderService.getOrderType(17));
		order = orderService.saveOrder(order, null);
		Order newOrder = orderService.getOrder(order.getOrderId());
		assertNotNull(order);
		assertEquals(DateUtil.truncateToSeconds(scheduledDate), order.getScheduledDate());
		assertNotNull(newOrder);
		assertEquals(DateUtil.truncateToSeconds(scheduledDate), newOrder.getScheduledDate());
	}
	
	/**
	 * @see OrderService#saveOrder(org.openmrs.Order, OrderContext)
	 */
	@Test
	public void saveOrder_shouldSetOrderNumberSpecifiedInTheContextIfSpecified() {
		GlobalProperty gp = new GlobalProperty(OpenmrsConstants.GP_ORDER_NUMBER_GENERATOR_BEAN_ID,
		        "orderEntry.OrderNumberGenerator");
		Context.getAdministrationService().saveGlobalProperty(gp);
		Order order = new TestOrder();
		order.setEncounter(encounterService.getEncounter(6));
		order.setPatient(patientService.getPatient(7));
		order.setConcept(conceptService.getConcept(5497));
		order.setOrderer(providerService.getProvider(1));
		order.setCareSetting(orderService.getCareSetting(1));
		order.setOrderType(orderService.getOrderType(2));
		order.setEncounter(encounterService.getEncounter(3));
		order.setDateActivated(new Date());
		OrderContext orderCtxt = new OrderContext();
		final String expectedOrderNumber = "Testing";
		orderCtxt.setAttribute(TimestampOrderNumberGenerator.NEXT_ORDER_NUMBER, expectedOrderNumber);
		order = orderService.saveOrder(order, orderCtxt);
		assertEquals(expectedOrderNumber, order.getOrderNumber());
	}
	
	/**
	 * @see OrderService#saveOrder(org.openmrs.Order, OrderContext)
	 */
	@Test
	public void saveOrder_shouldSetTheOrderNumberReturnedByTheConfiguredGenerator() {
		GlobalProperty gp = new GlobalProperty(OpenmrsConstants.GP_ORDER_NUMBER_GENERATOR_BEAN_ID,
		        "orderEntry.OrderNumberGenerator");
		Context.getAdministrationService().saveGlobalProperty(gp);
		Order order = new TestOrder();
		order.setPatient(patientService.getPatient(7));
		order.setConcept(conceptService.getConcept(5497));
		order.setOrderer(providerService.getProvider(1));
		order.setCareSetting(orderService.getCareSetting(1));
		order.setOrderType(orderService.getOrderType(2));
		order.setEncounter(encounterService.getEncounter(3));
		order.setDateActivated(new Date());
		order = orderService.saveOrder(order, null);
		assertTrue(order.getOrderNumber().startsWith(TimestampOrderNumberGenerator.ORDER_NUMBER_PREFIX));
	}
	
	/**
	 * @see OrderService#saveOrder(org.openmrs.Order, OrderContext)
	 */
	@Test
	@Ignore("Ignored because it fails after removal of deprecated methods TRUNK-4772")
	public void saveOrder_shouldFailForRevisionOrderIfAnActiveDrugOrderForTheSameConceptAndCareSettingsExists()
	{
		final Patient patient = patientService.getPatient(2);
		final Concept aspirin = conceptService.getConcept(88);
		DrugOrder firstOrder = new DrugOrder();
		firstOrder.setPatient(patient);
		firstOrder.setConcept(aspirin);
		firstOrder.setEncounter(encounterService.getEncounter(6));
		firstOrder.setOrderer(providerService.getProvider(1));
		firstOrder.setCareSetting(orderService.getCareSetting(2));
		firstOrder.setDrug(conceptService.getDrug(3));
		firstOrder.setDateActivated(new Date());
		firstOrder.setAutoExpireDate(DateUtils.addDays(new Date(), 10));
		firstOrder.setDosingType(FreeTextDosingInstructions.class);
		firstOrder.setDosingInstructions("2 for 5 days");
		firstOrder.setQuantity(10.0);
		firstOrder.setQuantityUnits(conceptService.getConcept(51));
		firstOrder.setNumRefills(0);
		orderService.saveOrder(firstOrder, null);
		
		//New order in future for same concept and care setting
		DrugOrder secondOrder = new DrugOrder();
		secondOrder.setPatient(firstOrder.getPatient());
		secondOrder.setConcept(firstOrder.getConcept());
		secondOrder.setEncounter(encounterService.getEncounter(6));
		secondOrder.setOrderer(providerService.getProvider(1));
		secondOrder.setCareSetting(firstOrder.getCareSetting());
		secondOrder.setDrug(conceptService.getDrug(3));
		secondOrder.setDateActivated(new Date());
		secondOrder.setScheduledDate(DateUtils.addDays(firstOrder.getEffectiveStopDate(), 1));
		secondOrder.setUrgency(Order.Urgency.ON_SCHEDULED_DATE);
		secondOrder.setDosingType(FreeTextDosingInstructions.class);
		secondOrder.setDosingInstructions("2 for 5 days");
		secondOrder.setQuantity(10.0);
		secondOrder.setQuantityUnits(conceptService.getConcept(51));
		secondOrder.setNumRefills(0);
		orderService.saveOrder(secondOrder, null);
		
		//Revise second order to have scheduled date overlapping with active order
		DrugOrder revision = secondOrder.cloneForRevision();
		revision.setScheduledDate(DateUtils.addDays(firstOrder.getEffectiveStartDate(), 2));
		revision.setEncounter(encounterService.getEncounter(6));
		revision.setOrderer(providerService.getProvider(1));
		
		expectedException.expect(APIException.class);
		expectedException.expectMessage("Order.cannot.have.more.than.one");
		orderService.saveOrder(revision, null);
	}
	
	/**
	 *           settings exists
	 * @see OrderService#saveOrder(Order, OrderContext)
	 */
	@Test
	@Ignore("Ignored because it fails after removal of deprecated methods TRUNK-4772")
	public void saveOrder_shouldPassForRevisionOrderIfAnActiveTestOrderForTheSameConceptAndCareSettingsExists()
	{
		final Patient patient = patientService.getPatient(2);
		final Concept cd4Count = conceptService.getConcept(5497);
		TestOrder activeOrder = new TestOrder();
		activeOrder.setPatient(patient);
		activeOrder.setConcept(cd4Count);
		activeOrder.setEncounter(encounterService.getEncounter(6));
		activeOrder.setOrderer(providerService.getProvider(1));
		activeOrder.setCareSetting(orderService.getCareSetting(2));
		activeOrder.setDateActivated(new Date());
		activeOrder.setAutoExpireDate(DateUtils.addDays(new Date(), 10));
		orderService.saveOrder(activeOrder, null);
		
		//New order in future for same concept
		TestOrder secondOrder = new TestOrder();
		secondOrder.setPatient(activeOrder.getPatient());
		secondOrder.setConcept(activeOrder.getConcept());
		secondOrder.setEncounter(encounterService.getEncounter(6));
		secondOrder.setOrderer(providerService.getProvider(1));
		secondOrder.setCareSetting(activeOrder.getCareSetting());
		secondOrder.setDateActivated(new Date());
		secondOrder.setScheduledDate(DateUtils.addDays(activeOrder.getEffectiveStopDate(), 1));
		secondOrder.setUrgency(Order.Urgency.ON_SCHEDULED_DATE);
		orderService.saveOrder(secondOrder, null);
		
		//Revise second order to have scheduled date overlapping with active order
		TestOrder revision = secondOrder.cloneForRevision();
		revision.setScheduledDate(DateUtils.addDays(activeOrder.getEffectiveStartDate(), 2));
		revision.setEncounter(encounterService.getEncounter(6));
		revision.setOrderer(providerService.getProvider(1));
		
		Order savedSecondOrder = orderService.saveOrder(revision, null);
		
		assertNotNull(orderService.getOrder(savedSecondOrder.getOrderId()));
	}
	
	/**
	 * @see OrderService#saveOrder(Order, OrderContext)
	 */
	@Test
	public void saveOrder_shouldFailIfAnActiveDrugOrderForTheSameConceptAndCareSettingExists() {
		final Patient patient = patientService.getPatient(2);
		final Concept triomuneThirty = conceptService.getConcept(792);
		//sanity check that we have an active order for the same concept
		DrugOrder duplicateOrder = (DrugOrder) orderService.getOrder(3);
		assertTrue(duplicateOrder.isActive());
		assertEquals(triomuneThirty, duplicateOrder.getConcept());
		
		DrugOrder drugOrder = new DrugOrder();
		drugOrder.setPatient(patient);
		drugOrder.setCareSetting(orderService.getCareSetting(1));
		drugOrder.setConcept(triomuneThirty);
		drugOrder.setEncounter(encounterService.getEncounter(6));
		drugOrder.setOrderer(providerService.getProvider(1));
		drugOrder.setCareSetting(duplicateOrder.getCareSetting());
		drugOrder.setDrug(duplicateOrder.getDrug());
		drugOrder.setDose(duplicateOrder.getDose());
		drugOrder.setDoseUnits(duplicateOrder.getDoseUnits());
		drugOrder.setRoute(duplicateOrder.getRoute());
		drugOrder.setFrequency(duplicateOrder.getFrequency());
		drugOrder.setQuantity(duplicateOrder.getQuantity());
		drugOrder.setQuantityUnits(duplicateOrder.getQuantityUnits());
		drugOrder.setNumRefills(duplicateOrder.getNumRefills());
		
		expectedException.expect(AmbiguousOrderException.class);
		expectedException.expectMessage("Order.cannot.have.more.than.one");
		orderService.saveOrder(drugOrder, null);
	}
	
	/**
	 * @see OrderService#saveOrder(org.openmrs.Order, OrderContext)
	 */
	@Test
	public void saveOrder_shouldPassIfAnActiveTestOrderForTheSameConceptAndCareSettingExists() {
		final Patient patient = patientService.getPatient(2);
		final Concept cd4Count = conceptService.getConcept(5497);
		//sanity check that we have an active order for the same concept
		TestOrder duplicateOrder = (TestOrder) orderService.getOrder(7);
		assertTrue(duplicateOrder.isActive());
		assertEquals(cd4Count, duplicateOrder.getConcept());
		
		Order order = new TestOrder();
		order.setPatient(patient);
		order.setCareSetting(orderService.getCareSetting(2));
		order.setConcept(cd4Count);
		order.setEncounter(encounterService.getEncounter(6));
		order.setOrderer(providerService.getProvider(1));
		order.setCareSetting(duplicateOrder.getCareSetting());
		
		Order savedOrder = orderService.saveOrder(order, null);
		
		assertNotNull(orderService.getOrder(savedOrder.getOrderId()));
	}
	
	/**
	 * @see OrderService#saveOrder(org.openmrs.Order, OrderContext)
	 */
	@Test
	@Ignore("Ignored because it fails after removal of deprecated methods TRUNK-4772")
	public void saveOrder_shouldSaveRevisionOrderScheduledOnDateNotOverlappingWithAnActiveOrderForTheSameConceptAndCareSetting()
	{
		//sanity check that we have an active order
		final Patient patient = patientService.getPatient(2);
		final Concept cd4Count = conceptService.getConcept(5497);
		TestOrder activeOrder = new TestOrder();
		activeOrder.setPatient(patient);
		activeOrder.setConcept(cd4Count);
		activeOrder.setEncounter(encounterService.getEncounter(6));
		activeOrder.setOrderer(providerService.getProvider(1));
		activeOrder.setCareSetting(orderService.getCareSetting(2));
		activeOrder.setDateActivated(new Date());
		activeOrder.setAutoExpireDate(DateUtils.addDays(new Date(), 10));
		orderService.saveOrder(activeOrder, null);
		
		//New Drug order in future for same concept
		TestOrder secondOrder = new TestOrder();
		secondOrder.setPatient(activeOrder.getPatient());
		secondOrder.setConcept(activeOrder.getConcept());
		secondOrder.setEncounter(encounterService.getEncounter(6));
		secondOrder.setOrderer(providerService.getProvider(1));
		secondOrder.setCareSetting(activeOrder.getCareSetting());
		secondOrder.setDateActivated(new Date());
		secondOrder.setScheduledDate(DateUtils.addDays(activeOrder.getEffectiveStopDate(), 1));
		secondOrder.setUrgency(Order.Urgency.ON_SCHEDULED_DATE);
		orderService.saveOrder(secondOrder, null);
		
		//Revise Second Order to have scheduled date not overlapping with active order
		TestOrder revision = secondOrder.cloneForRevision();
		revision.setScheduledDate(DateUtils.addDays(activeOrder.getEffectiveStopDate(), 2));
		revision.setEncounter(encounterService.getEncounter(6));
		revision.setOrderer(providerService.getProvider(1));
		
		Order savedRevisionOrder = orderService.saveOrder(revision, null);
		
		assertNotNull(orderService.getOrder(savedRevisionOrder.getOrderId()));
	}
	
	/**
	 * @see OrderService#saveOrder(org.openmrs.Order, OrderContext)
	 */
	@Test
	public void saveOrder_shouldPassIfAnActiveDrugOrderForTheSameConceptAndCareSettingButDifferentFormulationExists()
	{
		executeDataSet("org/openmrs/api/include/OrderServiceTest-drugOrdersWithSameConceptAndDifferentFormAndStrength.xml");
		final Patient patient = patientService.getPatient(2);
		//sanity check that we have an active order
		DrugOrder existingOrder = (DrugOrder) orderService.getOrder(1000);
		assertTrue(existingOrder.isActive());
		//New Drug order
		DrugOrder order = new DrugOrder();
		order.setPatient(patient);
		order.setConcept(existingOrder.getConcept());
		order.setEncounter(encounterService.getEncounter(6));
		order.setOrderer(providerService.getProvider(1));
		order.setCareSetting(existingOrder.getCareSetting());
		order.setDrug(conceptService.getDrug(3001));
		order.setDosingType(FreeTextDosingInstructions.class);
		order.setDosingInstructions("2 for 5 days");
		order.setQuantity(10.0);
		order.setQuantityUnits(conceptService.getConcept(51));
		order.setNumRefills(2);
		
		Order savedDrugOrder = orderService.saveOrder(order, null);
		
		assertNotNull(orderService.getOrder(savedDrugOrder.getOrderId()));
	}
	
	/**
	 * @see OrderService#saveOrder(org.openmrs.Order, OrderContext)
	 */
	@Test
	public void saveOrder_shouldThrowAmbiguousOrderExceptionIfAnActiveDrugOrderForTheSameDrugFormulationExists()
	{
		executeDataSet("org/openmrs/api/include/OrderServiceTest-drugOrdersWithSameConceptAndDifferentFormAndStrength.xml");
		final Patient patient = patientService.getPatient(2);
		//sanity check that we have an active order for the same concept
		DrugOrder existingOrder = (DrugOrder) orderService.getOrder(1000);
		assertTrue(existingOrder.isActive());
		
		//New Drug order
		DrugOrder order = new DrugOrder();
		order.setPatient(patient);
		order.setDrug(existingOrder.getDrug());
		order.setEncounter(encounterService.getEncounter(6));
		order.setOrderer(providerService.getProvider(1));
		order.setCareSetting(existingOrder.getCareSetting());
		order.setDosingType(FreeTextDosingInstructions.class);
		order.setDosingInstructions("2 for 5 days");
		order.setQuantity(10.0);
		order.setQuantityUnits(conceptService.getConcept(51));
		order.setNumRefills(2);
		
		expectedException.expect(AmbiguousOrderException.class);
		expectedException.expectMessage("Order.cannot.have.more.than.one");
		orderService.saveOrder(order, null);
	}
	
	/**
	 * @see OrderService#saveOrder(org.openmrs.Order, OrderContext)
	 */
	@Test
	public void saveOrder_shouldPassIfAnActiveOrderForTheSameConceptExistsInADifferentCareSetting() {
		final Patient patient = patientService.getPatient(2);
		final Concept cd4Count = conceptService.getConcept(5497);
		TestOrder duplicateOrder = (TestOrder) orderService.getOrder(7);
		final CareSetting inpatient = orderService.getCareSetting(2);
		assertNotEquals(inpatient, duplicateOrder.getCareSetting());
		assertTrue(duplicateOrder.isActive());
		assertEquals(cd4Count, duplicateOrder.getConcept());
		int initialActiveOrderCount = orderService.getActiveOrders(patient, null, null, null).size();
		
		TestOrder order = new TestOrder();
		order.setPatient(patient);
		order.setCareSetting(orderService.getCareSetting(2));
		order.setConcept(cd4Count);
		order.setEncounter(encounterService.getEncounter(6));
		order.setOrderer(providerService.getProvider(1));
		order.setCareSetting(inpatient);
		
		orderService.saveOrder(order, null);
		List<Order> activeOrders = orderService.getActiveOrders(patient, null, null, null);
		assertEquals(++initialActiveOrderCount, activeOrders.size());
	}
	
	/**
	 * @throws ParseException
	 * @see OrderService#saveOrder(org.openmrs.Order, OrderContext)
	 */
	@Test
	public void saveOrder_shouldRollTheAutoExpireDateToTheEndOfTheDayIfItHasNoTimeComponent() throws ParseException {
		Order order = new TestOrder();
		order.setPatient(patientService.getPatient(2));
		order.setCareSetting(orderService.getCareSetting(2));
		order.setConcept(conceptService.getConcept(5089));
		order.setEncounter(encounterService.getEncounter(6));
		order.setOrderer(providerService.getProvider(1));
		DateFormat dateformat = new SimpleDateFormat("dd/MM/yyyy");
		order.setDateActivated(dateformat.parse("14/08/2014"));
		order.setAutoExpireDate(dateformat.parse("18/08/2014"));
		
		orderService.saveOrder(order, null);
		dateformat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss.S");
		assertEquals(dateformat.parse("18/08/2014 23:59:59.000"), order.getAutoExpireDate());
	}
	
	/**
	 * @throws ParseException
	 * @see OrderService#saveOrder(org.openmrs.Order, OrderContext)
	 */
	@Test
	public void saveOrder_shouldNotChangeTheAutoExpireDateIfItHasATimeComponent() throws ParseException {
		Order order = new TestOrder();
		order.setPatient(patientService.getPatient(2));
		order.setCareSetting(orderService.getCareSetting(2));
		order.setConcept(conceptService.getConcept(5089));
		order.setEncounter(encounterService.getEncounter(6));
		order.setOrderer(providerService.getProvider(1));
		order.setDateActivated(new Date());
		DateFormat dateformat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		order.setDateActivated(dateformat.parse("14/08/2014 10:00:00"));
		Date autoExpireDate = dateformat.parse("18/08/2014 10:00:00");
		order.setAutoExpireDate(autoExpireDate);
		
		orderService.saveOrder(order, null);
		assertEquals(autoExpireDate, order.getAutoExpireDate());
	}
	
	/**
	 * @see OrderService#saveOrder(org.openmrs.Order, OrderContext)
	 */
	@Test
	public void saveOrder_shouldPassIfAnActiveDrugOrderForTheSameDrugFormulationExistsBeyondSchedule() {
		executeDataSet("org/openmrs/api/include/OrderServiceTest-DrugOrders.xml");
		final Patient patient = patientService.getPatient(2);
		
		DrugOrder existingOrder = (DrugOrder) orderService.getOrder(2000);
		int initialActiveOrderCount = orderService.getActiveOrders(patient, null, null, null).size();
		
		//New Drug order
		DrugOrder order = new DrugOrder();
		order.setPatient(patient);
		order.setDrug(existingOrder.getDrug());
		order.setEncounter(encounterService.getEncounter(6));
		order.setOrderer(providerService.getProvider(1));
		order.setCareSetting(existingOrder.getCareSetting());
		order.setDosingType(FreeTextDosingInstructions.class);
		order.setDosingInstructions("2 for 10 days");
		order.setQuantity(10.0);
		order.setQuantityUnits(conceptService.getConcept(51));
		order.setNumRefills(2);
		order.setUrgency(Order.Urgency.ON_SCHEDULED_DATE);
		
		order.setScheduledDate(DateUtils.addDays(existingOrder.getDateStopped(), 1));
		
		orderService.saveOrder(order, null);
		List<Order> activeOrders = orderService.getActiveOrders(patient, null, null, null);
		assertEquals(++initialActiveOrderCount, activeOrders.size());
	}
	
	/**
	 * @see OrderService#getOrderType(Integer)
	 */
	@Test
	public void getOrderType_shouldFindOrderTypeObjectGivenValidId() {
		assertEquals("Drug order", orderService.getOrderType(1).getName());
	}
	
	/**
	 * @see OrderService#getOrderType(Integer)
	 */
	@Test
	public void getOrderType_shouldReturnNullIfNoOrderTypeObjectFoundWithGivenId() {
		OrderType orderType = orderService.getOrderType(1000);
		assertNull(orderType);
	}
	
	/**
	 * @see OrderService#getOrderTypeByUuid(String)
	 */
	@Test
	public void getOrderTypeByUuid_shouldFindOrderTypeObjectGivenValidUuid() {
		OrderType orderType = orderService.getOrderTypeByUuid("131168f4-15f5-102d-96e4-000c29c2a5d7");
		assertEquals("Drug order", orderType.getName());
	}
	
	/**
	 * @see OrderService#getOrderTypeByUuid(String)
	 */
	@Test
	public void getOrderTypeByUuid_shouldReturnNullIfNoOrderTypeObjectFoundWithGivenUuid() {
		assertNull(orderService.getOrderTypeByUuid("some random uuid"));
	}
	
	/**
	 * @see OrderService#getOrderTypes(boolean)
	 */
	@Test
	public void getOrderTypes_shouldGetAllOrderTypesIfIncludeRetiredIsSetToTrue() {
		assertEquals(14, orderService.getOrderTypes(true).size());
	}
	
	/**
	 * @see OrderService#getOrderTypes(boolean)
	 */
	@Test
	public void getOrderTypes_shouldGetAllNonRetiredOrderTypesIfIncludeRetiredIsSetToFalse() {
		assertEquals(11, orderService.getOrderTypes(false).size());
	}
	
	/**
	 * @see OrderService#getOrderTypeByName(String)
	 */
	@Test
	public void getOrderTypeByName_shouldReturnTheOrderTypeThatMatchesTheSpecifiedName() {
		OrderType orderType = orderService.getOrderTypeByName("Drug order");
		assertEquals("131168f4-15f5-102d-96e4-000c29c2a5d7", orderType.getUuid());
	}
	
	/**
	 * @see OrderService#getOrders(org.openmrs.Patient, org.openmrs.CareSetting,
	 *      org.openmrs.OrderType, boolean)
	 */
	@Test
	public void getOrders_shouldFailIfPatientIsNull() {
		expectedException.expect(IllegalArgumentException.class);
		expectedException.expectMessage("Patient is required");
		orderService.getOrders(null, null, null, false);
	}
	
	/**
	 * @see OrderService#getOrders(org.openmrs.Patient, org.openmrs.CareSetting,
	 *      org.openmrs.OrderType, boolean)
	 */
	@Test
	public void getOrders_shouldFailIfCareSettingIsNull() {
		expectedException.expect(IllegalArgumentException.class);
		expectedException.expectMessage("CareSetting is required");
		orderService.getOrders(new Patient(), null, null, false);
	}
	
	/**
	 * @see OrderService#getOrders(org.openmrs.Patient, org.openmrs.CareSetting,
	 *      org.openmrs.OrderType, boolean)
	 */
	@Test
	public void getOrders_shouldGetTheOrdersThatMatchAllTheArguments() {
		Patient patient = patientService.getPatient(2);
		CareSetting outPatient = orderService.getCareSetting(1);
		OrderType testOrderType = orderService.getOrderType(2);
		List<Order> testOrders = orderService.getOrders(patient, outPatient, testOrderType, false);
		assertEquals(3, testOrders.size());
		TestUtil.containsId(testOrders, 6);
		TestUtil.containsId(testOrders, 7);
		TestUtil.containsId(testOrders, 9);
		
		OrderType drugOrderType = orderService.getOrderType(1);
		List<Order> drugOrders = orderService.getOrders(patient, outPatient, drugOrderType, false);
		assertEquals(5, drugOrders.size());
		TestUtil.containsId(drugOrders, 2);
		TestUtil.containsId(drugOrders, 3);
		TestUtil.containsId(drugOrders, 44);
		TestUtil.containsId(drugOrders, 444);
		TestUtil.containsId(drugOrders, 5);
		
		CareSetting inPatient = orderService.getCareSetting(2);
		List<Order> inPatientDrugOrders = orderService.getOrders(patient, inPatient, drugOrderType, false);
		assertEquals(222, inPatientDrugOrders.get(0).getOrderId().intValue());
	}
	
	/**
	 * @see OrderService#getOrders(org.openmrs.Patient, org.openmrs.CareSetting,
	 *      org.openmrs.OrderType, boolean)
	 */
	@Test
	public void getOrders_shouldGetAllUnvoidedMatchesIfIncludeVoidedIsSetToFalse() {
		Patient patient = patientService.getPatient(2);
		CareSetting outPatient = orderService.getCareSetting(1);
		OrderType testOrderType = orderService.getOrderType(2);
		assertEquals(3, orderService.getOrders(patient, outPatient, testOrderType, false).size());
	}
	
	/**
	 * @see OrderService#getOrders(org.openmrs.Patient, org.openmrs.CareSetting,
	 *      org.openmrs.OrderType, boolean)
	 */
	@Test
	public void getOrders_shouldIncludeVoidedMatchesIfIncludeVoidedIsSetToTrue() {
		Patient patient = patientService.getPatient(2);
		CareSetting outPatient = orderService.getCareSetting(1);
		OrderType testOrderType = orderService.getOrderType(2);
		assertEquals(4, orderService.getOrders(patient, outPatient, testOrderType, true).size());
	}
	
	/**
	 * @see OrderService#getOrders(org.openmrs.Patient, org.openmrs.CareSetting,
	 *      org.openmrs.OrderType, boolean)
	 */
	@Test
	public void getOrders_shouldIncludeOrdersForSubTypesIfOrderTypeIsSpecified() {
		executeDataSet("org/openmrs/api/include/OrderServiceTest-otherOrders.xml");
		Patient patient = patientService.getPatient(2);
		OrderType testOrderType = orderService.getOrderType(2);
		CareSetting outPatient = orderService.getCareSetting(1);
		List<Order> orders = orderService.getOrders(patient, outPatient, testOrderType, false);
		assertEquals(7, orders.size());
		Order[] expectedOrder1 = { orderService.getOrder(6), orderService.getOrder(7), orderService.getOrder(9),
		        orderService.getOrder(101), orderService.getOrder(102), orderService.getOrder(103),
		        orderService.getOrder(104) };
		assertThat(orders, hasItems(expectedOrder1));
		
		OrderType labTestOrderType = orderService.getOrderType(7);
		orders = orderService.getOrders(patient, outPatient, labTestOrderType, false);
		assertEquals(3, orderService.getOrders(patient, outPatient, labTestOrderType, false).size());
		Order[] expectedOrder2 = { orderService.getOrder(101), orderService.getOrder(103), orderService.getOrder(104) };
		assertThat(orders, hasItems(expectedOrder2));
	}
	
	/**
	 * @see OrderService#getAllOrdersByPatient(org.openmrs.Patient)
	 */
	@Test
	public void getAllOrdersByPatient_shouldFailIfPatientIsNull() {
		expectedException.expect(IllegalArgumentException.class);
		expectedException.expectMessage("Patient is required");
		orderService.getAllOrdersByPatient(null);
	}
	
	/**
	 * @see OrderService#getAllOrdersByPatient(org.openmrs.Patient)
	 */
	@Test
	public void getAllOrdersByPatient_shouldGetAllTheOrdersForTheSpecifiedPatient() {
		assertEquals(12, orderService.getAllOrdersByPatient(patientService.getPatient(2)).size());
		assertEquals(2, orderService.getAllOrdersByPatient(patientService.getPatient(7)).size());
	}
	
	/**
	 * @see OrderService#saveOrder(org.openmrs.Order, OrderContext)
	 */
	@Test
	public void saveOrder_shouldSetOrderTypeIfNullButMappedToTheConceptClass() {
		TestOrder order = new TestOrder();
		order.setPatient(patientService.getPatient(7));
		order.setConcept(conceptService.getConcept(5497));
		order.setOrderer(providerService.getProvider(1));
		order.setCareSetting(orderService.getCareSetting(1));
		order.setEncounter(encounterService.getEncounter(3));
		order.setDateActivated(new Date());
		orderService.saveOrder(order, null);
		assertEquals(2, order.getOrderType().getOrderTypeId().intValue());
	}
	
	/**
	 * @see OrderService#saveOrder(org.openmrs.Order, OrderContext)
	 */
	@Test
	public void saveOrder_shouldFailIfOrderTypeIsNullAndNotMappedToTheConceptClass() {
		Order order = new Order();
		order.setPatient(patientService.getPatient(7));
		order.setConcept(conceptService.getConcept(9));
		order.setOrderer(providerService.getProvider(1));
		order.setCareSetting(orderService.getCareSetting(1));
		order.setEncounter(encounterService.getEncounter(3));
		order.setDateActivated(new Date());
		expectedException.expect(OrderEntryException.class);
		expectedException.expectMessage("Order.type.cannot.determine");
		orderService.saveOrder(order, null);
	}
	
	/**
	 * @see OrderService#saveOrderType(org.openmrs.OrderType)
	 */
	@Test
	public void saveOrderType_shouldAddANewOrderTypeToTheDatabase() {
		int orderTypeCount = orderService.getOrderTypes(true).size();
		OrderType orderType = new OrderType();
		orderType.setName("New Order");
		orderType.setJavaClassName("org.openmrs.NewTestOrder");
		orderType.setDescription("New order type for testing");
		orderType.setRetired(false);
		orderType = orderService.saveOrderType(orderType);
		assertNotNull(orderType);
		assertEquals("New Order", orderType.getName());
		assertNotNull(orderType.getId());
		assertEquals((orderTypeCount + 1), orderService.getOrderTypes(true).size());
	}
	
	/**
	 * @see OrderService#saveOrderType(org.openmrs.OrderType)
	 */
	@Test
	public void saveOrderType_shouldEditAnExistingOrderType() {
		OrderType orderType = orderService.getOrderType(1);
		assertNull(orderType.getDateChanged());
		assertNull(orderType.getChangedBy());
		final String newDescription = "new";
		orderType.setDescription(newDescription);
		
		orderService.saveOrderType(orderType);
		Context.flushSession();
		assertNotNull(orderType.getDateChanged());
		assertNotNull(orderType.getChangedBy());
	}
	
	/**
	 * @see OrderService#purgeOrderType(org.openmrs.OrderType)
	 */
	@Test
	public void purgeOrderType_shouldDeleteOrderTypeIfNotInUse() {
		final Integer id = 13;
		OrderType orderType = orderService.getOrderType(id);
		assertNotNull(orderType);
		orderService.purgeOrderType(orderType);
		assertNull(orderService.getOrderType(id));
	}
	
	/**
	 * @see OrderService#purgeOrderType(org.openmrs.OrderType)
	 */
	@Test
	public void purgeOrderType_shouldNotAllowDeletingAnOrderTypeThatIsInUse() {
		OrderType orderType = orderService.getOrderType(1);
		assertNotNull(orderType);
		expectedException.expect(CannotDeleteObjectInUseException.class);
		expectedException.expectMessage(mss.getMessage("Order.type.cannot.delete"));
		orderService.purgeOrderType(orderType);
	}
	
	/**
	 * @see OrderService#retireOrderType(org.openmrs.OrderType, String)
	 */
	@Test
	public void retireOrderType_shouldRetireOrderType() {
		OrderType orderType = orderService.getOrderType(15);
		assertFalse(orderType.getRetired());
		assertNull(orderType.getRetiredBy());
		assertNull(orderType.getRetireReason());
		assertNull(orderType.getDateRetired());
		orderService.retireOrderType(orderType, "Retire for testing purposes");
		orderType = orderService.getOrderType(15);
		assertTrue(orderType.getRetired());
		assertNotNull(orderType.getRetiredBy());
		assertNotNull(orderType.getRetireReason());
		assertNotNull(orderType.getDateRetired());
	}
	
	/**
	 * @see OrderService#unretireOrderType(org.openmrs.OrderType)
	 */
	@Test
	public void unretireOrderType_shouldUnretireOrderType() {
		OrderType orderType = orderService.getOrderType(16);
		assertTrue(orderType.getRetired());
		assertNotNull(orderType.getRetiredBy());
		assertNotNull(orderType.getRetireReason());
		assertNotNull(orderType.getDateRetired());
		orderService.unretireOrderType(orderType);
		orderType = orderService.getOrderType(16);
		assertFalse(orderType.getRetired());
		assertNull(orderType.getRetiredBy());
		assertNull(orderType.getRetireReason());
		assertNull(orderType.getDateRetired());
	}
	
	/**
	 * @see OrderService#getSubtypes(org.openmrs.OrderType, boolean)
	 */
	@Test
	public void getOrderSubTypes_shouldGetAllSubOrderTypesWithRetiredOrderTypes() {
		List<OrderType> orderTypeList = orderService.getSubtypes(orderService.getOrderType(2), true);
		assertEquals(7, orderTypeList.size());
	}
	
	/**
	 * @see OrderService#getSubtypes(org.openmrs.OrderType, boolean)
	 */
	@Test
	public void getOrderSubTypes_shouldGetAllSubOrderTypesWithoutRetiredOrderTypes() {
		List<OrderType> orderTypeList = orderService.getSubtypes(orderService.getOrderType(2), false);
		assertEquals(6, orderTypeList.size());
	}
	
	/**
	 * @see OrderService#saveOrder(org.openmrs.Order, OrderContext)
	 */
	@Test
	public void saveOrder_shouldDefaultToCareSettingAndOrderTypeDefinedInTheOrderContextIfNull() {
		Order order = new TestOrder();
		order.setPatient(patientService.getPatient(7));
		Concept trimune30 = conceptService.getConcept(792);
		order.setConcept(trimune30);
		order.setOrderer(providerService.getProvider(1));
		order.setEncounter(encounterService.getEncounter(3));
		order.setDateActivated(new Date());
		OrderType expectedOrderType = orderService.getOrderType(2);
		CareSetting expectedCareSetting = orderService.getCareSetting(1);
		OrderContext orderContext = new OrderContext();
		orderContext.setOrderType(expectedOrderType);
		orderContext.setCareSetting(expectedCareSetting);
		order = orderService.saveOrder(order, orderContext);
		assertFalse(expectedOrderType.getConceptClasses().contains(trimune30.getConceptClass()));
		assertEquals(expectedOrderType, order.getOrderType());
		assertEquals(expectedCareSetting, order.getCareSetting());
	}
	
	/**
	 * @see OrderService#getDiscontinuationOrder(Order)
	 */
	@Test
	public void getDiscontinuationOrder_shouldReturnDiscontinuationOrderIfOrderHasBeenDiscontinued() {
		Order order = orderService.getOrder(111);
		Order discontinuationOrder = orderService.discontinueOrder(order, "no reason", new Date(),
		    providerService.getProvider(1), order.getEncounter());
		
		Order foundDiscontinuationOrder = orderService.getDiscontinuationOrder(order);
		
		assertThat(foundDiscontinuationOrder, is(discontinuationOrder));
	}
	
	/**
	 * @see OrderService#getDiscontinuationOrder(Order)
	 */
	@Test
	public void getDiscontinuationOrder_shouldReturnNullIfOrderHasNotBeenDiscontinued() {
		Order order = orderService.getOrder(111);
		Order discontinuationOrder = orderService.getDiscontinuationOrder(order);
		
		assertThat(discontinuationOrder, is(nullValue()));
	}
	
	/**
	 * @see OrderService#getOrderTypeByConceptClass(ConceptClass)
	 */
	@Test
	public void getOrderTypeByConceptClass_shouldGetOrderTypeMappedToTheGivenConceptClass() {
		OrderType orderType = orderService.getOrderTypeByConceptClass(Context.getConceptService().getConceptClass(1));
		
		Assert.assertNotNull(orderType);
		Assert.assertEquals(2, orderType.getOrderTypeId().intValue());
	}
	
	/**
	 * @see OrderService#getOrderTypeByConcept(Concept)
	 */
	@Test
	public void getOrderTypeByConcept_shouldGetOrderTypeMappedToTheGivenConcept() {
		OrderType orderType = orderService.getOrderTypeByConcept(Context.getConceptService().getConcept(5089));
		
		Assert.assertNotNull(orderType);
		Assert.assertEquals(2, orderType.getOrderTypeId().intValue());
	}
	
	/**
	 * @see OrderService#saveOrder(org.openmrs.Order, OrderContext)
	 */
	@Test
	public void saveOrder_shouldFailIfConceptInPreviousOrderDoesNotMatchThatOfTheRevisedOrder() {
		Order previousOrder = orderService.getOrder(7);
		Order order = previousOrder.cloneForRevision();
		order.setDateActivated(new Date());
		order.setOrderer(providerService.getProvider(1));
		order.setEncounter(encounterService.getEncounter(6));
		Concept newConcept = conceptService.getConcept(5089);
		assertFalse(previousOrder.getConcept().equals(newConcept));
		order.setConcept(newConcept);
		
		expectedException.expect(EditedOrderDoesNotMatchPreviousException.class);
		expectedException.expectMessage("The orderable of the previous order and the new one order don't match");
		orderService.saveOrder(order, null);
	}
	
	/**
	 * @see OrderService#saveOrder(org.openmrs.Order, OrderContext)
	 */
	@Test
	public void saveOrder_shouldFailIfTheExistingDrugOrderMatchesTheConceptAndNotDrugOfTheRevisedOrder() {
		final DrugOrder orderToDiscontinue = (DrugOrder) orderService.getOrder(5);
		
		//create a different test drug
		Drug discontinuationOrderDrug = new Drug();
		discontinuationOrderDrug.setConcept(orderToDiscontinue.getConcept());
		discontinuationOrderDrug = conceptService.saveDrug(discontinuationOrderDrug);
		assertNotEquals(discontinuationOrderDrug, orderToDiscontinue.getDrug());
		assertNotNull(orderToDiscontinue.getDrug());
		
		DrugOrder order = orderToDiscontinue.cloneForRevision();
		order.setDateActivated(new Date());
		order.setOrderer(providerService.getProvider(1));
		order.setEncounter(encounterService.getEncounter(6));
		order.setDrug(discontinuationOrderDrug);
		
		expectedException.expect(EditedOrderDoesNotMatchPreviousException.class);
		expectedException.expectMessage("The orderable of the previous order and the new one order don't match");
		orderService.saveOrder(order, null);
	}
	
	/**
	 * @see OrderService#saveOrder(org.openmrs.Order, OrderContext)
	 */
	@Test
	public void saveOrder_shouldFailIfTheOrderTypeOfThePreviousOrderDoesNotMatch() {
		Order order = orderService.getOrder(7);
		assertTrue(OrderUtilTest.isActiveOrder(order, null));
		Order discontinuationOrder = order.cloneForDiscontinuing();
		OrderType orderType = orderService.getOrderType(7);
		assertNotEquals(discontinuationOrder.getOrderType(), orderType);
		assertTrue(OrderUtil.isType(discontinuationOrder.getOrderType(), orderType));
		discontinuationOrder.setOrderType(orderType);
		discontinuationOrder.setOrderer(Context.getProviderService().getProvider(1));
		discontinuationOrder.setEncounter(Context.getEncounterService().getEncounter(6));
		
		expectedException.expect(EditedOrderDoesNotMatchPreviousException.class);
		expectedException.expectMessage(mss.getMessage("Order.type.doesnot.match"));
		orderService.saveOrder(discontinuationOrder, null);
	}
	
	/**
	 * @see OrderService#saveOrder(org.openmrs.Order, OrderContext)
	 */
	@Test
	public void saveOrder_shouldFailIfTheJavaTypeOfThePreviousOrderDoesNotMatch() {
		Order order = orderService.getOrder(7);
		assertTrue(OrderUtilTest.isActiveOrder(order, null));
		Order discontinuationOrder = new SomeTestOrder();
		discontinuationOrder.setCareSetting(order.getCareSetting());
		discontinuationOrder.setConcept(order.getConcept());
		discontinuationOrder.setAction(Action.DISCONTINUE);
		discontinuationOrder.setPreviousOrder(order);
		discontinuationOrder.setPatient(order.getPatient());
		assertTrue(order.getOrderType().getJavaClass().isAssignableFrom(discontinuationOrder.getClass()));
		discontinuationOrder.setOrderType(order.getOrderType());
		discontinuationOrder.setOrderer(Context.getProviderService().getProvider(1));
		discontinuationOrder.setEncounter(Context.getEncounterService().getEncounter(6));
		
		expectedException.expect(EditedOrderDoesNotMatchPreviousException.class);
		expectedException.expectMessage(mss.getMessage("Order.class.doesnot.match"));
		orderService.saveOrder(discontinuationOrder, null);
	}
	
	/**
	 * @see OrderService#saveOrder(org.openmrs.Order, OrderContext)
	 */
	@Test
	public void saveOrder_shouldFailIfTheCareSettingOfThePreviousOrderDoesNotMatch() {
		Order order = orderService.getOrder(7);
		assertTrue(OrderUtilTest.isActiveOrder(order, null));
		Order discontinuationOrder = order.cloneForDiscontinuing();
		CareSetting careSetting = orderService.getCareSetting(2);
		assertNotEquals(discontinuationOrder.getCareSetting(), careSetting);
		discontinuationOrder.setCareSetting(careSetting);
		discontinuationOrder.setOrderer(Context.getProviderService().getProvider(1));
		discontinuationOrder.setEncounter(Context.getEncounterService().getEncounter(6));
		
		expectedException.expect(EditedOrderDoesNotMatchPreviousException.class);
		expectedException.expectMessage(mss.getMessage("Order.care.setting.doesnot.match"));
		orderService.saveOrder(discontinuationOrder, null);
	}
	
	/**
	 * @see OrderService#saveOrder(org.openmrs.Order, OrderContext)
	 */
	@Test
	public void saveOrder_shouldSetConceptForDrugOrdersIfNull() {
		Patient patient = patientService.getPatient(7);
		CareSetting careSetting = orderService.getCareSetting(2);
		OrderType orderType = orderService.getOrderTypeByName("Drug order");
		
		//place drug order
		DrugOrder order = new DrugOrder();
		Encounter encounter = encounterService.getEncounter(3);
		order.setEncounter(encounter);
		order.setPatient(patient);
		order.setDrug(conceptService.getDrug(2));
		order.setCareSetting(careSetting);
		order.setOrderer(Context.getProviderService().getProvider(1));
		order.setDateActivated(encounter.getEncounterDatetime());
		order.setOrderType(orderType);
		order.setDosingType(FreeTextDosingInstructions.class);
		order.setInstructions("None");
		order.setDosingInstructions("Test Instruction");
		orderService.saveOrder(order, null);
		assertNotNull(order.getOrderId());
	}
	
	/**
	 * @see org.openmrs.api.OrderService#getDrugRoutes()
	 */
	@Test
	public void getDrugRoutes_shouldGetDrugRoutesAssociatedConceptPrividedInGlobalProperties() {
		List<Concept> drugRoutesList = orderService.getDrugRoutes();
		assertEquals(1, drugRoutesList.size());
		assertEquals(22, drugRoutesList.get(0).getConceptId().intValue());
	}
	
	/**
	 * @see OrderService#voidOrder(org.openmrs.Order, String)
	 */
	@Test
	public void voidOrder_shouldVoidAnOrder() {
		Order order = orderService.getOrder(1);
		assertFalse(order.getVoided());
		assertNull(order.getDateVoided());
		assertNull(order.getVoidedBy());
		assertNull(order.getVoidReason());
		
		orderService.voidOrder(order, "None");
		assertTrue(order.getVoided());
		assertNotNull(order.getDateVoided());
		assertNotNull(order.getVoidedBy());
		assertNotNull(order.getVoidReason());
	}
	
	/**
	 * @see OrderService#voidOrder(org.openmrs.Order, String)
	 */
	@Test
	public void voidOrder_shouldUnsetDateStoppedOfThePreviousOrderIfTheSpecifiedOrderIsADiscontinuation() {
		Order order = orderService.getOrder(22);
		assertEquals(Action.DISCONTINUE, order.getAction());
		Order previousOrder = order.getPreviousOrder();
		assertNotNull(previousOrder.getDateStopped());
		assertFalse(order.getVoided());
		
		orderService.voidOrder(order, "None");
		//Ensures order interceptor is okay with all the changes
		Context.flushSession();
		assertTrue(order.getVoided());
		assertNull(previousOrder.getDateStopped());
	}
	
	/**
	 * @see OrderService#voidOrder(org.openmrs.Order, String)
	 */
	@Test
	public void voidOrder_shouldUnsetDateStoppedOfThePreviousOrderIfTheSpecifiedOrderIsARevision() {
		Order order = orderService.getOrder(111);
		assertEquals(Action.REVISE, order.getAction());
		Order previousOrder = order.getPreviousOrder();
		assertNotNull(previousOrder.getDateStopped());
		assertFalse(order.getVoided());
		
		orderService.voidOrder(order, "None");
		Context.flushSession();
		assertTrue(order.getVoided());
		assertNull(previousOrder.getDateStopped());
	}
	
	/**
	 * @see OrderService#unvoidOrder(org.openmrs.Order)
	 */
	@Test
	public void unvoidOrder_shouldUnvoidAnOrder() {
		Order order = orderService.getOrder(8);
		assertTrue(order.getVoided());
		assertNotNull(order.getDateVoided());
		assertNotNull(order.getVoidedBy());
		assertNotNull(order.getVoidReason());
		
		orderService.unvoidOrder(order);
		assertFalse(order.getVoided());
		assertNull(order.getDateVoided());
		assertNull(order.getVoidedBy());
		assertNull(order.getVoidReason());
	}
	
	/**
	 * @see OrderService#unvoidOrder(org.openmrs.Order)
	 */
	@Test
	public void unvoidOrder_shouldStopThePreviousOrderIfTheSpecifiedOrderIsADiscontinuation() {
		Order order = orderService.getOrder(22);
		assertEquals(Action.DISCONTINUE, order.getAction());
		Order previousOrder = order.getPreviousOrder();
		assertNotNull(previousOrder.getDateStopped());
		assertFalse(order.getVoided());
		
		//void the DC order for testing purposes so we can unvoid it later
		orderService.voidOrder(order, "None");
		Context.flushSession();
		assertTrue(order.getVoided());
		assertNull(previousOrder.getDateStopped());
		
		orderService.unvoidOrder(order);
		Context.flushSession();
		assertFalse(order.getVoided());
		assertNotNull(previousOrder.getDateStopped());
	}
	
	/**
	 * @see OrderService#unvoidOrder(org.openmrs.Order)
	 */
	@Test
	public void unvoidOrder_shouldStopThePreviousOrderIfTheSpecifiedOrderIsARevision() {
		Order order = orderService.getOrder(111);
		assertEquals(Action.REVISE, order.getAction());
		Order previousOrder = order.getPreviousOrder();
		assertNotNull(previousOrder.getDateStopped());
		assertFalse(order.getVoided());
		
		//void the revise order for testing purposes so we can unvoid it later
		orderService.voidOrder(order, "None");
		Context.flushSession();
		assertTrue(order.getVoided());
		assertNull(previousOrder.getDateStopped());
		
		orderService.unvoidOrder(order);
		Context.flushSession();
		assertFalse(order.getVoided());
		assertNotNull(previousOrder.getDateStopped());
	}
	
	/**
	 * @throws InterruptedException
	 * @see OrderService#unvoidOrder(org.openmrs.Order)
	 */
	@Test
	public void unvoidOrder_shouldFailForADiscontinuationOrderIfThePreviousOrderIsInactive() throws InterruptedException {
		Order order = orderService.getOrder(22);
		assertEquals(Action.DISCONTINUE, order.getAction());
		Order previousOrder = order.getPreviousOrder();
		assertNotNull(previousOrder.getDateStopped());
		assertFalse(order.getVoided());
		
		//void the DC order for testing purposes so we can unvoid it later
		orderService.voidOrder(order, "None");
		assertTrue(order.getVoided());
		assertNull(previousOrder.getDateStopped());
		
		//stop the order with a different DC order
		orderService.discontinueOrder(previousOrder, "Testing", null, previousOrder.getOrderer(),
		    previousOrder.getEncounter());
		Thread.sleep(10);
		
		expectedException.expect(CannotUnvoidOrderException.class);
		expectedException.expectMessage(mss.getMessage("Order.action.cannot.unvoid", new Object[] { "discontinuation" },
		    null));
		orderService.unvoidOrder(order);
	}
	
	/**
	 * @throws InterruptedException
	 * @see OrderService#unvoidOrder(org.openmrs.Order)
	 */
	@Test
	public void unvoidOrder_shouldFailForAReviseOrderIfThePreviousOrderIsInactive() throws InterruptedException {
		Order order = orderService.getOrder(111);
		assertEquals(Action.REVISE, order.getAction());
		Order previousOrder = order.getPreviousOrder();
		assertNotNull(previousOrder.getDateStopped());
		assertFalse(order.getVoided());
		
		//void the DC order for testing purposes so we can unvoid it later
		orderService.voidOrder(order, "None");
		assertTrue(order.getVoided());
		assertNull(previousOrder.getDateStopped());
		
		//stop the order with a different REVISE order
		Order revise = previousOrder.cloneForRevision();
		revise.setOrderer(order.getOrderer());
		revise.setEncounter(order.getEncounter());
		orderService.saveOrder(revise, null);
		Thread.sleep(10);
		
		expectedException.expect(CannotUnvoidOrderException.class);
		expectedException.expectMessage(mss.getMessage("Order.action.cannot.unvoid", new Object[] { "revision" }, null));
		orderService.unvoidOrder(order);
	}
	
	/**
	 * @see OrderService#getRevisionOrder(org.openmrs.Order)
	 */
	@Test
	public void getRevisionOrder_shouldReturnRevisionOrderIfOrderHasBeenRevised() {
		assertEquals(orderService.getOrder(111), orderService.getRevisionOrder(orderService.getOrder(1)));
	}
	
	/**
	 * @see OrderService#getRevisionOrder(org.openmrs.Order)
	 */
	@Test
	public void getRevisionOrder_shouldReturnNullIfOrderHasNotBeenRevised() {
		assertNull(orderService.getRevisionOrder(orderService.getOrder(444)));
	}
	
	/**
	 * @see OrderService#getDiscontinuationOrder(Order)
	 */
	@Test
	public void getDiscontinuationOrder_shouldReturnNullIfDcOrderIsVoided() {
		Order order = orderService.getOrder(7);
		Order discontinueOrder = orderService.discontinueOrder(order, "Some reason", new Date(),
		    providerService.getProvider(1), encounterService.getEncounter(3));
		orderService.voidOrder(discontinueOrder, "Invalid reason");
		
		Order discontinuationOrder = orderService.getDiscontinuationOrder(order);
		assertThat(discontinuationOrder, is(nullValue()));
	}
	
	/**
	 * @see OrderService#getDrugDispensingUnits()
	 */
	@Test
	public void getDrugDispensingUnits_shouldReturnTheUnionOfTheDosingAndDispensingUnits() {
		List<Concept> dispensingUnits = orderService.getDrugDispensingUnits();
		assertEquals(2, dispensingUnits.size());
		assertThat(dispensingUnits, containsInAnyOrder(hasId(50), hasId(51)));
	}
	
	/**
	 * @see OrderService#getDrugDispensingUnits()
	 */
	@Test
	public void getDrugDispensingUnits_shouldReturnAnEmptyListIfNothingIsConfigured() {
		adminService.saveGlobalProperty(new GlobalProperty(OpenmrsConstants.GP_DRUG_DISPENSING_UNITS_CONCEPT_UUID, ""));
		adminService.saveGlobalProperty(new GlobalProperty(OpenmrsConstants.GP_DRUG_DOSING_UNITS_CONCEPT_UUID, ""));
		assertThat(orderService.getDrugDispensingUnits(), is(empty()));
	}
	
	/**
	 * @see OrderService#getDrugDosingUnits()
	 */
	@Test
	public void getDrugDosingUnits_shouldReturnAListIfGPIsSet() {
		List<Concept> dosingUnits = orderService.getDrugDosingUnits();
		assertEquals(2, dosingUnits.size());
		assertThat(dosingUnits, containsInAnyOrder(hasId(50), hasId(51)));
	}
	
	/**
	 * @see OrderService#getDrugDosingUnits()
	 */
	@Test
	public void getDrugDosingUnits_shouldReturnAnEmptyListIfNothingIsConfigured() {
		adminService.saveGlobalProperty(new GlobalProperty(OpenmrsConstants.GP_DRUG_DOSING_UNITS_CONCEPT_UUID, ""));
		assertThat(orderService.getDrugDosingUnits(), is(empty()));
	}
	
	/**
	 * @see OrderService#getDurationUnits()
	 */
	@Test
	public void getDurationUnits_shouldReturnAListIfGPIsSet() {
		List<Concept> durationConcepts = orderService.getDurationUnits();
		assertEquals(1, durationConcepts.size());
		assertEquals(28, durationConcepts.get(0).getConceptId().intValue());
	}
	
	/**
	 * @see OrderService#getDurationUnits()
	 */
	@Test
	public void getDurationUnits_shouldReturnAnEmptyListIfNothingIsConfigured() {
		adminService.saveGlobalProperty(new GlobalProperty(OpenmrsConstants.GP_DURATION_UNITS_CONCEPT_UUID, ""));
		assertThat(orderService.getDurationUnits(), is(empty()));
	}
	
	/**
	 * @see OrderService#getRevisionOrder(org.openmrs.Order)
	 */
	@Test
	public void getRevisionOrder_shouldNotReturnAVoidedRevisionOrder() {
		Order order = orderService.getOrder(7);
		Order revision1 = order.cloneForRevision();
		revision1.setEncounter(order.getEncounter());
		revision1.setOrderer(order.getOrderer());
		orderService.saveOrder(revision1, null);
		assertEquals(revision1, orderService.getRevisionOrder(order));
		orderService.voidOrder(revision1, "Testing");
		assertThat(orderService.getRevisionOrder(order), is(nullValue()));
		
		//should return the new unvoided revision
		Order revision2 = order.cloneForRevision();
		revision2.setEncounter(order.getEncounter());
		revision2.setOrderer(order.getOrderer());
		orderService.saveOrder(revision2, null);
		assertEquals(revision2, orderService.getRevisionOrder(order));
	}
	
	/**
	 * @see OrderService#saveOrder(org.openmrs.Order, OrderContext)
	 */
	@Test
	public void saveOrder_shouldPassForADiscontinuationOrderWithNoPreviousOrder() {
		TestOrder dcOrder = new TestOrder();
		dcOrder.setAction(Action.DISCONTINUE);
		dcOrder.setPatient(patientService.getPatient(2));
		dcOrder.setCareSetting(orderService.getCareSetting(2));
		dcOrder.setConcept(conceptService.getConcept(5089));
		dcOrder.setEncounter(encounterService.getEncounter(6));
		dcOrder.setOrderer(providerService.getProvider(1));
		orderService.saveOrder(dcOrder, null);
	}
	
	/**
	 * @see OrderService#getTestSpecimenSources()
	 */
	@Test
	public void getTestSpecimenSources_shouldReturnAListIfGPIsSet() {
		List<Concept> specimenSourceList = orderService.getTestSpecimenSources();
		assertEquals(1, specimenSourceList.size());
		assertEquals(22, specimenSourceList.get(0).getConceptId().intValue());
	}
	
	/**
	 * @see OrderService#getTestSpecimenSources()
	 */
	@Test
	public void getTestSpecimenSources_shouldReturnAnEmptyListIfNothingIsConfigured() {
		adminService.saveGlobalProperty(new GlobalProperty(OpenmrsConstants.GP_TEST_SPECIMEN_SOURCES_CONCEPT_UUID, ""));
		assertThat(orderService.getTestSpecimenSources(), is(empty()));
	}
	
	/**
	 * @see OrderService#retireOrderType(org.openmrs.OrderType, String)
	 */
	@Test
	public void retireOrderType_shouldNotRetireIndependentField() {
		OrderType orderType = orderService.getOrderType(2);
		ConceptClass conceptClass = conceptService.getConceptClass(1);
		Assert.assertFalse(conceptClass.getRetired());
		orderType.addConceptClass(conceptClass);
		orderService.retireOrderType(orderType, "test retire reason");
		Assert.assertFalse(conceptClass.getRetired());
	}
	
	/**
	 * @see OrderService#saveOrder(org.openmrs.Order, OrderContext)
	 */
	@Test
	public void saveOrder_shouldSetOrderTypeOfDrugOrderToDrugOrderIfNotSetAndConceptNotMapped() {
		Drug drug = conceptService.getDrug(2);
		Concept unmappedConcept = conceptService.getConcept(113);
		
		Assert.assertNull(orderService.getOrderTypeByConcept(unmappedConcept));
		drug.setConcept(unmappedConcept);
		
		DrugOrder drugOrder = new DrugOrder();
		Encounter encounter = encounterService.getEncounter(3);
		drugOrder.setEncounter(encounter);
		drugOrder.setPatient(patientService.getPatient(7));
		drugOrder.setCareSetting(orderService.getCareSetting(1));
		drugOrder.setOrderer(Context.getProviderService().getProvider(1));
		drugOrder.setDateActivated(encounter.getEncounterDatetime());
		drugOrder.setDrug(drug);
		drugOrder.setDosingType(SimpleDosingInstructions.class);
		drugOrder.setDose(300.0);
		drugOrder.setDoseUnits(conceptService.getConcept(50));
		drugOrder.setQuantity(20.0);
		drugOrder.setQuantityUnits(conceptService.getConcept(51));
		drugOrder.setFrequency(orderService.getOrderFrequency(3));
		drugOrder.setRoute(conceptService.getConcept(22));
		drugOrder.setNumRefills(10);
		drugOrder.setOrderType(null);
		
		orderService.saveOrder(drugOrder, null);
		Assert.assertNotNull(drugOrder.getOrderType());
		Assert.assertEquals(orderService.getOrderTypeByUuid(OrderType.DRUG_ORDER_TYPE_UUID), drugOrder.getOrderType());
	}
	
	/**
	 * @see OrderService#saveOrder(org.openmrs.Order, OrderContext)
	 */
	@Test
	public void saveOrder_shouldSetOrderTypeOfTestOrderToTestOrderIfNotSetAndConceptNotMapped() {
		TestOrder testOrder = new TestOrder();
		testOrder.setPatient(patientService.getPatient(7));
		Concept unmappedConcept = conceptService.getConcept(113);
		
		Assert.assertNull(orderService.getOrderTypeByConcept(unmappedConcept));
		testOrder.setConcept(unmappedConcept);
		testOrder.setOrderer(providerService.getProvider(1));
		testOrder.setCareSetting(orderService.getCareSetting(1));
		Encounter encounter = encounterService.getEncounter(3);
		testOrder.setEncounter(encounter);
		testOrder.setDateActivated(encounter.getEncounterDatetime());
		testOrder.setClinicalHistory("Patient had a negative reaction to the test in the past");
		testOrder.setFrequency(orderService.getOrderFrequency(3));
		testOrder.setSpecimenSource(conceptService.getConcept(22));
		testOrder.setNumberOfRepeats(3);
		
		orderService.saveOrder(testOrder, null);
		Assert.assertNotNull(testOrder.getOrderType());
		Assert.assertEquals(orderService.getOrderTypeByUuid(OrderType.TEST_ORDER_TYPE_UUID), testOrder.getOrderType());
	}
	
	@Test
	public void saveOrder_shouldSetAutoExpireDateOfDrugOrderIfAutoExpireDateIsNotSet() throws ParseException {
		executeDataSet("org/openmrs/api/include/OrderServiceTest-drugOrderAutoExpireDate.xml");
		Drug drug = conceptService.getDrug(3000);
		DrugOrder drugOrder = new DrugOrder();
		Encounter encounter = encounterService.getEncounter(3);
		drugOrder.setEncounter(encounter);
		drugOrder.setPatient(patientService.getPatient(7));
		drugOrder.setCareSetting(orderService.getCareSetting(1));
		drugOrder.setOrderer(Context.getProviderService().getProvider(1));
		drugOrder.setDrug(drug);
		drugOrder.setDosingType(SimpleDosingInstructions.class);
		drugOrder.setDose(300.0);
		drugOrder.setDoseUnits(conceptService.getConcept(50));
		drugOrder.setQuantity(20.0);
		drugOrder.setQuantityUnits(conceptService.getConcept(51));
		drugOrder.setFrequency(orderService.getOrderFrequency(3));
		drugOrder.setRoute(conceptService.getConcept(22));
		drugOrder.setNumRefills(0);
		drugOrder.setOrderType(null);
		drugOrder.setDateActivated(TestUtil.createDateTime("2014-08-03"));
		drugOrder.setDuration(20);// 20 days
		drugOrder.setDurationUnits(conceptService.getConcept(1001));
		
		Order savedOrder = orderService.saveOrder(drugOrder, null);
		
		Order loadedOrder = orderService.getOrder(savedOrder.getId());
		Assert.assertEquals(TestUtil.createDateTime("2014-08-22 23:59:59"), loadedOrder.getAutoExpireDate());
	}
	
	@Test
	public void saveOrder_shouldSetAutoExpireDateForReviseOrderWithSimpleDosingInstructions() {
		executeDataSet("org/openmrs/api/include/OrderServiceTest-drugOrderAutoExpireDate.xml");
		DrugOrder originalOrder = (DrugOrder) orderService.getOrder(111);
		assertTrue(originalOrder.isActive());
		DrugOrder revisedOrder = originalOrder.cloneForRevision();
		revisedOrder.setOrderer(originalOrder.getOrderer());
		revisedOrder.setEncounter(originalOrder.getEncounter());
		
		revisedOrder.setNumRefills(0);
		revisedOrder.setAutoExpireDate(null);
		revisedOrder.setDuration(10);
		revisedOrder.setDurationUnits(conceptService.getConcept(1001));
		
		orderService.saveOrder(revisedOrder, null);
		
		assertNotNull(revisedOrder.getAutoExpireDate());
	}
	
	/**
	 * @see OrderServiceImpl#discontinueExistingOrdersIfNecessary()
	 */
	@Test(expected = AmbiguousOrderException.class)
	public void saveOrder_shouldThrowAmbiguousOrderExceptionIfDisconnectingMultipleActiveOrdersForTheGivenConcepts()
	{
		executeDataSet("org/openmrs/api/include/OrderServiceTest-discontinueAmbiguousOrderByConcept.xml");
		DrugOrder order = new DrugOrder();
		order.setAction(Order.Action.DISCONTINUE);
		order.setOrderReasonNonCoded("Discontinue this");
		order.setConcept(conceptService.getConcept(88));
		order.setEncounter(encounterService.getEncounter(7));
		order.setPatient(patientService.getPatient(9));
		order.setOrderer(providerService.getProvider(1));
		order.setCareSetting(orderService.getCareSetting(1));
		order = (DrugOrder) orderService.saveOrder(order, null);
	}
	
	/**
	 * @see OrderServiceImpl#discontinueExistingOrdersIfNecessary()
	 */
	@Test(expected = AmbiguousOrderException.class)
	public void saveOrder_shouldThrowAmbiguousOrderExceptionIfDisconnectingMultipleActiveDrugOrdersWithTheSameDrug()
	{
		executeDataSet("org/openmrs/api/include/OrderServiceTest-ambiguousDrugOrders.xml");
		DrugOrder order = new DrugOrder();
		order.setAction(Order.Action.DISCONTINUE);
		order.setOrderReasonNonCoded("Discontinue this");
		order.setDrug(conceptService.getDrug(3));
		order.setEncounter(encounterService.getEncounter(7));
		order.setPatient(patientService.getPatient(9));
		order.setOrderer(providerService.getProvider(1));
		order.setCareSetting(orderService.getCareSetting(1));
		order = (DrugOrder) orderService.saveOrder(order, null);
	}
	
	/**
	 * @see OrderService#saveOrder(org.openmrs.Order, OrderContext, org.openmrs.Order[])
	 */
	@Test
	public void saveOrder_shouldPassIfAnKnownDrugOrderForTheSameDrugFormulationSpecified() {
		executeDataSet("org/openmrs/api/include/OrderServiceTest-drugOrdersWithSameConceptAndDifferentFormAndStrength.xml");
		final Patient patient = patientService.getPatient(2);
		//sanity check that we have an active order for the same concept
		DrugOrder existingOrder = (DrugOrder) orderService.getOrder(1000);
		assertTrue(existingOrder.isActive());
		
		//New Drug order
		DrugOrder order = new DrugOrder();
		order.setPatient(patient);
		order.setDrug(existingOrder.getDrug());
		order.setEncounter(encounterService.getEncounter(6));
		order.setOrderer(providerService.getProvider(1));
		order.setCareSetting(existingOrder.getCareSetting());
		order.setDosingType(FreeTextDosingInstructions.class);
		order.setDosingInstructions("2 for 5 days");
		order.setQuantity(10.0);
		order.setQuantityUnits(conceptService.getConcept(51));
		order.setNumRefills(2);
		OrderContext orderContext = new OrderContext();
		orderContext.setAttribute(OrderService.PARALLEL_ORDERS, new String[] { existingOrder.getUuid() });
		orderService.saveOrder(order, orderContext);
		assertNotNull(orderService.getOrder(order.getOrderId()));
	}
	
	/**
	 * @see OrderService#getNonCodedDrugConcept()
	 */
	@Test
	public void getNonCodedDrugConcept_shouldReturnNullIfNothingIsConfigured() {
		adminService.saveGlobalProperty(new GlobalProperty(OpenmrsConstants.GP_DRUG_ORDER_DRUG_OTHER, ""));
		assertNull(orderService.getNonCodedDrugConcept());
	}
	
	/**
	 * @see OrderService#getNonCodedDrugConcept()
	 */
	@Test
	public void getNonCodedDrugConcept_shouldReturnAConceptIfGPIsSet() {
		executeDataSet("org/openmrs/api/include/OrderServiceTest-nonCodedDrugs.xml");
		Concept nonCodedDrugConcept = orderService.getNonCodedDrugConcept();
		assertNotNull(nonCodedDrugConcept);
		assertThat(nonCodedDrugConcept.getConceptId(), is(5584));
		assertEquals(nonCodedDrugConcept.getName().getName(), "DRUG OTHER");
		
	}
	
	/**
	 * @see OrderService#saveOrder(Order, OrderContext)
	 */
	@Test
	public void saveOrder_shouldPassIfAnActiveDrugOrderForTheSameConceptAndDifferentDrugNonCodedExists() {
		executeDataSet("org/openmrs/api/include/OrderServiceTest-nonCodedDrugs.xml");
		final Concept nonCodedConcept = orderService.getNonCodedDrugConcept();
		//sanity check that we have an active order for the same concept
		DrugOrder duplicateOrder = (DrugOrder) orderService.getOrder(584);
		assertTrue(duplicateOrder.isActive());
		assertEquals(nonCodedConcept, duplicateOrder.getConcept());
		
		DrugOrder drugOrder = duplicateOrder.copy();
		drugOrder.setDrugNonCoded("non coded drug paracetemol");
		
		Order savedOrder = orderService.saveOrder(drugOrder, null);
		assertNotNull(orderService.getOrder(savedOrder.getOrderId()));
	}
	
	/**
	 * @see OrderService#saveOrder(Order, OrderContext)
	 */
	@Test
	public void saveOrder_shouldFailIfAnActiveDrugOrderForTheSameConceptAndDrugNonCodedAndCareSettingExists()
	{
		executeDataSet("org/openmrs/api/include/OrderServiceTest-nonCodedDrugs.xml");
		final Concept nonCodedConcept = orderService.getNonCodedDrugConcept();
		//sanity check that we have an active order for the same concept
		DrugOrder duplicateOrder = (DrugOrder) orderService.getOrder(584);
		assertTrue(duplicateOrder.isActive());
		assertEquals(nonCodedConcept, duplicateOrder.getConcept());
		
		DrugOrder drugOrder = duplicateOrder.copy();
		drugOrder.setDrugNonCoded("non coded drug crocine");
		
		expectedException.expect(AmbiguousOrderException.class);
		expectedException.expectMessage("Order.cannot.have.more.than.one");
		orderService.saveOrder(drugOrder, null);
	}
	
	@Test
	public void saveOrder_shouldDiscontinuePreviousNonCodedOrderIfItIsNotAlreadyDiscontinued() {
		//We are trying to discontinue order id 584 in OrderServiceTest-nonCodedDrugs.xml
		executeDataSet("org/openmrs/api/include/OrderServiceTest-nonCodedDrugs.xml");
		DrugOrder previousOrder = (DrugOrder) orderService.getOrder(584);
		DrugOrder drugOrder = previousOrder.cloneForDiscontinuing();
		drugOrder.setPreviousOrder(previousOrder);
		drugOrder.setDateActivated(new Date());
		drugOrder.setOrderer(previousOrder.getOrderer());
		drugOrder.setEncounter(previousOrder.getEncounter());
		
		Order saveOrder = orderService.saveOrder(drugOrder, null);
		Assert.assertNotNull("previous order should be discontinued", previousOrder.getDateStopped());
		assertNotNull(orderService.getOrder(saveOrder.getOrderId()));
	}
	
	@Test
	public void saveOrder_shouldFailDiscontinueNonCodedDrugOrderIfOrderableOfPreviousAndNewOrderDontMatch() {
		executeDataSet("org/openmrs/api/include/OrderServiceTest-nonCodedDrugs.xml");
		DrugOrder previousOrder = (DrugOrder) orderService.getOrder(584);
		DrugOrder drugOrder = previousOrder.cloneForDiscontinuing();
		drugOrder.setDrugNonCoded("non coded drug citrigine");
		drugOrder.setPreviousOrder(previousOrder);
		drugOrder.setDateActivated(new Date());
		drugOrder.setOrderer(providerService.getProvider(1));
		drugOrder.setEncounter(encounterService.getEncounter(6));
		
		expectedException.expect(EditedOrderDoesNotMatchPreviousException.class);
		expectedException.expectMessage("The orderable of the previous order and the new one order don't match");
		orderService.saveOrder(drugOrder, null);
	}
	
	@Test
	public void saveOrder_shouldFailIfDrugNonCodedInPreviousDrugOrderDoesNotMatchThatOfTheRevisedDrugOrder()
	{
		executeDataSet("org/openmrs/api/include/OrderServiceTest-nonCodedDrugs.xml");
		DrugOrder previousOrder = (DrugOrder) orderService.getOrder(584);
		DrugOrder order = previousOrder.cloneForRevision();
		String drugNonCodedParacetemol = "non coded aspirin";
		
		order.setDateActivated(new Date());
		order.setOrderer(providerService.getProvider(1));
		order.setEncounter(encounterService.getEncounter(6));
		assertFalse(previousOrder.getDrugNonCoded().equals(drugNonCodedParacetemol));
		order.setDrugNonCoded(drugNonCodedParacetemol);
		order.setPreviousOrder(previousOrder);
		
		expectedException.expect(EditedOrderDoesNotMatchPreviousException.class);
		expectedException.expectMessage("The orderable of the previous order and the new one order don't match");
		orderService.saveOrder(order, null);
	}
	
	@Test
	public void saveOrder_shouldRevisePreviousNonCodedOrderIfItIsAlreadyExisting() {
		//We are trying to discontinue order id 584 in OrderServiceTest-nonCodedDrugs.xml
		executeDataSet("org/openmrs/api/include/OrderServiceTest-nonCodedDrugs.xml");
		DrugOrder previousOrder = (DrugOrder) orderService.getOrder(584);
		DrugOrder order = previousOrder.cloneForRevision();
		
		order.setDateActivated(new Date());
		order.setOrderer(providerService.getProvider(1));
		order.setEncounter(encounterService.getEncounter(6));
		order.setAsNeeded(true);
		order.setPreviousOrder(previousOrder);
		
		DrugOrder saveOrder = (DrugOrder) orderService.saveOrder(order, null);
		Assert.assertTrue(saveOrder.getAsNeeded());
		assertNotNull(orderService.getOrder(saveOrder.getOrderId()));
	}
	
	@Test
	public void saveRetrospectiveOrder_shouldDiscontinueOrderInRetrospectiveEntry() throws ParseException {
		executeDataSet("org/openmrs/api/include/OrderServiceTest-ordersWithAutoExpireDate.xml");
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.S");
		Date originalOrderDateActivated = dateFormat.parse("2008-11-19 09:24:10.0");
		Date discontinuationOrderDate = DateUtils.addDays(originalOrderDateActivated, 2);
		
		Order originalOrder = orderService.getOrder(201);
		assertNull(originalOrder.getDateStopped());
		assertEquals(dateFormat.parse("2008-11-23 09:24:09.0"), originalOrder.getAutoExpireDate());
		assertFalse(originalOrder.isActive());
		assertTrue(originalOrder.isActive(discontinuationOrderDate));
		
		Order discontinueationOrder = originalOrder.cloneForDiscontinuing();
		discontinueationOrder.setPreviousOrder(originalOrder);
		discontinueationOrder.setEncounter(encounterService.getEncounter(17));
		discontinueationOrder.setOrderer(providerService.getProvider(1));
		discontinueationOrder.setDateActivated(discontinuationOrderDate);
		orderService.saveRetrospectiveOrder(discontinueationOrder, null);
		
		assertNotNull(originalOrder.getDateStopped());
		assertEquals(discontinueationOrder.getAutoExpireDate(), discontinueationOrder.getDateActivated());
	}
	
	@Test
	public void saveRetrospectiveOrder_shouldDiscontinueAndStopActiveOrderInRetrospectiveEntry() throws ParseException {
		executeDataSet("org/openmrs/api/include/OrderServiceTest-ordersWithAutoExpireDate.xml");
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.S");
		Date originalOrderDateActivated = dateFormat.parse("2008-11-19 09:24:10.0");
		Date discontinuationOrderDate = DateUtils.addDays(originalOrderDateActivated, 2);
		
		Order originalOrder = orderService.getOrder(202);
		assertNull(originalOrder.getDateStopped());
		assertEquals(dateFormat.parse("2008-11-23 09:24:09.0"), originalOrder.getAutoExpireDate());
		assertFalse(originalOrder.isActive());
		assertTrue(originalOrder.isActive(discontinuationOrderDate));
		
		Order discontinuationOrder = originalOrder.cloneForDiscontinuing();
		discontinuationOrder.setPreviousOrder(null);
		discontinuationOrder.setEncounter(encounterService.getEncounter(17));
		discontinuationOrder.setOrderer(providerService.getProvider(1));
		discontinuationOrder.setDateActivated(discontinuationOrderDate);
		orderService.saveRetrospectiveOrder(discontinuationOrder, null);
		
		assertNotNull(originalOrder.getDateStopped());
		assertEquals(discontinuationOrder.getAutoExpireDate(), discontinuationOrder.getDateActivated());
	}
	
	@Test
	public void saveOrder_shouldNotRevisePreviousIfAlreadyStopped() throws ParseException {
		executeDataSet("org/openmrs/api/include/OrderServiceTest-ordersWithAutoExpireDate.xml");
		Order previousOrder = orderService.getOrder(203);
		Date dateActivated = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse("2008-10-19 13:00:00");
		Order order = previousOrder.cloneForRevision();
		
		order.setDateActivated(dateActivated);
		order.setOrderer(providerService.getProvider(1));
		order.setEncounter(encounterService.getEncounter(18));
		order.setPreviousOrder(previousOrder);
		
		expectedException.expect(CannotStopInactiveOrderException.class);
		expectedException.expectMessage(mss.getMessage("Order.cannot.discontinue.inactive"));
		orderService.saveRetrospectiveOrder(order, null);
	}
	
	@Test
	public void saveRetrospectiveOrder_shouldFailIfAnActiveDrugOrderForTheSameConceptAndCareSettingExistsAtOrderDateActivated()
	        throws ParseException {
		executeDataSet("org/openmrs/api/include/OrderServiceTest-ordersWithAutoExpireDate.xml");
		Date newOrderDateActivated = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse("2008-11-19 13:00:10");
		final Patient patient = patientService.getPatient(12);
		final Concept orderConcept = conceptService.getConcept(88);
		//sanity check that we have an active order for the same concept
		DrugOrder duplicateOrder = (DrugOrder) orderService.getOrder(202);
		assertTrue(duplicateOrder.isActive(newOrderDateActivated));
		assertEquals(orderConcept, duplicateOrder.getConcept());
		
		DrugOrder order = new DrugOrder();
		order.setPatient(patient);
		order.setConcept(orderConcept);
		order.setEncounter(encounterService.getEncounter(17));
		order.setOrderer(providerService.getProvider(1));
		order.setCareSetting(duplicateOrder.getCareSetting());
		order.setDateActivated(newOrderDateActivated);
		order.setDrug(duplicateOrder.getDrug());
		order.setDose(duplicateOrder.getDose());
		order.setDoseUnits(duplicateOrder.getDoseUnits());
		order.setRoute(duplicateOrder.getRoute());
		order.setFrequency(duplicateOrder.getFrequency());
		order.setQuantity(duplicateOrder.getQuantity());
		order.setQuantityUnits(duplicateOrder.getQuantityUnits());
		order.setNumRefills(duplicateOrder.getNumRefills());
		
		expectedException.expect(AmbiguousOrderException.class);
		expectedException.expectMessage("Order.cannot.have.more.than.one");
		orderService.saveRetrospectiveOrder(order, null);
	}
	
	@Test
	public void shouldSaveOrdersWithSortWeightWhenWithinAOrderGroup() {
		executeDataSet(ORDER_SET);
		
		Encounter encounter = encounterService.getEncounter(3);
		
		OrderSet orderSet = Context.getOrderSetService().getOrderSet(2000);
		OrderGroup orderGroup = new OrderGroup();
		orderGroup.setOrderSet(orderSet);
		orderGroup.setPatient(encounter.getPatient());
		
		orderGroup.setEncounter(encounter);
		
		Order firstOrderWithOrderGroup = new OrderBuilder().withAction(Order.Action.NEW).withPatient(7).withConcept(1000)
		        .withCareSetting(1).withOrderer(1).withEncounter(3).withDateActivated(new Date()).withOrderType(17)
		        .withUrgency(Order.Urgency.ON_SCHEDULED_DATE).withScheduledDate(new Date()).withOrderGroup(orderGroup)
		        .build();
		
		Order secondOrderWithOrderGroup = new OrderBuilder().withAction(Order.Action.NEW).withPatient(7).withConcept(1001)
		        .withCareSetting(1).withOrderer(1).withEncounter(3).withDateActivated(new Date()).withOrderType(17)
		        .withUrgency(Order.Urgency.ON_SCHEDULED_DATE).withScheduledDate(new Date()).withOrderGroup(orderGroup)
		        .build();
		
		Order orderWithoutOrderGroup = new OrderBuilder().withAction(Order.Action.NEW).withPatient(7).withConcept(1000)
		        .withCareSetting(1).withOrderer(1).withEncounter(3).withDateActivated(new Date()).withOrderType(17)
		        .withUrgency(Order.Urgency.ON_SCHEDULED_DATE).withScheduledDate(new Date()).build();
		
		Set<Order> orders = new LinkedHashSet<>();
		orders.add(firstOrderWithOrderGroup);
		orders.add(secondOrderWithOrderGroup);
		orders.add(orderWithoutOrderGroup);
		
		encounter.setOrders(orders);
		
		for (OrderGroup og : encounter.getOrderGroups()) {
			if (og.getId() == null) {
				Context.getOrderService().saveOrderGroup(og);
			}
		}
		
		for (Order o : encounter.getOrdersWithoutOrderGroups()) {
			if (o.getId() == null) {
				Context.getOrderService().saveOrder(o, null);
			}
		}
		
		Context.flushSession();
		
		OrderGroup savedOrderGroup = Context.getOrderService().getOrderGroupByUuid(orderGroup.getUuid());
		Order savedOrder = Context.getOrderService().getOrderByUuid(orderWithoutOrderGroup.getUuid());
		
		assertEquals("The first order in  savedOrderGroup is the same which is sent first in the List",
		    firstOrderWithOrderGroup.getUuid(), savedOrderGroup.getOrders().get(0).getUuid());
		
		assertEquals("The second order in  savedOrderGroup is the same which is sent second in the List",
		    secondOrderWithOrderGroup.getUuid(), savedOrderGroup.getOrders().get(1).getUuid());
		assertNull("The order which doesn't belong to an orderGroup has no sortWeight", savedOrder.getSortWeight());
		assertThat("The first order has a lower sortWeight than the second", savedOrderGroup.getOrders().get(0)
		        .getSortWeight().compareTo(savedOrderGroup.getOrders().get(1).getSortWeight()), is(-1));
	}
	
	@Test
	public void shouldSetTheCorrectSortWeightWhenAddingAnOrderInOrderGroup() {
		executeDataSet(ORDER_SET);
		
		Encounter encounter = encounterService.getEncounter(3);
		
		OrderSet orderSet = Context.getOrderSetService().getOrderSet(2000);
		OrderGroup orderGroup = new OrderGroup();
		orderGroup.setOrderSet(orderSet);
		orderGroup.setPatient(encounter.getPatient());
		
		orderGroup.setEncounter(encounter);
		
		Order firstOrderWithOrderGroup = new OrderBuilder().withAction(Order.Action.NEW).withPatient(7).withConcept(1000)
		        .withCareSetting(1).withOrderer(1).withEncounter(3).withDateActivated(new Date()).withOrderType(17)
		        .withUrgency(Order.Urgency.ON_SCHEDULED_DATE).withScheduledDate(new Date()).withOrderGroup(orderGroup)
		        .build();
		
		Order secondOrderWithOrderGroup = new OrderBuilder().withAction(Order.Action.NEW).withPatient(7).withConcept(1001)
		        .withCareSetting(1).withOrderer(1).withEncounter(3).withDateActivated(new Date()).withOrderType(17)
		        .withUrgency(Order.Urgency.ON_SCHEDULED_DATE).withScheduledDate(new Date()).withOrderGroup(orderGroup)
		        .build();
		
		Set<Order> orders = new LinkedHashSet<>();
		orders.add(firstOrderWithOrderGroup);
		orders.add(secondOrderWithOrderGroup);
		
		encounter.setOrders(orders);
		
		for (OrderGroup og : encounter.getOrderGroups()) {
			if (og.getId() == null) {
				Context.getOrderService().saveOrderGroup(og);
			}
		}
		
		Context.flushSession();
		
		OrderGroup savedOrderGroup = Context.getOrderService().getOrderGroupByUuid(orderGroup.getUuid());
		assertEquals("The first order in  savedOrderGroup is the same which is sent first in the List",
		    firstOrderWithOrderGroup.getUuid(), savedOrderGroup.getOrders().get(0).getUuid());
		
		assertEquals("The second order in  savedOrderGroup is the same which is sent second in the List",
		    secondOrderWithOrderGroup.getUuid(), savedOrderGroup.getOrders().get(1).getUuid());
		assertThat("The first order has a lower sortWeight than the second", savedOrderGroup.getOrders().get(0)
		        .getSortWeight().compareTo(savedOrderGroup.getOrders().get(1).getSortWeight()), is(-1));
		
		Order newOrderWithoutAnyPosition = new OrderBuilder().withAction(Order.Action.NEW).withPatient(7).withConcept(1000)
		        .withCareSetting(1).withOrderer(1).withEncounter(3).withDateActivated(new Date()).withOrderType(17)
		        .withUrgency(Order.Urgency.ON_SCHEDULED_DATE).withScheduledDate(new Date()).withOrderGroup(savedOrderGroup)
		        .build();
		
		savedOrderGroup.addOrder(newOrderWithoutAnyPosition);
		
		Context.getOrderService().saveOrderGroup(savedOrderGroup);
		Context.flushSession();
		
		OrderGroup secondSavedOrderGroup = Context.getOrderService().getOrderGroupByUuid(orderGroup.getUuid());
		
		assertEquals("The first order in  savedOrderGroup is the same which is sent first in the List",
		    firstOrderWithOrderGroup.getUuid(), savedOrderGroup.getOrders().get(0).getUuid());
		
		assertEquals("The second order in  savedOrderGroup is the same which is sent second in the List",
		    secondOrderWithOrderGroup.getUuid(), savedOrderGroup.getOrders().get(1).getUuid());
		
		assertEquals("The third order in  savedOrderGroup is the same which is sent third in the List",
		    secondSavedOrderGroup.getOrders().get(2).getUuid(), newOrderWithoutAnyPosition.getUuid());
		
		assertThat("The third order has a higher sortWeight than the second", savedOrderGroup.getOrders().get(2)
		        .getSortWeight().compareTo(savedOrderGroup.getOrders().get(1).getSortWeight()), is(1));
	}
	
	@Test
	public void shouldSetTheCorrectSortWeightWhenAddingAnOrderAtAPosition() {
		executeDataSet(ORDER_SET);
		
		Encounter encounter = encounterService.getEncounter(3);
		
		OrderSet orderSet = Context.getOrderSetService().getOrderSet(2000);
		OrderGroup orderGroup = new OrderGroup();
		orderGroup.setOrderSet(orderSet);
		orderGroup.setPatient(encounter.getPatient());
		orderGroup.setEncounter(encounter);
		
		Order firstOrderWithOrderGroup = new OrderBuilder().withAction(Order.Action.NEW).withPatient(7).withConcept(1000)
		        .withCareSetting(1).withOrderer(1).withEncounter(3).withDateActivated(new Date()).withOrderType(17)
		        .withUrgency(Order.Urgency.ON_SCHEDULED_DATE).withScheduledDate(new Date()).withOrderGroup(orderGroup)
		        .build();
		
		Order secondOrderWithOrderGroup = new OrderBuilder().withAction(Order.Action.NEW).withPatient(7).withConcept(1001)
		        .withCareSetting(1).withOrderer(1).withEncounter(3).withDateActivated(new Date()).withOrderType(17)
		        .withUrgency(Order.Urgency.ON_SCHEDULED_DATE).withScheduledDate(new Date()).withOrderGroup(orderGroup)
		        .build();
		
		Set<Order> orders = new LinkedHashSet<>();
		orders.add(firstOrderWithOrderGroup);
		orders.add(secondOrderWithOrderGroup);
		
		encounter.setOrders(orders);
		
		for (OrderGroup og : encounter.getOrderGroups()) {
			if (og.getId() == null) {
				Context.getOrderService().saveOrderGroup(og);
			}
		}
		
		Context.flushSession();
		
		OrderGroup savedOrderGroup = Context.getOrderService().getOrderGroupByUuid(orderGroup.getUuid());
		assertEquals("The first order in  savedOrderGroup is the same which is sent first in the List",
		    firstOrderWithOrderGroup.getUuid(), savedOrderGroup.getOrders().get(0).getUuid());
		
		assertEquals("The second order in  savedOrderGroup is the same which is sent second in the List",
		    secondOrderWithOrderGroup.getUuid(), savedOrderGroup.getOrders().get(1).getUuid());
		assertThat("The first order has a lower sortWeight than the second", savedOrderGroup.getOrders().get(0)
		        .getSortWeight().compareTo(savedOrderGroup.getOrders().get(1).getSortWeight()), is(-1));
		
		Order newOrderAtPosition1 = new OrderBuilder().withAction(Order.Action.NEW).withPatient(7).withConcept(1000)
		        .withCareSetting(1).withOrderer(1).withEncounter(3).withDateActivated(new Date()).withOrderType(17)
		        .withUrgency(Order.Urgency.ON_SCHEDULED_DATE).withScheduledDate(new Date()).withOrderGroup(savedOrderGroup)
		        .build();
		
		Order newOrderAtPosition2 = new OrderBuilder().withAction(Order.Action.NEW).withPatient(7).withConcept(1000)
		        .withCareSetting(1).withOrderer(1).withEncounter(3).withDateActivated(new Date()).withOrderType(17)
		        .withUrgency(Order.Urgency.ON_SCHEDULED_DATE).withScheduledDate(new Date()).withOrderGroup(savedOrderGroup)
		        .build();
		
		savedOrderGroup.addOrder(newOrderAtPosition1, 0);
		savedOrderGroup.addOrder(newOrderAtPosition2, 1);
		
		Context.getOrderService().saveOrderGroup(savedOrderGroup);
		
		OrderGroup secondSavedOrderGroup = Context.getOrderService().getOrderGroupByUuid(orderGroup.getUuid());
		assertEquals(4, savedOrderGroup.getOrders().size());
		
		assertEquals("The first order in  savedOrderGroup is the same which is sent first in the List",
		    newOrderAtPosition1.getUuid(), secondSavedOrderGroup.getOrders().get(0).getUuid());
		
		assertEquals("The second order in  savedOrderGroup is the same which is sent second in the List",
		    newOrderAtPosition2.getUuid(), secondSavedOrderGroup.getOrders().get(1).getUuid());
		
		assertEquals("The third order in  savedOrderGroup is the same which is sent third in the List",
		    firstOrderWithOrderGroup.getUuid(), savedOrderGroup.getOrders().get(2).getUuid());
		
		assertEquals("The fourth order in  savedOrderGroup is the same which is sent first in the List",
		    secondOrderWithOrderGroup.getUuid(), savedOrderGroup.getOrders().get(3).getUuid());
		
		assertThat("The third order has a lower sortWeight than the fourth", savedOrderGroup.getOrders().get(2)
		        .getSortWeight().compareTo(savedOrderGroup.getOrders().get(3).getSortWeight()), is(-1));
		assertThat("The second order has a lower sortWeight than the third", savedOrderGroup.getOrders().get(1)
		        .getSortWeight().compareTo(savedOrderGroup.getOrders().get(2).getSortWeight()), is(-1));
		assertThat("The first order has a lower sortWeight than the second", savedOrderGroup.getOrders().get(0)
		        .getSortWeight().compareTo(savedOrderGroup.getOrders().get(1).getSortWeight()), is(-1));
	}
	
	@Test
	public void shouldSetTheCorrectSortWeightWhenAddingAnOrderWithANegativePosition() {
		executeDataSet(ORDER_SET);
		
		Encounter encounter = encounterService.getEncounter(3);
		
		OrderSet orderSet = Context.getOrderSetService().getOrderSet(2000);
		OrderGroup orderGroup = new OrderGroup();
		orderGroup.setOrderSet(orderSet);
		orderGroup.setPatient(encounter.getPatient());
		orderGroup.setEncounter(encounter);
		
		Order firstOrderWithOrderGroup = new OrderBuilder().withAction(Order.Action.NEW).withPatient(7).withConcept(1000)
		        .withCareSetting(1).withOrderer(1).withEncounter(3).withDateActivated(new Date()).withOrderType(17)
		        .withUrgency(Order.Urgency.ON_SCHEDULED_DATE).withScheduledDate(new Date()).withOrderGroup(orderGroup)
		        .build();
		
		Order secondOrderWithOrderGroup = new OrderBuilder().withAction(Order.Action.NEW).withPatient(7).withConcept(1001)
		        .withCareSetting(1).withOrderer(1).withEncounter(3).withDateActivated(new Date()).withOrderType(17)
		        .withUrgency(Order.Urgency.ON_SCHEDULED_DATE).withScheduledDate(new Date()).withOrderGroup(orderGroup)
		        .build();
		
		Set<Order> orders = new LinkedHashSet<>();
		orders.add(firstOrderWithOrderGroup);
		orders.add(secondOrderWithOrderGroup);
		
		encounter.setOrders(orders);
		
		for (OrderGroup og : encounter.getOrderGroups()) {
			if (og.getId() == null) {
				Context.getOrderService().saveOrderGroup(og);
			}
		}
		
		Context.flushSession();
		
		OrderGroup savedOrderGroup = Context.getOrderService().getOrderGroupByUuid(orderGroup.getUuid());
		
		Order newOrderWithNegativePosition = new OrderBuilder().withAction(Order.Action.NEW).withPatient(7)
		        .withConcept(1000).withCareSetting(1).withOrderer(1).withEncounter(3).withDateActivated(new Date())
		        .withOrderType(17).withUrgency(Order.Urgency.ON_SCHEDULED_DATE).withScheduledDate(new Date())
		        .withOrderGroup(savedOrderGroup).build();
		
		savedOrderGroup.addOrder(newOrderWithNegativePosition, -1);
		
		Context.getOrderService().saveOrderGroup(savedOrderGroup);
		Context.flushSession();
		
		OrderGroup secondSavedOrderGroup = Context.getOrderService().getOrderGroupByUuid(orderGroup.getUuid());
		assertEquals(3, secondSavedOrderGroup.getOrders().size());
		
		assertEquals("The new order gets added at the last position", newOrderWithNegativePosition.getUuid(),
		    secondSavedOrderGroup.getOrders().get(2).getUuid());
		
		assertThat("The new order has a higher sortWeight than the second", secondSavedOrderGroup.getOrders().get(2)
		        .getSortWeight().compareTo(secondSavedOrderGroup.getOrders().get(1).getSortWeight()), is(1));
		
		Order newOrderWithInvalidPosition = new OrderBuilder().withAction(Order.Action.NEW).withPatient(7).withConcept(1000)
		        .withCareSetting(1).withOrderer(1).withEncounter(3).withDateActivated(new Date()).withOrderType(17)
		        .withUrgency(Order.Urgency.ON_SCHEDULED_DATE).withScheduledDate(new Date()).withOrderGroup(savedOrderGroup)
		        .build();
		expectedException.expect(APIException.class);
		expectedException.expectMessage("Cannot add a member which is out of range of the list");
		secondSavedOrderGroup.addOrder(newOrderWithInvalidPosition, secondSavedOrderGroup.getOrders().size() + 1);
	}
}
