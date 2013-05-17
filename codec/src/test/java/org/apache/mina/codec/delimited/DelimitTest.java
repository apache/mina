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
package org.apache.mina.codec.delimited;

import static org.junit.Assert.assertEquals;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

/**
 * An abstract {@link SizePrefixedEncoder} and {@link SizePrefixedDecoder} test.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public abstract class DelimitTest<T> {

    public abstract List<T> getObjects();

    protected abstract ByteBuffer delimitWithOriginal() throws Exception;

    public abstract SizePrefixedEncoder<T> getSerializer() throws Exception;

    final protected ByteBuffer delimitWithMina() throws Exception {
        SizePrefixedEncoder<T> pe = getSerializer();

        List<ByteBuffer> buffers = new LinkedList<ByteBuffer>();
        for (T p : getObjects()) {
            buffers.add(pe.encode(p, null));
        }

        int size = 0;
        for (ByteBuffer b : buffers) {
            size += b.remaining();
        }

        ByteBuffer buffer = ByteBuffer.allocate(size);
        for (ByteBuffer b : buffers) {
            buffer.put(b);
        }
        buffer.flip();
        return buffer;
    }

    @Test
    public void testDelimit() throws Exception {
        assertEquals(delimitWithOriginal(), delimitWithMina());
    }

}
