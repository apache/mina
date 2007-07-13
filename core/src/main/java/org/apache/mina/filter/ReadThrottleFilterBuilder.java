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

import java.util.Iterator;
import java.util.List;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.DefaultIoFilterChainBuilder;
import org.apache.mina.common.IoFilterAdapter;
import org.apache.mina.common.IoFilterChain;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.IoFilterChain.Entry;
import org.apache.mina.filter.executor.ExecutorFilter;

/**
 * This filter will automatically disable reads on an <code>IoSession</code> once the data
 * batched for that session in the {@link ExecutorFilter} reaches a defined threshold (the
 * default is 1 megabytes). It accomplishes this by being in the filter chain before
 * <strong>and</strong> after the {@link ExecutorFilter}. It is possible to subvert the
 * behavior of this filter by adding filters immediately after the {@link ExecutorFilter}
 * after adding this filter. Thus, it is recommended to add this filter towards the end of
 * your filter chain construction, if you need to ensure that other filters need to be right
 * after the {@link ExecutorFilter}
 *
 * <p>Usage:
 *
 * <pre><code>
 * DefaultFilterChainBuilder builder = ...
 * ReadThrottleFilterBuilder filter = new ReadThrottleFilterBuilder();
 * filter.attach( builder );
 * </code></pre>
 *
 * or
 *
 * <pre><code>
 * IoFilterChain chain = ...
 * ReadThrottleFilterBuilder filter = new ReadThrottleFilterBuilder();
 * filter.attach( chain );
 * </code></pre>
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev: 406554 $, $Date: 2006-05-15 06:46:02Z $
 */
public class ReadThrottleFilterBuilder {
    public static final String COUNTER = ReadThrottleFilterBuilder.class
            .getName()
            + ".counter";

    public static final String SUSPENDED_READS = ReadThrottleFilterBuilder.class
            .getName()
            + ".suspendedReads";

    private volatile int maximumConnectionBufferSize = 1024 * 1024; // 1mb

    /**
     * Set the maximum amount of data to buffer in the ThreadPoolFilter prior to disabling
     * reads. Changing the value will only take effect when new data is received for a
     * connection, including existing connections. Default value is 1 megabytes.
     *
     * @param maximumConnectionBufferSize New buffer size. Must be > 0
     */
    public void setMaximumConnectionBufferSize(int maximumConnectionBufferSize) {
        this.maximumConnectionBufferSize = maximumConnectionBufferSize;
    }

    /**
     * Attach this filter to the specified filter chain. It will search for the ThreadPoolFilter, and attach itself
     * before and after that filter.
     *
     * @param chain {@link IoFilterChain} to attach self to.
     */
    public void attach(IoFilterChain chain) {
        String name = getThreadPoolFilterEntryName(chain.getAll());

        chain.addBefore(name, getClass().getName() + ".add", new Add());
        chain.addAfter(name, getClass().getName() + ".release", new Release());
    }

    /**
     * Attach this filter to the specified builder. It will search for the
     * {@link ExecutorFilter}, and attach itself before and after that filter.
     *
     * @param builder {@link DefaultIoFilterChainBuilder} to attach self to.
     */
    public void attach(DefaultIoFilterChainBuilder builder) {
        String name = getThreadPoolFilterEntryName(builder.getAll());

        builder.addBefore(name, getClass().getName() + ".add", new Add());
        builder
                .addAfter(name, getClass().getName() + ".release",
                        new Release());
    }

    private String getThreadPoolFilterEntryName(List<Entry> entries) {
        Iterator<Entry> i = entries.iterator();

        while (i.hasNext()) {
            IoFilterChain.Entry entry = i.next();

            if (entry.getFilter().getClass().isAssignableFrom(
                    ExecutorFilter.class)) {
                return entry.getName();
            }
        }

        throw new IllegalStateException(
                "Chain does not contain a ExecutorFilter");
    }

    private void add(IoSession session, int size) {
        synchronized (session) {
            int counter = getCounter(session) + size;

            session.setAttribute(COUNTER, new Integer(counter));

            if (counter >= maximumConnectionBufferSize
                    && session.getTrafficMask().isReadable()) {
                session.suspendRead();
                session.setAttribute(SUSPENDED_READS);
            }
        }
    }

    private void release(IoSession session, int size) {
        synchronized (session) {
            int counter = Math.max(0, getCounter(session) - size);

            session.setAttribute(COUNTER, new Integer(counter));

            if (counter < maximumConnectionBufferSize
                    && isSuspendedReads(session)) {
                session.resumeRead();
                session.removeAttribute(SUSPENDED_READS);
            }

        }
    }

    private boolean isSuspendedReads(IoSession session) {
        Boolean flag = (Boolean) session.getAttribute(SUSPENDED_READS);

        return null != flag && flag.booleanValue();
    }

    private int getCounter(IoSession session) {
        Integer i = (Integer) session.getAttribute(COUNTER);
        return null == i ? 0 : i.intValue();
    }

    private class Add extends IoFilterAdapter {
        public void messageReceived(NextFilter nextFilter, IoSession session,
                Object message) throws Exception {
            if (message instanceof ByteBuffer) {
                add(session, ((ByteBuffer) message).remaining());
            }

            nextFilter.messageReceived(session, message);
        }
    }

    private class Release extends IoFilterAdapter {
        public void messageReceived(NextFilter nextFilter, IoSession session,
                Object message) throws Exception {
            if (message instanceof ByteBuffer) {
                release(session, ((ByteBuffer) message).remaining());
            }

            nextFilter.messageReceived(session, message);
        }
    }
}
