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

import org.apache.mina.common.IoFilter;
import org.apache.mina.common.IoFilterAdapter;
import org.apache.mina.common.IoService;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.IoSessionLogger;
import org.apache.mina.common.WriteException;
import org.apache.mina.common.WriteRequest;

/**
 * An {@link IoFilter} that throttles outgoing traffic to prevent a unwanted
 * {@link OutOfMemoryError} under heavy load.
 * <p>
 * This filter will automatically enforce the specified {@link WriteThrottlePolicy}
 * when the {@link IoSession#getScheduledWriteBytes() sessionScheduledWriteBytes},
 * {@link IoSession#getScheduledWriteMessages() sessionScheduledWriteMessages},
 * {@link IoService#getScheduledWriteBytes() serviceScheduledWriteBytes} or
 * {@link IoService#getScheduledWriteMessages() serviceScheduledWriteMessages}
 * exceeds the specified limit values.
 * <p>
 * Please add this filter at the end of the filter chain.

 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 * 
 * TODO provide global limitation
 */
public class WriteThrottleFilter extends IoFilterAdapter {

    private final Object logLock = new Object();
    private final Object blockLock = new Object();

    private long lastLogTime = -1;
    private int blockWaiters = 0;
    
    private volatile WriteThrottlePolicy policy;
    
    private volatile int maxSessionScheduledWriteMessages;
    private volatile long maxSessionScheduledWriteBytes;
    private volatile int maxServiceScheduledWriteMessages;
    private volatile long maxServiceScheduledWriteBytes;
    
    /**
     * Creates a new instance with the default policy
     * ({@link WriteThrottlePolicy#LOG}) and limit values.
     */
    public WriteThrottleFilter() {
        this(WriteThrottlePolicy.LOG);
    }
    
    /**
     * Creates a new instance with the specified <tt>policy</tt> and the
     * default limit values.
     */
    public WriteThrottleFilter(WriteThrottlePolicy policy) {
        this(policy, 4096, 65536, 131072, 1048576 * 128);
    }
    
    /**
     * Creates a new instance with the default policy
     * ({@link WriteThrottlePolicy#LOG}) and the specified limit values.
     */
    public WriteThrottleFilter(
            int maxSessionScheduledWriteMessages, long maxSessionScheduledWriteBytes,
            int globalMaxScheduledWriteMessages, long globalMaxScheduledWriteBytes) {
        this(WriteThrottlePolicy.LOG,
             maxSessionScheduledWriteMessages, maxSessionScheduledWriteBytes,
             globalMaxScheduledWriteMessages, globalMaxScheduledWriteBytes);
    }
    
    /**
     * Creates a new instance with the specified <tt>policy</tt> and limit
     * values.
     */
    public WriteThrottleFilter(
            WriteThrottlePolicy policy,
            int maxSessionScheduledWriteMessages, long maxSessionScheduledWriteBytes,
            int maxServiceScheduledWriteMessages, long maxServiceScheduledWriteBytes) {

        setPolicy(policy);
        setMaxSessionScheduledWriteMessages(maxSessionScheduledWriteMessages);
        setMaxSessionScheduledWriteBytes(maxSessionScheduledWriteBytes);
        setMaxServiceScheduledWriteMessages(maxServiceScheduledWriteMessages);
        setMaxServiceScheduledWriteBytes(maxServiceScheduledWriteBytes);
    }
    
    public WriteThrottlePolicy getPolicy() {
        return policy;
    }
    
    public void setPolicy(WriteThrottlePolicy policy) {
        if (policy == null) {
            throw new NullPointerException("policy");
        }
        this.policy = policy;
    }

    public int getMaxSessionScheduledWriteMessages() {
        return maxSessionScheduledWriteMessages;
    }

    public void setMaxSessionScheduledWriteMessages(int maxSessionScheduledWriteMessages) {
        if (maxSessionScheduledWriteMessages < 0) {
            maxSessionScheduledWriteMessages = 0;
        }
        this.maxSessionScheduledWriteMessages = maxSessionScheduledWriteMessages;
    }

    public long getMaxSessionScheduledWriteBytes() {
        return maxSessionScheduledWriteBytes;
    }

    public void setMaxSessionScheduledWriteBytes(long maxSessionScheduledWriteBytes) {
        if (maxSessionScheduledWriteBytes < 0) {
            maxSessionScheduledWriteBytes = 0;
        }
        this.maxSessionScheduledWriteBytes = maxSessionScheduledWriteBytes;
    }

    public int getMaxServiceScheduledWriteMessages() {
        return maxServiceScheduledWriteMessages;
    }

    public void setMaxServiceScheduledWriteMessages(int maxServiceScheduledWriteMessages) {
        if (maxServiceScheduledWriteMessages < 0) {
            maxServiceScheduledWriteMessages = 0;
        }
        this.maxServiceScheduledWriteMessages = maxServiceScheduledWriteMessages;
    }

    public long getMaxServiceScheduledWriteBytes() {
        return maxServiceScheduledWriteBytes;
    }

