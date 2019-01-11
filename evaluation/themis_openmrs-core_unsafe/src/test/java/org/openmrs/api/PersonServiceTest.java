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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.openmrs.test.TestUtil.containsId;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.GlobalProperty;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.Person;
import org.openmrs.PersonAddress;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonAttributeType;
import org.openmrs.PersonName;
import org.openmrs.Relationship;
import org.openmrs.RelationshipType;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.person.PersonMergeLog;
import org.openmrs.person.PersonMergeLogData;
import org.openmrs.test.BaseContextSensitiveTest;
import org.openmrs.test.TestUtil;
import org.openmrs.util.OpenmrsConstants;

/**
 * This class tests methods in the PersonService class. TODO: Test all methods in the PersonService
 * class.
 */
public class PersonServiceTest extends BaseContextSensitiveTest {
	
	protected static final String CREATE_PATIENT_XML = "org/openmrs/api/include/PatientServiceTest-createPatient.xml";
	
	protected static final String CREATE_RELATIONSHIP_XML = "org/openmrs/api/include/PersonServiceTest-createRelationship.xml";
	
	protected static final String CREATE_PERSON_PROPERTY_XML = "org/openmrs/api/include/PersonServiceTest-PersonAttributeType.xml";
	
	private static final Integer RETIRED_PERSON_ATTRIBUTE_TYPE = 1;
	
	private static final Integer UNRETIRED_PERSON_ATTRIBUTE_TYPE = 2;
	
	protected static final DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
	
	protected PatientService ps = null;
	
	protected AdministrationService adminService = null;
	
	protected PersonService personService = null;
	
	@Before
	public void onSetUpInTransaction() {
		if (ps == null) {
			ps = Context.getPatientService();
			adminService = Context.getAdministrationService();
			personService = Context.getPersonService();
		}
	}
	
	/**
	 * Tests a voided relationship between personA and Person B to see if it is still listed when
	 * retrieving unvoided relationships for personA and if it is still listed when retrieving
	 * unvoided relationships for personB.
	 * 
	 * @see PersonService#getRelationshipsByPerson(Person)
	 */
	@Test
	public void getRelationshipsByPerson_shouldOnlyGetUnvoidedRelationships() {
		executeDataSet(CREATE_PATIENT_XML);
		executeDataSet(CREATE_RELATIONSHIP_XML);
		
		Patient p1 = ps.getPatient(6);
		Patient p2 = ps.getPatient(8);
		
		// Create a sibling relationship between o1 and p2
		Relationship sibling = new Relationship();
		sibling.setPersonA(p1);
		sibling.setPersonB(p2);
		sibling.setRelationshipType(personService.getRelationshipType(4));
		personService.saveRelationship(sibling);
		
		// Make p2 the Doctor of p1.
		Relationship doctor = new Relationship();
		doctor.setPersonB(p1);
		doctor.setPersonA(p2);
		doctor.setRelationshipType(personService.getRelationshipType(3));
		personService.saveRelationship(doctor);
		
		// Void all relationships.
		List<Relationship> allRels = personService.getAllRelationships();
		for (Relationship r : allRels) {
			personService.voidRelationship(r, "Because of a JUnit test.");
		}
		
		List<Relationship> updatedARels = personService.getRelationshipsByPerson(p1);
		List<Relationship> updatedBRels = personService.getRelationshipsByPerson(p2);
		
		// Neither p1 or p2 should have any relationships now.
		assertEquals(0, updatedARels.size());
		assertEquals(updatedARels, updatedBRels);
	}
	
	/**
	 * Tests a voided relationship between personA and Person B to see if it is still listed when
	 * retrieving unvoided relationships for personA and if it is still listed when retrieving
	 * unvoided relationships for personB.
	 * 
	 * @see PersonService#getRelationshipsByPerson(Person,Date)
	 */
	@Test
	public void getRelationshipsByPerson_shouldOnlyGetUnvoidedRelationshipsRegardlessOfEffectiveDate() throws Exception {
		executeDataSet(CREATE_PATIENT_XML);
		executeDataSet(CREATE_RELATIONSHIP_XML);
		
		Patient p1 = ps.getPatient(6);
		Patient p2 = ps.getPatient(8);
		
		// Create a sibling relationship between o1 and p2
		Relationship sibling = new Relationship();
		sibling.setPersonA(p1);
		sibling.setPersonB(p2);
		sibling.setRelationshipType(personService.getRelationshipType(4));
		personService.saveRelationship(sibling);
		
		// Make p2 the Doctor of p1.
		Relationship doctor = new Relationship();
		doctor.setPersonB(p1);
		doctor.setPersonA(p2);
		doctor.setRelationshipType(personService.getRelationshipType(3));
		personService.saveRelationship(doctor);
		
		// Void all relationships.
		List<Relationship> allRels = personService.getAllRelationships();
		for (Relationship r : allRels) {
			personService.voidRelationship(r, "Because of a JUnit test.");
		}
		
		// Get unvoided relationships after voiding all of them.
		// (specified date should not matter as no relationships have date specified)
		
		List<Relationship> updatedARels = personService.getRelationshipsByPerson(p1, new Date());
		List<Relationship> updatedBRels = personService.getRelationshipsByPerson(p2, new Date());
		
		// Neither p1 or p2 should have any relationships now.
		assertEquals(0, updatedARels.size());
		assertEquals(updatedARels, updatedBRels);
	}
	
	/*
	 * Helper to create patient that does not have any existing relationships. Returns created Patient.
	 */
	private Patient createTestPatient() {
		Patient patient = new Patient();
		PersonName pName = new PersonName();
		pName.setGivenName("Tom");
		pName.setMiddleName("E.");
		pName.setFamilyName("Patient");
		patient.addName(pName);
		PersonAddress pAddress = new PersonAddress();
		pAddress.setAddress1("123 My street");
		pAddress.setAddress2("Apt 402");
		pAddress.setCityVillage("Anywhere city");
		pAddress.setCountry("Some Country");
		Set<PersonAddress> pAddressList = patient.getAddresses();
		pAddressList.add(pAddress);
		patient.setAddresses(pAddressList);
		patient.addAddress(pAddress);
		patient.setBirthdate(new Date());
		patient.setBirthdateEstimated(true);
		patient.setDeathDate(new Date());
		patient.setCauseOfDeath(new Concept(1));
		patient.setGender("male");
		List<PatientIdentifierType> patientIdTypes = ps.getAllPatientIdentifierTypes();
		assertNotNull(patientIdTypes);
		PatientIdentifier patientIdentifier = new PatientIdentifier();
		patientIdentifier.setIdentifier("123-0");
		patientIdentifier.setIdentifierType(patientIdTypes.get(0));
		patientIdentifier.setLocation(new Location(1));
		patientIdentifier.setPreferred(true);
		Set<PatientIdentifier> patientIdentifiers = new TreeSet<>();
		patientIdentifiers.add(patientIdentifier);
		patient.setIdentifiers(patientIdentifiers);
		
		ps.savePatient(patient);
		return patient;
	}
	
	/*
	 * Helper to create relationships with start and/or endDate. Returns a List of the relationships created.
	 */
	private List<Relationship> createTestDatedRelationships(Person personA, Person personB, RelationshipType rt)
	        throws Exception {
		List<Relationship> rels = new ArrayList<>();
		
		// Start & end dates
		Relationship r = new Relationship(); // 0
		r.setPersonA(personA);
		r.setPersonB(personB);
		r.setRelationshipType(rt);
		r.setStartDate(df.parse("1980-01-01"));
		r.setEndDate(df.parse("2010-01-01"));
		personService.saveRelationship(r);
		rels.add(r);
		
		r = new Relationship(); // 1
		r.setPersonA(personA);
		r.setPersonB(personB);
		r.setRelationshipType(rt);
		r.setStartDate(df.parse("1990-01-01"));
		r.setEndDate(df.parse("2010-01-01"));
		personService.saveRelationship(r);
		rels.add(r);
		
		r = new Relationship(); // 2
		r.setPersonA(personA);
		r.setPersonB(personB);
		r.setRelationshipType(rt);
		r.setStartDate(df.parse("1980-01-01"));
		r.setEndDate(df.parse("1990-01-01"));
		personService.saveRelationship(r);
		rels.add(r);
		
		// Only start dates
		r = new Relationship(); // 3
		r.setPersonA(personA);
		r.setPersonB(personB);
		r.setRelationshipType(rt);
		r.setStartDate(df.parse("1980-01-01"));
		personService.saveRelationship(r);
		rels.add(r);
		
		r = new Relationship(); // 4
		r.setPersonA(personA);
		r.setPersonB(personB);
		r.setRelationshipType(rt);
		r.setStartDate(df.parse("1990-01-01"));
		personService.saveRelationship(r);
		rels.add(r);
		
		r = new Relationship(); // 5
		r.setPersonA(personA);
		r.setPersonB(personB);
		r.setRelationshipType(rt);
		r.setStartDate(df.parse("2010-01-01"));
		personService.saveRelationship(r);
		rels.add(r);
		
		// Only end dates
		r = new Relationship(); // 6
		r.setPersonA(personA);
		r.setPersonB(personB);
		r.setRelationshipType(rt);
		r.setEndDate(df.parse("1980-01-01"));
		personService.saveRelationship(r);
		rels.add(r);
		
		r = new Relationship(); // 7
		r.setPersonA(personA);
		r.setPersonB(personB);
		r.setRelationshipType(rt);
		r.setEndDate(df.parse("1990-01-01"));
		personService.saveRelationship(r);
		rels.add(r);
		
		r = new Relationship(); // 8
		r.setPersonA(personA);
		r.setPersonB(personB);
		r.setRelationshipType(rt);
		r.setEndDate(df.parse("2010-01-01"));
		personService.saveRelationship(r);
		rels.add(r);
		
		return rels;
	}
	
	/**
	 * Creates several relationships. Tests that a relationship is returned only when the effective
	 * date is as follows: - for relationships with both a start date and an end date, the effective
	 * date falls between the start and end dates; - for relationships with only a start date, the
	 * effective date falls after the start date; - for relationships with only an end date, the
	 * effective date falls before the end date; - relationship with neither a start nor end date
	 * are always returned.
	 * 
	 * @see PersonService#getRelationshipsByPerson(Person,Date)
	 */
	@Test
	public void getRelationshipsByPerson_shouldFetchRelationshipsThatWereActiveDuringEffectiveDate() throws Exception {
		executeDataSet(CREATE_PATIENT_XML);
		executeDataSet(CREATE_RELATIONSHIP_XML);
		
		// TODO use xml imported in BaseContextSensitiveTest#baseSetupWithStandardDataAndAuthentication()
		Patient patient = createTestPatient();
		List<Relationship> rels = createTestDatedRelationships(ps.getPatient(2), patient, personService
		        .getRelationshipType(4));
		
		// Get relationships effective 1988-01-01
		List<Relationship> res = personService.getRelationshipsByPerson(patient, df.parse("1988-01-01"));
		
		// Verify # of results and which results we have received
		assertEquals(5, res.size());
		for (Relationship rr : res) {
			if (!rr.equals(rels.get(0)) && !rr.equals(rels.get(2)) && !rr.equals(rels.get(3)) && !rr.equals(rels.get(7))
			        && !rr.equals(rels.get(8))) {
				if (rr.equals(rels.get(1))) {
					fail("unexpected relationship 1 in results from getRelationshipsByPerson with effeciveDate of 1988-01-01");
				} else if (rr.equals(rels.get(4))) {
					fail("unexpected relationship 4 in results from getRelationshipsByPerson with effeciveDate of 1988-01-01");
				} else if (rr.equals(rels.get(5))) {
					fail("unexpected relationship 5 in results from getRelationshipsByPerson with effeciveDate of 1988-01-01");
				} else if (rr.equals(rels.get(6))) {
					fail("unexpected relationship 6 in results from getRelationshipsByPerson with effeciveDate of 1988-01-01");
				} else {
					fail("unrecognized unexpected relationship in results from getRelationshipsByPerson with effeciveDate of 1988-01-01");
				}
			}
		}
	}
	
