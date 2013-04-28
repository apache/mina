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
import org.apache.mina.codec.delimited.ByteBufferDecoder;
import org.apache.mina.codec.delimited.ByteBufferEncoder;

/**
 * Class providing a variable length representation of integers.
 * 
 * <style type="text/css"> pre-fw { color: rgb(0, 0, 0); display: block;
 * font-family:courier, "courier new", monospace; font-size: 13px; white-space:
 * pre; } </style>
 * 
 * <h2>Base 128 Varints serializer</h2>
 * <p>
 * This serializer is efficient in terms of computing costs as well as
 * bandwith/memory usage.
 * </p>
 * <p>
 * The average memory usage overall the range 0 to
 * {@link java.lang.Integer#MAX_VALUE} is 4.87 bytes per number which is not far
 * from the canonical form ({@link RawInt32}), however varints are an
 * interesting solution since the small values (which are supposed to be more
 * frequent) are using less bytes.
 * </p>
 * <p>
 * All bytes forming a varint except the last one have the most significant bit
 * (MSB) set. The lower 7 bits of each byte contains the actual representation
 * of the two's complement representation of the number (least significant group
 * first).
 * </p>
 * <p>
 * n.b. This serializer is fully compatible with the 128 Varint mechanism
 * shipped with the <a
 * href="https://developers.google.com/protocol-buffers/docs/encoding#varints" >
 * Google Protocol Buffer stack</a> as default representation of messages sizes.
 * </p>
 * <h2>On-wire representation</h2>
 * <p>
 * Encoding of the value 812
 * 
 * <pre-fw>
 * 
 * 1001 1100  0000 0110
 * ↑          ↑           
 * 1          0           // the most significant bit being unset designs the last byte
 *  ___↑____   ___↑____   
 *  001 1100   000 0110   // the remaining bits defines the value itself
 * →      44          6   // 44 + 128 * 6 = 812
 * </pre-fw>
 * 
 * </p>
 * <p>
 * n.b. This class doesn't have any dependency against Google Protocol Buffer or
 * any other library in order to provide this convenient integer serialization
 * module to any software using FramedMINA.
 * </p>
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * 
 */
public class VarInt {
    // this class should not be instanciated
    private VarInt() {
    }

    /**
     * Documentation available in the {@link VarInt} enclosing class.
     * 
     * @author <a href="http://mina.apache.org">Apache MINA Project</a>
     * 
     */
    static public class Decoder extends ByteBufferDecoder<Integer> {

        @Override
        public Integer decode(ByteBuffer input) throws ProtocolDecoderException {
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
                        throw new ProtocolDecoderException("Not the varint representation of a signed int32");
                }
            } catch (BufferUnderflowException bue) {
                input.position(origpos);
            }
            return null;
        }
    }

    /**
     * Documentation available in the {@link VarInt} enclosing class.
     * 
     * @author <a href="http://mina.apache.org">Apache MINA Project</a>
     * 
     */
    static public class Encoder extends ByteBufferEncoder<Integer> {

        @Override
        public void writeTo(Integer message, ByteBuffer buffer) {
            // VarInts don't support negative values
            if (message < 0)
                message = 0;
            int value = message;

            while (value > 0x7f) {
                buffer.put((byte) ((value & 0x7f) | 0x80));
                value >>= 7;
            }

            buffer.put((byte) value);
            buffer.flip();
        }

        @Override
        public int getEncodedSize(Integer message) {
            if (message == 0)
                return 1;
            else {
                int log2 = 32 - Integer.numberOfLeadingZeros(message);
                return (log2 + 6) / 7;
            }
        }
    }
}
