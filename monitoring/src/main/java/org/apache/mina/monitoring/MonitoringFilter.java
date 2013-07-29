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

package org.apache.mina.monitoring;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import org.apache.mina.api.AbstractIoFilter;
import org.apache.mina.api.IdleStatus;
import org.apache.mina.api.IoSession;
import org.apache.mina.filterchain.ReadFilterChainController;
import org.apache.mina.filterchain.WriteFilterChainController;
import org.apache.mina.session.WriteRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

/**
 * Monitoring Filter. Captures the basic events. It needs to be the first Filter in the Chain
 *
 *  MonitoringFilter -> Custom Filter/codec -> IoHandler
 */
public class MonitoringFilter extends AbstractIoFilter {
    public static final Logger LOG = LoggerFactory.getLogger(MonitoringFilter.class);

    // Counter to monitor Events
    private final Counter sessionOpenedCounter;
    private final Counter sessionClosedCounter;
    private final Counter sessionIdleCounter;
    private final Counter rawMessagesReceivedCounter;
    private final Counter messagesSentCounter;
    private final Counter bytesIn;
    private final Counter bytesOut;

    // Metrics Resgistry instance
    private MetricRegistry metricRegistry;

    public MonitoringFilter(MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
        sessionOpenedCounter = this.metricRegistry.counter("Sessions Opened");
        sessionClosedCounter = this.metricRegistry.counter("Sessions Closed");
        sessionIdleCounter = this.metricRegistry.counter("Sessions Idle");
        rawMessagesReceivedCounter = this.metricRegistry.counter("Messages Received");
        messagesSentCounter = this.metricRegistry.counter("Messages Sent");
        bytesIn = this.metricRegistry.counter("Bytes In");
        bytesOut = this.metricRegistry.counter("Bytes Out");
    }

    @Override
    public void sessionOpened(IoSession session) {
        sessionOpenedCounter.inc();
        super.sessionOpened(session);
    }

    @Override
    public void sessionClosed(IoSession session) {
        sessionClosedCounter.inc();
        super.sessionClosed(session);
    }

    @Override
    public void sessionIdle(IoSession session, IdleStatus status) {
        sessionIdleCounter.inc();
        super.sessionIdle(session, status);
    }

    @Override
    public void messageReceived(IoSession session, Object message, ReadFilterChainController controller) {
        rawMessagesReceivedCounter.inc();
        if(message instanceof ByteBuffer) {
            bytesIn.inc(((ByteBuffer)message).remaining());
        }
        super.messageReceived(session, message, controller);
    }

    @Override
    public void messageWriting(IoSession session, WriteRequest message, WriteFilterChainController controller) {
        if(message.getMessage() instanceof ByteBuffer) {
            bytesOut.inc(((ByteBuffer)message.getMessage()).remaining());
        }
        super.messageWriting(session, message, controller);
    }

    @Override
    public void messageSent(IoSession session, Object message) {
        messagesSentCounter.inc();
        super.messageSent(session, message);
    }


}
