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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.mina.common.IoFilter;
import org.apache.mina.common.IoFilterAdapter;
import org.apache.mina.common.IoFilterChain;
import org.apache.mina.common.IoService;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.WriteException;
import org.apache.mina.common.WriteRequest;
import org.apache.mina.util.CopyOnWriteMap;
import org.apache.mina.util.MapBackedSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 */
public class WriteThrottleFilter extends IoFilterAdapter {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    
    private static final Set<IoService> activeServices =
        new MapBackedSet<IoService>(new CopyOnWriteMap<IoService, Boolean>()); 
    
    public static int getGlobalScheduledWriteMessages() {
        int answer = 0;
        List<IoService> inactiveServices = null;
        for (IoService s: activeServices) {
            if (s.isActive()) {
                answer += s.getScheduledWriteMessages();
            } else {
                if (inactiveServices == null) {
                    inactiveServices = new ArrayList<IoService>();
                }
                inactiveServices.add(s);
            }
        }
        
        if (inactiveServices != null) {
            activeServices.removeAll(inactiveServices);
        }
        
        return answer;
    }
    
    public static long getGlobalScheduledWriteBytes() {
        long answer = 0;
        List<IoService> inactiveServices = null;
        for (IoService s: activeServices) {
            if (s.isActive()) {
                answer += s.getScheduledWriteBytes();
            } else {
                if (inactiveServices == null) {
                    inactiveServices = new ArrayList<IoService>();
                }
                inactiveServices.add(s);
            }
        }
        
        if (inactiveServices != null) {
            activeServices.removeAll(inactiveServices);
        }
        
        return answer;
    }
    
    private static int getGlobalScheduledWriteMessages(IoService service) {
        if (!activeServices.contains(service)) {
            activeServices.add(service);
        }
        return getGlobalScheduledWriteMessages();
    }
    
    private static long getGlobalScheduledWriteBytes(IoService service) {
        if (!activeServices.contains(service)) {
            activeServices.add(service);
        }
        return getGlobalScheduledWriteBytes();
    }
    
    private final Object logLock = new Object();
    private final Object blockLock = new Object();

    private long lastLogTime = 0;
    private int blockWaiters = 0;
    
    private volatile WriteThrottlePolicy policy;
    