	/**
	 * This test should get the first/last name out of a string into a PersonName object.
	 * 
	 * @see PersonService#parsePersonName(String)
	 */
	@Test
	public void parsePersonName_shouldParseTwoPersonNameWithComma() throws Exception {
		PersonName pname = Context.getPersonService().parsePersonName("Doe, John");
		assertEquals("Doe", pname.getFamilyName());
		assertEquals("John", pname.getGivenName());
		
		// try without a space
		pname = Context.getPersonService().parsePersonName("Doe,John");
		assertEquals("Doe", pname.getFamilyName());
		assertEquals("John", pname.getGivenName());
	}
	
	/**
	 * @see PersonService#parsePersonName(String)
	 */
	@Test
	public void parsePersonName_shouldParseTwoPersonNameWithoutComma() throws Exception {
		PersonName pname2 = Context.getPersonService().parsePersonName("John Doe");
		assertEquals("Doe", pname2.getFamilyName());
		assertEquals("John", pname2.getGivenName());
	}
	
	/**
	 * @see PersonService#savePersonAttributeType(PersonAttributeType)
	 */
	@Test
	public void savePersonAttributeType_shouldSetTheDateCreatedAndCreatorOnNew() throws Exception {
		PersonService service = Context.getPersonService();
		
		PersonAttributeType pat = new PersonAttributeType();
		pat.setName("attr type name");
		pat.setDescription("attr type desc");
		pat.setFormat("java.lang.String");
		
		service.savePersonAttributeType(pat);
		
		assertEquals(1, pat.getCreator().getId().intValue());
		assertNotNull(pat.getDateCreated());
	}
	
	/**
	 * @see PersonService#savePersonAttributeType(PersonAttributeType)
	 */
	@Test
	public void savePersonAttributeType_shouldSetTheDateChangedAndChangedByOnUpdate() throws Exception {
		PersonService service = Context.getPersonService();
		
		// get the type and change something about it
		PersonAttributeType pat = service.getPersonAttributeType(2);
		pat.setName("attr type name");
		
		// save the type again
		service.savePersonAttributeType(pat);
		
		assertEquals(1, pat.getChangedBy().getId().intValue());
		assertNotNull(pat.getDateChanged());
	}
	
	/**
	 * @see PersonService#savePersonAttributeType(PersonAttributeType)
	 */
	@Test
	public void savePersonAttributeType_shouldUpdateAnyGlobalPropertyWhichReferenceThisType() throws Exception {
		executeDataSet("org/openmrs/api/include/PersonServiceTest-updatePersonAttributeType.xml");
		PersonService service = Context.getPersonService();
		AdministrationService as = Context.getAdministrationService();
		
		// get the type and change its name
		PersonAttributeType pat = service.getPersonAttributeType(1);
		assertEquals("Race", pat.getName());
		
		String patientHeader = as.getGlobalProperty(OpenmrsConstants.GLOBAL_PROPERTY_PATIENT_HEADER_ATTRIBUTES);
		assertEquals("Race,Birthpalce", patientHeader);
		String patientListing = as.getGlobalProperty(OpenmrsConstants.GLOBAL_PROPERTY_PATIENT_LISTING_ATTRIBUTES);
		assertEquals("Race,Birthpalce", patientListing);
		String patientViewing = as.getGlobalProperty(OpenmrsConstants.GLOBAL_PROPERTY_PATIENT_VIEWING_ATTRIBUTES);
		assertEquals("Birthpalce", patientViewing);
		
		pat.setName("Race Updated");
		pat = service.savePersonAttributeType(pat);
		assertEquals("Race Updated", pat.getName());
		
		patientHeader = as.getGlobalProperty(OpenmrsConstants.GLOBAL_PROPERTY_PATIENT_HEADER_ATTRIBUTES);
		assertEquals("Race Updated,Birthpalce", patientHeader);
		patientListing = as.getGlobalProperty(OpenmrsConstants.GLOBAL_PROPERTY_PATIENT_LISTING_ATTRIBUTES);
		assertEquals("Race Updated,Birthpalce", patientListing);
		patientViewing = as.getGlobalProperty(OpenmrsConstants.GLOBAL_PROPERTY_PATIENT_VIEWING_ATTRIBUTES);
		assertEquals("Birthpalce", patientViewing);
	}
	
	/**
	 * @see PersonService#getSimilarPeople(String,Integer,String)
	 */
	@Test
	public void getSimilarPeople_shouldAcceptGreaterThanThreeNames() throws Exception {
		executeDataSet("org/openmrs/api/include/PersonServiceTest-names.xml");
		Set<Person> matches = Context.getPersonService().getSimilarPeople("Darius Graham Jazayeri Junior", 1979, "M");
		Assert.assertEquals(2, matches.size());
		assertTrue(containsId(matches, 1006));
		assertTrue(containsId(matches, 1007));
	}
	
	/**
	 * @see PersonService#getSimilarPeople(String,Integer,String)
	 */
	@Test
	public void getSimilarPeople_shouldMatchSingleSearchToAnyNamePart() throws Exception {
		executeDataSet("org/openmrs/api/include/PersonServiceTest-names.xml");
		Set<Person> matches = Context.getPersonService().getSimilarPeople("Darius", 1979, "M");
		Assert.assertEquals(9, matches.size());
		assertTrue(containsId(matches, 1000));
		assertTrue(containsId(matches, 1001));
		assertTrue(containsId(matches, 1002));
		assertTrue(containsId(matches, 1003));
		assertTrue(containsId(matches, 1004));
		assertTrue(containsId(matches, 1005));
		assertTrue(containsId(matches, 1006));
		assertTrue(containsId(matches, 1007));
		assertTrue(containsId(matches, 1008));
	}
	
	/**
	 * @see PersonService#getSimilarPeople(String,Integer,String)
	 */
	@Test
	public void getSimilarPeople_shouldMatchTwoWordSearchToAnyNamePart() throws Exception {
		executeDataSet("org/openmrs/api/include/PersonServiceTest-names.xml");
		Set<Person> matches = Context.getPersonService().getSimilarPeople("Darius Graham", 1979, "M");
		Assert.assertEquals(6, matches.size());
		assertTrue(containsId(matches, 1000));
		assertTrue(containsId(matches, 1003));
		assertTrue(containsId(matches, 1004));
		assertTrue(containsId(matches, 1005));
		assertTrue(containsId(matches, 1006));
		assertTrue(containsId(matches, 1007));
	}
	
	/**
	 * @see PersonService#getSimilarPeople(String,Integer,String)
	 */
	@Test
	public void getSimilarPeople_shouldMatchThreeWordSearchToAnyNamePart() throws Exception {
		executeDataSet("org/openmrs/api/include/PersonServiceTest-names.xml");
		Set<Person> matches = Context.getPersonService().getSimilarPeople("Darius Graham Jazayeri", 1979, "M");
		Assert.assertEquals(3, matches.size());
		assertTrue(containsId(matches, 1003));
		assertTrue(containsId(matches, 1006));
		assertTrue(containsId(matches, 1007));
	}
	
	/**
	 * @see PersonService#getPeople(String,Boolean)
	 */
	@Test
	public void getPeople_shouldMatchSearchToFamilyName2() throws Exception {
		executeDataSet("org/openmrs/api/include/PersonServiceTest-extranames.xml");
		updateSearchIndex();
		
		List<Person> people = Context.getPersonService().getPeople("Johnson", false);
		Assert.assertEquals(3, people.size());
		assertTrue(TestUtil.containsId(people, 2));
		assertTrue(TestUtil.containsId(people, 4));
		assertTrue(TestUtil.containsId(people, 5));
	}
	
	/**
	 * @see PersonService#getSimilarPeople(String,Integer,String)
	 */
	@Test
	public void getSimilarPeople_shouldMatchSearchToFamilyName2() throws Exception {
		executeDataSet("org/openmrs/api/include/PersonServiceTest-extranames.xml");
		
		Set<Person> people = Context.getPersonService().getSimilarPeople("Johnson", null, "M");
		Assert.assertEquals(2, people.size());
		assertTrue(TestUtil.containsId(people, 2));
		assertTrue(TestUtil.containsId(people, 4));
	}
	
	/**
	 * @see PersonService#getAllPersonAttributeTypes()
	 */
	@Test
	public void getAllPersonAttributeTypes_shouldReturnAllPersonAttributeTypesIncludingRetired() throws Exception {
		executeDataSet("org/openmrs/api/include/PersonServiceTest-createRetiredPersonAttributeType.xml");
		
		List<PersonAttributeType> attributeTypes = Context.getPersonService().getAllPersonAttributeTypes();
		assertTrue("At least one element, otherwise no checking for retired will take place",
		    attributeTypes.size() > 0);
		
		boolean foundRetired = false;
		for (PersonAttributeType personAttributeType : attributeTypes) {
			if (personAttributeType.getRetired()) {
				foundRetired = true;
				break;
			}
		}
		
		assertTrue("There should be at least one retired person attribute type found in the list", foundRetired);
	}
	
	/**
	 * @see PersonService#getAllPersonAttributeTypes(null)
	 */
	@Test
	public void getAllPersonAttributeTypes_shouldReturnAllPersonAttributeTypesExcludingRetiredWhenIncludeRetiredIsFalse()
	        throws Exception {
		executeDataSet("org/openmrs/api/include/PersonServiceTest-createRetiredPersonAttributeType.xml");
		
		List<PersonAttributeType> attributeTypes = Context.getPersonService().getAllPersonAttributeTypes(false);
		assertTrue("At least one element, otherwise no checking for retired will take place",
		    attributeTypes.size() > 0);
		
		boolean foundRetired = false;
		for (PersonAttributeType personAttributeType : attributeTypes) {
			if (personAttributeType.getRetired()) {
				foundRetired = true;
				break;
			}
		}
		
		Assert.assertFalse("There should be no retired person attribute type found in the list", foundRetired);
	}
	
	/**
	 * @see PersonService#getAllPersonAttributeTypes(null)
	 */
	@Test
	public void getAllPersonAttributeTypes_shouldReturnAllPersonAttributeTypesIncludingRetiredWhenIncludeRetiredIsTrue()
	        throws Exception {
		executeDataSet("org/openmrs/api/include/PersonServiceTest-createRetiredPersonAttributeType.xml");
		
		//TODO: is this the correct way? or should we loop to find a retired type and then perform the following?
		List<PersonAttributeType> attributeTypes = Context.getPersonService().getAllPersonAttributeTypes(true);
		assertTrue("At least one element, otherwise no checking for retired will take place",
		    attributeTypes.size() > 0);
		
		boolean foundRetired = false;
		for (PersonAttributeType personAttributeType : attributeTypes) {
			if (personAttributeType.getRetired()) {
				foundRetired = true;
				break;
			}
		}
		
		assertTrue("There should be at least one retired person attribute type found in the list", foundRetired);
	}
	
	/**
	 * @see PersonService#getAllRelationships()
	 */
	@Test
	public void getAllRelationships_shouldReturnAllUnvoidedRelationships() throws Exception {
		executeDataSet("org/openmrs/api/include/PersonServiceTest-createRetiredRelationship.xml");
		
		List<Relationship> relationships = Context.getPersonService().getAllRelationships();
		assertTrue("At least one element, otherwise no checking for voided will take place",
		            relationships.size() > 0);
		
		boolean foundVoided = false;
		for (Relationship relationship : relationships) {
			if (relationship.getVoided()) {
				foundVoided = true;
				break;
			}
		}
		
		Assert.assertFalse("There should be no voided relationship here", foundVoided);
	}
	
	/**
	 * @see PersonService#getAllRelationships(null)
	 */
	@Test
	public void getAllRelationships_shouldReturnAllRelationshipIncludingVoidedWhenIncludeVoidedEqualsTrue() throws Exception {
		executeDataSet("org/openmrs/api/include/PersonServiceTest-createRetiredRelationship.xml");
		
		List<Relationship> relationships = Context.getPersonService().getAllRelationships(true);
		assertTrue("At least one element, otherwise no checking for voided will take place",
		            relationships.size() > 0);
		
		boolean foundVoided = false;
		for (Relationship relationship : relationships) {
			if (relationship.getVoided()) {
				foundVoided = true;
				break;
			}
		}
		
		assertTrue("There should be voided relationship here", foundVoided);
	}
	
