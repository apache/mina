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
public interface PartialDecoder<T> {
    /**
     * Consume the buffer so as to decode a value. Not all the input buffer
     * may be consumed.
     * 
     * @param buffer the input buffer to decode
     * @return true if a value is available false if more data is requested
     */
    public boolean consume(ByteBuffer buffer);
    
    /**
     * Return the decoded value.
     * 
     * @return the decoded value
     */
    public T getValue();
    
    /**
     * Reset the internal state of the decoder to that new decoding can take place.
     */
    public void reset();

}
