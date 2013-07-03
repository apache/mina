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

import org.apache.mina.codec.IoBuffer;
import org.apache.mina.codec.ProtocolDecoder;

/**
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class SizePrefixedDecoder<OUT> implements ProtocolDecoder<IoBuffer, OUT, SizePrefixedDecoder.MutableInt> {

    /**
     * A mutable {@link Integer} wrapper.
     * 
     * @author <a href="http://mina.apache.org">Apache MINA Project</a>
     * 
     */
    protected static final class MutableInt {

        private Integer value = null;

        /**
         * Private constructor to avoid use of this class from other places.
         */
        private MutableInt() {

        }

        /**
         * 
         * Gets the value as a Integer instance.
         * 
         * @return the value as a Integer.
         */
        public Integer getValue() {
            return value;
        }

        /**
         * Returns the existence (or not) of an integer in this mutable.
         * 
         * @return true if it contains a value, false otherwise.
         */
        public boolean isDefined() {
            return value != null;
        }

        /**
         * Remove the value.
         */
        public void reset() {
            value = null;
        }

        /**
         * Set the value.
         * 
         * @param value
         *            the value to set
         */
        public void setValue(Integer value) {
            this.value = value;
        }
    }

    private final IoBufferDecoder<Integer> sizeDecoder;

    private final IoBufferDecoder<OUT> payloadDecoder;

    public SizePrefixedDecoder(IoBufferDecoder<Integer> sizeDecoder, IoBufferDecoder<OUT> payloadDecoder) {
        super();
        this.sizeDecoder = sizeDecoder;
        this.payloadDecoder = payloadDecoder;
    }

    @Override
    public MutableInt createDecoderState() {

        return new MutableInt();
    }

    @Override
    public OUT decode(IoBuffer input, MutableInt nextBlockSize) {
        OUT output = null;

        if (nextBlockSize.getValue() == null) {
            nextBlockSize.setValue(sizeDecoder.decode(input));
        }

        if (nextBlockSize.isDefined() && (input.remaining() >= nextBlockSize.getValue())) {
            IoBuffer buffer = input.slice();
            buffer.limit(buffer.position() + nextBlockSize.getValue());

            output = payloadDecoder.decode(buffer);
            nextBlockSize.reset();
        }

        return output;
    }

    @Override
    public void finishDecode(MutableInt context) {
        //
    }
}
