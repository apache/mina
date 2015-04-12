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
public class IntPartialDecoder implements PartialDecoder<Integer> {
    private int size;
    private int remaining;
    private int value;
    
    /**
     * Decode an integer whose size is different from the standard 4.
     * 
     * @param size the size (1,2,3,4) to decode
     */
    public IntPartialDecoder(int size) {
        this.remaining = size;
        this.size = size;
    }

    /**
     * Decode a 4 bytes integer 
     */
    public IntPartialDecoder() {
        this(4);
    }
    
    public boolean consume(ByteBuffer buffer) {
        if (remaining == 0) {
            throw new IllegalStateException();
        }
        while (remaining > 0 && buffer.hasRemaining()) {
            value = (value << 8) + (buffer.get() & 0x00FF);
            --remaining;
        }
        return remaining == 0;
    }
    
    public Integer getValue() {
        if (remaining > 0) {
            throw new IllegalStateException();
        }
        return value;
    }

    /* (non-Javadoc)
     * @see org.apache.mina.http2.api.PartialDecoder#reset()
     */
    @Override
    public void reset() {
        remaining = size;
        value = 0;
    }
    
    
}
