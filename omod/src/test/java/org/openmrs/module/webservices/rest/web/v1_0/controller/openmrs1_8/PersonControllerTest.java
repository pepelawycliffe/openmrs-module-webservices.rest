/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.webservices.rest.web.v1_0.controller.openmrs1_8;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.apache.commons.beanutils.PropertyUtils;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Person;
import org.openmrs.PersonAddress;
import org.openmrs.PersonName;
import org.openmrs.api.PersonService;
import org.openmrs.api.context.Context;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.test.Util;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.response.ConversionException;
import org.openmrs.module.webservices.rest.web.response.ResourceDoesNotSupportOperationException;
import org.openmrs.module.webservices.rest.web.v1_0.controller.BaseCrudControllerTest;
import org.openmrs.module.webservices.rest.web.v1_0.resource.openmrs1_8.ResourceTestConstants;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Tests CRUD operations for {@link Person}s via web service calls
 */
public class PersonControllerTest extends BaseCrudControllerTest {
	
	private PersonService service;
	
	@Override
	public String getURI() {
		return "person";
	}
	
	@Override
	public String getUuid() {
		return ResourceTestConstants.PERSON_UUID;
	}
	
	@Override
	public long getAllCount() {
		return 0;
	}
	
	@Before
	public void before() {
		this.service = Context.getPersonService();
	}
	
	@Test(expected = ResourceDoesNotSupportOperationException.class)
	public void shouldGetAll() throws Exception {
		super.shouldGetAll();
	}
	
	@Test
	public void shouldGetAPersonByUuid() throws Exception {
		MockHttpServletRequest req = request(RequestMethod.GET, getURI() + "/" + getUuid());
		SimpleObject result = deserialize(handle(req));
		
		Person person = service.getPersonByUuid(getUuid());
		assertEquals(person.getUuid(), PropertyUtils.getProperty(result, "uuid"));
		assertNotNull(PropertyUtils.getProperty(result, "preferredName"));
		assertEquals(person.getGender(), PropertyUtils.getProperty(result, "gender"));
		assertNull(PropertyUtils.getProperty(result, "auditInfo"));
	}
	
	@Test
	public void shouldReturnTheAuditInfoForTheFullRepresentation() throws Exception {
		MockHttpServletRequest req = request(RequestMethod.GET, getURI() + "/" + getUuid());
		req.addParameter(RestConstants.REQUEST_PROPERTY_FOR_REPRESENTATION, RestConstants.REPRESENTATION_FULL);
		SimpleObject result = deserialize(handle(req));
		
		assertNotNull(PropertyUtils.getProperty(result, "auditInfo"));
	}
	
	@Test
	public void shouldCreateAPerson() throws Exception {
		long originalCount = service.getPeople("", false).size();
		String json = "{ \"names\": [{ \"givenName\":\"Helen\", \"familyName\":\"of Troy\" }, "
		        + "{\"givenName\":\"Leda\", \"familyName\":\"Nemesis\"} ], "
		        + "\"birthdate\":\"2003-01-01\", \"gender\":\"F\" }";
		
		SimpleObject newPerson = deserialize(handle(newPostRequest(getURI(), json)));
		
		String uuid = PropertyUtils.getProperty(newPerson, "uuid").toString();
		Person person = Context.getPersonService().getPersonByUuid(uuid);
		assertEquals(2, person.getNames().size());
		assertEquals("Helen of Troy", person.getPersonName().getFullName());
		assertEquals(++originalCount, service.getPeople("", false).size());
	}
	
	@Test
	public void shouldCreateAPersonWithAttributes() throws Exception {
		long originalCount = service.getPeople("", false).size();
		final String birthPlace = "Nsambya";
		String json = "{ \"names\": [{ \"givenName\":\"Helen\", \"familyName\":\"of Troy\" }], "
		        + "\"birthdate\":\"2003-01-01\", \"gender\":\"F\", \"attributes\":"
		        + "[{\"attributeType\":\"54fc8400-1683-4d71-a1ac-98d40836ff7c\",\"value\": \"" + birthPlace + "\"}] }";
		
		SimpleObject newPerson = deserialize(handle(newPostRequest(getURI(), json)));
		
		String uuid = PropertyUtils.getProperty(newPerson, "uuid").toString();
		Person person = Context.getPersonService().getPersonByUuid(uuid);
		assertEquals(++originalCount, service.getPeople("", false).size());
		assertEquals(birthPlace, person.getAttribute("Birthplace").getValue());
	}
	
