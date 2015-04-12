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
package org.apache.mina.http2.api;

import java.nio.ByteBuffer;

import static org.apache.mina.http2.api.Http2Constants.HTTP2_HEADER_LENGTH;

/**
 * An HTTP2 frame.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * 
 */
public abstract class Http2Frame {

    private final int length;
    
    private final short type;
    
    private final byte flags;
    
    private final int streamID;
    
    public int getLength() {
        return length;
    }

    public short getType() {
        return type;
    }
    
    public byte getFlags() {
        return flags;
    }

    /**
     * Utility method to test if a specific bit is set in the flags.
     * 
     * @param flag the bit to test
     * @return
     */
    protected boolean isFlagSet(byte flag) {
        return (getFlags() & flag) == flag;
    }

    public int getStreamID() {
        return streamID;
    }
    
    /**
     * Serialize the frame to a buffer.
     * 
     * @return the allocated buffer
     */
    public ByteBuffer toBuffer() {
        ByteBuffer buffer = ByteBuffer.allocateDirect(HTTP2_HEADER_LENGTH + getLength());
        buffer.put((byte) (getLength() >> 16));
        buffer.put((byte) (getLength() >> 8));
        buffer.put((byte) (getLength() ));
        buffer.put((byte) getType());
        buffer.put(getFlags());
        buffer.putInt(getStreamID());
        writePayload(buffer);
        buffer.flip();
        return buffer;
    }
    
    /**
     * Writes the frame specific payload to the allocated buffer.
     * Must be implemented by frames implementation.
     *  
     * @param buffer the buffer to write to
     */
    public abstract void writePayload(ByteBuffer buffer);

    protected <T extends AbstractHttp2FrameBuilder<T,V>, V extends Http2Frame> Http2Frame(AbstractHttp2FrameBuilder<T, V> builder) {
        this.length = builder.getLength();
        this.type = builder.getType();
        this.flags = builder.getFlags();
        this.streamID = builder.getStreamID();
    }

    public static abstract class AbstractHttp2FrameBuilder<T extends AbstractHttp2FrameBuilder<T,V>, V extends Http2Frame>  {
        private int length = (-1);
        
        private short type;
        
        private byte flags;
        
        private int streamID;
        
        public void setLength(int length) {
            this.length = length;
        }
        
        @SuppressWarnings("unchecked")
        public T length(int length) {
            setLength(length);
            return (T) this;
        }
        
        public int getLength() {
            return length;
        }

        @SuppressWarnings("unchecked")
        public T type(short type) {
            this.type = type;
            return (T) this;
        }
        
        public short getType() {
            return type;
        }
        
        public void setFlags(byte flags) {
            this.flags = flags;
        }

        @SuppressWarnings("unchecked")
        public T flags(byte flags) {
            setFlags(flags);
            return (T) this;
        }
        
        public byte getFlags() {
            return flags;
        }
        
        /**
         * Utility method for setting a specific bit in the flags.
         * 
         * @param flag the bit to set
         */
        protected void addFlag(byte flag) {
            setFlags((byte) (getFlags() | flag));
        }
        
        /**
         * Utility method to test if a specific bit is set in the flags.
         * 
         * @param flag the bit to test
         * @return
         */
        protected boolean isFlagSet(byte flag) {
            return (getFlags() & flag) == flag;
        }

        @SuppressWarnings("unchecked")
        public T streamID(int streamID) {
            this.streamID = streamID;
            return (T) this;
        }
        
        public int getStreamID() {
            return streamID;
        }
        
        public abstract V build();
    }
}