	/**
	 * @see PersonService#getAllRelationships(null)
	 */
	@Test
	public void getAllRelationships_shouldReturnAllRelationshipExcludingVoidedWhenIncludeVoidedEqualsFalse()
	        throws Exception {
		executeDataSet("org/openmrs/api/include/PersonServiceTest-createRetiredRelationship.xml");
		
		List<Relationship> relationships = Context.getPersonService().getAllRelationships(false);
		assertTrue("At least one element, otherwise no checking for voided will take place",
		            relationships.size() > 0);
		
		boolean foundVoided = false;
		for (Relationship relationship : relationships) {
			if (relationship.getVoided()) {
				foundVoided = true;
				break;
			}
		}
		
		Assert.assertFalse("There should be no voided relationship here", foundVoided);
	}
	
	/**
	 * @see PersonService#getAllRelationshipTypes()
	 */
	@Test
	public void getAllRelationshipTypes_shouldReturnAllRelationshipTypes() throws Exception {
		executeDataSet("org/openmrs/api/include/PersonServiceTest-createRetiredRelationship.xml");
		
		List<RelationshipType> relationshipTypes = Context.getPersonService().getAllRelationshipTypes();
		assertTrue("Number of relationship type are 6", relationshipTypes.size() == 6);
	}
	
	@Test(expected = APIException.class)
	public void retireRelationshipType_shouldFailIfGivenReasonIsNull() {
		
		personService.retireRelationshipType(new RelationshipType(), null);
	}
	
	@Test(expected = APIException.class)
	public void retireRelationshipType_shouldFailIfGivenReasonIsEmptyString() {
		
		personService.retireRelationshipType(new RelationshipType(), "");
	}
	
	@Test
	public void retireRelationshipType_shouldRetireGivenRelationshipType() {
		
		RelationshipType rt = personService.getRelationshipType(1);
		assertFalse(rt.getRetired());
		String reason = "reason";
		
		personService.retireRelationshipType(rt, reason);
		
		assertTrue(rt.getRetired());
		assertThat(rt.getRetiredBy(), is(Context.getAuthenticatedUser()));
		assertNotNull(rt.getDateRetired());
		assertThat(rt.getRetireReason(), is(reason));
	}
	
	@Test
	public void unretireRelationshipType_shouldRetireGivenRelationshipType() {
		
		RelationshipType rt = personService.getRelationshipType(1);
		personService.retireRelationshipType(rt, "reason");
		assertTrue(rt.getRetired());
		
		personService.unretireRelationshipType(rt);
		
		assertFalse(rt.getRetired());
		assertNull(rt.getRetiredBy());
		assertNull(rt.getDateRetired());
		assertNull(rt.getRetireReason());
	}
	
	/**
	 * @see PersonService#getPerson(Integer)
	 */
	@Test
	public void getPerson_shouldReturnNullWhenPersonNull() throws Exception {
		Assert.assertNull(Context.getPersonService().getPerson(null));
	}
	
	/**
	 * @see PersonService#getPerson(Integer)
	 */
	@Test
	public void getPerson_shouldReturnNullWhenNoPersonHasTheGivenId() throws Exception {
		Person person = Context.getPersonService().getPerson(10000);
		Assert.assertNull(person);
	}
	
	/**
	 * @see PersonService#getPersonAttribute(Integer)
	 */
	@Test
	public void getPersonAttribute_shouldReturnNullWhenGivenIdDoesNotExist() throws Exception {
		PersonAttribute personAttribute = Context.getPersonService().getPersonAttribute(10000);
		Assert.assertNull(personAttribute);
	}
	
	/**
	 * @see PersonService#getPersonAttribute(Integer)
	 */
	@Test
	public void getPersonAttribute_shouldReturnPersonAttributeWhenGivenIdDoesExist() throws Exception {
		PersonAttribute personAttribute = Context.getPersonService().getPersonAttribute(17);
		Assert.assertNotNull(personAttribute);
		assertTrue("Expecting the return is of a person attribute", personAttribute.getClass().equals(
		    PersonAttribute.class));
	}
	
	/**
	 * @see PersonService#getPersonAttributeType(Integer)
	 */
	@Test
	public void getPersonAttributeType_shouldReturnNullWhenNoPersonAttributeWithTheGivenIdExist() throws Exception {
		PersonAttributeType attributeType = Context.getPersonService().getPersonAttributeType(10000);
		Assert.assertNull(attributeType);
	}
	
	/**
	 * @see PersonService#getPersonAttributeTypeByName(String)
	 */
	@Test
	public void getPersonAttributeTypeByName_shouldReturnPersonAttributeTypeWhenNameMatchesGivenTypeName() throws Exception {
		PersonAttributeType attributeType = Context.getPersonService().getPersonAttributeTypeByName("Birthplace");
		Assert.assertNotNull(attributeType);
	}
	
	/**
	 * @see PersonService#getPersonAttributeTypeByName(String)
	 */
	@Test
	public void getPersonAttributeTypeByName_shouldReturnNullWhenNoPersonAttributeTypeMatchGivenTypeName() throws Exception {
		PersonAttributeType attributeType = Context.getPersonService().getPersonAttributeTypeByName("Credit Card");
		Assert.assertNull(attributeType);
	}
	
	/**
	 * @see PersonService#getPersonAttributeTypes(String,String,Integer,Boolean)
	 */
	@Test
	public void getPersonAttributeTypes_shouldReturnPersonAttributeTypesMatchingGivenParameters() throws Exception {
		executeDataSet("org/openmrs/api/include/PersonServiceTest-createRetiredPersonAttributeType.xml");
		
		List<PersonAttributeType> attributeTypes = Context.getPersonService().getPersonAttributeTypes(
		    "A nonexistent attr type name", null, null, null);
		Assert.assertNotNull(attributeTypes);
		assertTrue("Number of matched attribute type is 0", attributeTypes.isEmpty());
		
		attributeTypes = Context.getPersonService().getPersonAttributeTypes(null, "org.openmrs.Concept", null, null);
		Assert.assertNotNull(attributeTypes);
		assertTrue("Number of matched attribute type is 1", attributeTypes.size() == 1);
		
		attributeTypes = Context.getPersonService().getPersonAttributeTypes(null, null, null, false);
		Assert.assertNotNull(attributeTypes);
		assertTrue("Number of matched attribute type is 6", attributeTypes.size() == 6);
	}
	
	/**
	 * @see PersonService#getPersonAttributeTypes(String,String,Integer,Boolean)
	 */
	@Test
	public void getPersonAttributeTypes_shouldReturnEmptyListWhenNoPersonAttributeTypesMatchGivenParameters()
	        throws Exception {
		executeDataSet("org/openmrs/api/include/PersonServiceTest-createRetiredPersonAttributeType.xml");
		
		List<PersonAttributeType> attributeTypes = Context.getPersonService().getPersonAttributeTypes(
		    "A non-existent attr type name", "java.lang.String", null, false);
		Assert.assertNotNull(attributeTypes);
		assertTrue("Should return empty list", attributeTypes.isEmpty());
	}
	
	/**
	 * @see PersonService#getRelationship(Integer)
	 */
	@Test
	public void getRelationship_shouldReturnRelationshipWithGivenId() throws Exception {
		Relationship relationship = Context.getPersonService().getRelationship(1);
		Assert.assertNotNull(relationship);
	}
	
	/**
	 * @see PersonService#getRelationship(Integer)
	 */
	@Test
	public void getRelationship_shouldReturnNullWhenRelationshipWithGivenIdDoesNotExist() throws Exception {
		Relationship relationship = Context.getPersonService().getRelationship(10000);
		Assert.assertNull(relationship);
	}
	
	/**
	 * @see PersonService#getRelationshipMap(RelationshipType)
	 */
	@Test
	public void getRelationshipMap_shouldReturnEmptyMapWhenNoRelationshipHasTheMatchingRelationshipType() throws Exception {
		executeDataSet("org/openmrs/api/include/PersonServiceTest-createRetiredRelationship.xml");
		
		PersonService personService = Context.getPersonService();
		
		RelationshipType relationshipType = personService.getRelationshipType(15);
		Map<Person, List<Person>> relationshipMap = personService.getRelationshipMap(relationshipType);
		Assert.assertNotNull(relationshipMap);
		assertTrue("There should be no element in the map", relationshipMap.isEmpty());
	}
	
	/**
	 * @see PersonService#getRelationships(Person,Person,RelationshipType)
	 */
	@Test
	public void getRelationships_shouldFetchRelationshipsMatchingTheGivenFromPerson() throws Exception {
		PersonService personService = Context.getPersonService();
		
		Person firstPerson = personService.getPerson(502);
		List<Relationship> relationships = personService.getRelationships(firstPerson, null, null);
		Assert.assertNotNull(relationships);
		assertTrue("There should be relationship found given the from person", relationships.size() > 0);
	}
	
	/**
	 * @see PersonService#getRelationships(Person,Person,RelationshipType)
	 */
	@Test
	public void getRelationships_shouldFetchRelationshipsMatchingTheGivenToPerson() throws Exception {
		PersonService personService = Context.getPersonService();
		
		Person secondPerson = personService.getPerson(7);
		List<Relationship> relationships = personService.getRelationships(null, secondPerson, null);
		Assert.assertNotNull(relationships);
		assertTrue("There should be relationship found given the to person", relationships.size() > 0);
	}
	
	/**
	 * @see PersonService#getRelationships(Person,Person,RelationshipType)
	 */
	@Test
	public void getRelationships_shouldFetchRelationshipsMatchingTheGivenRelType() throws Exception {
		PersonService personService = Context.getPersonService();
		
		RelationshipType relationshipType = personService.getRelationshipType(1);
		List<Relationship> relationships = personService.getRelationships(null, null, relationshipType);
		Assert.assertNotNull(relationships);
		assertTrue("There should be relationship found given the relationship type", relationships.size() > 0);
	}
	
	/**
	 * @see PersonService#getRelationships(Person,Person,RelationshipType,Date)
	 */
	@Test
	public void getRelationships2_shouldFetchRelationshipsMatchingTheGivenFromPerson() throws Exception {
		PersonService personService = Context.getPersonService();
		
		Person firstPerson = personService.getPerson(502);
		List<Relationship> relationships = personService.getRelationships(firstPerson, null, null, new Date());
		Assert.assertNotNull(relationships);
		assertTrue("There should be relationship found given the from person", relationships.size() > 0);
	}
	
	/**
	 * @see PersonService#getRelationships(Person,Person,RelationshipType,Date)
	 */
	@Test
	public void getRelationships2_shouldFetchRelationshipsMatchingTheGivenToPerson() throws Exception {
		PersonService personService = Context.getPersonService();
		
		Person secondPerson = personService.getPerson(7);
		List<Relationship> relationships = personService.getRelationships(null, secondPerson, null, new Date());
		Assert.assertNotNull(relationships);
		assertTrue("There should be relationship found given the to person", relationships.size() > 0);
	}
	
	/**
	 * @see PersonService#getRelationships(Person,Person,RelationshipType,Date)
	 */
	@Test
	public void getRelationships2_shouldFetchRelationshipsMatchingTheGivenRelType() throws Exception {
		PersonService personService = Context.getPersonService();
		
		RelationshipType relationshipType = personService.getRelationshipType(1);
		List<Relationship> relationships = personService.getRelationships(null, null, relationshipType, new Date());
		Assert.assertNotNull(relationships);
		assertTrue("There should be relationship found given the relationship type", relationships.size() > 0);
	}
	
	/**
	 * @see PersonService#getRelationships(Person,Person,RelationshipType,Date)
	 */
	@Test
	public void getRelationships2_shouldReturnEmptyListWhenNoRelationshipMatchingGivenParametersExist() throws Exception {
		PersonService personService = Context.getPersonService();
		
		Person firstPerson = personService.getPerson(501);
		Person secondPerson = personService.getPerson(2);
		RelationshipType relationshipType = personService.getRelationshipType(1);
		List<Relationship> relationships = personService.getRelationships(firstPerson, secondPerson, relationshipType,
		    new Date());
		Assert.assertNotNull(relationships);
		assertTrue("There should be no relationship found given the from person", relationships.isEmpty());
	}
	
