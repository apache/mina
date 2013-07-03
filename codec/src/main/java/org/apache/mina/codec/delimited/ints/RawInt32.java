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
import java.nio.ByteOrder;

import org.apache.mina.codec.IoBuffer;
import org.apache.mina.codec.delimited.ByteBufferEncoder;
import org.apache.mina.codec.delimited.IoBufferDecoder;

/**
 * 
 * Class providing raw/canonical representation of integers.
 * 
 * 
 * <style type="text/css"> pre-fw { color: rgb(0, 0, 0); display: block;
 * font-family:courier, "courier new", monospace; font-size: 13px; white-space:
 * pre; } </style>
 * 
 * <h2>RawInt32 encoder and decoder</h2>
 * <p>
 * This pair provides a mechanism called canonical form serialization.
 * 
 * In this representations all 32-bits integer are encoded over 4 bytes.
 * 
 * 
 * This library provides two variants <i>big-endian</i> and <i>small-endian</i>.
 * 
 * 
 * In both cases, the inner bits of each byte are ordered from the most to the
 * least significant bit.
 * 
 * The difference between the two variants is the ordering of the four bytes.
 * 
 * <ul>
 * <li>Big-endian: <i>The bytes are ordered from the most to the least
 * significant one</i></li>
 * <li>Little-endian: <i>The bytes are ordered from the least to the most
 * significant one</i></li>
 * </ul>
 * 
 * <p>
 * This representation is often used since it is used internally in CPUs,
 * therefore programmers using a low level languages (assembly, C, ...)
 * appreciate using it (for ease of use or performance reasons). When integers
 * are directly copied from memory, it is required to ensure this serializer
 * uses the appropriate endianness on both ends.
 * <ul>
 * <li>Big-endian: 68k, MIPS, Alpha, SPARC</li>
 * <li>Little-endian: x86, x86-64, ARM</li>
 * <li><i>Bi-</i>endian (depends of the operating system): PowerPC, Itanium</li>
 * </ul>
 * </p>
 * 
 * <p>
 * More details availabile on the Wikipedia
 * "<a href="http://en.wikipedia.org/wiki/Endianness">Endianness page</a>".
 * </p>
 * 
 * <h2>On-wire representation</h2>
 * <p>
 * Encoding of the value 67305985
 * </p>
 * <i>Big-Endian variant:</i>
 * 
 * <pre-fw>
 * 0000 0100  0000 0011  0000 0010  0000 0001
 * ‾‾‾‾↓‾‾‾‾  ‾‾‾‾↓‾‾‾‾  ‾‾‾‾↓‾‾‾‾  ‾‾‾‾↓‾‾‾‾
 *     4          3          2          1      // 4·2<sup>24</sup> + 3·2<sup>16</sup> + 2·2<sup>8</sup> + 1·2<sup>0</sup>  = 67305985
 * 
 * </pre-fw>
 * 
 * <i>Little-Endian variant:</i>
 * 
 * <pre-fw>
 * 0000 0001  0000 0010  0000 0011  0000 0100
 * ‾‾‾‾↓‾‾‾‾  ‾‾‾‾↓‾‾‾‾  ‾‾‾‾↓‾‾‾‾  ‾‾‾‾↓‾‾‾‾
 *     1          2          3          4      // 1·2<sup>0</sup> + 2·2<sup>8</sup> + 3·2<sup>16</sup> + 4·2<sup>24</sup>  = 67305985
 * </pre-fw>
 * 
 * </p>
 * 
 * <p>
 * n.b. This class doesn't have any dependency against Apache Thrift or any
 * other library in order to provide this convenient integer serialization
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
public final class RawInt32 implements IntTranscoder {
    private final ByteOrder bo;

    public RawInt32(ByteOrder bo) {
        super();
        this.bo = bo == null ? ByteOrder.BIG_ENDIAN : bo;
    }

    @Override
    public IoBufferDecoder<Integer> getDecoder() {
        return new Decoder();
    }

    @Override
    public ByteBufferEncoder<Integer> getEncoder() {
        return new Encoder();
    }

    private static final int BYTE_MASK = 0xff;

    /**
     * Documentation available in the {@link RawInt32} enclosing class.
     * 
     * @author <a href="http://mina.apache.org">Apache MINA Project</a>
     * 
     */
    private class Decoder extends IoBufferDecoder<Integer> {

        @Override
        public Integer decode(IoBuffer input) {
            if (input.remaining() < 4) {
                return null;
            }

            int out = 0;
            for (int i = 0; i < 32; i += 8) {
                out |= (input.get() & 0xff) << (bo == ByteOrder.BIG_ENDIAN ? 24 - i : i);
            }
            return out;
        }
    }

    /**
     * Documentation available in the {@link RawInt32} enclosing class.
     * 
     * @author <a href="http://mina.apache.org">Apache MINA Project</a>
     * 
     */
    private class Encoder extends ByteBufferEncoder<Integer> {

        @Override
        public void writeTo(Integer message, ByteBuffer buffer) {

            if (buffer.remaining() < 4) {
                throw new BufferUnderflowException();
            }
            for (int i = 0; i < 32; i += 8) {
                buffer.put((byte) (message >> (bo == ByteOrder.BIG_ENDIAN ? 24 - i : i)));
            }
        }

        @Override
        public int getEncodedSize(Integer message) {
            return 4;
        }

    }
}
