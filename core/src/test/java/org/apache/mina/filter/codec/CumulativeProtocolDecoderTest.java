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
package org.apache.mina.filter.codec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.DefaultTransportMetadata;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


/**
 * Tests {@link CumulativeProtocolDecoder}.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class CumulativeProtocolDecoderTest {
    private final ProtocolCodecSession session = new ProtocolCodecSession();

    private IoBuffer buf;
    private IntegerDecoder decoder;

    @Before
    public void setUp() throws Exception {
        buf = IoBuffer.allocate(16);
        decoder = new IntegerDecoder();
        session.setTransportMetadata(
                new DefaultTransportMetadata(
                        "mina", "dummy", false, true, SocketAddress.class,
                        IoSessionConfig.class, IoBuffer.class));
    }

    @After
    public void tearDown() throws Exception {
        decoder.dispose(session);
    }

    @Test
    public void testCumulation() throws Exception {
        buf.put((byte) 0);
        buf.flip();

        decoder.decode(session, buf, session.getDecoderOutput());
        assertEquals(0, session.getDecoderOutputQueue().size());
        assertEquals(buf.limit(), buf.position());

        buf.clear();
        buf.put((byte) 0);
        buf.put((byte) 0);
        buf.put((byte) 1);
        buf.flip();

        decoder.decode(session, buf, session.getDecoderOutput());
        assertEquals(1, session.getDecoderOutputQueue().size());
        assertEquals(new Integer(1), session.getDecoderOutputQueue().poll());
        assertEquals(buf.limit(), buf.position());
    }

    @Test
    public void testRepeatitiveDecode() throws Exception {
        for (int i = 0; i < 4; i++) {
            buf.putInt(i);
        }
        buf.flip();

        decoder.decode(session, buf, session.getDecoderOutput());
        assertEquals(4, session.getDecoderOutputQueue().size());
        assertEquals(buf.limit(), buf.position());

        List<Object> expected = new ArrayList<Object>();
        
        for (int i = 0; i < 4; i++) {
            assertTrue( session.getDecoderOutputQueue().contains(i));
        }
    }

    @Test
    public void testWrongImplementationDetection() throws Exception {
        try {
            new WrongDecoder().decode(session, buf, session.getDecoderOutput());
            fail();
        } catch (IllegalStateException e) {
            // OK
        }
    }
    
    @Test
    public void testBufferDerivation() throws Exception {
        decoder = new DuplicatingIntegerDecoder();
        
        buf.putInt(1);
        
        // Put some extra byte to make the decoder create an internal buffer.
        buf.put((byte) 0);
        buf.flip();

        decoder.decode(session, buf, session.getDecoderOutput());
        assertEquals(1, session.getDecoderOutputQueue().size());
        assertEquals(1, session.getDecoderOutputQueue().poll());
        assertEquals(buf.limit(), buf.position());

        // Keep appending to the internal buffer.
        // DuplicatingIntegerDecoder will keep duplicating the internal
        // buffer to disable auto-expansion, and CumulativeProtocolDecoder
        // should detect that user derived its internal buffer.
        // Consequently, CumulativeProtocolDecoder will perform 
        // reallocation to avoid putting incoming data into
        // the internal buffer with auto-expansion disabled.
        for (int i = 2; i < 10; i ++) {
            buf.clear();
            buf.putInt(i);
            // Put some extra byte to make the decoder keep the internal buffer.
            buf.put((byte) 0);
            buf.flip();
            buf.position(1);
    
            decoder.decode(session, buf, session.getDecoderOutput());
            assertEquals(1, session.getDecoderOutputQueue().size());
            assertEquals(i, session.getDecoderOutputQueue().poll());
            assertEquals(buf.limit(), buf.position());
        }
    }

    private static class IntegerDecoder extends CumulativeProtocolDecoder {
        /**
         * Default constructor
         */
        public IntegerDecoder() {
            super();
        }
        
        @Override
        protected boolean doDecode(IoSession session, IoBuffer in,
                ProtocolDecoderOutput out) throws Exception {
            assertTrue(in.hasRemaining());
            
            if (in.remaining() < 4) {
                return false;
            }

            out.write(new Integer(in.getInt()));
            return true;
        }

        public void dispose() throws Exception {
            // Do nothing
        }
    }
    
    private static class WrongDecoder extends CumulativeProtocolDecoder {
        /**
         * Default constructor
         */
        public WrongDecoder() {
            super();
        }
        
        @Override
        protected boolean doDecode(IoSession session, IoBuffer in,
                ProtocolDecoderOutput out) throws Exception {
            return true;
        }

        public void dispose() throws Exception {
            // Do nothing
        }
    }

    private static class DuplicatingIntegerDecoder extends IntegerDecoder {
        /**
         * Default constructor
         */
        public DuplicatingIntegerDecoder() {
            super();
        }
        
        @Override
        protected boolean doDecode(IoSession session, IoBuffer in,
                ProtocolDecoderOutput out) throws Exception {
            in.duplicate(); // Will disable auto-expansion.
            assertFalse(in.isAutoExpand());
            return super.doDecode(session, in, out);
        }

        public void dispose() throws Exception {
            // Do nothing
        }
    }
}