	/**
	 * @see PersonService#getRelationships(Person,Person,RelationshipType,Date)
	 */
	@Test
	public void getRelationships2_shouldFetchRelationshipsThatWereActiveDuringEffectiveDate() throws Exception {
		executeDataSet(CREATE_PATIENT_XML);
		executeDataSet(CREATE_RELATIONSHIP_XML);
		
		// TODO use xml imported in BaseContextSensitiveTest#baseSetupWithStandardDataAndAuthentication()
		Patient patient = createTestPatient();
		List<Relationship> rels = createTestDatedRelationships(ps.getPatient(2), patient, personService
		        .getRelationshipType(4));
		
		// Get relationships effective 1988-01-01
		List<Relationship> res = personService.getRelationships(ps.getPatient(2), patient, null, df.parse("1988-01-01"));
		
		// Verify # of results and which results we have received
		assertEquals(5, res.size());
		for (Relationship rr : res) {
			if (!rr.equals(rels.get(0)) && !rr.equals(rels.get(2)) && !rr.equals(rels.get(3)) && !rr.equals(rels.get(7))
			        && !rr.equals(rels.get(8))) {
				if (rr.equals(rels.get(1))) {
					fail("unexpected relationship 1 in results from getRelationshipsByPerson with effeciveDate of 1988-01-01");
				} else if (rr.equals(rels.get(4))) {
					fail("unexpected relationship 4 in results from getRelationshipsByPerson with effeciveDate of 1988-01-01");
				} else if (rr.equals(rels.get(5))) {
					fail("unexpected relationship 5 in results from getRelationshipsByPerson with effeciveDate of 1988-01-01");
				} else if (rr.equals(rels.get(6))) {
					fail("unexpected relationship 6 in results from getRelationshipsByPerson with effeciveDate of 1988-01-01");
				} else {
					fail("unrecognized unexpected relationship in results from getRelationshipsByPerson with effeciveDate of 1988-01-01");
				}
			}
		}
	}
	
	/**
	 * @see PersonService#getRelationships(Person,Person,RelationshipType,Date,Date)
	 */
	@Test
	public void getRelationships3_shouldFetchRelationshipsMatchingTheGivenFromPerson() throws Exception {
		PersonService personService = Context.getPersonService();
		
		Person firstPerson = personService.getPerson(502);
		List<Relationship> relationships = personService.getRelationships(firstPerson, null, null, new Date(), new Date());
		Assert.assertNotNull(relationships);
		assertTrue("There should be relationship found given the from person", relationships.size() > 0);
	}
	
	/**
	 * @see PersonService#getRelationships(Person,Person,RelationshipType,Date,Date)
	 */
	@Test
	public void getRelationships3_shouldFetchRelationshipsMatchingTheGivenToPerson() throws Exception {
		PersonService personService = Context.getPersonService();
		
		Person secondPerson = personService.getPerson(7);
		List<Relationship> relationships = personService.getRelationships(null, secondPerson, null, new Date(), new Date());
		Assert.assertNotNull(relationships);
		assertTrue("There should be relationship found given the to person", relationships.size() > 0);
	}
	
	/**
	 * @see PersonService#getRelationships(Person,Person,RelationshipType,Date,Date)
	 */
	@Test
	public void getRelationships3_shouldFetchRelationshipsMatchingTheGivenRelType() throws Exception {
		PersonService personService = Context.getPersonService();
		
		RelationshipType relationshipType = personService.getRelationshipType(1);
		List<Relationship> relationships = personService.getRelationships(null, null, relationshipType, new Date(),
		    new Date());
		Assert.assertNotNull(relationships);
		assertTrue("There should be relationship found given the relationship type", relationships.size() > 0);
	}
	
	/**
	 * @see PersonService#getRelationships(Person,Person,RelationshipType,Date,Date)
	 */
	@Test
	public void getRelationships3_shouldReturnEmptyListWhenNoRelationshipMatchingGivenParametersExist() throws Exception {
		PersonService personService = Context.getPersonService();
		
		Person firstPerson = personService.getPerson(501);
		Person secondPerson = personService.getPerson(2);
		RelationshipType relationshipType = personService.getRelationshipType(1);
		List<Relationship> relationships = personService.getRelationships(firstPerson, secondPerson, relationshipType,
		    new Date(), new Date());
		Assert.assertNotNull(relationships);
		assertTrue("There should be no relationship found given the from person", relationships.isEmpty());
	}
	
	/**
	 * @see PersonService#getRelationships(Person,Person,RelationshipType,Date,Date)
	 */
	@Test
	public void getRelationships3_shouldFetchRelationshipsThatWereActiveDuringTheSpecifiedDateRange() throws Exception {
		executeDataSet(CREATE_PATIENT_XML);
		executeDataSet(CREATE_RELATIONSHIP_XML);
		
		// TODO use xml imported in BaseContextSensitiveTest#baseSetupWithStandardDataAndAuthentication()
		Patient patient = createTestPatient();
		List<Relationship> rels = createTestDatedRelationships(ps.getPatient(2), patient, personService
		        .getRelationshipType(4));
		
		// Get relationships effective between 1987-01-01 and 1988-01-01
		List<Relationship> res = personService.getRelationships(ps.getPatient(2), patient, null, df.parse("1987-01-01"), df
		        .parse("1988-01-01"));
		
		// Verify # of results and which results we have received
		assertEquals(5, res.size());
		for (Relationship rr : res) {
			if (!rr.equals(rels.get(0)) && !rr.equals(rels.get(2)) && !rr.equals(rels.get(3)) && !rr.equals(rels.get(7))
			        && !rr.equals(rels.get(8))) {
				if (rr.equals(rels.get(1))) {
					fail("unexpected relationship 1 in results from getRelationshipsByPerson effective between 1987-01-01 and 1988-01-01");
				} else if (rr.equals(rels.get(4))) {
					fail("unexpected relationship 4 in results from getRelationshipsByPerson effective between 1987-01-01 and 1988-01-01");
				} else if (rr.equals(rels.get(5))) {
					fail("unexpected relationship 5 in results from getRelationshipsByPerson effective between 1987-01-01 and 1988-01-01");
				} else if (rr.equals(rels.get(6))) {
					fail("unexpected relationship 6 in results from getRelationshipsByPerson effective between 1987-01-01 and 1988-01-01");
				} else {
					fail("unrecognized unexpected relationship in results from getRelationshipsByPerson effective between 1987-01-01 and 1988-01-01");
				}
			}
		}
	}
	
	/**
	 * @see PersonService#getRelationshipsByPerson(Person)
	 */
	@Test
	public void getRelationshipsByPerson_shouldFetchRelationshipsAssociatedWithTheGivenPerson() throws Exception {
		PersonService personService = Context.getPersonService();
		
		Person person = personService.getPerson(2);
		List<Relationship> relationships = personService.getRelationshipsByPerson(person);
		Assert.assertNotNull(relationships);
		assertTrue("There should be relationship found given the person", relationships.size() > 0);
	}
	
	/**
	 * @see PersonService#getRelationshipsByPerson(Person)
	 */
	@Test
	public void getRelationshipsByPerson_shouldFetchUnvoidedRelationshipsOnly() throws Exception {
		executeDataSet("org/openmrs/api/include/PersonServiceTest-createRetiredRelationship.xml");
		
		PersonService personService = Context.getPersonService();
		
		Person person = personService.getPerson(6);
		List<Relationship> relationships = personService.getRelationshipsByPerson(person);
		Assert.assertNotNull(relationships);
		assertTrue("There should be no relationship found given the person", relationships.isEmpty());
		
	}
	
	/**
	 * @see PersonService#getRelationshipsByPerson(Person)
	 */
	@Test
	public void getRelationshipsByPerson2_shouldFetchRelationshipsAssociatedWithTheGivenPerson() throws Exception {
		PersonService personService = Context.getPersonService();
		
		Person person = personService.getPerson(2);
		List<Relationship> relationships = personService.getRelationshipsByPerson(person, new Date());
		Assert.assertNotNull(relationships);
		assertTrue("There should be relationship found given the person", relationships.size() > 0);
	}
	
	/**
	 * @see PersonService#getRelationshipsByPerson(Person)
	 */
	@Test
	public void getRelationshipsByPerson2_shouldFetchUnvoidedRelationshipsOnly() throws Exception {
		executeDataSet("org/openmrs/api/include/PersonServiceTest-createRetiredRelationship.xml");
		
		PersonService personService = Context.getPersonService();
		
		Person person = personService.getPerson(6);
		List<Relationship> relationships = personService.getRelationshipsByPerson(person, new Date());
		Assert.assertNotNull(relationships);
		assertTrue("There should be no relationship found given the person", relationships.isEmpty());
		
	}
	
	/**
	 * @see PersonService#getRelationshipType(Integer)
	 */
	@Test
	public void getRelationshipType_shouldReturnRelationshipTypeWithTheGivenRelationshipTypeId() throws Exception {
		RelationshipType relationshipType = Context.getPersonService().getRelationshipType(1);
		Assert.assertNotNull(relationshipType);
		assertTrue("Expecting the return is of a relationship type", relationshipType.getClass().equals(
		    RelationshipType.class));
	}
	
	/**
	 * @see PersonService#getRelationshipType(Integer)
	 */
	@Test
	public void getRelationshipType_shouldReturnNullWhenNoRelationshipTypeMatchesGivenRelationshipTypeId() throws Exception {
		RelationshipType relationshipType = Context.getPersonService().getRelationshipType(10000);
		Assert.assertNull(relationshipType);
	}
	
	/**
	 * @see PersonService#getRelationshipTypeByName(String)
	 */
	@Test
	public void getRelationshipTypeByName_shouldReturnNullWhenNoRelationshipTypeMatchTheGivenName() throws Exception {
		RelationshipType relationshipType = Context.getPersonService().getRelationshipTypeByName("Supervisor");
		Assert.assertNull(relationshipType);
	}
	
	/**
	 * @see PersonService#getRelationshipTypes(String)
	 */
	@Test
	public void getRelationshipTypes_shouldReturnEmptyListWhenNoRelationshipTypeMatchTheSearchString() throws Exception {
		List<RelationshipType> relationshipTypes = Context.getPersonService().getRelationshipTypes("Doctor");
		Assert.assertNotNull(relationshipTypes);
		assertTrue("There should be no relationship type for the given name", relationshipTypes.isEmpty());
	}
	
	/**
	 * @see PersonService#getRelationshipTypes(String,Boolean) TODO Needs to test
	 *      "preferred"
	 */
	@Test
	public void getRelationshipTypes_shouldReturnListOfPreferredRelationshipTypeMatchingGivenName() throws Exception {
		executeDataSet("org/openmrs/api/include/PersonServiceTest-createRetiredRelationship.xml");
		List<RelationshipType> relationshipTypes = Context.getPersonService().getRelationshipTypes("Sibling/Sibling", true);
		Assert.assertNotNull(relationshipTypes);
		assertTrue("There should be relationship type for the given name", relationshipTypes.size() > 0);
	}
	
	/**
	 * @see PersonService#getRelationshipTypes(String,Boolean)
	 */
	@Test
	public void getRelationshipTypes_shouldReturnEmptyListWhenNoPreferredRelationshipTypeMatchTheGivenName()
	        throws Exception {
		List<RelationshipType> relationshipTypes = Context.getPersonService().getRelationshipTypes("Doctor/Patient", true);
		Assert.assertNotNull(relationshipTypes);
		assertTrue("There should be no relationship type for the given name", relationshipTypes.isEmpty());
	}
	
	/**
	 * @see PersonService#purgePerson(Person)
	 */
	@Test
	public void purgePerson_shouldDeletePersonFromTheDatabase() throws Exception {
		PersonService personService = Context.getPersonService();
		
		User user = Context.getAuthenticatedUser();
		Person person = new Person();
		person.setPersonCreator(user);
		person.setPersonDateCreated(new Date());
		person.setPersonChangedBy(user);
		person.setPersonDateChanged(new Date());
		person.setGender("F");
		Assert.assertNull(person.getId());
		person.addName(new PersonName("givenName", "middleName", "familyName"));
		person = personService.savePerson(person);
		Assert.assertNotNull(person.getId());
		
		personService.purgePerson(person);
		
		Person deletedPerson = personService.getPerson(person.getId());
		Assert.assertNull(deletedPerson);
	}
	
	/**
	 * @see PersonService#purgePersonAttributeType(PersonAttributeType)
	 */
	@Test
	public void purgePersonAttributeType_shouldDeletePersonAttributeTypeFromDatabase() throws Exception {
		PersonService service = Context.getPersonService();
		
		PersonAttributeType pat = new PersonAttributeType();
		pat.setName("attr type name");
		pat.setDescription("attr type desc");
		pat.setFormat("java.lang.String");
		
		service.savePersonAttributeType(pat);
		
		assertNotNull(pat.getId());
		
		service.purgePersonAttributeType(pat);
		
		PersonAttributeType deletedPersonAttributeType = service.getPersonAttributeType(pat.getId());
		Assert.assertNull(deletedPersonAttributeType);
	}
	