    private volatile int maxSessionScheduledWriteMessages;
    private volatile long maxSessionScheduledWriteBytes;
    private volatile int maxServiceScheduledWriteMessages;
    private volatile long maxServiceScheduledWriteBytes;
    private volatile int maxGlobalScheduledWriteMessages;
    private volatile long maxGlobalScheduledWriteBytes;
    
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
        // 4K, 64K, 128K, 64M, 256K, 128M
        this(policy, 4096, 65536, 1024 * 128, 1048576 * 64, 1024 * 256, 1028576 * 128);
    }
    
    /**
     * Creates a new instance with the default policy
     * ({@link WriteThrottlePolicy#LOG}) and the specified limit values.
     */
    public WriteThrottleFilter(
            int maxSessionScheduledWriteMessages, long maxSessionScheduledWriteBytes,
            int maxServiceScheduledWriteMessages, long maxServiceScheduledWriteBytes,
            int maxGlobalScheduledWriteMessages,  long maxGlobalScheduledWriteBytes) {
        this(WriteThrottlePolicy.LOG,
             maxSessionScheduledWriteMessages, maxSessionScheduledWriteBytes,
             maxServiceScheduledWriteMessages, maxServiceScheduledWriteBytes,
             maxGlobalScheduledWriteMessages,  maxGlobalScheduledWriteBytes);
    }
    
    /**
     * Creates a new instance with the specified <tt>policy</tt> and limit
     * values.
     */
    public WriteThrottleFilter(
            WriteThrottlePolicy policy,
            int maxSessionScheduledWriteMessages, long maxSessionScheduledWriteBytes,
            int maxServiceScheduledWriteMessages, long maxServiceScheduledWriteBytes,
            int maxGlobalScheduledWriteMessages,  long maxGlobalScheduledWriteBytes) {

        setPolicy(policy);
        setMaxSessionScheduledWriteMessages(maxSessionScheduledWriteMessages);
        setMaxSessionScheduledWriteBytes(maxSessionScheduledWriteBytes);
        setMaxServiceScheduledWriteMessages(maxServiceScheduledWriteMessages);
        setMaxServiceScheduledWriteBytes(maxServiceScheduledWriteBytes);
        setMaxGlobalScheduledWriteMessages(maxGlobalScheduledWriteMessages);
        setMaxGlobalScheduledWriteBytes(maxGlobalScheduledWriteBytes);
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

    public int getMaxGlobalScheduledWriteMessages() {
        return maxGlobalScheduledWriteMessages;
    }

    public void setMaxGlobalScheduledWriteMessages(int maxGlobalScheduledWriteMessages) {
        if (maxGlobalScheduledWriteMessages < 0) {
            maxGlobalScheduledWriteMessages = 0;
        }
        this.maxGlobalScheduledWriteMessages = maxGlobalScheduledWriteMessages;
    }

    public long getMaxGlobalScheduledWriteBytes() {
        return maxGlobalScheduledWriteBytes;
    }

    public void setMaxGlobalScheduledWriteBytes(long maxGlobalScheduledWriteBytes) {
        if (maxGlobalScheduledWriteBytes < 0) {
            maxGlobalScheduledWriteBytes = 0;
        }
        this.maxGlobalScheduledWriteBytes = maxGlobalScheduledWriteBytes;
    }

    @Override
    public void onPreAdd(
            IoFilterChain parent, String name, NextFilter nextFilter) throws Exception {
        if (parent.contains(WriteThrottleFilter.class)) {
            throw new IllegalStateException(
                    "Only one " + WriteThrottleFilter.class.getName() + " is allowed per chain.");
        }
    }

    @Override
    public void filterWrite(NextFilter nextFilter, IoSession session,
            WriteRequest writeRequest) throws Exception {
        
        WriteThrottlePolicy policy = getPolicy();
        if (policy != WriteThrottlePolicy.OFF) {
            if (!readyToWrite(session)) {
                switch (policy) {
                case FAIL:
                    log(session);
                    fail(session, writeRequest);
                    break;
                case BLOCK:
                    log(session);
                    block(session);
                    break;
                case LOG:
                    log(session);
                    break;
                }
            }
        }
        
        nextFilter.filterWrite(session, writeRequest);
    }
    
    private boolean readyToWrite(IoSession session) {
        if (session.isClosing()) {
            return true;
        }

        int  mSession = maxSessionScheduledWriteMessages;
        long bSession = maxSessionScheduledWriteBytes;
        int  mService = maxServiceScheduledWriteMessages;
        long bService = maxServiceScheduledWriteBytes;
        int  mGlobal  = maxGlobalScheduledWriteMessages;
        long bGlobal  = maxGlobalScheduledWriteBytes;
        
        return (mSession == 0 || session.getScheduledWriteMessages() < mSession) &&
               (bSession == 0 || session.getScheduledWriteBytes() < bSession) &&
               (mService == 0 || session.getService().getScheduledWriteMessages() < mService) &&
               (bService == 0 || session.getService().getScheduledWriteBytes() < bService) &&
               (mGlobal  == 0 || getGlobalScheduledWriteMessages(session.getService()) < mGlobal) &&
               (bGlobal  == 0 || getGlobalScheduledWriteBytes(session.getService()) < bGlobal);
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
            logger.warn(getMessage(session));
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

    private void fail(IoSession session, WriteRequest writeRequest) throws WriteException {
        throw new WriteFloodException(writeRequest, getMessage(session));
    }
    
    private String getMessage(IoSession session) {
        int  mSession = maxSessionScheduledWriteMessages;
        long bSession = maxSessionScheduledWriteBytes;
        int  mService = maxServiceScheduledWriteMessages;
        long bService = maxServiceScheduledWriteBytes;
        int  mGlobal  = maxGlobalScheduledWriteMessages;
        long bGlobal  = maxGlobalScheduledWriteBytes;

        StringBuilder buf = new StringBuilder(512);
        buf.append("Write requests flooded - session: ");
        if (mSession != 0) {
            buf.append(session.getScheduledWriteMessages());
            buf.append(" / ");
            buf.append(mSession);
            buf.append(" msgs, ");
        } else {
            buf.append(session.getScheduledWriteMessages());
            buf.append(" / unlimited msgs, ");
        }
        
        if (bSession != 0) {
            buf.append(session.getScheduledWriteBytes());
            buf.append(" / ");
            buf.append(bSession);
            buf.append(" bytes, ");
        } else {
            buf.append(session.getScheduledWriteBytes());
            buf.append(" / unlimited bytes, ");
        }
        
        buf.append("service: ");
        if (mService != 0) {
            buf.append(session.getService().getScheduledWriteMessages());
            buf.append(" / ");
            buf.append(mService);
            buf.append(" msgs, ");
        } else {
            buf.append(session.getService().getScheduledWriteMessages());
            buf.append(" / unlimited msgs, ");
        }
        
        if (bService != 0) {
            buf.append(session.getService().getScheduledWriteBytes());
            buf.append(" / ");
            buf.append(bService);
            buf.append(" bytes, ");
        } else {
            buf.append(session.getService().getScheduledWriteBytes());
            buf.append(" / unlimited bytes, ");
        }
        
        buf.append("global: ");
        if (mGlobal != 0) {
            buf.append(getGlobalScheduledWriteMessages());
            buf.append(" / ");
            buf.append(mGlobal);
            buf.append(" msgs, ");
        } else {
            buf.append(getGlobalScheduledWriteMessages());
            buf.append(" / unlimited msgs, ");
        }
        
        if (bGlobal != 0) {
            buf.append(getGlobalScheduledWriteBytes());
            buf.append(" / ");
            buf.append(bGlobal);
            buf.append(" bytes.");
        } else {
            buf.append(getGlobalScheduledWriteBytes());
            buf.append(" / unlimited bytes.");
        }
        
        return buf.toString();
    }
}
