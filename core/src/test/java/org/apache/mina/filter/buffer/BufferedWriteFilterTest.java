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
package org.apache.mina.filter.buffer;

import static org.junit.Assert.assertEquals;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.session.DummySession;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.filter.logging.LoggingFilter;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests {@link BufferedWriteFilter}.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * @since MINA 2.0.0-M2
 */
public class BufferedWriteFilterTest {
    static final Logger LOGGER = LoggerFactory
            .getLogger(BufferedWriteFilterTest.class);

    @Test
    public void testNonExpandableBuffer() throws Exception {
        IoBuffer dest = IoBuffer.allocate(1);
        assertEquals(false, dest.isAutoExpand());
    }

    @Test
    public void testBasicBuffering() {
        DummySession sess = new DummySession();
        sess.getFilterChain().addFirst("peer", new IoFilterAdapter() {

            private int counter;

            @Override
            public void filterClose(NextFilter nextFilter, IoSession session)
                    throws Exception {
                LOGGER.debug("Filter closed !");
                assertEquals(3, counter);
            }

            @Override
            public void filterWrite(NextFilter nextFilter, IoSession session,
                    WriteRequest writeRequest) throws Exception {
                LOGGER.debug("New buffered message written !");
                counter++;
                try {
                    IoBuffer buf = (IoBuffer) writeRequest.getMessage();
                    if (counter == 3) {
                        assertEquals(1, buf.limit());
                        assertEquals(0, buf.get());
                    } else {
                        assertEquals(10, buf.limit());
                    }
                } catch (Exception ex) {
                    throw new AssertionError("Wrong message type");
                }
            }

        });
        sess.getFilterChain().addFirst("logger", new LoggingFilter());
        BufferedWriteFilter bFilter = new BufferedWriteFilter(10);
        sess.getFilterChain().addLast("buffer", bFilter);

        IoBuffer data = IoBuffer.allocate(1);
        for (byte i = 0; i < 20; i++) {
            data.put((byte) (0x30 + i));
            data.flip();
            sess.write(data);
            data.clear();
        }

        // Add one more byte to overflow the final buffer
        data.put((byte) 0);
        data.flip();
        sess.write(data);
        
        // Flush the final byte
        bFilter.flush(sess);
        
        sess.close(true);
    }
}