    public void setMaxServiceScheduledWriteBytes(long maxServiceScheduledWriteBytes) {
        if (maxServiceScheduledWriteBytes < 0) {
            maxServiceScheduledWriteBytes = 0;
        }
        this.maxServiceScheduledWriteBytes = maxServiceScheduledWriteBytes;
    }

    @Override
    public void filterWrite(NextFilter nextFilter, IoSession session,
            WriteRequest writeRequest) throws Exception {
        
        WriteThrottlePolicy policy = getPolicy();
        if (policy != WriteThrottlePolicy.OFF) {
            if (!readyToWrite(session)) {
                switch (getPolicy()) {
                case LOG:
                    log(session);
                    break;
                case BLOCK:
                    block(session);
                    break;
                case LOG_AND_BLOCK:
                    log(session);
                    block(session);
                    break;
                case EXCEPTION:
                    raiseException(session, writeRequest);
                default:
                    throw new InternalError();    
                }
            }
        }
        
        nextFilter.filterWrite(session, writeRequest);
    }
    
    private boolean readyToWrite(IoSession session) {
        if (session.isClosing()) {
            return true;
        }

        int lmswm = maxSessionScheduledWriteMessages;
        long lmswb = maxSessionScheduledWriteBytes;
        int gmswm = maxServiceScheduledWriteMessages;
        long gmswb = maxServiceScheduledWriteBytes;
        
        return (lmswm == 0 || session.getScheduledWriteMessages() < lmswm) &&
               (lmswb == 0 || session.getScheduledWriteBytes() < lmswb) &&
               (gmswm == 0 || session.getService().getScheduledWriteMessages() < gmswm) &&
               (gmswb == 0 || session.getService().getScheduledWriteBytes() < gmswb);
    }
    
    private void log(IoSession session) {
        long currentTime = System.currentTimeMillis();
        
        // Prevent log flood by logging every 3 seconds.
        boolean log;
        synchronized (logLock) {
            if (currentTime - lastLogTime > 3000) {
                lastLogTime = currentTime;
                log = true;
            } else {
                log = false;
            }
        }
        
        if (log) {
            IoSessionLogger.getLogger(session, getClass()).warn(getMessage(session));
        }
    }
    
    @Override
    public void messageSent(
            NextFilter nextFilter, IoSession session, WriteRequest writeRequest) throws Exception {
        notifyWaitingWriters();
        nextFilter.messageSent(session, writeRequest);
    }

    @Override
    public void exceptionCaught(NextFilter nextFilter, IoSession session,
            Throwable cause) throws Exception {
        try {
            nextFilter.exceptionCaught(session, cause);
        } finally {
            notifyWaitingWriters();
        }
    }

    @Override
    public void sessionClosed(NextFilter nextFilter, IoSession session)
            throws Exception {
        notifyWaitingWriters();
        nextFilter.sessionClosed(session);
    }

    private void block(IoSession session) {
        synchronized (blockLock) {
            blockWaiters ++;
            while (!readyToWrite(session)) {
                try {
                    blockLock.wait();
                } catch (InterruptedException e) {
                    // Ignore.
                }
            }
            blockWaiters --;
        }
    }
    
    private void notifyWaitingWriters() {
        synchronized (blockLock) {
            if (blockWaiters != 0) {
                blockLock.notifyAll();
            }
        }
    }

    private void raiseException(IoSession session, WriteRequest writeRequest) throws WriteException {
        throw new TooManyScheduledWritesException(writeRequest, getMessage(session));
    }
    
    private String getMessage(IoSession session) {
        int lmswm = maxSessionScheduledWriteMessages;
        long lmswb = maxSessionScheduledWriteBytes;
        int gmswm = maxServiceScheduledWriteMessages;
        long gmswb = maxServiceScheduledWriteBytes;

        StringBuilder buf = new StringBuilder(512);
        buf.append("Write requests flooded - local: ");
        if (lmswm != 0) {
            buf.append(session.getScheduledWriteMessages());
            buf.append(" / ");
            buf.append(lmswm);
            buf.append(" msgs, ");
        } else {
            buf.append(session.getScheduledWriteMessages());
            buf.append(" / unlimited msgs, ");
        }
        
        if (lmswb != 0) {
            buf.append(session.getScheduledWriteBytes());
            buf.append(" / ");
            buf.append(lmswb);
            buf.append(" bytes, ");
        } else {
            buf.append(session.getScheduledWriteBytes());
            buf.append(" / unlimited bytes, ");
        }
        
        buf.append("global: ");
        if (gmswm != 0) {
            buf.append(session.getService().getScheduledWriteMessages());
            buf.append(" / ");
            buf.append(gmswm);
            buf.append(" msgs, ");
        } else {
            buf.append(session.getService().getScheduledWriteMessages());
            buf.append(" / unlimited msgs, ");
        }
        
        if (gmswb != 0) {
            buf.append(session.getService().getScheduledWriteBytes());
            buf.append(" / ");
            buf.append(gmswb);
            buf.append(" bytes");
        } else {
            buf.append(session.getService().getScheduledWriteBytes());
            buf.append(" / unlimited bytes");
        }
        
        return buf.toString();
    }
}
