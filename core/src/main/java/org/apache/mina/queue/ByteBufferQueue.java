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
package org.apache.mina.queue;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.NoSuchElementException;

/**
 * An {@link IoQueue} of {@link ByteBuffer}s.  {@link ByteBufferQueue} is
 * different from an ordinary {@link IoQueue} in that it provides additional
 * access methods which allows reading and writing the content of the
 * elements (i.e. {@link ByteBuffer}) in byte-wise level.
 *
 * @author The Apache MINA project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public interface ByteBufferQueue extends IoQueue<ByteBuffer> {

    /**
     * Returns the {@link ByteOrder} of this queue.  This property is used
     * when retrieving the queue contents as <tt>short</tt>, <tt>int</tt>,
     * <tt>long</tt>, <tt>float</tt> and <tt>double</tt>.
     */
    ByteOrder order();

    /**
     * Returns the total number of bytes that can be accessed.
     */
    int length();

    /**
     * Inserts the specified <tt>byte</tt> value to the tail of this queue.
     * If there's not enough room, a new {@link ByteBuffer} will be inserted
     * first.  Otherwise, the value will be put into the last (i.e. tail)
     * {@link ByteBuffer}.
     *
     * @return <tt>true</tt> if and only if the insertion succeeded
     */
    boolean offerByte(byte value);

    /**
     * Inserts the specified <tt>short</tt> value to the tail of this queue.
     * If there's not enough room, a new {@link ByteBuffer} will be inserted
     * first.  Otherwise, the value will be put into the last (i.e. tail)
     * {@link ByteBuffer}.
     *
     * @return <tt>true</tt> if and only if the insertion succeeded
     */
    boolean offerShort(short value);

    /**
     * Inserts the specified <tt>int</tt> value to the tail of this queue.
     * If there's not enough room, a new {@link ByteBuffer} will be inserted
     * first.  Otherwise, the value will be put into the last (i.e. tail)
     * {@link ByteBuffer}.
     *
     * @return <tt>true</tt> if and only if the insertion succeeded
     */
    boolean offerInt(int value);

    /**
     * Inserts the specified <tt>long</tt> value to the tail of this queue.
     * If there's not enough room, a new {@link ByteBuffer} will be inserted
     * first.  Otherwise, the value will be put into the last (i.e. tail)
     * {@link ByteBuffer}.
     *
     * @return <tt>true</tt> if and only if the insertion succeeded
     */
    boolean offerLong(long value);

    /**
     * Inserts the specified <tt>float</tt> value to the tail of this queue.
     * If there's not enough room, a new {@link ByteBuffer} will be inserted
     * first.  Otherwise, the value will be put into the last (i.e. tail)
     * {@link ByteBuffer}.
     *
     * @return <tt>true</tt> if and only if the insertion succeeded
     */
    boolean offerFloat(float value);

    /**
     * Inserts the specified <tt>double</tt> value to the tail of this queue.
     * If there's not enough room, a new {@link ByteBuffer} will be inserted
     * first.  Otherwise, the value will be put into the last (i.e. tail)
     * {@link ByteBuffer}.
     *
     * @return <tt>true</tt> if and only if the insertion succeeded
     */
    boolean offerDouble(double value);

    /**
     * Retrieves and removes the specified number of bytes from this queue,
     * or returns <tt>null</tt> if the length of this queue is less than the
     * specified length.
     *
     * @param length the number of bytes to retrieve and remove
     */
    ByteBufferQueue pollSlice(int length);

    /**
     * Retrieves and removes the specified number of bytes from this queue.
     * This method differs from {@link #removeSlice(int)} only in that it
     * throws a {@link NoSuchElementException} when the length of this queue
     * is less than the specified length.
     *
     * @param length the number of bytes to retrieve and remove
     * @throws NoSuchElementException if the length of this queue is less
     *                                than the specified length
     */
    ByteBufferQueue removeSlice(int length);

    /**
     * Retrieves, but does not remove, the specified number of bytes from this
     * queue, or returns <tt>null</tt> if the length of this queue is less
     * than the specified length.
     *
     * @param length the number of bytes to retrieve
     */
    ByteBufferQueue peekSlice(int length);

    /**
     * Retrieves and removes one byte from the head of this queue.
     *
     * @throws NoSuchElementException if this queue is empty
     */
    byte   removeByte();

    /**
     * Retrieves and removes a short integer (2 bytes) from the head of this
     * queue.
     *
     * @throws NoSuchElementException if the length of this queue is less than
     *                                <tt>2</tt>
     */
    short  removeShort();

    /**
     * Retrieves and removes an integer (4 bytes) from the head of this queue.
     *
     * @throws NoSuchElementException if the length of this queue is less than
     *                                <tt>4</tt>
     */
    int    removeInt();

    /**
     * Retrieves and removes a long integer (8 bytes) from the head of this
     * queue.
     *
     * @throws NoSuchElementException if the length of this queue is less than
     *                                <tt>8</tt>
     */
    long   removeLong();

    /**
     * Retrieves and removes a float (4 bytes) from the head of this queue.
     *
     * @throws NoSuchElementException if the length of this queue is less than
     *                                <tt>4</tt>
     */
    float  removeFloat();

    /**
     * Retrieves and removes a double (8 bytes) from the head of this queue.
     *
     * @throws NoSuchElementException if the length of this queue is less than
     *                                <tt>8</tt>
     */
    double removeDouble();

    /**
     * Removes the specified number of bytes from this queue.
     *
     * @param length the number of bytes to remove
     * @throws NoSuchElementException if the length of this queue is less than
     *                                the specified length
     */
    void   discard(int length);

    /**
     * Retrieves, but does not remove, one byte from the head of this queue.
     *
     * @throws NoSuchElementException if this queue is empty
     */
    byte   elementAsByte  ();

    /**
     * Retrieves, but does not remove, a short integer (2 bytes) from the head
     * of this queue.
     *
     * @throws NoSuchElementException if the length of this queue is less than
     *                                <tt>2</tt>
     */
    short  elementAsShort ();

    /**
     * Retrieves, but does not remove, an integer (4 bytes) from the head
     * of this queue.
     *
     * @throws NoSuchElementException if the length of this queue is less than
     *                                <tt>4</tt>
     */
    int    elementAsInt   ();

    /**
     * Retrieves, but does not remove, a long integer (8 bytes) from the head
     * of this queue.
     *
     * @throws NoSuchElementException if the length of this queue is less than
     *                                <tt>8</tt>
     */
    long   elementAsLong  ();

    /**
     * Retrieves, but does not remove, a float (4 bytes) from the head
     * of this queue.
     *
     * @throws NoSuchElementException if the length of this queue is less than
     *                                <tt>4</tt>
     */
    float  elementAsFloat ();

    /**
     * Retrieves, but does not remove, a double (8 bytes) from the head
     * of this queue.
     *
     * @throws NoSuchElementException if the length of this queue is less than
     *                                <tt>8</tt>
     */
    double elementAsDouble();

    /**
     * Retrieves, but does not remove, one byte from the specified position of
     * this queue.
     *
     * @param byteIndex the offset (byte-unit position) of this queue
     *
     * @throws NoSuchElementException if the length of this queue is less than
     *                                <tt>byteIndex + 1</tt>
     */
    byte   elementAsByte  (int byteIndex);

    /**
     * Retrieves, but does not remove, a short integer (2 bytes) from the
     * specified position of this queue.
     *
     * @param byteIndex the offset (byte-unit position) of this queue
     *
     * @throws NoSuchElementException if the length of this queue is less than
     *                                <tt>byteIndex + 2</tt>
     */
    short  elementAsShort (int byteIndex);

    /**
     * Retrieves, but does not remove, an integer (4 bytes) from the
     * specified position of this queue.
     *
     * @param byteIndex the offset (byte-unit position) of this queue
     *
     * @throws NoSuchElementException if the length of this queue is less than
     *                                <tt>byteIndex + 4</tt>
     */
    int    elementAsInt   (int byteIndex);

    /**
     * Retrieves, but does not remove, a long integer (8 bytes) from the
     * specified position of this queue.
     *
     * @param byteIndex the offset (byte-unit position) of this queue
     *
     * @throws NoSuchElementException if the length of this queue is less than
     *                                <tt>byteIndex + 8</tt>
     */
    long   elementAsLong  (int byteIndex);

    /**
     * Retrieves, but does not remove, a float (4 bytes) from the
     * specified position of this queue.
     *
     * @param byteIndex the offset (byte-unit position) of this queue
     *
     * @throws NoSuchElementException if the length of this queue is less than
     *                                <tt>byteIndex + 4</tt>
     */
    float  elementAsFloat (int byteIndex);

    /**
     * Retrieves, but does not remove, a double (8 bytes) from the
     * specified position of this queue.
     *
     * @param byteIndex the offset (byte-unit position) of this queue
     *
     * @throws NoSuchElementException if the length of this queue is less than
     *                                <tt>byteIndex + 8</tt>
     */
    double elementAsDouble(int byteIndex);

    /**
     * Retrieves, but does not remove, the specified number of bytes from the
     * specified position of this queue, or returns <tt>null</tt> if the length
     * of this queue is less than <tt>byteIndex + length</tt>.
     *
     * @param byteIndex the offset (byte-unit position) of this queue
     * @param length the number of bytes to retrieve
     */
    ByteBufferQueue peekSlice(int byteIndex, int length);

    /**
     * Retrieves, but does not remove, the specified number of bytes from the
     * specified position of this queue.  This method differs from
     * {@link #peekSlice(int, int)} only in that it throws a
     * {@link NoSuchElementException} when the length of this queue
     * is less than <tt>byteIndex + length</tt>.
     *
     * @param byteIndex the offset (byte-unit position) of this queue
     * @param length the number of bytes to retrieve
     *
     * @throws NoSuchElementException if the length of this queue is less
     *                                than <tt>byteIndex + length</tt>
     */
    ByteBufferQueue elementAsSlice(int byteIndex, int length);

    /**
     * Returns the shallow copy of this queue.  This operation copies only the
     * list of the {@link ByteBuffer}s, not {@link ByteBuffer}s themselves.
     */
    ByteBufferQueue duplicate();

    /**
     * Merges all elements of this queue into one {@link ByteBuffer}.  This
     * operation doesn't change the state of this queue.
     *
     * @return the merged buffer
     */
    ByteBuffer merge();
}
