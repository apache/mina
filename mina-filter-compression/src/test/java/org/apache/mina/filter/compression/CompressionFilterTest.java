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
package org.apache.mina.filter.compression;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilter.NextFilter;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.session.AttributeKey;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.DefaultWriteRequest;
import org.apache.mina.core.write.WriteRequest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class CompressionFilterTest {
    // the sample data to be used for testing
    private static final String STR_COMPRESS = repeat("The quick brown fox jumps over the lazy dog.  ", 25);

    private CompressionFilter filter;

    private IoSession session;

    private IoFilterChain filterChain;

    private NextFilter nextFilter;

    private static String repeat(String value, int count) {
        StringBuilder builder = new StringBuilder(value.length() * count);
        for (int i = 0; i < count; i++) {
            builder.append(value);
        }
        return builder.toString();
    }

    @Before
    public void setUp() {
        filter = new CompressionFilter(CompressionFilter.COMPRESSION_MAX);
        
        // a mock session whose attributes are stored in a real map, so that the deflater and inflater
        // created by onPreAdd() are actually retrieved by filterWrite() and messageReceived().
        session = mock(IoSession.class);
        final Map<Object, Object> attributes = new HashMap<>();
        when(session.setAttribute(any(), any()))
                .thenAnswer(invocation -> attributes.put(invocation.getArgument(0), invocation.getArgument(1)));
        when(session.getAttribute(any())).thenAnswer(invocation -> attributes.get(invocation.getArgument(0)));
        when(session.containsAttribute(any())).thenAnswer(invocation -> attributes.containsKey(invocation.getArgument(0)));
        when(session.removeAttribute(any())).thenAnswer(invocation -> attributes.remove(invocation.getArgument(0)));

        filterChain = mock(IoFilterChain.class);
        when(filterChain.contains(CompressionFilter.class)).thenReturn(false);
        when(filterChain.getSession()).thenReturn(session);

        nextFilter = mock(NextFilter.class);
    }

    public void testDeflaterAndInflaterNotSwapped() throws Exception {
        filter.onPreAdd(filterChain, "CompressionFilter", nextFilter);
        IoBuffer input = IoBuffer.wrap(STR_COMPRESS.getBytes(StandardCharsets.UTF_8));
        Zlib deflater = (Zlib) session.getAttribute(new AttributeKey(CompressionFilter.class, "deflater"));
        assertNotNull(deflater);
        assertThrows(IllegalStateException.class, () -> deflater.inflate(input));

        Zlib inflater = (Zlib) session.getAttribute(new AttributeKey(CompressionFilter.class, "inflater"));
        assertNotNull(inflater);
        assertThrows(IllegalStateException.class, () -> inflater.deflate(input));
    }
}
