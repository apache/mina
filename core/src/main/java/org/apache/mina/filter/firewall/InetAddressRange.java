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
import java.net.Inet6Address;
import java.net.InetAddress;

/**
 * Represents a range of IP4 or IP6 addresses.
 *
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev: 616100 $, $Date: 2008-01-28 23:58:32 +0100 (Mon, 28 Jan 2008) $
 */
public class InetAddressRange {

	private InetAddress lower;
	private InetAddress upper;

	/**
	 * Construct an IP range
	 * @param lower The lower {@link InetAddress}, must be an {@link Inet4Address} or {@link Inet6Address}
	 * @param upper The upper {@link InetAddress}, must be an {@link Inet4Address} or {@link Inet6Address}
	 * @throws IllegalArgumentException If the lower {@link InetAddress} is larger than the upper {@link InetAddress}
	 * @throws IllegalArgumentException Is not a {@link Inet4Address} or {@link Inet6Address}
	 * @throws IllegalArgumentException If lower and upper are of different types
	 * @throws NullPointerException If the lower or upper {@link InetAddress} is null
	 */
	public InetAddressRange(InetAddress lower, InetAddress upper) {
		if(lower == null) {
			throw new NullPointerException("Lower IP can not be null");
		}
		if(upper == null) {
			throw new NullPointerException("Upper IP can not be null");
		}
		if(!(lower instanceof Inet4Address || lower instanceof Inet6Address)) {
			throw new IllegalArgumentException("Lower address must be a Inet4Address or Inet6Address");
		}
		if(!(upper instanceof Inet4Address || upper instanceof Inet6Address)) {
			throw new IllegalArgumentException("Upper address must be a Inet4Address or Inet6Address");
		}
		if(!upper.getClass().equals(lower.getClass())) {
			throw new IllegalArgumentException("Lower and upper address must be of the same type");
		}
		
		this.lower = lower;
		this.upper = upper;

		if (compare(lower, upper) > 0) {
			throw new IllegalArgumentException("Lower must be lower than upper");
		}
	}
	
	private int compare(InetAddress a1, InetAddress a2) {
		byte[] b1 = a1.getAddress();
		byte[] b2 = a2.getAddress();
		
		for (int i = 0; i < b1.length; i++) {
			if (b1[i] != b2[i]) {
				return (b1[i] & 0xff) - (b2[i] & 0xff);
			}
		}
		return 0;
		
	}

	/**
	 * Get the lower IP of this range
	 * @return A {@link Inet4Address} or {@link Inet6Address}
	 */
	public InetAddress getLower() {
		return lower;
	}

	/**
	 * Get the upper IP of this range
	 * @return A {@link Inet4Address} or {@link Inet6Address}
	 */
	public InetAddress getUpper() {
		return upper;
	}

	/**
	 * Check if the {@link InetAddress} is within the range
	 * @param address The address to be checked against the range
	 * @return true if the address is within the range (inclusive)
	 * @throws NullPointerException If address is null
	 * @throws IllegalArgumentException If the address type differs from the range
	 */
	public boolean contains(InetAddress address) {
		if(address == null) {
			throw new NullPointerException("Address can not be null");
		}
		if(!address.getClass().equals(lower.getClass())) {
			throw new IllegalArgumentException("Address is not of the same InetAddress type as the range");
		}
		
		
		return compare(lower, address) <= 0 && compare(address, upper) <=0; 
	}

}
