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

import org.apache.mina.codec.IoBuffer;
import org.apache.mina.codec.ProtocolDecoderException;
import org.apache.mina.codec.delimited.ByteBufferEncoder;
import org.apache.mina.codec.delimited.IoBufferDecoder;

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
 */
/*
 * About the suppression of warnings:
 * This class contains a lot of bit-shifting, logical and/or operations order to handle
 * VarInt conversions. The code contains a lot of hard-coded integer that tools like
 * Sonar classify as "magic numbers". Using final static variables for all of them
 * would have resulted in a code less readable.
 * The "all" scope is too generic, but Sonar doesn't not handle properly others scopes 
 * like "MagicNumber" (Sonar 3.6 - 03July2013)
 */
@SuppressWarnings("all")
public final class VarInt implements IntTranscoder {
    
    @Override
    public IoBufferDecoder<Integer> getDecoder() {
        return new Decoder();
    }

    @Override
    public ByteBufferEncoder<Integer> getEncoder() {
        return new Encoder();
    }

    /**
     * Documentation available in the {@link VarInt} enclosing class.
     * 
     * @author <a href="http://mina.apache.org">Apache MINA Project</a>
     * 
     */
    private class Decoder extends IoBufferDecoder<Integer> {

        @Override
        public Integer decode(IoBuffer input) {
            int origpos = input.position();

            try {
                byte tmp = input.get();
                if (tmp >= 0) {
                    return (int) tmp;
                }
                int result = tmp & 0x7f;
                if ((tmp = input.get()) >= 0) {
                    result |= tmp << 7;
                } else {
                    result |= (tmp & 0x7f) << 7;                    
                    if ((tmp = input.get()) >= 0) {
                        result |= tmp << 14;
                    } else {
                        result |= (tmp & 0x7f) << 14;               
                        if ((tmp = input.get()) >= 0) {
                            result |= tmp << 21;
                        } else {
                            result |= (tmp & 0x7f) << 21;
                            
                            // check that there are at most 3 significant bits available
                            if (((tmp = input.get()) & ~0x7) == 0) {
                                result |= tmp << 28;
                            } else {
                                throw new ProtocolDecoderException("Not the varint representation of a signed int32");
                            }
                        }
                    }
                }
                return result;

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
    private class Encoder extends ByteBufferEncoder<Integer> {

        @Override
        public void writeTo(Integer message, ByteBuffer buffer) {
            // VarInts don't support negative values
            int value = Math.max(0,message);            

            while (true) {
                if ((value & ~0x7F) == 0) {
                    buffer.put((byte) value);
                    return;
                } else {
                    buffer.put((byte) ((value & 0x7F) | 0x80));
                    value >>>= 7;
                }
            }

        }

        @Override
        public int getEncodedSize(Integer value) {
            if ((value & (0xffffffff << 7)) == 0) {
                return 1;
            }
            if ((value & (0xffffffff << 14)) == 0) {
                return 2;
            }
            if ((value & (0xffffffff << 21)) == 0) {
                return 3;
            }
            if ((value & (0xffffffff << 28)) == 0) {
                return 4;
            }
            return 5;
        }

    }
}