	/**
	 * @see PersonService#savePersonMergeLog(PersonMergeLog)
	 */
	@Test(expected = APIException.class)
	public void savePersonMergeLog_shouldRequireLoser() throws Exception {
		PersonMergeLog personMergeLog = getTestPersonMergeLog();
		personMergeLog.setLoser(null);
		Context.getPersonService().savePersonMergeLog(personMergeLog);
	}
	
	/**
	 * @see PersonService#savePersonMergeLog(PersonMergeLog)
	 */
	@Test(expected = APIException.class)
	public void savePersonMergeLog_shouldRequireWinner() throws Exception {
		PersonMergeLog personMergeLog = getTestPersonMergeLog();
		personMergeLog.setWinner(null);
		Context.getPersonService().savePersonMergeLog(personMergeLog);
	}
	
	/**
	 * @see PersonService#savePersonMergeLog(PersonMergeLog)
	 */
	@Test(expected = APIException.class)
	public void savePersonMergeLog_shouldRequirePersonMergeLogData() throws Exception {
		PersonMergeLog personMergeLog = new PersonMergeLog();
		personMergeLog.setPersonMergeLogData(null);
		Context.getPersonService().savePersonMergeLog(personMergeLog);
	}
	
	/**
	 * @see PersonService#savePersonMergeLog(PersonMergeLog)
	 */
	@Test
	public void savePersonMergeLog_shouldSavePersonMergeLog() throws Exception {
		PersonMergeLog personMergeLog = getTestPersonMergeLog();
		try {
			PersonMergeLog persisted = Context.getPersonService().savePersonMergeLog(personMergeLog);
			Assert.assertNotNull("patientMergeLogId has not been set which indicates a problem saving the object", persisted
			        .getPersonMergeLogId());
		}
		catch (Exception ex) {
			Assert.fail("should not fail when all required fields are set. " + ex.getMessage());
		}
	}
	
	/**
	 * @see PersonService#savePersonMergeLog(PersonMergeLog)
	 */
	@Test
	public void savePersonMergeLog_shouldSerializePersonMergeLogData() throws Exception {
		PersonMergeLog personMergeLog = getTestPersonMergeLog();
		personMergeLog.setSerializedMergedData(null);
		PersonMergeLog persisted = Context.getPersonService().savePersonMergeLog(personMergeLog);
		Assert.assertNotNull("PatientMergeLogData has not been serialized", persisted.getSerializedMergedData());
	}
	
	/**
	 * @see PersonService#savePersonMergeLog(PersonMergeLog)
	 */
	@Test
	public void savePersonMergeLog_shouldSetDateCreatedIfNull() throws Exception {
		PersonMergeLog personMergeLog = getTestPersonMergeLog();
		personMergeLog.setDateCreated(null);
		PersonMergeLog persisted = Context.getPersonService().savePersonMergeLog(personMergeLog);
		Assert.assertNotNull("dateCreated has not been set", persisted.getDateCreated());
	}
	
	/**
	 * @see PersonService#savePersonMergeLog(PersonMergeLog)
	 */
	@Test
	public void savePersonMergeLog_shouldSetCreatorIfNull() throws Exception {
		PersonMergeLog personMergeLog = getTestPersonMergeLog();
		personMergeLog.setCreator(null);
		PersonMergeLog persisted = Context.getPersonService().savePersonMergeLog(personMergeLog);
		Assert.assertEquals("creator has not been correctly set", Context.getAuthenticatedUser().getUserId(), persisted
		        .getCreator().getUserId());
	}
	
	/**
	 * @see PersonService#getLosingPersonMergeLog(Person)
	 */
	@Test
	public void getLosingPersonMergeLog_shouldFindPersonMergeLogByLoser() throws Exception {
		//log merge 1 >> 2
		PersonMergeLog personMergeLog12 = getTestPersonMergeLog();
		personMergeLog12.setLoser(new Person(1));
		personMergeLog12.setWinner(new Person(2));
		Context.getPersonService().savePersonMergeLog(personMergeLog12);
		//log merge 2 >> 6
		PersonMergeLog personMergeLog26 = getTestPersonMergeLog();
		personMergeLog26.setLoser(new Person(2));
		personMergeLog26.setWinner(new Person(6));
		Context.getPersonService().savePersonMergeLog(personMergeLog26);
		//find where loser is 2
		PersonMergeLog l = Context.getPersonService().getLosingPersonMergeLog(new Person(2), true);
		Assert.assertEquals("Incorrect PersonMergeLog found by loser", l.getUuid(), personMergeLog26.getUuid());
	}
	
	/**
	 * @see PersonService#getWinningPersonMergeLogs(Person)
	 */
	@Test
	public void getWinningPersonMergeLogs_shouldRetrievePersonMergeLogsByWinner() throws Exception {
		//log merge 1 >> 2
		PersonMergeLog personMergeLog12 = getTestPersonMergeLog();
		personMergeLog12.setLoser(new Person(1));
		personMergeLog12.setWinner(new Person(2));
		Context.getPersonService().savePersonMergeLog(personMergeLog12);
		//log merge 1 >> 6
		PersonMergeLog personMergeLog16 = getTestPersonMergeLog();
		personMergeLog16.setLoser(new Person(1));
		personMergeLog16.setWinner(new Person(6));
		Context.getPersonService().savePersonMergeLog(personMergeLog16);
		//log merge 2 >> 6
		PersonMergeLog personMergeLog26 = getTestPersonMergeLog();
		personMergeLog26.setLoser(new Person(2));
		personMergeLog26.setWinner(new Person(6));
		Context.getPersonService().savePersonMergeLog(personMergeLog26);
		//find where winner is 6
		List<PersonMergeLog> lst = Context.getPersonService().getWinningPersonMergeLogs(new Person(6), true);
		Assert.assertEquals("Incorrect number of PersonMergeLog objects found by winner", 2, lst.size());
		for (PersonMergeLog l : lst) {
			if (!l.getUuid().equals(personMergeLog16.getUuid()) && !l.getUuid().equals(personMergeLog26.getUuid())) {
				fail("Unexpected PersonMergeLog found by winner");
			}
		}
	}
	
	private PersonMergeLog getTestPersonMergeLog() {
		PersonMergeLog personMergeLog = new PersonMergeLog();
		personMergeLog.setLoser(new Person(1));
		personMergeLog.setWinner(new Person(2));
		PersonMergeLogData data = new PersonMergeLogData();
		data.addCreatedAddress("1");
		data.addCreatedAttribute("2");
		data.addCreatedIdentifier("3");
		data.addCreatedName("4");
		data.addCreatedOrder("5");
		data.addCreatedProgram("6");
		data.addCreatedRelationship("7");
		data.addMovedEncounter("8");
		data.addMovedIndependentObservation("9");
		data.addMovedUser("10");
		data.addVoidedRelationship("11");
		data.setPriorCauseOfDeath("test");
		data.setPriorDateOfBirth(new Date());
		data.setPriorDateOfBirthEstimated(true);
		data.setPriorDateOfDeath(new Date());
		data.setPriorGender("F");
		personMergeLog.setPersonMergeLogData(data);
		return personMergeLog;
	}
	
	/**
	 * @see PersonService#getPersonMergeLogByUuid(String,boolean)
	 */
	@Test(expected = APIException.class)
	public void getPersonMergeLogByUuid_shouldRequireUuid() throws Exception {
		Context.getPersonService().getPersonMergeLogByUuid(null, false);
	}
	
	/**
	 * @see PersonService#getPersonMergeLogByUuid(String,boolean)
	 */
	@Test
	public void getPersonMergeLogByUuid_shouldRetrievePersonMergeLogAndDeserializeData() throws Exception {
		PersonMergeLog personMergeLog = getTestPersonMergeLog();
		int originalHashValue = personMergeLog.getPersonMergeLogData().computeHashValue();
		PersonMergeLog persisted = Context.getPersonService().savePersonMergeLog(personMergeLog);
		PersonMergeLog retrieved = Context.getPersonService().getPersonMergeLogByUuid(persisted.getUuid(), true);
		Assert.assertNotNull("problem retrieving PersonMergeLog by UUID", retrieved);
		Assert.assertEquals("deserialized data is not identical to original data", originalHashValue, retrieved
		        .getPersonMergeLogData().computeHashValue());
	}
	
	/**
	 * @see PersonService#getPersonMergeLogByUuid(String,boolean)
	 */
	@Test
	public void getPersonMergeLogByUuid_shouldRetrievePersonMergeLogWithoutDeserializingData() throws Exception {
		PersonMergeLog personMergeLog = getTestPersonMergeLog();
		PersonMergeLog persisted = Context.getPersonService().savePersonMergeLog(personMergeLog);
		PersonMergeLog retrieved = Context.getPersonService().getPersonMergeLogByUuid(persisted.getUuid(), false);
		Assert.assertNotNull("problem retrieving PersonMergeLog by UUID", retrieved);
		
	}
	
	/**
	 * @see PersonService#getAllPersonMergeLogs(boolean)
	 */
	@Test
	public void getAllPersonMergeLogs_shouldRetrieveAllPersonMergeLogsAndDeserializeThem() throws Exception {
		PersonMergeLog personMergeLog = getTestPersonMergeLog();
		Context.getPersonService().savePersonMergeLog(personMergeLog);
		List<PersonMergeLog> result = Context.getPersonService().getAllPersonMergeLogs(true);
		Assert.assertEquals("could not retrieve expected number of PersonMergeLog objects", 1, result.size());
		Assert.assertNotNull("PersonMergeLog at index 0 is null", result.get(0));
		Assert.assertNotNull("PersonMergeLog data has not been deserialized", result.get(0).getPersonMergeLogData());
	}
	
	/**
	 * @see PersonService#getAllPersonMergeLogs(boolean)
	 */
	@Test
	public void getAllPersonMergeLogs_shouldRetrieveAllPersonMergeLogsFromTheModel() throws Exception {
		PersonMergeLog personMergeLog = getTestPersonMergeLog();
		Context.getPersonService().savePersonMergeLog(personMergeLog);
		List<PersonMergeLog> result = Context.getPersonService().getAllPersonMergeLogs(false);
		Assert.assertEquals("could not retrieve expected number of PersonMergeLog objects", 1, result.size());
	}
	
	/**
	 * @see PersonService#purgeRelationship(Relationship)
	 */
	@Test
	public void purgeRelationship_shouldDeleteRelationshipFromTheDatabase() throws Exception {
		PersonService personService = Context.getPersonService();
		
		Relationship relationship = personService.getRelationship(1);
		personService.purgeRelationship(relationship);
		
		Relationship deletedRelationship = personService.getRelationship(1);
		Assert.assertNull(deletedRelationship);
	}
	
	/**
	 * @see PersonService#purgeRelationshipType(RelationshipType)
	 */
	@Test
	public void purgeRelationshipType_shouldDeleteRelationshipTypeFromTheDatabase() throws Exception {
		PersonService personService = Context.getPersonService();
		
		RelationshipType relationshipType = new RelationshipType();
		relationshipType.setDescription("Test relationship");
		relationshipType.setaIsToB("Sister");
		relationshipType.setbIsToA("Brother");
		relationshipType = personService.saveRelationshipType(relationshipType);
		assertNotNull(relationshipType.getId());
		
		personService.purgeRelationshipType(relationshipType);
		
		RelationshipType deletedRelationshipType = personService.getRelationshipType(relationshipType.getId());
		Assert.assertNull(deletedRelationshipType);
	}
	
	/**
	 * @see PersonService#savePerson(Person)
	 */
	@Test
	public void savePerson_shouldCreateNewObjectWhenPersonIdIsNull() throws Exception {
		User user = Context.getAuthenticatedUser();
		Person person = new Person();
		person.setPersonCreator(user);
		person.setPersonDateCreated(new Date());
		person.setPersonChangedBy(user);
		person.setPersonDateChanged(new Date());
		person.setGender("F");
		Assert.assertNull(person.getId());
		person.addName(new PersonName("givenName", "middleName", "familyName"));
		Person personSaved = Context.getPersonService().savePerson(person);
		Assert.assertNotNull(personSaved.getId());
	}
	
