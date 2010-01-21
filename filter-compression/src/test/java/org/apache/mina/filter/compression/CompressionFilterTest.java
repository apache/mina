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

import static org.junit.Assert.assertTrue;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.filterchain.IoFilter.NextFilter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.DefaultWriteRequest;
import org.apache.mina.core.write.WriteRequest;
import org.easymock.AbstractMatcher;
import org.easymock.MockControl;
import org.junit.Before;
import org.junit.Test;

/**
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class CompressionFilterTest {
    private MockControl mockSession;

    private MockControl mockNextFilter;

    private MockControl mockIoFilterChain;

    private IoSession session;

    private NextFilter nextFilter;

    private IoFilterChain ioFilterChain;

    private CompressionFilter filter;

    private Zlib deflater;

    private Zlib inflater;

    private Zlib actualDeflater;

    private Zlib actualInflater;

    // the sample data to be used for testing
    String strCompress = "The quick brown fox jumps over the lazy dog.  "
            + "The quick brown fox jumps over the lazy dog.  "
            + "The quick brown fox jumps over the lazy dog.  "
            + "The quick brown fox jumps over the lazy dog.  "
            + "The quick brown fox jumps over the lazy dog.  "
            + "The quick brown fox jumps over the lazy dog.  "
            + "The quick brown fox jumps over the lazy dog.  "
            + "The quick brown fox jumps over the lazy dog.  "
            + "The quick brown fox jumps over the lazy dog.  "
            + "The quick brown fox jumps over the lazy dog.  "
            + "The quick brown fox jumps over the lazy dog.  "
            + "The quick brown fox jumps over the lazy dog.  "
            + "The quick brown fox jumps over the lazy dog.  "
            + "The quick brown fox jumps over the lazy dog.  "
            + "The quick brown fox jumps over the lazy dog.  "
            + "The quick brown fox jumps over the lazy dog.  "
            + "The quick brown fox jumps over the lazy dog.  "
            + "The quick brown fox jumps over the lazy dog.  "
            + "The quick brown fox jumps over the lazy dog.  "
            + "The quick brown fox jumps over the lazy dog.  "
            + "The quick brown fox jumps over the lazy dog.  "
            + "The quick brown fox jumps over the lazy dog.  "
            + "The quick brown fox jumps over the lazy dog.  "
            + "The quick brown fox jumps over the lazy dog.  "
            + "The quick brown fox jumps over the lazy dog.  ";

    @Before
    public void setUp() {
        // create the necessary mock controls.
        mockSession = MockControl.createControl(IoSession.class);
        mockNextFilter = MockControl.createControl(NextFilter.class);
        mockIoFilterChain = MockControl.createControl(IoFilterChain.class);

        // set the default matcher
        mockNextFilter.setDefaultMatcher(new DataMatcher());

        session = (IoSession) mockSession.getMock();
        nextFilter = (NextFilter) mockNextFilter.getMock();
        ioFilterChain = (IoFilterChain) mockIoFilterChain.getMock();

        // create an instance of the filter
        filter = new CompressionFilter(CompressionFilter.COMPRESSION_MAX);

        // deflater and inflater that will be used by the filter
        deflater = new Zlib(Zlib.COMPRESSION_MAX, Zlib.MODE_DEFLATER);
        inflater = new Zlib(Zlib.COMPRESSION_MAX, Zlib.MODE_INFLATER);

        // create instances of the deflater and inflater to help test the output
        actualDeflater = new Zlib(Zlib.COMPRESSION_MAX, Zlib.MODE_DEFLATER);
        actualInflater = new Zlib(Zlib.COMPRESSION_MAX, Zlib.MODE_INFLATER);
    }

    @Test
    public void testCompression() throws Exception {
        // prepare the input data
        IoBuffer buf = IoBuffer.wrap(strCompress.getBytes("UTF8"));
        IoBuffer actualOutput = actualDeflater.deflate(buf);
        WriteRequest writeRequest = new DefaultWriteRequest(buf);

        // record all the mock calls
        ioFilterChain.contains(CompressionFilter.class);
        mockIoFilterChain.setReturnValue(false);

        ioFilterChain.getSession();
        mockIoFilterChain.setReturnValue(session);

        session.setAttribute(CompressionFilter.class.getName() + ".Deflater",
                deflater);
        mockSession.setDefaultMatcher(new DataMatcher());
        mockSession.setReturnValue(null, MockControl.ONE);

        session.setAttribute(CompressionFilter.class.getName() + ".Inflater",
                inflater);
        mockSession.setReturnValue(null, MockControl.ONE);

        session.containsAttribute(CompressionFilter.DISABLE_COMPRESSION_ONCE);
        mockSession.setReturnValue(false);

        session.getAttribute(CompressionFilter.class.getName() + ".Deflater");
        mockSession.setReturnValue(deflater);

        nextFilter.filterWrite(session, new DefaultWriteRequest(actualOutput));

        // switch to playback mode
        mockSession.replay();
        mockIoFilterChain.replay();
        mockNextFilter.replay();

        // make the actual calls on the filter
        filter.onPreAdd(ioFilterChain, "CompressionFilter", nextFilter);
        filter.filterWrite(nextFilter, session, writeRequest);

        // verify that all the calls happened as recorded
        mockNextFilter.verify();

        assertTrue(true);
    }

    @Test
    public void testDecompression() throws Exception {
        // prepare the input data
        IoBuffer buf = IoBuffer.wrap(strCompress.getBytes("UTF8"));
        IoBuffer byteInput = actualDeflater.deflate(buf);
        IoBuffer actualOutput = actualInflater.inflate(byteInput);

        // record all the mock calls
        ioFilterChain.contains(CompressionFilter.class);
        mockIoFilterChain.setReturnValue(false);

        ioFilterChain.getSession();
        mockIoFilterChain.setReturnValue(session);

        session.setAttribute(CompressionFilter.class.getName() + ".Deflater",
                deflater);
        mockSession.setDefaultMatcher(new DataMatcher());
        mockSession.setReturnValue(null, MockControl.ONE);

        session.setAttribute(CompressionFilter.class.getName() + ".Inflater",
                inflater);
        mockSession.setReturnValue(null, MockControl.ONE);

        session.getAttribute(CompressionFilter.class.getName() + ".Inflater");
        mockSession.setReturnValue(inflater);

        nextFilter.messageReceived(session, actualOutput);

        // switch to playback mode
        mockSession.replay();
        mockIoFilterChain.replay();
        mockNextFilter.replay();

        // make the actual calls on the filter
        filter.onPreAdd(ioFilterChain, "CompressionFilter", nextFilter);
        filter.messageReceived(nextFilter, session, byteInput);

        // verify that all the calls happened as recorded
        mockNextFilter.verify();

        assertTrue(true);
    }

    /**
     * A matcher used to check if the actual and expected outputs matched
     */
    class DataMatcher extends AbstractMatcher {
        @Override
        protected boolean argumentMatches(Object arg0, Object arg1) {
            // we need to only verify the ByteBuffer output
            if (arg0 instanceof WriteRequest) {
                WriteRequest expected = (WriteRequest) arg0;
                WriteRequest actual = (WriteRequest) arg1;
                IoBuffer bExpected = (IoBuffer) expected.getMessage();
                IoBuffer bActual = (IoBuffer) actual.getMessage();
                return bExpected.equals(bActual);
            }
            return true;
        }
    }
}
