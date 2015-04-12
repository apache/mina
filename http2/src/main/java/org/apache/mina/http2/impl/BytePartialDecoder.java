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

/**
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class BytePartialDecoder implements PartialDecoder<byte[]> {
    private int offset;
    private byte[] value;
    
    /**
     * Decode an byte array.
     * 
     * @param size the size of the byte array to decode
     */
    public BytePartialDecoder(int size) {
        this.offset = 0;
        this.value = new byte[size];
    }

    public boolean consume(ByteBuffer buffer) {
        if (value.length - offset == 0) {
            throw new IllegalStateException();
        }
        int length = Math.min(buffer.remaining(), value.length - offset);
        buffer.get(value, offset, length);
        offset += length;
        return value.length - offset == 0;
    }
    
    public byte[] getValue() {
        if (value.length - offset > 0) {
            throw new IllegalStateException();
        }
        return value;
    }

    /** 
     * {@inheritDoc}
     */
    @Override
    public void reset() {
        offset = 0;
    }
}