	/**
	 * @see PersonService#savePerson(Person)
	 */
	@Test
	public void savePerson_shouldUpdateExistingObjectWhenPersonIdIsNotNull() throws Exception {
		Person personSaved = Context.getPersonService().getPerson(1);
		Assert.assertNotNull(personSaved.getId());
		personSaved.setGender("M");
		Person personUpdated = Context.getPersonService().savePerson(personSaved);
		Assert.assertEquals("M", personUpdated.getGender());
	}
	
	/**
	 * @see PersonService#saveRelationship(Relationship)
	 */
	@Test(expected = APIException.class)
	public void saveRelationship_shouldThrowAPIException() {
		Relationship relationship = new Relationship();
		Person person = new Person();
		relationship.setPersonA(person);
		relationship.setPersonB(person);
		
		personService.saveRelationship(relationship);
		
	}
	
	/**
	 * @see PersonService#saveRelationship(Relationship)
	 */
	@Test
	public void saveRelationship_shouldCreateNewObjectWhenRelationshipIdIsNull() throws Exception {
		PersonService personService = Context.getPersonService();
		
		Relationship relationship = new Relationship();
		relationship.setPersonA(personService.getPerson(1));
		relationship.setPersonB(personService.getPerson(2));
		relationship.setRelationshipType(personService.getRelationshipType(1));
		Assert.assertNull(relationship.getRelationshipId());
		Relationship savedRelationship = personService.saveRelationship(relationship);
		Assert.assertNotNull(savedRelationship.getRelationshipId());
	}
	
	/**
	 * @see PersonService#saveRelationship(Relationship)
	 */
	@Test
	public void saveRelationship_shouldUpdateExistingObjectWhenRelationshipIdIsNotNull() throws Exception {
		PersonService personService = Context.getPersonService();
		
		Relationship savedRelationship = personService.getRelationship(1);
		Assert.assertNotNull(savedRelationship.getRelationshipId());
		
		savedRelationship.setRelationshipType(personService.getRelationshipType(2));
		Relationship updatedRelationship = personService.saveRelationship(savedRelationship);
		Assert.assertEquals(personService.getRelationshipType(2), updatedRelationship.getRelationshipType());
	}
	
	/**
	 * @see PersonService#saveRelationshipType(RelationshipType)
	 */
	@Test
	public void saveRelationshipType_shouldCreateNewObjectWhenRelationshipTypeIdIsNull() throws Exception {
		RelationshipType relationshipType = new RelationshipType();
		relationshipType.setDescription("Test relationship");
		relationshipType.setaIsToB("Sister");
		relationshipType.setbIsToA("Brother");
		Assert.assertNull(relationshipType.getRelationshipTypeId());
		RelationshipType savedRelationshipType = personService.saveRelationshipType(relationshipType);
		Assert.assertNotNull(savedRelationshipType.getRelationshipTypeId());
	}
	
	/**
	 * @see PersonService#saveRelationshipType(RelationshipType)
	 */
	@Test
	public void saveRelationshipType_shouldUpdateExistingObjectWhenRelationshipTypeIdIsNotNull() throws Exception {
		RelationshipType savedRelationshipType = Context.getPersonService().getRelationshipType(1);
		Assert.assertNotNull(savedRelationshipType.getRelationshipTypeId());
		
		savedRelationshipType.setPreferred(true);
		RelationshipType updatedRelationshipType = personService.saveRelationshipType(savedRelationshipType);
		Assert.assertEquals(true, updatedRelationshipType.getPreferred());
	}
	
	@Test
	public void unvoidPerson_shouldReturnNullwhenGivenNull() {
		Assert.assertNull(Context.getPersonService().unvoidPerson(null));
	}
	
	/**
	 * @see PersonService#unvoidPerson(Person) TODO NullPointerException during
	 *      RequiredDataAdvice.before() TODO Should we be able to unvoid an already not voided
	 *      record? This test assumes yes.
	 */
	@Test
	public void unvoidPerson_shouldUnvoidTheGivenPerson() throws Exception {
		executeDataSet("org/openmrs/api/include/PersonServiceTest-createPersonPurgeVoidTest.xml");
		Person person = Context.getPersonService().getPerson(1002);
		
		assertTrue(person.getVoided());
		Assert.assertNotNull(person.getDateVoided());
		
		Person unvoidedPerson = Context.getPersonService().unvoidPerson(person);
		
		Assert.assertFalse(unvoidedPerson.getVoided());
		Assert.assertNull(unvoidedPerson.getVoidedBy());
		Assert.assertNull(unvoidedPerson.getPersonVoidReason());
		Assert.assertNull(unvoidedPerson.getPersonDateVoided());
	}
	
	/**
	 * @see PersonService#unvoidRelationship(Relationship)
	 */
	@Test
	public void unvoidRelationship_shouldUnvoidVoidedRelationship() throws Exception {
		Relationship relationship = Context.getPersonService().getRelationship(1);
		Relationship voidedRelationship = Context.getPersonService().voidRelationship(relationship,
		    "Test Voiding Relationship");
		
		assertTrue(voidedRelationship.getVoided());
		Assert.assertNotNull(voidedRelationship.getVoidedBy());
		Assert.assertNotNull(voidedRelationship.getVoidReason());
		Assert.assertNotNull(voidedRelationship.getDateVoided());
		
		Relationship unvoidedRelationship = Context.getPersonService().unvoidRelationship(voidedRelationship);
		
		Assert.assertFalse(unvoidedRelationship.getVoided());
		Assert.assertNull(unvoidedRelationship.getVoidedBy());
		Assert.assertNull(unvoidedRelationship.getVoidReason());
		Assert.assertNull(unvoidedRelationship.getDateVoided());
	}

	/**
	 * @see PersonService#voidPerson(Person,String)
	 */
	@Test
	public void voidPerson_shouldReturnNullwhenGivenNull() {
		Assert.assertEquals(Context.getPersonService().voidPerson(null, "Testing person null"), null);
		
	}
	
	/**
	 * @see PersonService#voidPerson(Person, String)
	 */
	@Test
	public void voidPerson_shouldReturnVoidedPersonWithGivenReason() throws Exception {
		executeDataSet("org/openmrs/api/include/PersonServiceTest-createPersonPurgeVoidTest.xml");
		Person person = Context.getPersonService().getPerson(1001);
		Context.getPersonService().voidPerson(person, "Test Voiding Person");
		
		Context.flushSession();
		Context.clearSession();
		
		Person voidedPerson = Context.getPersonService().getPerson(1001);
		
		assertTrue(voidedPerson.getVoided());
		Assert.assertNotNull(voidedPerson.getVoidedBy());
		Assert.assertNotNull(voidedPerson.getPersonVoidReason());
		Assert.assertNotNull(voidedPerson.getPersonDateVoided());
		Assert.assertEquals(voidedPerson.getPersonVoidReason(), "Test Voiding Person");
	}

	@Test
	public void voidRelationship_shouldVoidRelationshipIfGivenRelationshipIsNotVoided() {
		
		Relationship relationship = personService.getRelationship(1);
		assertFalse("We need an unvoided relationship to test the method", relationship.getVoided());
		String voidReason = "Something";

		// TODO - voiding is done by the BaseVoidHandler called via AOP before voidRelationship
		// is executed. Coverage of voidRelationship is low because relationship.getVoided() is true
		// when entering voidRelationship
		// Documented at TRUNK-5151
		personService.voidRelationship(relationship, voidReason);

		Relationship voidedRelationship = personService.getRelationship(1);
		assertTrue(voidedRelationship.getVoided());
		assertThat(voidedRelationship.getVoidReason(), is(voidReason));
		assertNotNull(voidedRelationship.getDateVoided());
		assertEquals(voidedRelationship.getVoidedBy(), Context.getAuthenticatedUser());
	}

	@Test
	public void voidRelationship_shouldVoidRelationshipWithVoidReasonNullIfGivenRelationshipIsNotVoided() {
		
		Relationship relationship = personService.getRelationship(1);
		assertFalse("We need an unvoided relationship to test the method", relationship.getVoided());
		String voidReason = null;

		// TODO - voiding is done by the BaseVoidHandler called via AOP before voidRelationship
		// is executed. Coverage of voidRelationship is low because relationship.getVoided() is true
		// when entering voidRelationship
		// Documented at TRUNK-5151
		personService.voidRelationship(relationship, voidReason);
		
		Relationship voidedRelationship = personService.getRelationship(1);
		assertTrue(voidedRelationship.getVoided());
		assertThat(voidedRelationship.getVoidReason(), is(voidReason));
		assertNotNull(voidedRelationship.getDateVoided());
		assertEquals(voidedRelationship.getVoidedBy(), Context.getAuthenticatedUser());
	}
	
	@Test
	public void voidRelationship_shouldVoidRelationshipAndSetVoidedByToGivenUserIfGivenRelationshipIsNotVoided() {
		
		Relationship relationship = personService.getRelationship(1);
		assertFalse("We need an unvoided relationship to test the method", relationship.getVoided());
		String voidReason = "Something";
		User user = Context.getUserService().getUser(501);
		assertNotNull("need a user to void", user);
		relationship.setVoidedBy(user);

		// TODO - voiding is done by the BaseVoidHandler called via AOP before voidRelationship
		// is executed. Coverage of voidRelationship is low because relationship.getVoided() is true
		// when entering voidRelationship
		// Documented at TRUNK-5151
		personService.voidRelationship(relationship, voidReason);
		
		Relationship voidedRelationship = personService.getRelationship(1);
		assertTrue(voidedRelationship.getVoided());
		assertThat(voidedRelationship.getVoidReason(), is(voidReason));
		assertNotNull(voidedRelationship.getDateVoided());
		assertEquals(voidedRelationship.getVoidedBy(), user);
	}
	
	/**
	 * @see PersonService#getPersonAddressByUuid(String)
	 */
	@Test
	public void getPersonAddressByUuid_shouldFindObjectGivenValidUuid() throws Exception {
		String uuid = "3350d0b5-821c-4e5e-ad1d-a9bce331e118";
		PersonAddress personAddress = Context.getPersonService().getPersonAddressByUuid(uuid);
		Assert.assertEquals(2, (int) personAddress.getPersonAddressId());
	}
	
	/**
	 * @see PersonService#getPersonAddressByUuid(String)
	 */
	@Test
	public void getPersonAddressByUuid_shouldReturnNullIfNoObjectFoundWithGivenUuid() throws Exception {
		Assert.assertNull(Context.getPersonService().getPersonAddressByUuid("some invalid uuid"));
	}
	
	/**
	 * @see PersonService#getPersonAttributeByUuid(String)
	 */
	@Test
	public void getPersonAttributeByUuid_shouldFindObjectGivenValidUuid() throws Exception {
		String uuid = "0768f3da-b692-44b7-a33f-abf2c450474e";
		PersonAttribute person = Context.getPersonService().getPersonAttributeByUuid(uuid);
		Assert.assertEquals(1, (int) person.getPersonAttributeId());
	}
	
	/**
	 * @see PersonService#getPersonAttributeByUuid(String)
	 */
	@Test
	public void getPersonAttributeByUuid_shouldReturnNullIfNoObjectFoundWithGivenUuid() throws Exception {
		Assert.assertNull(Context.getPersonService().getPersonAttributeByUuid("some invalid uuid"));
	}
	
	/**
	 * @see PersonService#getPersonAttributeTypeByUuid(String)
	 */
	@Test
	public void getPersonAttributeTypeByUuid_shouldFindObjectGivenValidUuid() throws Exception {
		String uuid = "b3b6d540-a32e-44c7-91b3-292d97667518";
		PersonAttributeType personAttributeType = Context.getPersonService().getPersonAttributeTypeByUuid(uuid);
		Assert.assertEquals(1, (int) personAttributeType.getPersonAttributeTypeId());
	}
	
	/**
	 * @see PersonService#getPersonAttributeTypeByUuid(String)
	 */
	@Test
	public void getPersonAttributeTypeByUuid_shouldReturnNullIfNoObjectFoundWithGivenUuid() throws Exception {
		Assert.assertNull(Context.getPersonService().getPersonAttributeTypeByUuid("some invalid uuid"));
	}
	
