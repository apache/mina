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

/**
 * Provides restricted, relative, read-only access to the bytes in a
 * <code>CompositeByteArray</code>. Using this interface has the advantage
 * that it can be automatically determined when a component
 * <code>ByteArray</code> can no longer be read, and thus components can be
 * automatically freed. This makes it easier to use pooling for underlying
 * <code>ByteArray</code>s.
 */
public class CompositeByteArrayRelativeReader extends
        CompositeByteArrayRelativeBase implements IoRelativeReader {

    /**
     * Whether or not to free component <code>CompositeByteArray</code>s when
     * the cursor moves past them.
     */
    private final boolean autoFree;

    public CompositeByteArrayRelativeReader(CompositeByteArray cba,
            boolean autoFree) {
        super(cba);
        this.autoFree = autoFree;
    }

    @Override
    protected void cursorPassedFirstComponent() {
        if (autoFree) {
            cba.removeFirst().free();
        }
    }
    
    /**
     * @inheritDoc
     */
    public void skip(int length) {
        cursor.skip(length);
    }
    
    /**
     * @inheritDoc
     */
    public ByteArray slice(int length) {
        return cursor.slice(length);
    }

    public byte get() {
        return cursor.get();
    }

    public void get(ByteBuffer bb) {
        cursor.get(bb);
    }
    /**
     * @inheritDoc
     */
    public short getShort() {
        return cursor.getShort();
    }

    /**
     * @inheritDoc
     */
    public int getInt() {
        return cursor.getInt();
    }

    /**
     * @inheritDoc
     */
    public long getLong() {
        return cursor.getLong();
    }

    /**
     * @inheritDoc
     */
    public float getFloat() {
        return cursor.getFloat();
    }

    /**
     * @inheritDoc
     */
    public double getDouble() {
        return cursor.getDouble();
    }

    /**
     * @inheritDoc
     */
    public char getChar() {
        return cursor.getChar();
    }

}
