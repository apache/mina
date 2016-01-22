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
public interface IoRelativeReader {

    /**
     * @return the number of remaining bytes that can be read.
     */
    int getRemaining();

    /**
     * Checks if there are any remaining bytes that can be read.
     * 
     * @return <tt>true</tt> if there are some remaining bytes in the buffer
     */
    boolean hasRemaining();

    /**
     * Advances the reader by the given number of bytes.
     * 
     * @param length the number of bytes to skip
     */
    void skip(int length);

    /**
     * @param length The number of bytes to get
     * @return an array with a view of part of this array.
     */
    ByteArray slice(int length);

    /**
     * @return the bytes' order
     */
    ByteOrder order();

    /**
     * @return the <code>byte</code> at the current position and advances the reader.
     */
    byte get();

    /**
     * Gets enough bytes to fill the <code>IoBuffer</code> and advances the reader.
     * 
     * @param bb The IoBuffer that will contain the read bytes
     */
    void get(IoBuffer bb);

    /**
     * @return a <code>short</code> and advances the reader.
     */
    short getShort();

    /**
     * @return an <code>int</code> and advances the reader.
     */
    int getInt();

    /**
     * @return a <code>long</code> and advances the reader.
     */
    long getLong();

    /**
     * @return a <code>float</code> and advances the reader.
     */
    float getFloat();

    /**
     * @return a <code>double</code> and advances the reader.
     */
    double getDouble();

    /**
     * @return a <code>char</code> and advances the reader.
     */
    char getChar();
}