	/**
	 * @see PersonService#getPersonByUuid(String)
	 */
	@Test
	public void getPersonByUuid_shouldFindObjectGivenValidUuid() throws Exception {
		String uuid = "ba1b19c2-3ed6-4f63-b8c0-f762dc8d7562";
		Person person = Context.getPersonService().getPersonByUuid(uuid);
		Assert.assertEquals(1, (int) person.getPersonId());
	}
	
	/**
	 * @see PersonService#getPersonByUuid(String)
	 */
	@Test
	public void getPersonByUuid_shouldReturnNullIfNoObjectFoundWithGivenUuid() throws Exception {
		Assert.assertNull(Context.getPersonService().getPersonByUuid("some invalid uuid"));
	}
	
	/**
	 * @see PersonService#getPersonNameByUuid(String)
	 */
	@Test
	public void getPersonNameByUuid_shouldFindObjectGivenValidUuid() throws Exception {
		String uuid = "399e3a7b-6482-487d-94ce-c07bb3ca3cc7";
		PersonName personName = Context.getPersonService().getPersonNameByUuid(uuid);
		Assert.assertEquals(2, (int) personName.getPersonNameId());
	}
	
	/**
	 * @see PersonService#getPersonNameByUuid(String)
	 */
	@Test
	public void getPersonNameByUuid_shouldReturnNullIfNoObjectFoundWithGivenUuid() throws Exception {
		Assert.assertNull(Context.getPersonService().getPersonNameByUuid("some invalid uuid"));
	}
	
	@Test
	public void getPersonNameById_shouldFindObjectGivenValidId() throws Exception {
		PersonName personName = Context.getPersonService().getPersonName(2);
		Assert.assertEquals(2, (int) personName.getId());
	}
	
	@Test
	public void getPersonNameById_shouldNotFindAnyObjectGivenInvalidId() throws Exception {
		PersonName personName = Context.getPersonService().getPersonName(-1);
		Assert.assertNull(personName);
	}
	
	/**
	 * @see PersonService#getRelationshipByUuid(String)
	 */
	@Test
	public void getRelationshipByUuid_shouldFindObjectGivenValidUuid() throws Exception {
		String uuid = "c18717dd-5d78-4a0e-84fc-ee62c5f0676a";
		Relationship relationship = Context.getPersonService().getRelationshipByUuid(uuid);
		Assert.assertEquals(1, (int) relationship.getRelationshipId());
	}
	
	/**
	 * @see PersonService#getRelationshipByUuid(String)
	 */
	@Test
	public void getRelationshipByUuid_shouldReturnNullIfNoObjectFoundWithGivenUuid() throws Exception {
		Assert.assertNull(Context.getPersonService().getRelationshipByUuid("some invalid uuid"));
	}
	
	/**
	 * @see PersonService#getRelationshipTypeByUuid(String)
	 */
	@Test
	public void getRelationshipTypeByUuid_shouldFindObjectGivenValidUuid() throws Exception {
		String uuid = "6d9002ea-a96b-4889-af78-82d48c57a110";
		RelationshipType relationshipType = Context.getPersonService().getRelationshipTypeByUuid(uuid);
		Assert.assertEquals(1, (int) relationshipType.getRelationshipTypeId());
	}
	
	/**
	 * @see PersonService#getRelationshipTypeByUuid(String)
	 */
	@Test
	public void getRelationshipTypeByUuid_shouldReturnNullIfNoObjectFoundWithGivenUuid() throws Exception {
		Assert.assertNull(Context.getPersonService().getRelationshipTypeByUuid("some invalid uuid"));
	}
	
	/**
	 * @see PersonService#parsePersonName(String)
	 */
	@Test
	public void parsePersonName_shouldNotFailWhenEndingWithWhitespace() throws Exception {
		PersonName pname = Context.getPersonService().parsePersonName("John ");
		assertEquals("John", pname.getGivenName());
	}
	
	/**
	 * @see PersonService#parsePersonName(String)
	 */
	@Test
	public void parsePersonName_shouldNotFailWhenEndingWithAComma() throws Exception {
		PersonName pname = Context.getPersonService().parsePersonName("John,");
		assertEquals("John", pname.getGivenName());
		
	}
	
	/**
	 * @see PersonService#parsePersonName(String)
	 */
	@Test
	public void parsePersonName_shouldParseFourPersonName() throws Exception {
		PersonName pname = Context.getPersonService().parsePersonName("John David Alex Smith");
		assertEquals("John", pname.getGivenName());
		assertEquals("David", pname.getMiddleName());
		assertEquals("Alex", pname.getFamilyName());
		assertEquals("Smith", pname.getFamilyName2());
	}
	
	/**
	 * @see PersonService#voidPersonName(org.openmrs.PersonName, String)
	 */
	@Test
	public void voidPersonName_shouldVoidPersonNameWithTheGivenReason() throws Exception {
		executeDataSet("org/openmrs/api/include/PersionServiceTest-voidUnvoidPersonName.xml");
		PersonName personName = Context.getPersonService().getPersonNameByUuid("5e6571cc-c7f2-41de-b289-f55f8fe79c6f");
		
		Assert.assertFalse(personName.getVoided());
		
		PersonName voidedPersonName = Context.getPersonService().voidPersonName(personName, "Test Voiding PersonName");
		
		assertTrue(voidedPersonName.getVoided());
		Assert.assertNotNull(voidedPersonName.getVoidedBy());
		Assert.assertNotNull(voidedPersonName.getDateVoided());
		Assert.assertEquals(voidedPersonName.getVoidReason(), "Test Voiding PersonName");
	}
	
	/**
	 * @see PersonService#unvoidPersonName(org.openmrs.PersonName)
	 */
	@Test
	public void unvoidPersonName_shouldUnvoidVoidedPersonName() throws Exception {
		executeDataSet("org/openmrs/api/include/PersionServiceTest-voidUnvoidPersonName.xml");
		PersonName voidedPersonName = Context.getPersonService().getPersonNameByUuid("a6ghgh7e-1384-493a-a55b-d325924acd94");
		
		assertTrue(voidedPersonName.getVoided());
		
		PersonName unvoidedPersonName = Context.getPersonService().unvoidPersonName(voidedPersonName);
		
		Assert.assertFalse(unvoidedPersonName.getVoided());
		Assert.assertNull(unvoidedPersonName.getVoidedBy());
		Assert.assertNull(unvoidedPersonName.getDateVoided());
		Assert.assertNull(unvoidedPersonName.getVoidReason());
		
	}
	
	/**
	 * @throws APIException
	 * @see PersonService#savePersonName(org.openmrs.PersonName)
	 */
	@Test(expected = APIException.class)
	public void savePersonName_shouldFailIfYouTryToVoidTheLastNonVoidedName() throws Exception {
		executeDataSet("org/openmrs/api/include/PersionServiceTest-voidUnvoidPersonName.xml");
		PersonName personName = Context.getPersonService().getPersonNameByUuid("39ghgh7b-6482-487d-94ce-c07bb3ca3cc1");
		Assert.assertFalse(personName.getVoided());
		Context.getPersonService().voidPersonName(personName, "Test Voiding PersonName");
	}
	
	/**
	 * @see PersonService#voidPersonAddress(org.openmrs.PersonAddress, String)
	 */
	@Test
	public void voidPersonAddress_shouldVoidPersonAddressWithTheGivenReason() throws Exception {
		executeDataSet("org/openmrs/api/include/PersionServiceTest-voidUnvoidPersonAddress.xml");
		PersonAddress personAddress = Context.getPersonService().getPersonAddressByUuid(
		    "33ghd0b5-821c-4e5e-ad1d-a9bce331e118");
		
		Assert.assertFalse(personAddress.getVoided());
		
		PersonAddress voidedPersonAddress = Context.getPersonService().voidPersonAddress(personAddress,
		    "Test Voiding PersonAddress");
		
		assertTrue(voidedPersonAddress.getVoided());
		Assert.assertNotNull(voidedPersonAddress.getVoidedBy());
		Assert.assertNotNull(voidedPersonAddress.getDateVoided());
		Assert.assertEquals(voidedPersonAddress.getVoidReason(), "Test Voiding PersonAddress");
	}
	
	/**
	 * @see PersonService#unvoidPersonAddress(org.openmrs.PersonAddress)
	 */
	@Test
	public void unvoidPersonAddress_shouldUnvoidVoidedpersonAddress() throws Exception {
		executeDataSet("org/openmrs/api/include/PersionServiceTest-voidUnvoidPersonAddress.xml");
		PersonAddress voidedPersonAddress = Context.getPersonService().getPersonAddressByUuid(
		    "33ghghb5-821c-4e5e-ad1d-a9bce331e777");
		
		assertTrue(voidedPersonAddress.getVoided());
		
		PersonAddress unvoidedPersonAddress = Context.getPersonService().unvoidPersonAddress(voidedPersonAddress);
		
		Assert.assertFalse(unvoidedPersonAddress.getVoided());
		Assert.assertNull(unvoidedPersonAddress.getVoidedBy());
		Assert.assertNull(unvoidedPersonAddress.getDateVoided());
		Assert.assertNull(unvoidedPersonAddress.getVoidReason());
		
	}
	
	/**
	 * @see PersonService#unvoidPerson(Person)
	 */
	@Test
	public void unvoidPerson_shouldNotUnretireUsers() throws Exception {
		//given
		Person person = personService.getPerson(2);
		User user = new User(person);
		Context.getUserService().createUser(user, "Admin123");
		personService.voidPerson(person, "reason");
		
		//when
		personService.unvoidPerson(person);
		
		//then
		assertTrue(Context.getUserService().getUsersByPerson(person, false).isEmpty());
	}
	
	/**
	 * @see PersonService#unvoidPerson(Person)
	 */
	@Test
	public void unvoidPerson_shouldUnvoidPatient() throws Exception {
		//given
		Person person = personService.getPerson(2);
		personService.voidPerson(person, "reason");
		
		//when
		personService.unvoidPerson(person);
		
		//then
		Assert.assertFalse(person.getVoided());
	}
	
	/**
	 * @see PersonService#voidPerson(Person,String)
	 */
	@Test
	public void voidPerson_shouldRetireUsers() throws Exception {
		//given
		Person person = personService.getPerson(2);
		User user = new User(person);
		Context.getUserService().createUser(user, "Admin123");
		Assert.assertFalse(Context.getUserService().getUsersByPerson(person, false).isEmpty());
		
		//when
		personService.voidPerson(person, "reason");
		
		//then
		assertTrue(Context.getUserService().getUsersByPerson(person, false).isEmpty());
	}
	
	/**
	 * @see PersonService#voidPerson(Person,String)
	 */
	@Test
	public void voidPerson_shouldVoidPatient() throws Exception {
		//given
		Person person = personService.getPerson(2);
		
		//when
		personService.voidPerson(person, "reason");
		
		//then
		assertTrue(person.getVoided());
	}
	
	/**
	 * @see PersonService#saveRelationshipType(RelationshipType)
	 */
	@Test(expected = APIException.class)
	public void saveRelationshipType_shouldFailIfTheDescriptionIsNotSpecified() throws Exception {
		RelationshipType relationshipType = new RelationshipType();
		relationshipType.setaIsToB("Sister");
		relationshipType.setbIsToA("Brother");
		personService.saveRelationshipType(relationshipType);
	}
	
	/**
	 * @see PersonService#savePerson(Person)
	 */
	@Test
	public void savePerson_shouldSetThePreferredNameAndAddressIfNoneIsSpecified() throws Exception {
		Person person = new Person();
		person.setGender("M");
		PersonName name = new PersonName("givenName", "middleName", "familyName");
		person.addName(name);
		PersonAddress address = new PersonAddress();
		address.setAddress1("some address");
		person.addAddress(address);
		
		personService.savePerson(person);
		assertTrue(name.getPreferred());
		assertTrue(address.getPreferred());
	}
	
