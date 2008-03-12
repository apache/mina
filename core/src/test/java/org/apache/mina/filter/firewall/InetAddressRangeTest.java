/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package org.apache.mina.filter.firewall;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import junit.framework.TestCase;


public class InetAddressRangeTest extends TestCase {

	public void testContainsInRangeIp4() throws UnknownHostException {
		Inet4Address lower = (Inet4Address) InetAddress.getByName("1.2.2.4");
		Inet4Address middle = (Inet4Address) InetAddress.getByName("1.2.3.5");
		Inet4Address upper = (Inet4Address) InetAddress.getByName("1.2.4.6");
		Inet4Address toSmall = (Inet4Address) InetAddress.getByName("1.2.1.6");
		Inet4Address toLarge = (Inet4Address) InetAddress.getByName("1.2.155.6");
		
		InetAddressRange range = new InetAddressRange(lower, upper);
		
		assertTrue(range.contains(lower));
		assertTrue(range.contains(middle));
		assertTrue(range.contains(upper));
		assertFalse(range.contains(toSmall));
		assertFalse(range.contains(toLarge));
	}

	public void testContainsInRangeIpp() throws UnknownHostException {
		InetAddress lower = InetAddress.getByName("2001:0db8:85a3:08d3:1319:8a2e:0370:7344");
		InetAddress middle = InetAddress.getByName("2001:0db8:85a3:08d3:1319:8a2e:0470:1234");
		InetAddress upper = InetAddress.getByName("2001:0db8:85a3:08d3:1319:9a2e:0370:7344");
		InetAddress toSmall = InetAddress.getByName("1001:0db8:85a3:08d3:1319:8a2e:0370:7344");
		InetAddress toLarge = InetAddress.getByName("2001:0db8:85b3:08d3:1319:8a2e:0370:7344");
		
		InetAddressRange range = new InetAddressRange(lower, upper);
		
		assertTrue(range.contains(lower));
		assertTrue(range.contains(middle));
		assertTrue(range.contains(upper));
		assertFalse(range.contains(toSmall));
		assertFalse(range.contains(toLarge));
	}

	public void testLowerEqualToUpper() throws UnknownHostException {
		Inet4Address lower = (Inet4Address) InetAddress.getByName("1.2.3.4");
		Inet4Address upper = (Inet4Address) InetAddress.getByName("1.2.3.4");
		
		InetAddressRange range = new InetAddressRange(lower, upper);
		
		assertTrue(range.contains(lower));
		assertTrue(range.contains(upper));
	}
	
	public void testLowerLargerThanUpper() throws UnknownHostException {
		Inet4Address lower = (Inet4Address) InetAddress.getByName("1.2.3.5");
		Inet4Address upper = (Inet4Address) InetAddress.getByName("1.2.3.4");
		
		try {
			new InetAddressRange(lower, upper);
			fail("Must throw IllegalArgumentException");
		} catch(IllegalArgumentException e) {
			// OK
		}
	}
}
