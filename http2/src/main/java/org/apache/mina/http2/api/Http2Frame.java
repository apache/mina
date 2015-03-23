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

/**
 * An SPY data frame
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * 
 */
public abstract class Http2Frame {

    private final int length;
    
    private final short type;
    
    private final short flags;
    
    private final int streamID;
    
    public int getLength() {
        return length;
    }

    public short getType() {
        return type;
    }
    
    public short getFlags() {
        return flags;
    }

    public int getStreamID() {
        return streamID;
    }

    protected <T extends AbstractHttp2FrameBuilder<T,V>, V extends Http2Frame> Http2Frame(AbstractHttp2FrameBuilder<T, V> builder) {
        this.length = builder.getLength();
        this.type = builder.getType();
        this.flags = builder.getFlags();
        this.streamID = builder.getStreamID();
    }

    public static abstract class AbstractHttp2FrameBuilder<T extends AbstractHttp2FrameBuilder<T,V>, V extends Http2Frame>  {
        private int length;
        
        private short type;
        
        private short flags;
        
        private int streamID;
        
        @SuppressWarnings("unchecked")
        public T length(int length) {
            this.length = length;
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

        @SuppressWarnings("unchecked")
        public T flags(short flags) {
            this.flags = flags;
            return (T) this;
        }
        
        public short getFlags() {
            return flags;
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