	/**
	 * @see PersonService#savePerson(Person)
	 */
	@Test
	public void savePerson_shouldNotSetThePreferredNameAndAddressIfTheyAlreadyExist() throws Exception {
		Person person = new Person();
		person.setGender("M");
		PersonName name = new PersonName("givenName", "middleName", "familyName");
		PersonName preferredName = new PersonName("givenName", "middleName", "familyName");
		preferredName.setPreferred(true);
		person.addName(name);
		person.addName(preferredName);
		
		PersonAddress address = new PersonAddress();
		address.setAddress1("some address");
		PersonAddress preferredAddress = new PersonAddress();
		preferredAddress.setAddress1("another address");
		preferredAddress.setPreferred(true);
		person.addAddress(address);
		person.addAddress(preferredAddress);
		
		personService.savePerson(person);
		assertTrue(preferredName.getPreferred());
		assertTrue(preferredAddress.getPreferred());
		Assert.assertFalse(name.getPreferred());
		Assert.assertFalse(address.getPreferred());
	}
	
	/**
	 * @see PersonService#savePerson(Person)
	 */
	@Test
	public void savePerson_shouldNotSetAVoidedNameOrAddressAsPreferred() throws Exception {
		Person person = new Person();
		person.setGender("M");
		PersonName name = new PersonName("givenName", "middleName", "familyName");
		PersonName preferredName = new PersonName("givenName", "middleName", "familyName");
		preferredName.setPreferred(true);
		preferredName.setVoided(true);
		person.addName(name);
		person.addName(preferredName);
		
		PersonAddress address = new PersonAddress();
		address.setAddress1("some address");
		PersonAddress preferredAddress = new PersonAddress();
		preferredAddress.setAddress1("another address");
		preferredAddress.setPreferred(true);
		preferredAddress.setVoided(true);
		person.addAddress(address);
		person.addAddress(preferredAddress);
		
		personService.savePerson(person);
		Assert.assertFalse(preferredName.getPreferred());
		Assert.assertFalse(preferredAddress.getPreferred());
		assertTrue(name.getPreferred());
		assertTrue(address.getPreferred());
	}
	
	/**
	 * Creates a new Global Property to lock person attribute types by setting its value
	 * @param propertyValue value for person attribute types locked GP
	 */
	public void createPersonAttributeTypeLockedGPAndSetValue(String propertyValue) {
		GlobalProperty gp = new GlobalProperty(OpenmrsConstants.GLOBAL_PROPERTY_PERSON_ATRIBUTE_TYPES_LOCKED);
		gp.setPropertyValue(propertyValue);
		Context.getAdministrationService().saveGlobalProperty(gp);
	}
	
	/**
	 * @see PersonService#savePersonAttributeType(PersonAttributeType)
	 * @throws PersonAttributeTypeLockedException
	 */
	@Test(expected = PersonAttributeTypeLockedException.class)
	public void savePersonAttributeType_shouldThrowAnErrorWhenTryingToSavePersonAttributeTypeWhilePersonAttributeTypesAreLocked()
	        throws Exception {
		PersonService ps = Context.getPersonService();
		createPersonAttributeTypeLockedGPAndSetValue("true");
		PersonAttributeType pat = ps.getPersonAttributeType(1);
		pat.setDescription("New person attribute type");
		ps.savePersonAttributeType(pat);
	}
	
	@Test(expected = PersonAttributeTypeLockedException.class)
	public void shouldFailToRetirePersonAttributeTypeWhilePersonAttributeTypesAreLocked() {
		
		createPersonAttributeTypeLockedGPAndSetValue("true");
		PersonAttributeType pat = personService.getPersonAttributeType(UNRETIRED_PERSON_ATTRIBUTE_TYPE);
		
		personService.retirePersonAttributeType(pat, "Retire test");
	}
	
	@Test(expected = APIException.class)
	public void shouldFailToRetirePersonAttributeTypeIfGivenReasonIsNull() {
		
		PersonAttributeType pat = personService.getPersonAttributeType(UNRETIRED_PERSON_ATTRIBUTE_TYPE);
		
		personService.retirePersonAttributeType(pat, null);
	}
	
	@Test(expected = APIException.class)
	public void shouldFailToRetirePersonAttributeTypeIfGivenReasonIsEmpty() {
		
		PersonAttributeType pat = personService.getPersonAttributeType(UNRETIRED_PERSON_ATTRIBUTE_TYPE);
		
		personService.retirePersonAttributeType(pat, "");
	}
	
	@Test
	public void shouldRetirePersonAttributeType() {
		
		PersonAttributeType pat = personService.getPersonAttributeType(UNRETIRED_PERSON_ATTRIBUTE_TYPE);
		assertFalse("need an unretired PersonAttributeType", pat.getRetired());
		String retireReason = "reason";
		
		personService.retirePersonAttributeType(pat, retireReason);
		
		PersonAttributeType retiredPat = personService.getPersonAttributeType(UNRETIRED_PERSON_ATTRIBUTE_TYPE);
		assertTrue(retiredPat.getRetired());
		assertThat(retiredPat.getRetiredBy(), is(Context.getAuthenticatedUser()));
		assertThat(retiredPat.getRetireReason(), is(retireReason));
		assertNotNull(retiredPat.getDateRetired());
	}
	
	@Test(expected = PersonAttributeTypeLockedException.class)
	public void unretirePersonAttributeType_shouldThrowAnErrorWhenTryingToUnretirePersonAttributeTypeWhilePersonAttributeTypesAreLocked() {
		
		createPersonAttributeTypeLockedGPAndSetValue("true");
		PersonAttributeType pat = personService.getPersonAttributeType(RETIRED_PERSON_ATTRIBUTE_TYPE);
		
		personService.unretirePersonAttributeType(pat);
	}
	
	@Test
	public void shouldUnretirePersonAttributeType() {
		
		PersonAttributeType pat = personService.getPersonAttributeType(RETIRED_PERSON_ATTRIBUTE_TYPE);
		
		personService.unretirePersonAttributeType(pat);
		
		PersonAttributeType unretiredPat = personService.getPersonAttributeType(UNRETIRED_PERSON_ATTRIBUTE_TYPE);
		assertFalse(unretiredPat.getRetired());
		assertNull(unretiredPat.getRetiredBy());
		assertNull(unretiredPat.getRetireReason());
		assertNull(unretiredPat.getDateRetired());
	}
	
	/**
	 * @see PersonService#purgePersonAttributeType(PersonAttributeType)
	 * @throws PersonAttributeTypeLockedException
	 */
	@Test(expected = PersonAttributeTypeLockedException.class)
	public void purgePersonAttributeType_shouldThrowAnErrorWhileTryingToDeletePersonAttributeTypeWhenPersonAttributeTypesAreLocked()
	        throws Exception {
		PersonService ps = Context.getPersonService();
		createPersonAttributeTypeLockedGPAndSetValue("true");
		PersonAttributeType pat = ps.getPersonAttributeType(1);
		ps.purgePersonAttributeType(pat);
	}
	
	@Test
	public void getPersonAttributeTypes_shouldReturnAllPersonAttributeTypesWithViewTypeNull() {
		List<PersonAttributeType> expected = personService.getAllPersonAttributeTypes();
		
		List<PersonAttributeType> result = personService.getPersonAttributeTypes(null, null);
		
		assertThat(result, containsInAnyOrder(expected.toArray()));
	}
	
	@Test
	public void getPersonAttributeTypes_shouldReturnEmptyListWithViewTypeListing() {
		
		List<PersonAttributeType> result = personService.getPersonAttributeTypes(null, PersonService.ATTR_VIEW_TYPE.LISTING);
		
		assertTrue(result.isEmpty());
	}
	
	@Test
	public void getPersonAttributeTypes_shouldReturnEmptyListWhenViewTypeListingAndPerson() {
		
		List<PersonAttributeType> result = personService.getPersonAttributeTypes(OpenmrsConstants.PERSON_TYPE.PERSON, PersonService.ATTR_VIEW_TYPE.LISTING);
		
		assertTrue(result.isEmpty());
	}
	@Test
	public void getPersonAttributeTypes_shouldReturnEmptyListWhenViewTypePatientAndViewing() {
		
		List<PersonAttributeType> result = personService.getPersonAttributeTypes(OpenmrsConstants.PERSON_TYPE.PATIENT, PersonService.ATTR_VIEW_TYPE.VIEWING);
		
		assertTrue(result.isEmpty());
	}
	
	@Test
	public void getPersonAttributeTypes_shouldReturnListWithNullWhenGlobalPropertyNotExists() {
		adminService.setGlobalProperty(OpenmrsConstants.GLOBAL_PROPERTY_PATIENT_LISTING_ATTRIBUTES, "9");
		assertNull(personService.getPersonAttributeType(9));
		
		List<PersonAttributeType> result = personService.getPersonAttributeTypes(null, PersonService.ATTR_VIEW_TYPE.LISTING);
		
		// This is a bug Trunk-5149
		assertEquals(result.size(), 1);
		assertNull(result.get(0));
	}
	
	@Test
	public void getPersonAttributeTypes_shouldReturnNothingWhenGlobalPropertyLargerNineExists() {
		adminService.setGlobalProperty(OpenmrsConstants.GLOBAL_PROPERTY_PATIENT_LISTING_ATTRIBUTES, "99");
		executeDataSet(CREATE_PERSON_PROPERTY_XML);
		
		assertNotNull(personService.getPersonAttributeType(99));
		
		List<PersonAttributeType> result = personService.getPersonAttributeTypes(null, PersonService.ATTR_VIEW_TYPE.LISTING);
		
		// This is probably a bug TRUNK-5148
		assertEquals(result.size(), 1);
		assertNull(result.get(0));
	}
	
	@Test
	public void getPersonAttributeTypes_shouldReturnPatientAttributesWhenGivenViewTypeListing() {
		adminService.setGlobalProperty(OpenmrsConstants.GLOBAL_PROPERTY_PATIENT_LISTING_ATTRIBUTES, "1");
		PersonAttributeType race = personService.getPersonAttributeType(1);
		
		List<PersonAttributeType> result = personService.getPersonAttributeTypes(null, PersonService.ATTR_VIEW_TYPE.LISTING);
		
		assertThat(result, contains(race));
		assertEquals(result.size(), 1);
	}
	
	@Test
	public void getPersonAttributeTypes_shouldReturnUserAndPatientAttributesWhenViewTypeListiningAndPerson() {
		adminService.setGlobalProperty(OpenmrsConstants.GLOBAL_PROPERTY_PATIENT_LISTING_ATTRIBUTES, "1");
		adminService.setGlobalProperty(OpenmrsConstants.GLOBAL_PROPERTY_USER_LISTING_ATTRIBUTES, "2");
		PersonAttributeType race = personService.getPersonAttributeType(1);
		PersonAttributeType birthplace = personService.getPersonAttributeType(2);
		
		List<PersonAttributeType> result = personService.getPersonAttributeTypes(OpenmrsConstants.PERSON_TYPE.PERSON, PersonService.ATTR_VIEW_TYPE.LISTING);
		
		assertThat(result, contains(race, birthplace));
		assertEquals(result.size(), 2);
	}
	
	@Test
	public void getPersonAttributeTypes_shouldReturnPatientAttributesWhenViewTypeViewingAndPatient() {
		adminService.setGlobalProperty(OpenmrsConstants.GLOBAL_PROPERTY_PATIENT_VIEWING_ATTRIBUTES, "1");
		adminService.setGlobalProperty(OpenmrsConstants.GLOBAL_PROPERTY_USER_VIEWING_ATTRIBUTES, "2");
		PersonAttributeType race = personService.getPersonAttributeType(1);
		
		List<PersonAttributeType> result = personService.getPersonAttributeTypes(OpenmrsConstants.PERSON_TYPE.PATIENT, PersonService.ATTR_VIEW_TYPE.VIEWING);
		
		assertThat(result, contains(race));
		assertEquals(result.size(), 1);
	}
	
	@Test
	public void getPersonAttributeTypes_shouldReturnUserAttributesWhenViewTypeHeaderAndUser() {
		adminService.setGlobalProperty(OpenmrsConstants.GLOBAL_PROPERTY_PATIENT_HEADER_ATTRIBUTES, "1");
		adminService.setGlobalProperty(OpenmrsConstants.GLOBAL_PROPERTY_USER_HEADER_ATTRIBUTES, "2");
		PersonAttributeType birthplace = personService.getPersonAttributeType(2);
		
		List<PersonAttributeType> result = personService.getPersonAttributeTypes(OpenmrsConstants.PERSON_TYPE.USER, PersonService.ATTR_VIEW_TYPE.HEADER);
		
		assertThat(result, contains(birthplace));
		assertEquals(result.size(), 1);
	}
}
