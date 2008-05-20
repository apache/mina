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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Provides absolute write access to a sequence of bytes.
 */
public interface IoAbsoluteWriter {

    /**
     * Get the index of the first byte that can be accessed.
     */
    int first();

    /**
     * Gets the index after the last byte that can be accessed.
     */
    int last();

    /**
     * Gets the order of the bytes.
     */
    ByteOrder order();

    /**
     * Puts a <code>byte</code> at the given index.
     */
    void put(int index, byte b);

    /**
     * Puts bytes from the <code>ByteBuffer</code> at the given index.
     */
    public void put(int index, ByteBuffer bb);

    /**
     * Puts an <code>int</code> at the given index.
     */
    void putInt(int index, int i);
}
