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
package org.apache.mina.coap.retry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.mina.api.AbstractIoFilter;
import org.apache.mina.api.IoFilter;
import org.apache.mina.api.IoSession;
import org.apache.mina.coap.CoapMessage;
import org.apache.mina.filterchain.ReadFilterChainController;
import org.apache.mina.filterchain.WriteFilterChainController;
import org.apache.mina.session.WriteRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link IoFilter} in charge of messages retransmissions.
 * 
 * <p>
 * In case of messages to be sent to the client, the filter retransmits the
 * <i>Confirmable</i> message at exponentially increasing intervals, until it
 * receives an acknowledgment (or <i>Reset</i> message), or runs out of
 * attempts.
 * </p>
 * 
 * <p>
 * In case of received <i>Confirmable</i> messages, the filter keeps track of
 * the acknowledged transmissions in order to avoid multiple processing of
 * duplicated messages.
 * </p>
 */
public class CoapRetryFilter extends AbstractIoFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CoapRetryFilter.class);

    /** The executor in charge of scheduling the retransmissions */
    private ScheduledExecutorService retryExecutor = Executors.newSingleThreadScheduledExecutor();

    /** The confirmable messages waiting to be acknowledged */
    private Map<String, CoapTransmission> inFlight = new ConcurrentHashMap<>();

    /**
     * The list of processed messages used to handle duplicate copies of
     * Confirmable messages
     */
    private ExpiringMap<String, CoapMessage> processed = new ExpiringMap<String, CoapMessage>(retryExecutor);

    /**
     * {@inheritDoc}
     */
    @Override
    public void messageReceived(IoSession session, Object in, ReadFilterChainController controller) {
        LOGGER.debug("Processing a MESSAGE_RECEIVED for session {}", session);

        CoapMessage coapMsg = (CoapMessage) in;
        String transmissionId = CoapTransmission.uniqueId(session, coapMsg);

        switch (coapMsg.getType()) {
        case NON_CONFIRMABLE:
            // non confirmable message, let's move to the next filter
            super.messageReceived(session, coapMsg, controller);
            break;

        case CONFIRMABLE:
            // check if this is a duplicate of a message already processed
            CoapMessage ack = processed.get(transmissionId);

            if (ack != null) {
                // stop the filter chain and send again the ack since it was
                // probably lost
                LOGGER.debug("Duplicated messages detected with ID {} in session {}", coapMsg.requestId(), session);
                controller.callWriteMessageForRead(ack);
            } else {
                super.messageReceived(session, coapMsg, controller);
            }

            break;
        case ACK:
        case RESET:
            CoapTransmission t = inFlight.get(transmissionId);

            if (t != null) {
                // cancel the scheduled retransmission
                t.getRetryFuture().cancel(false);
                inFlight.remove(transmissionId);
            }

            super.messageReceived(session, coapMsg, controller);
            break;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void messageWriting(final IoSession session, final WriteRequest message, WriteFilterChainController controller) {
        LOGGER.debug("Processing a MESSAGE_WRITING for session {}", session);

        final CoapMessage coapMsg = (CoapMessage) message.getMessage();
        final String transmissionId = CoapTransmission.uniqueId(session, coapMsg);

        switch (coapMsg.getType()) {

        case NON_CONFIRMABLE:
            controller.callWriteNextFilter(message);
            break;
        case RESET:
        case ACK:
            // let's keep track of the message to avoid processing it again in
            // case of duplicate copy.
            processed.put(transmissionId, coapMsg);

            controller.callWriteNextFilter(message);
            break;

        case CONFIRMABLE:
            // initialize a transmission if this is not a retry
            CoapTransmission t = inFlight.get(transmissionId);
            if (t == null) {
                t = new CoapTransmission(session, coapMsg);
                inFlight.put(t.getId(), t);
            }

            // schedule a retry
            ScheduledFuture<?> future = retryExecutor.schedule(new Runnable() {

                @Override
                public void run() {
                    CoapTransmission t = inFlight.get(transmissionId);

                    // send again the message if the maximum number of attempts
                    // is not reached
                    if (t != null && t.timeout()) {
                        LOGGER.debug("Retry for message with ID {}", coapMsg.requestId());
                        session.write(coapMsg);
                    } else {
                        // abort transmission
                        LOGGER.debug("No more retry for message with ID {}", coapMsg.requestId());
                    }
                }
            }, t.getNextTimeout(), TimeUnit.MILLISECONDS);

            t.setRetryFuture(future);

            // move to the next filter
            controller.callWriteNextFilter(message);
            break;
        }

    }

    /**
     * clear the running executor
     */
    @Override
    protected void finalize() throws Throwable {
        retryExecutor.shutdown();
    }
}
