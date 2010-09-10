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
package org.apache.mina.util.byteaccess;


import java.nio.ByteOrder;

import org.apache.mina.core.buffer.IoBuffer;


/**
 * Provides relative read access to a sequence of bytes.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public interface IoRelativeWriter
{

    /**
     * Gets the number of remaining bytes that can be read.
     */
    int getRemaining();


    /**
     * Checks if there are any remaining bytes that can be read.
     */
    boolean hasRemaining();


    /**
     * Advances the writer by the given number of bytes.
     */
    void skip( int length );


    /**
     * Gets the order of the bytes.
     */
    ByteOrder order();


    /**
     * Puts a <code>byte</code> and advances the reader.
     */
    void put( byte b );


    /**
     * Puts enough bytes to fill the <code>IoBuffer</code> and advances the reader.
     */
    void put( IoBuffer bb );


    /**
     * Puts a <code>short</code> and advances the reader.
     */
    void putShort( short s );


    /**
     * Puts an <code>int</code> and advances the reader.
     */
    void putInt( int i );


    /**
     * Puts a <code>long</code> and advances the reader.
     */
    void putLong( long l );


    /**
     * Puts a <code>float</code> and advances the reader.
     */
    void putFloat( float f );


    /**
     * Puts a <code>double</code> and advances the reader.
     */
    void putDouble( double d );


    /**
     * Puts a <code>char</code> and advances the reader.
     */
    void putChar( char c );
}
