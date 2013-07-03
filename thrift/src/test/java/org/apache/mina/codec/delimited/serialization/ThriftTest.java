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
package org.apache.mina.codec.delimited.serialization;

import static org.junit.Assert.assertEquals;

import java.util.LinkedList;
import java.util.List;

import org.apache.mina.codec.IoBuffer;
import org.apache.mina.codec.delimited.ByteBufferEncoder;
import org.apache.mina.codec.delimited.IoBufferDecoder;
import org.apache.mina.generated.thrift.UserProfile;
import org.junit.Test;

/**
 * A {@link ThriftMessageEncoder} and {@link ThriftMessageDecoder} test.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class ThriftTest extends GenericSerializerTest<UserProfile> {

	@Override
	public List<UserProfile> getObjects() {
		List<UserProfile> list = new LinkedList<UserProfile>();

		list.add(new UserProfile().setUid(1).setName("Jean Dupond"));
		list.add(new UserProfile().setUid(2).setName("Marie Blanc"));

		return list;
	}

	@Override
	public IoBufferDecoder<UserProfile> getDecoder() throws Exception {
		return ThriftMessageDecoder.newInstance(UserProfile.class);
	}

	@Override
	public ByteBufferEncoder<UserProfile> getEncoder() throws Exception {
		return ThriftMessageEncoder.newInstance(UserProfile.class);
	}

	@Test
	public void testDynamic() throws Exception {
		ByteBufferEncoder<UserProfile> encoder = getEncoder();
		ThriftDynamicMessageDecoder decoder = new ThriftDynamicMessageDecoder();

		for (UserProfile object : getObjects()) {
			ThriftDynamicMessageDecoder.ThriftSerializedMessage message = decoder
					.decode(IoBuffer.wrap(encoder.encode(object)));
			assertEquals(object, message.get(UserProfile.class));
		}
	}
}
