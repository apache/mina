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
package org.apache.mina.example.udp;

import java.net.SocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.mina.common.IoSession;
import org.apache.mina.common.IoSessionRecycler;

public class SimpleSessionRecycler implements IoSessionRecycler {

	Map<Long, IoSession> map = Collections.synchronizedMap(new HashMap<Long, IoSession>());

	public void put(IoSession session) {
		SocketAddress local = session.getLocalAddress();
		SocketAddress remote = session.getRemoteAddress();
		map.put(getKey(local, remote), session);
	}

	long getKey(SocketAddress local, SocketAddress remote) {
		long key = ((long) local.hashCode() << 32) | remote.hashCode();
		return key;
	}

	/**
	 * Attempts to retrieve a recycled {@link IoSession}.
	 * 
	 * @param localAddress
	 *            the local socket address of the {@link IoSession} the
	 *            transport wants to recycle.
	 * @param remoteAddress
	 *            the remote socket address of the {@link IoSession} the
	 *            transport wants to recycle.
	 * @return a recycled {@link IoSession}, or null if one cannot be found.
	 */
	public IoSession recycle(SocketAddress localAddress,
			SocketAddress remoteAddress) {
		IoSession session = map.get(getKey(localAddress, remoteAddress));
		return session;
	}

	public void remove(IoSession session) {
		map.remove(getKey(session.getLocalAddress(), session.getRemoteAddress()));
	}
}