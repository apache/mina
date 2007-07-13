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
package org.apache.mina.filter;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoFilterChain;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.IoFilter.NextFilter;
import org.apache.mina.filter.support.Zlib;
import org.apache.mina.common.IoFilter.WriteRequest;
import org.easymock.MockControl;
import org.easymock.AbstractMatcher;

import junit.framework.TestCase;

/**
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class CompressionFilterTest extends TestCase {
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

    protected void setUp() {
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

    public void testCompression() throws Exception {
        // prepare the input data
        ByteBuffer buf = ByteBuffer.wrap(strCompress.getBytes("UTF8"));
        ByteBuffer actualOutput = actualDeflater.deflate(buf);
        WriteRequest writeRequest = new WriteRequest(buf);

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

        nextFilter.filterWrite(session, new WriteRequest(actualOutput));

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

    public void testDecompression() throws Exception {
        // prepare the input data
        ByteBuffer buf = ByteBuffer.wrap(strCompress.getBytes("UTF8"));
        ByteBuffer byteInput = actualDeflater.deflate(buf);
        ByteBuffer actualOutput = actualInflater.inflate(byteInput);

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
     * 
     * @author The Apache Directory MINA subproject (mina-dev@directory.apache.org)
     */
    class DataMatcher extends AbstractMatcher {
        protected boolean argumentMatches(Object arg0, Object arg1) {
            // we need to only verify the ByteBuffer output
            if (arg0 instanceof WriteRequest) {
                WriteRequest expected = (WriteRequest) arg0;
                WriteRequest actual = (WriteRequest) arg1;
                ByteBuffer bExpected = (ByteBuffer) expected.getMessage();
                ByteBuffer bActual = (ByteBuffer) actual.getMessage();
                return bExpected.equals(bActual);
            }
            return true;
        }
    }
}
