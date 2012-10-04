package org.apache.mina.http;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;

import org.apache.mina.http.api.HttpMethod;
import org.apache.mina.http.api.HttpRequest;
import org.apache.mina.http.api.HttpVersion;
import org.junit.Test;

public class HttpRequestImplTestCase {

	@Test
	public void testGetParameterNoParameter() {
		HttpRequest req = new HttpRequestImpl(HttpVersion.HTTP_1_1, HttpMethod.GET, "/","", null);
		assertNull("p0 doesn't exist", req.getParameter("p0"));
	}

	@Test
	public void testGetParameterOneEmptyParameter() {
		HttpRequest req = new HttpRequestImpl(HttpVersion.HTTP_1_1, HttpMethod.GET, "/", "p0=", null);
		assertEquals("p0 is emtpy", "", req.getParameter("p0"));
		assertNull("p1 doesn't exist", req.getParameter("p1"));
	}

	@Test
	public void testGetParameterOneParameter() {
		HttpRequest req = new HttpRequestImpl(HttpVersion.HTTP_1_1, HttpMethod.GET, "/", "p0=0", null);
		assertEquals("p0 is '0'", "0", req.getParameter("p0"));
		assertNull("p1 doesn't exist", req.getParameter("p1"));
	}

	@Test
	public void testGetParameter3Parameters() {
		HttpRequest req = new HttpRequestImpl(HttpVersion.HTTP_1_1, HttpMethod.GET, "/", "p0=&p1=1&p2=2", null);
		assertEquals("p0 is emtpy", "", req.getParameter("p0"));
		assertEquals("p1 is '1'", "1", req.getParameter("p1"));
		assertEquals("p2 is '2'", "2", req.getParameter("p2"));
		assertNull("p3 doesn't exist", req.getParameter("p3"));
	}

	@Test
	public void testGetParametersNoParameter() {
		HttpRequest req = new HttpRequestImpl(HttpVersion.HTTP_1_1, HttpMethod.GET, "/", "", null);
		assertTrue("Empty Map", req.getParameters().isEmpty());
	}

	@Test
	public void testGetParameters3Parameters() {
		HttpRequest req = new HttpRequestImpl(HttpVersion.HTTP_1_1, HttpMethod.GET, "/","p0=&p1=1&p2=2", null);
		Map<String, List<String>> parameters = req.getParameters();
		assertEquals("3 parameters", 3, parameters.size());
		assertEquals("one p0", 1, parameters.get("p0").size());
		assertEquals("p0 is emtpy", "", parameters.get("p0").get(0));
		assertEquals("one p1", 1, parameters.get("p1").size());
		assertEquals("p1 is '1'", "1", parameters.get("p1").get(0));
		assertEquals("one p2", 1, parameters.get("p2").size());
		assertEquals("p2 is '2'", "2", parameters.get("p2").get(0));
	}

	@Test
	public void testGetParameters3ParametersWithDuplicate() {
		HttpRequest req = new HttpRequestImpl(HttpVersion.HTTP_1_1, HttpMethod.GET, "/","p0=&p1=1&p0=2", null);
		Map<String, List<String>> parameters = req.getParameters();
		assertEquals("2 parameters", 2, parameters.size());
		assertEquals("two p0", 2, parameters.get("p0").size());
		assertEquals("1st p0 is emtpy", "", parameters.get("p0").get(0));
		assertEquals("2nd p0 is '2'", "2", parameters.get("p0").get(1));
		assertEquals("one p1", 1, parameters.get("p1").size());
		assertEquals("p1 is '1'", "1", parameters.get("p1").get(0));
		assertNull("No p2", parameters.get("p2"));
	}

}
