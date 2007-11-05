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
package org.apache.mina.filter.traffic;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.mina.common.AttributeKey;
import org.apache.mina.common.IoBuffer;
import org.apache.mina.common.IoFilter;
import org.apache.mina.common.IoFilterAdapter;
import org.apache.mina.common.IoFilterChain;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.TrafficMask;
import org.apache.mina.common.WriteRequest;

/**
 * An {@link IoFilter} that limits bandwidth (bytes per second) related with
 * read and write operations.
 * <p>
 * It is always recommended to add this filter in the first place of the
 * {@link IoFilterChain}.
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class TrafficShapingFilter extends IoFilterAdapter {

    private final AttributeKey STATE = new AttributeKey(getClass(), "state");

    private final ScheduledExecutorService scheduledExecutor;
    private final MessageSizeEstimator messageSizeEstimator;
    private volatile int maxReadThroughput;
    private volatile int maxWriteThroughput;
    
    public TrafficShapingFilter(
            ScheduledExecutorService scheduledExecutor,
            int maxReadThroughput, int maxWriteThroughput) {
        this(scheduledExecutor, null, maxReadThroughput, maxWriteThroughput);
    }
    
    public TrafficShapingFilter(
            ScheduledExecutorService scheduledExecutor,
            MessageSizeEstimator messageSizeEstimator,
            int maxReadThroughput, int maxWriteThroughput) {
        if (scheduledExecutor == null) {
            throw new NullPointerException("scheduledExecutor");
        }
        
        if (messageSizeEstimator == null) {
            messageSizeEstimator = new DefaultMessageSizeEstimator() {
                @Override
                public int estimateSize(Object message) {
                    if (message instanceof IoBuffer) {
                        return ((IoBuffer) message).remaining();
                    }
                    return super.estimateSize(message);
                }
            };
        }
        
        this.scheduledExecutor = scheduledExecutor;
        this.messageSizeEstimator = messageSizeEstimator;
        setMaxReadThroughput(maxReadThroughput);
        setMaxWriteThroughput(maxWriteThroughput);
    }
    
    public ScheduledExecutorService getScheduledExecutor() {
        return scheduledExecutor;
    }
    
    public MessageSizeEstimator getMessageSizeEstimator() {
        return messageSizeEstimator;
    }
    
    public int getMaxReadThroughput() {
        return maxReadThroughput;
    }
    
    public void setMaxReadThroughput(int maxReadThroughput) {
        if (maxReadThroughput < 0) {
            maxReadThroughput = 0;
        }
        this.maxReadThroughput = maxReadThroughput;
    }
    
    public int getMaxWriteThroughput() {
        return maxWriteThroughput;
    }
    
    public void setMaxWriteThroughput(int maxWriteThroughput) {
        if (maxWriteThroughput < 0) {
            maxWriteThroughput = 0;
        }
        this.maxWriteThroughput = maxWriteThroughput;
    }
    
    @Override
    public void onPreAdd(IoFilterChain parent, String name,
            NextFilter nextFilter) throws Exception {
        if (parent.contains(this)) {
            throw new IllegalArgumentException(
                    "You can't add the same filter instance more than once.  Create another instance and add it.");
        }
        parent.getSession().setAttribute(STATE, new State());
    }

    @Override
    public void onPostRemove(IoFilterChain parent, String name,
            NextFilter nextFilter) throws Exception {
        parent.getSession().removeAttribute(STATE);
    }

    @Override
    public void messageReceived(NextFilter nextFilter, final IoSession session,
            Object message) throws Exception {
        
        int maxReadThroughput = this.maxReadThroughput;
        if (maxReadThroughput == 0) {
            nextFilter.messageReceived(session, message);
        }
        
        final State state = (State) session.getAttribute(STATE);
        long currentTime = System.currentTimeMillis();
        
        long suspendTime = 0;
        boolean firstRead = false;
        synchronized (state) {
            state.readBytes += messageSizeEstimator.estimateSize(message);

            if (!state.suspendedRead) {
                if (state.readStartTime == 0) {
                    firstRead = true;
                    state.readStartTime = currentTime - 1000; 
                }

                long throughput = 
                    (state.readBytes * 1000 / (currentTime - state.readStartTime));
                if (throughput >= maxReadThroughput) {
                    suspendTime = Math.max(
                            0,
                            state.readBytes * 1000 / maxReadThroughput - 
                            (firstRead? 0 : currentTime - state.readStartTime));
                    
                    state.readBytes = 0;
                    state.readStartTime = 0;
                    state.suspendedRead = suspendTime != 0;

                    if (session.getConfig().getReadBufferSize() > maxReadThroughput) {
                        session.getConfig().setReadBufferSize(maxReadThroughput);
                    }
                    if (session.getConfig().getMaxReadBufferSize() > maxReadThroughput) {
                        session.getConfig().setMaxReadBufferSize(maxReadThroughput);
                    }
                }
            }
        }
        
        if (suspendTime != 0) {
            session.suspendRead();
            System.out.println(messageSizeEstimator.estimateSize(message) + ", " + suspendTime);
            scheduledExecutor.schedule(new Runnable() {
                public void run() {
                    synchronized (state) {
                        state.suspendedRead = false;
                    }
                    session.resumeRead();
                }
            }, suspendTime, TimeUnit.MILLISECONDS);
        }
        
        nextFilter.messageReceived(session, message);
    }

    @Override
    public void messageSent(NextFilter nextFilter, final IoSession session,
            WriteRequest writeRequest) throws Exception {
        
        int maxWriteThroughput = this.maxWriteThroughput;
        if (maxWriteThroughput == 0) {
            nextFilter.messageSent(session, writeRequest);
        }
        
        final State state = (State) session.getAttribute(STATE);
        long currentTime = System.currentTimeMillis();
        
        long suspendTime = 0;
        boolean firstWrite = false;
        synchronized (state) {
            state.writtenBytes += messageSizeEstimator.estimateSize(writeRequest.getMessage());
            if (!state.suspendedWrite) {
                if (state.writeStartTime == 0) {
                    firstWrite = true;
                    state.writeStartTime = currentTime - 1000; 
                }
                
                long throughput = 
                    (state.writtenBytes * 1000 / (currentTime - state.writeStartTime));
                if (throughput >= maxWriteThroughput) {
                    suspendTime = Math.max(
                            0,
                            state.writtenBytes * 1000 / maxWriteThroughput -
                            (firstWrite? 0 : currentTime - state.writeStartTime));
                    
                    state.writtenBytes = 0;
                    state.writeStartTime = 0;
                    state.suspendedWrite = suspendTime != 0;
                }
            }
        }
        
        if (suspendTime != 0) {
            session.suspendWrite();
            scheduledExecutor.schedule(new Runnable() {
                public void run() {
                    synchronized (state) {
                        state.suspendedWrite = false;
                    }
                    session.resumeWrite();
                }
            }, suspendTime, TimeUnit.MILLISECONDS);
        }
        
        nextFilter.messageSent(session, writeRequest);
    }

    @Override
    public void filterSetTrafficMask(NextFilter nextFilter, IoSession session,
            TrafficMask trafficMask) throws Exception {
        State state = (State) session.getAttribute(STATE);
        boolean suspendedRead;
        boolean suspendedWrite;
        synchronized (state) {
            suspendedRead = state.suspendedRead;
            suspendedWrite = state.suspendedWrite;
        }
        
        if (suspendedRead) {
            trafficMask = trafficMask.and(TrafficMask.WRITE);
        }
        
        if (suspendedWrite) {
            trafficMask = trafficMask.and(TrafficMask.READ);
        }
        
        nextFilter.filterSetTrafficMask(session, trafficMask);
    }
    
    private static class State {
        private long readStartTime;
        private long writeStartTime;
        private boolean suspendedRead;
        private boolean suspendedWrite;
        private long readBytes;
        private long writtenBytes;
    }
}
