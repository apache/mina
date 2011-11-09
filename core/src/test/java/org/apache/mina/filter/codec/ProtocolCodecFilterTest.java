/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.mina.filter.codec;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import org.apache.mina.api.IoSession;
import org.junit.Test;

/**
 * Unit test for {@link ProtocolCodecFilter}
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class ProtocolCodecFilterTest {

    @Test
    public void constructor_args() {
        try {
            new ProtocolCodecFilter(null);
            fail();
        } catch (IllegalArgumentException e) {
            // happy
        } catch (Exception e2) {
            fail();
        }

        try {
            new ProtocolCodecFilter(null, mock(ProtocolDecoder.class));
            fail();
        } catch (IllegalArgumentException e) {
            // happy
        } catch (Exception e2) {
            fail();
        }

        try {
            new ProtocolCodecFilter(mock(ProtocolEncoder.class), null);
            fail();
        } catch (IllegalArgumentException e) {
            // happy
        } catch (Exception e2) {
            fail();
        }

        try {
            new ProtocolCodecFilter(null, ProtocolDecoder.class);
            fail();
        } catch (IllegalArgumentException e) {
            // happy
        } catch (Exception e2) {
            fail();
        }

        try {
            new ProtocolCodecFilter(ProtocolEncoder.class, null);
            fail();
        } catch (IllegalArgumentException e) {
            // happy
        } catch (Exception e2) {
            fail();
        }
    }

    @Test
    public void codec_factory() {
        ProtocolEncoder encoder = mock(ProtocolEncoder.class);
        ProtocolDecoder decoder = mock(ProtocolDecoder.class);

        ProtocolCodecFactory factory = mock(ProtocolCodecFactory.class);
        when(factory.getEncoder(any(IoSession.class))).thenReturn(encoder);
        when(factory.getDecoder(any(IoSession.class))).thenReturn(decoder);

        ProtocolCodecFilter codec = new ProtocolCodecFilter(factory);
        assertEquals(encoder, codec.getEncoder(null));
        assertEquals(decoder, codec.getDecoder(null));

        codec = new ProtocolCodecFilter(encoder, decoder);
        assertEquals(encoder, codec.getEncoder(null));
        assertEquals(decoder, codec.getDecoder(null));
    }

}