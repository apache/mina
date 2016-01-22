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
 * Provides absolute write access to a sequence of bytes.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public interface IoAbsoluteWriter {

    /**
     * @return the index of the first byte that can be accessed.
     */
    int first();

    /**
     * @return the index after the last byte that can be accessed.
     */
    int last();

    /**
     * @return the order of the bytes.
     */
    ByteOrder order();

    /**
     * Puts a <code>byte</code> at the given index.
     * 
     * @param index The position
     * @param b The byte to put
     */
    void put(int index, byte b);

    /**
     * Puts bytes from the <code>IoBuffer</code> at the given index.
     * 
     * @param index The position
     * @param bb The bytes to put
     */
    void put(int index, IoBuffer bb);

    /**
     * Puts a <code>short</code> at the given index.
     * 
     * @param index The position
     * @param s The short to put
     */
    void putShort(int index, short s);

    /**
     * Puts an <code>int</code> at the given index.
     * 
     * @param index The position
     * @param i The int to put
     */
    void putInt(int index, int i);

    /**
     * Puts a <code>long</code> at the given index.
     * 
     * @param index The position
     * @param l The long to put
     */
    void putLong(int index, long l);

    /**
     * Puts a <code>float</code> at the given index.
     * 
     * @param index The position
     * @param f The float to put
     */
    void putFloat(int index, float f);

    /**
     * Puts a <code>double</code> at the given index.
     * 
     * @param index The position
     * @param d The doubvle to put
     */
    void putDouble(int index, double d);

    /**
     * Puts a <code>char</code> at the given index.
     * 
     * @param index The position
     * @param c The char to put
     */
    void putChar(int index, char c);
}
