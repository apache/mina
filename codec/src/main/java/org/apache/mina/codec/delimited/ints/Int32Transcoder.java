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

import org.apache.mina.codec.ProtocolDecoderException;

public class Int32Transcoder extends IntTranscoder {
    final private Endianness endianness;

    public Int32Transcoder(Endianness endianness) {
        super();
        this.endianness = endianness;
    }

    @Override
    public Integer decode(ByteBuffer input) throws ProtocolDecoderException {
        if (input.remaining() < 4)
            return null;

        if (endianness == Endianness.BIG) {
            if ((input.get(0) & 0x80) != 0)
                throw new ProtocolDecoderException("Not the big endian representation of a signed int32");
            return ((input.get() & 0xff) << 24) | ((input.get() & 0xff) << 16) | ((input.get() & 0xff) << 8)
                    | ((input.get() & 0xff));
        } else {
            if ((input.get(3) & 0x80) != 0)
                throw new ProtocolDecoderException("Not the small endian representation of a signed int32");
            return ((input.get() & 0xff)) | ((input.get() & 0xff) << 8) | ((input.get() & 0xff) << 16)
                    | ((input.get() & 0xff) << 24);
        }
    }

    @Override
    public ByteBuffer encode(Integer message) {
       
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
