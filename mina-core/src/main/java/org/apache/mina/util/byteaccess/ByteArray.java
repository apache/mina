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
 * Represents a sequence of bytes that can be read or written directly or
 * through a cursor.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public interface ByteArray extends IoAbsoluteReader, IoAbsoluteWriter {

    /**
     * {@inheritDoc}
     */
    @Override
    int first();

    /**
     * {@inheritDoc}
     */
    @Override
    int last();

    /**
     * {@inheritDoc}
     */
    @Override
    ByteOrder order();

    /**
     * Set the byte order of the array.
     * 
     * @param order The ByteOrder to use
     */
    void order(ByteOrder order);

    /**
     * Remove any resources associated with this object. Using the object after
     * this method is called may result in undefined behaviour.
     */
    void free();

    /**
     * @return the sequence of <code>IoBuffer</code>s that back this array.
     * Compared to <code>getSingleIoBuffer()</code>, this method should be
     * relatively efficient for all implementations.
     */
    Iterable<IoBuffer> getIoBuffers();

    /**
     * @return a single <code>IoBuffer</code> that backs this array. Some
     * implementations may initially have data split across multiple buffers, so
     * calling this method may require a new buffer to be allocated and
     * populated.
     */
    IoBuffer getSingleIoBuffer();

    /**
     * A ByteArray is equal to another ByteArray if they start and end at the
     * same index, have the same byte order, and contain the same bytes at each
     * index.
     * 
     * @param other The ByteArray we want to compare with
     * @return <tt>true</tt> if both ByteArray are equals
     */
    @Override
    boolean equals(Object other);

    /**
     * {@inheritDoc}
     */
    @Override
    byte get(int index);

    /**
     * {@inheritDoc}
     */
    @Override
    void get(int index, IoBuffer bb);

    /**
     * {@inheritDoc}
     */
    @Override
    int getInt(int index);

    /**
     * @return a cursor starting at index 0 (which may not be the start of the array).
     */
    Cursor cursor();

    /**
     * @param index The starting point
     * @return a cursor starting at the given index.
     */
    Cursor cursor(int index);

    /**
     * Provides relocatable, relative access to the underlying array. Multiple
     * cursors may be used simultaneously, and cursors will stay consistent with
     * the underlying array, even across modifications.
     *
     * Should this be <code>Cloneable</code> to allow cheap mark/position
     * emulation?
     */
    interface Cursor extends IoRelativeReader, IoRelativeWriter {

        /**
         * @return the current index of the cursor.
         */
        int getIndex();

        /**
         * Sets the current index of the cursor. No bounds checking will occur
         * until an access occurs.
         * 
         * @param index The current index to set
         */
        void setIndex(int index);

        /**
         * {@inheritDoc}
         */
        @Override
        int getRemaining();

        /**
         * {@inheritDoc}
         */
        @Override
        boolean hasRemaining();

        /**
         * {@inheritDoc}
         */
        @Override
        byte get();

        /**
         * {@inheritDoc}
         */
        @Override
        void get(IoBuffer bb);

        /**
         * {@inheritDoc}
         */
        @Override
        int getInt();
    }
}
