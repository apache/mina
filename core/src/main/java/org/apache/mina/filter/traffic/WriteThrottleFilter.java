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
 * when the {@link IoSession#getScheduledWriteBytes() localScheduledWriteBytes},
 * {@link IoSession#getScheduledWriteMessages() localScheduledWriteMessages},
 * {@link IoService#getScheduledWriteBytes() globalScheduledWriteBytes} or
 * {@link IoService#getScheduledWriteMessages() globalScheduledWriteMessages}
 * exceeds the specified limit values.

 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class WriteThrottleFilter extends IoFilterAdapter {

    private final Object logLock = new Object();
    private final Object blockLock = new Object();

    private long lastLogTime = -1;
    private int blockWaiters = 0;
    
    private volatile WriteThrottlePolicy policy;
    
    private volatile int localMaxScheduledWriteMessages;
    private volatile long localMaxScheduledWriteBytes;
    private volatile int globalMaxScheduledWriteMessages;
    private volatile long globalMaxScheduledWriteBytes;
    
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
            int localMaxScheduledWriteMessages, long localMaxScheduledWriteBytes,
            int globalMaxScheduledWriteMessages, long globalMaxScheduledWriteBytes) {
        this(WriteThrottlePolicy.LOG,
             localMaxScheduledWriteMessages, localMaxScheduledWriteBytes,
             globalMaxScheduledWriteMessages, globalMaxScheduledWriteBytes);
    }
    
    /**
     * Creates a new instance with the specified <tt>policy</tt> and limit
     * values.
     */
    public WriteThrottleFilter(
            WriteThrottlePolicy policy,
            int localMaxScheduledWriteMessages, long localMaxScheduledWriteBytes,
            int globalMaxScheduledWriteMessages, long globalMaxScheduledWriteBytes) {

        setPolicy(policy);
        setLocalMaxScheduledWriteMessages(localMaxScheduledWriteMessages);
        setLocalMaxScheduledWriteBytes(localMaxScheduledWriteBytes);
        setGlobalMaxScheduledWriteMessages(globalMaxScheduledWriteMessages);
        setGlobalMaxScheduledWriteBytes(globalMaxScheduledWriteBytes);
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

    public int getLocalMaxScheduledWriteMessages() {
        return localMaxScheduledWriteMessages;
    }

    public void setLocalMaxScheduledWriteMessages(int localMaxScheduledWriteMessages) {
        if (localMaxScheduledWriteMessages < 0) {
            localMaxScheduledWriteMessages = 0;
        }
        this.localMaxScheduledWriteMessages = localMaxScheduledWriteMessages;
    }

    public long getLocalMaxScheduledWriteBytes() {
        return localMaxScheduledWriteBytes;
    }

    public void setLocalMaxScheduledWriteBytes(long localMaxScheduledWriteBytes) {
        if (localMaxScheduledWriteBytes < 0) {
            localMaxScheduledWriteBytes = 0;
        }
        this.localMaxScheduledWriteBytes = localMaxScheduledWriteBytes;
    }

    public int getGlobalMaxScheduledWriteMessages() {
        return globalMaxScheduledWriteMessages;
    }

    public void setGlobalMaxScheduledWriteMessages(int globalMaxScheduledWriteMessages) {
        if (globalMaxScheduledWriteMessages < 0) {
            globalMaxScheduledWriteMessages = 0;
        }
        this.globalMaxScheduledWriteMessages = globalMaxScheduledWriteMessages;
    }

    public long getGlobalMaxScheduledWriteBytes() {
        return globalMaxScheduledWriteBytes;
    }

    public void setGlobalMaxScheduledWriteBytes(long globalMaxScheduledWriteBytes) {
        if (globalMaxScheduledWriteBytes < 0) {
            globalMaxScheduledWriteBytes = 0;
        }
        this.globalMaxScheduledWriteBytes = globalMaxScheduledWriteBytes;
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

        int lmswm = localMaxScheduledWriteMessages;
        long lmswb = localMaxScheduledWriteBytes;
        int gmswm = globalMaxScheduledWriteMessages;
        long gmswb = globalMaxScheduledWriteBytes;
        
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
        int lmswm = localMaxScheduledWriteMessages;
        long lmswb = localMaxScheduledWriteBytes;
        int gmswm = globalMaxScheduledWriteMessages;
        long gmswb = globalMaxScheduledWriteBytes;

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
