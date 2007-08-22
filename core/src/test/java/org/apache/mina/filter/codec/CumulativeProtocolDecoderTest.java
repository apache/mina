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

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;

/**
 * Tests {@link CumulativeProtocolDecoder}.
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$ 
 */
public class CumulativeProtocolDecoderTest extends TestCase {
    private final ProtocolCodecSession session = new ProtocolCodecSession();

    private ByteBuffer buf;
    private IntegerDecoder decoder;

    public static void main(String[] args) {
        junit.textui.TestRunner.run(CumulativeProtocolDecoderTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        buf = ByteBuffer.allocate(16);
        decoder = new IntegerDecoder();
    }

    @Override
    protected void tearDown() throws Exception {
        decoder.dispose(session);
    }

    public void testCumulation() throws Exception {
        buf.put((byte) 0);
        buf.flip();

        decoder.decode(session, buf, session.getDecoderOutput());
        Assert.assertEquals(0, session.getDecoderOutputQueue().size());
        Assert.assertEquals(buf.limit(), buf.position());

        buf.clear();
        buf.put((byte) 0);
        buf.put((byte) 0);
        buf.put((byte) 1);
        buf.flip();

        decoder.decode(session, buf, session.getDecoderOutput());
        Assert.assertEquals(1, session.getDecoderOutputQueue().size());
        Assert.assertEquals(new Integer(1), session.getDecoderOutputQueue().poll());
        Assert.assertEquals(buf.limit(), buf.position());
    }

    public void testRepeatitiveDecode() throws Exception {
        for (int i = 0; i < 4; i++) {
            buf.putInt(i);
        }
        buf.flip();

        decoder.decode(session, buf, session.getDecoderOutput());
        Assert.assertEquals(4, session.getDecoderOutputQueue().size());
        Assert.assertEquals(buf.limit(), buf.position());

        List<Object> expected = new ArrayList<Object>();
        for (int i = 0; i < 4; i++) {
            expected.add(new Integer(i));
        }
        Assert.assertEquals(expected, session.getDecoderOutputQueue());
    }

    public void testWrongImplementationDetection() throws Exception {
        try {
            new WrongDecoder().decode(session, buf, session.getDecoderOutput());
            Assert.fail();
        } catch (IllegalStateException e) {
            // OK
        }
    }

    private static class IntegerDecoder extends CumulativeProtocolDecoder {

        @Override
        protected boolean doDecode(IoSession session, ByteBuffer in,
                ProtocolDecoderOutput out) throws Exception {
            Assert.assertTrue(in.hasRemaining());
            if (in.remaining() < 4) {
                return false;
            }

            out.write(new Integer(in.getInt()));
            return true;
        }

        public void dispose() throws Exception {
        }

    }

    private static class WrongDecoder extends CumulativeProtocolDecoder {

        @Override
        protected boolean doDecode(IoSession session, ByteBuffer in,
                ProtocolDecoderOutput out) throws Exception {
            return true;
        }

        public void dispose() throws Exception {
        }
    }
}
