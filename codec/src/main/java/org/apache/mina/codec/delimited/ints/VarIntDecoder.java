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

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

import org.apache.mina.codec.ProtocolDecoderException;

public class VarIntDecoder implements IntDecoder {

	@Override
	public Void createDecoderState() {
		return null;
	}

	@Override
	public Integer decode(ByteBuffer input, Void context)
			throws ProtocolDecoderException {
		int origpos = input.position();
		int size = 0;

		try {
			for (int i = 0;; i += 7) {
				byte tmp = input.get();

				if ((tmp & 0x80) == 0 && (i != 4 * 7 || tmp < 1 << 3))
					return size | (tmp << i);
				else if (i < 4 * 7)
					size |= (tmp & 0x7f) << i;
				else
					throw new ProtocolDecoderException(
							"Not the varint representation of a signed int32");
			}
		} catch (BufferUnderflowException bue) {
			input.position(origpos);
		}
		return null;
	}

	@Override
	public void finishDecode(Void context) {
	}
}
