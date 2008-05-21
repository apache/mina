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

public interface ByteBufferQueue extends IoQueue<ByteBuffer> {
    ByteOrder order();

    int length();

    boolean offerByte(byte value);
    boolean offerShort(short value);
    boolean offerInt(int value);
    boolean offerLong(long value);
    boolean offerFloat(float value);
    boolean offerDouble(double value);

    ByteBufferQueue pollSlice(int length);
    ByteBufferQueue removeSlice(int length);

    byte   removeByte();
    short  removeShort();
    int    removeInt();
    long   removeLong();
    float  removeFloat();
    double removeDouble();

    void   discard(int length);

    byte   elementAsByte  ();
    short  elementAsShort ();
    int    elementAsInt   ();
    long   elementAsLong  ();
    float  elementAsFloat ();
    double elementAsDouble();

    byte   elementAsByte  (int byteIndex);
    short  elementAsShort (int byteIndex);
    int    elementAsInt   (int byteIndex);
    long   elementAsLong  (int byteIndex);
    float  elementAsFloat (int byteIndex);
    double elementAsDouble(int byteIndex);

    ByteBufferQueue peekSlice(int byteIndex, int length);
    ByteBufferQueue elementAsSlice(int byteIndex, int length);

    ByteBufferQueue duplicate();
}