	@Test
	public void shouldEditAPerson() throws Exception {
		Person person = service.getPersonByUuid(getUuid());
		assertFalse("F".equals(person.getGender()));
		assertFalse(person.isDead());
		assertNull(person.getCauseOfDeath());
		String json = "{\"gender\":\"F\",\"dead\":true, \"causeOfDeath\":\"15f83cd6-64e9-4e06-a5f9-364d3b14a43d\"}";
		handle(newPostRequest(getURI() + "/" + getUuid(), json));
		assertEquals("F", person.getGender());
		assertTrue(person.isDead());
		assertNotNull(person.getCauseOfDeath());
	}
	
	@Test(expected = ConversionException.class)
	public void shouldNotAllowUpdatingNamesProperty() throws Exception {
		handle(newPostRequest(getURI() + "/" + getUuid(), "{\"names\":\"[]\"}"));
	}
	
	@Test(expected = ConversionException.class)
	public void shouldNotAllowUpdatingAddressesProperty() throws Exception {
		handle(newPostRequest(getURI() + "/" + getUuid(), "{\"addresses\":\"[]\"}"));
	}
	
	@Test
	public void shouldSetThePreferredAddressAndUnmarkTheOldOne() throws Exception {
		executeDataSet("PersonControllerTest-otherPersonData.xml");
		Person person = service.getPersonByUuid(getUuid());
		PersonAddress preferredAddress = service.getPersonAddressByUuid("8a806d8c-822d-11e0-872f-18a905e044dc");
		PersonAddress notPreferredAddress = service.getPersonAddressByUuid("3350d0b5-821c-4e5e-ad1d-a9bce331e118");
		assertTrue(preferredAddress.isPreferred());
		assertFalse(notPreferredAddress.isPreferred());
		assertFalse(notPreferredAddress.isVoided());
		//sanity check that the addresses belong to the person
		assertEquals(person, preferredAddress.getPerson());
		assertEquals(person, notPreferredAddress.getPerson());
		
		handle(newPostRequest(getURI() + "/" + getUuid(), "{ \"preferredAddress\":\"" + notPreferredAddress.getUuid()
		        + "\" }"));
		
		assertEquals(notPreferredAddress, person.getPersonAddress());
		assertTrue(notPreferredAddress.isPreferred());
		assertFalse(preferredAddress.isPreferred());
	}
	
	@Test
	public void shouldSetThePreferredNameAndUnmarkTheOldOne() throws Exception {
		executeDataSet("PersonControllerTest-otherPersonData.xml");
		Person person = service.getPersonByUuid(getUuid());
		PersonName preferredName = service.getPersonNameByUuid("399e3a7b-6482-487d-94ce-c07bb3ca3cc7");
		PersonName notPreferredName = service.getPersonNameByUuid("499e3a7b-6482-487d-94ce-c07bb3ca3cc8");
		assertTrue(preferredName.isPreferred());
		assertFalse(notPreferredName.isPreferred());
		assertFalse(notPreferredName.isVoided());
		//sanity check that the names belong to the person
		assertEquals(person, preferredName.getPerson());
		assertEquals(person, notPreferredName.getPerson());
		
		handle(newPostRequest(getURI() + "/" + getUuid(), "{ \"preferredName\":\"" + notPreferredName.getUuid() + "\" }"));
		
		assertEquals(notPreferredName, person.getPersonName());
		assertTrue(notPreferredName.isPreferred());
		assertFalse(preferredName.isPreferred());
	}
	
	@Test
	public void shouldVoidAPerson() throws Exception {
		Person person = service.getPersonByUuid(getUuid());
		final String reason = "some random reason";
		assertEquals(false, person.isVoided());
		MockHttpServletRequest req = newDeleteRequest(getURI() + "/" + getUuid(), new Parameter("!purge", ""),
		    new Parameter("reason", reason));
		handle(req);
		person = service.getPersonByUuid(getUuid());
		assertTrue(person.isVoided());
		assertEquals(reason, person.getVoidReason());
	}
	
	@Test
	public void shouldPurgeAPerson() throws Exception {
		final String uuid = "86526ed6-3c11-11de-a0ba-001e378eb67e";
		assertNotNull(service.getPersonByUuid(uuid));
		MockHttpServletRequest req = newDeleteRequest(getURI() + "/" + uuid, new Parameter("purge", ""));
		handle(req);
		assertNull(service.getPersonByUuid(uuid));
	}
	
	@Test
	public void shouldSearchAndReturnAListOfPersonsMatchingTheQueryString() throws Exception {
		MockHttpServletRequest req = request(RequestMethod.GET, getURI());
		req.addParameter("q", "Horatio");
		SimpleObject result = deserialize(handle(req));
		assertEquals(1, Util.getResultsSize(result));
		assertEquals(getUuid(), PropertyUtils.getProperty(Util.getResultsList(result).get(0), "uuid"));
	}
}
