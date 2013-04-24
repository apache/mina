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

import java.nio.ByteBuffer;

import org.apache.mina.codec.ProtocolDecoder;
import org.apache.mina.codec.ProtocolDecoderException;
import org.apache.mina.codec.StatelessProtocolDecoder;
import org.apache.mina.codec.delimited.ints.IntSizeTranscoder;

/**
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class SizePrefixedDecoder implements ProtocolDecoder<ByteBuffer, ByteBuffer, SizePrefixedDecoder.IntRef> {

    final static protected class IntRef {
        private Integer value = null;

        public Integer get() {
            return value;
        }

        public void reset() {
            value = null;
        }

        public boolean isDefined() {
            return value != null;
        }

        public void set(Integer value) {
            this.value = value;
        }
    }

    final private StatelessProtocolDecoder<ByteBuffer, Integer> transcoder;

    public SizePrefixedDecoder(IntSizeTranscoder transcoder) {
        super();
        this.transcoder = transcoder;
    }

    @Override
    public IntRef createDecoderState() {
        return new IntRef();
    }

    @Override
    public ByteBuffer decode(ByteBuffer input, IntRef nextBlockSize) throws ProtocolDecoderException {
        ByteBuffer output = null;
        if (nextBlockSize.get() == null) {
            nextBlockSize.set(transcoder.decode(input, null));
        }

        if (nextBlockSize.isDefined()) {
            if (input.remaining() >= nextBlockSize.get()) {
                output = input.slice();
                output.limit(output.position() + nextBlockSize.get());
                nextBlockSize.reset();
            }
        }
        return output;
    }

    @Override
    public void finishDecode(IntRef context) {
        //
    }

}
