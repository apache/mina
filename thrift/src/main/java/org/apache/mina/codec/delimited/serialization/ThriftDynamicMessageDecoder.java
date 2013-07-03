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

import org.apache.mina.codec.IoBuffer;
import org.apache.mina.codec.delimited.IoBufferDecoder;
import org.apache.thrift.TBase;
import org.apache.thrift.TDeserializer;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;

/**
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class ThriftDynamicMessageDecoder extends
		IoBufferDecoder<ThriftDynamicMessageDecoder.ThriftSerializedMessage> {
	private final TDeserializer deserializer = new TDeserializer(
			new TBinaryProtocol.Factory());

	@Override
	public ThriftSerializedMessage decode(IoBuffer buffer) {
		return new ThriftSerializedMessage(deserializer, buffer);
	}

	public static final class ThriftSerializedMessage {
		private final IoBuffer buffer;

		private final TDeserializer deserializer;

		public ThriftSerializedMessage(TDeserializer deserializer,
				IoBuffer buffer) {
			this.buffer = buffer;
			this.deserializer = deserializer;
		}

		public <L extends TBase<?, ?>> L get(Class<L> clazz)
				throws InstantiationException, IllegalAccessException,
				TException {
			L object = clazz.newInstance();
			byte array[] = new byte[buffer.remaining()];
			buffer.get(array);
			deserializer.deserialize(object, array);
			return object;
		}
	}

	public static ThriftDynamicMessageDecoder newInstance() {
		return new ThriftDynamicMessageDecoder();
	}
}