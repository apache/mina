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
package org.apache.mina.codec.delimited;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

import org.apache.mina.codec.delimited.serialization.ThriftMessageDecoder;
import org.apache.mina.codec.delimited.serialization.ThriftMessageEncoder;
import org.apache.thrift.TSerializer;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TMemoryBuffer;

import ch.fever.code.mina.thrift.UserProfile;


/**
 * A {@link ThriftEncoder} and {@link ThriftDecoder} test.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class ThriftTest extends DelimitTest<UserProfile> {

	public List<UserProfile> getObjects() {

		List<UserProfile> list = new LinkedList<UserProfile>();

		list.add(new UserProfile().setUid(1).setName("Jean Dupond"));
		list.add(new UserProfile().setUid(2).setName("Marie Blanc"));

		return list;
	}

	protected ByteBuffer delimitWithOriginal() throws Exception {

		TMemoryBuffer m = new TMemoryBuffer(1000000);
		TFramedTransport t = new TFramedTransport(m);
		TSerializer tt = new TSerializer();
		for (UserProfile up : getObjects()) {
			t.write(tt.serialize(up));
			t.flush();
		}
		return ByteBuffer.wrap(m.getArray(), 0, m.length());

	}

	public SizePrefixedEncoder<UserProfile> getSerializer()
			throws SecurityException, NoSuchMethodException {
		return ThriftEncoder.newInstance(UserProfile.class);
	}

}
