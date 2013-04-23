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
package org.apache.mina.codec.delimited.ints;

import java.nio.ByteBuffer;

public class Int32Encoder implements IntEncoder {
	final private Endianness endianness;

	/**
	 * @param endianness
	 */
	public Int32Encoder(Endianness endianness) {
		super();
		this.endianness = endianness;
	}

	@Override
	public Void createEncoderState() {
		return null;
	}

	@Override
	public ByteBuffer encode(Integer message, Void context) {

		ByteBuffer buffer = ByteBuffer.allocate(4);
		if (endianness == Endianness.BIG) {
			buffer.put((byte) (0xff & (message >> 24)));
			buffer.put((byte) (0xff & (message >> 16)));
			buffer.put((byte) (0xff & (message >> 8)));
			buffer.put((byte) (0xff & (message)));
		} else {
			buffer.put((byte) (0xff & (message)));
			buffer.put((byte) (0xff & (message >> 8)));
			buffer.put((byte) (0xff & (message >> 16)));
			buffer.put((byte) (0xff & (message >> 24)));
		}
		buffer.flip();
		return buffer;

	}

}
