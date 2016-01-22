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
public interface IoAbsoluteReader {

    /**
     * @return the index of the first byte that can be accessed.
     */
    int first();

    /**
     * @return the index after the last byte that can be accessed.
     */
    int last();

    /**
     * @return the total number of bytes that can be accessed.
     */
    int length();

    /**
     * Creates an array with a view of part of this array.
     * 
     * @param index The starting position
     * @param length The number of bytes to copy
     * @return The ByteArray that is a view on the original array 
     */
    ByteArray slice(int index, int length);

    /**
     * @return the order of the bytes.
     */
    ByteOrder order();

    /**
     * @param index The starting position
     * @return a <tt>byte</tt> from the given index.
     */
    byte get(int index);

    /**
     * Gets enough bytes to fill the <tt>IoBuffer</tt> from the given index.
     * 
     * @param index The starting position
     * @param bb The IoBuffer that will be filled with the bytes
     */
    void get(int index, IoBuffer bb);

    /**
     * @param index The starting position
     * @return a <tt>short</tt> from the given index.
     */
    short getShort(int index);

    /**
     * @param index The starting position
     * @return an <tt>int</tt> from the given index.
     */
    int getInt(int index);

    /**
     * @param index The starting position
     * @return a <tt>long</tt> from the given index.
     */
    long getLong(int index);

    /**
     * @param index The starting position
     * @return a <tt>float</tt> from the given index.
     */
    float getFloat(int index);

    /**
     * @param index The starting position
     * @return a <tt>double</tt> from the given index.
     */
    double getDouble(int index);

    /**
     * @param index The starting position
     * @return a <tt>char</tt> from the given index.
     */
    char getChar(int index);
}
