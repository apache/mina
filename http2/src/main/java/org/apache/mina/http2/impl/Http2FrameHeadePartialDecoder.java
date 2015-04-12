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
package org.apache.mina.http2.impl;

import java.nio.ByteBuffer;

import static org.apache.mina.http2.api.Http2Constants.HTTP2_31BITS_MASK;

/**
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class Http2FrameHeadePartialDecoder implements PartialDecoder<Http2FrameHeadePartialDecoder.Http2FrameHeader> {
    public static class Http2FrameHeader {
        private int length;
        private short type;
        private byte flags;
        private int streamID;

        public int getLength() {
            return length;
        }
       
        public void setLength(int length) {
            this.length = length;
        }
        
        public short getType() {
            return type;
        }
        
        public void setType(short type) {
            this.type = type;
        }
        
        public byte getFlags() {
            return flags;
        }
        
        public void setFlags(byte flags) {
            this.flags = flags;
        }
        
        public int getStreamID() {
            return streamID;
        }
        
        public void setStreamID(int streamID) {
            this.streamID = streamID;
        }
    }
    
    private static enum State {
        LENGTH,
        TYPE_FLAGS,
        STREAMID,
        END
    }
    
    private State state;
    private PartialDecoder<?> decoder;
    private Http2FrameHeader value;
    
    public Http2FrameHeadePartialDecoder() {
        reset();
    }
    
    public boolean consume(ByteBuffer buffer) {
        while (buffer.hasRemaining() && state != State.END) {
            if (decoder.consume(buffer)) {
                switch (state) {
                case LENGTH:
                    value.setLength(((IntPartialDecoder)decoder).getValue().intValue());
                    decoder = new BytePartialDecoder(2);
                    state = State.TYPE_FLAGS;
                    break;
                case TYPE_FLAGS:
                    value.setType(((BytePartialDecoder)decoder).getValue()[0]);
                    value.setFlags(((BytePartialDecoder)decoder).getValue()[1]);
                    decoder = new IntPartialDecoder(4);
                    state = State.STREAMID;
                    break;
                case STREAMID:
                    value.setStreamID(((IntPartialDecoder)decoder).getValue() & HTTP2_31BITS_MASK);
                    state = State.END;
                    break;
                }
            }
        }
        return state == State.END;
    }
    
    public Http2FrameHeader getValue() {
        return value;
    }

    /* (non-Javadoc)
     * @see org.apache.mina.http2.api.PartialDecoder#reset()
     */
    @Override
    public void reset() {
        state = State.LENGTH;
        decoder = new IntPartialDecoder(3);
        value = new Http2FrameHeader();
    }
}
