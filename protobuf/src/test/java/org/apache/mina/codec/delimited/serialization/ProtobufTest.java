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
import org.apache.mina.codec.delimited.serialization.ProtobufDynamicMessageDecoder.ProtobufSerializedMessage;
import org.apache.mina.generated.protoc.AddressBookProtos.Person;
import org.junit.Test;

/**
 * A {@link ProtobufMessageEncoder} and {@link ProtobufMessageDecoder} test.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class ProtobufTest extends GenericSerializerTest<Person> {

	@Override
	public List<Person> getObjects() {
		List<Person> list = new LinkedList<Person>();

		list.add(Person.newBuilder().setId(1).setName("Jean Dupond")
				.setEmail("john.white@bigcorp.com").build());
		list.add(Person.newBuilder().setId(2).setName("Marie Blanc")
				.setEmail("marie.blanc@bigcorp.com").build());

		return list;
	}

	@Override
	public IoBufferDecoder<Person> getDecoder() throws Exception {
		return ProtobufMessageDecoder.newInstance(Person.class);
	}

	@Override
	public ByteBufferEncoder<Person> getEncoder() throws Exception {
		return new ProtobufMessageEncoder<Person>();
	}

	@Test
	public void testDynamic() throws Exception {
		ByteBufferEncoder<Person> encoder = getEncoder();
		ProtobufDynamicMessageDecoder decoder = new ProtobufDynamicMessageDecoder();

		for (Person object : getObjects()) {
			ProtobufSerializedMessage message = decoder.decode(IoBuffer
					.wrap(encoder.encode(object)));
			assertEquals(object, message.get(Person.class));
		}
	}
}
