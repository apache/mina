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

package org.apache.mina.filter.ssl;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.net.ssl.SSLException;

import org.apache.mina.core.filterchain.IoFilter.NextFilter;
import org.apache.mina.core.session.DummySession;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.DefaultWriteRequest;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.filter.FilterEvent;
import org.junit.Before;
import org.junit.Test;

/**
 * A test for DIRMINA-1019
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
abstract class AbstractNextFilter implements NextFilter {
    public abstract void messageReceived(IoSession session, Object message);
    
    public abstract void filterWrite(IoSession session, WriteRequest writeRequest);
    
    // Following are unimplemented as they aren't used in test
    public void sessionCreated(IoSession session) { }

    public void sessionOpened(IoSession session) { }

    public void sessionClosed(IoSession session) { }

    public void sessionIdle(IoSession session, IdleStatus status) { }

    public void exceptionCaught(IoSession session, Throwable cause) { }

    public void inputClosed(IoSession session) { }

    public void messageSent(IoSession session, WriteRequest writeRequest) { }

    public void filterClose(IoSession session) { }

    public void event(IoSession session, FilterEvent event) { }

    public String toString() {
        return null;
    }
};

/**
 * A test for DIRMINA-1019
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class SslFilterTest {
    SslHandler test_class;
    
    @Before
    public void init() throws SSLException {
        test_class = new SslHandler(null, new DummySession());
    }
    
    @Test
    public void testFlushRaceCondition() {
        final ExecutorService executor = Executors.newFixedThreadPool(1);
        final List<Object> message_received_messages = new ArrayList<Object>();
        final List<WriteRequest> filter_write_requests = new ArrayList<WriteRequest>();
        
        final AbstractNextFilter write_filter = new AbstractNextFilter()
        {
            @Override
            public void messageReceived(IoSession session, Object message) { }

            @Override
            public void filterWrite(IoSession session, WriteRequest writeRequest) {
                filter_write_requests.add(writeRequest);
            }
        };
        
        AbstractNextFilter receive_filter = new AbstractNextFilter()
        {
            @Override
            public void messageReceived(IoSession session, Object message) {
                message_received_messages.add(message);
                
                // This is where the race condition occurs. If a thread calls SslHandler.scheduleFilterWrite(),
                // followed by SslHandler.flushScheduledEvents(), the queued event will not be processed as
                // the current thread owns the SslHandler.sslLock and has already "dequeued" all the queued
                // filterWriteEventQueue.
                Future<?> write_scheduler = executor.submit(new Runnable() {
                    public void run() {
                	synchronized(test_class) {
                	    test_class.scheduleFilterWrite(write_filter, new DefaultWriteRequest(new byte[] {}));
                	    test_class.flushFilterWrite();
                	}
                    }
                });
                
                try {
                    write_scheduler.get();
                } catch (Exception e) { }
            }

            @Override
            public void filterWrite(IoSession session, WriteRequest writeRequest) { }
        };
        
        synchronized(test_class) {
            test_class.scheduleMessageReceived(receive_filter, new byte[] {});
        }
        
        test_class.flushMessageReceived();
        
        assertEquals(1, message_received_messages.size());
        assertEquals(1, filter_write_requests.size());
    }
}
