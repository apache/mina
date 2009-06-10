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
 * Provides absolute read access to a sequence of bytes.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public interface IoAbsoluteReader
{

    /**
     * Get the index of the first byte that can be accessed.
     */
    int first();


    /**
     * Gets the index after the last byte that can be accessed.
     */
    int last();


    /**
     * Gets the total number of bytes that can be accessed.
     */
    int length();


    /**
     * Creates an array with a view of part of this array.
     */
    ByteArray slice( int index, int length );


    /**
     * Gets the order of the bytes.
     */
    ByteOrder order();


    /**
     * Gets a <code>byte</code> from the given index.
     */
    byte get( int index );


    /**
     * Gets enough bytes to fill the <code>IoBuffer</code> from the given index.
     */
    public void get( int index, IoBuffer bb );


    /**
     * Gets a <code>short</code> from the given index.
     */
    short getShort( int index );


    /**
     * Gets an <code>int</code> from the given index.
     */
    int getInt( int index );


    /**
     * Gets a <code>long</code> from the given index.
     */
    long getLong( int index );


    /**
     * Gets a <code>float</code> from the given index.
     */
    float getFloat( int index );


    /**
     * Gets a <code>double</code> from the given index.
     */
    double getDouble( int index );


    /**
     * Gets a <code>char</code> from the given index.
     */
    char getChar( int index );
}
