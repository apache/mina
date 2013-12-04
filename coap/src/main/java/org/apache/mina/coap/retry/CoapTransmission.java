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

import java.util.Random;
import java.util.concurrent.ScheduledFuture;

import org.apache.mina.api.IoSession;
import org.apache.mina.coap.CoapMessage;

/**
 * A transmission is a wrapper of a <i>Confirmable</i> {@link CoapMessage} carrying additional data used to ensure a
 * reliable communication.
 * 
 * <p>
 * Basically, retransmission is controlled by two things : a timeout and retransmission counter.
 * </p>
 */
public class CoapTransmission {

    /** Default value of the initial timeout - in milliseconds */
    private static final long ACK_TIMEOUT = 2000L;

    /** Default value of the random factor used to compute the initial timeout */
    private static final float ACK_RANDOM_FACTOR = 1.5F;

    /** Default value of the maximum number of retransmissions */
    private static final int MAX_RETRANSMIT = 4;

    /**
     * The unique transmission identifier
     */
    private String id;

    /**
     * The CoAP message waiting to be acknowledged
     */
    private CoapMessage message;

    /**
     * The future in charge of the retransmission when the timeout is reached. It is needed to keep track of this future
     * to be able to cancel it when the expected acknowledgment is received
     */
    private ScheduledFuture<?> retryFuture;

    /**
     * The number of transmission retry
     */
    private int transmissionCount;

    /**
     * the timeout in millisecond before the next retransmission
     */
    private long nextTimeout;

    public CoapTransmission(IoSession session, CoapMessage message) {
        this.id = uniqueId(session, message);
        this.message = message;

        this.transmissionCount = 0;

        // the initial timeout is set to a random duration between ACK_TIMEOUT and (ACK_TIMEOUT * ACK_RANDOM_FACTOR)
        this.nextTimeout = ACK_TIMEOUT + new Random().nextInt((int) ((ACK_RANDOM_FACTOR - 1.0F) * ACK_TIMEOUT));
    }

    /**
     * This method is called when a timeout is triggered for this transmission.
     * 
     * @return <code>true</code> if the message must be retransmitted and <code>false</code> if the transmission attempt
     *         must be canceled
     */
    public boolean timeout() {
        if (transmissionCount < MAX_RETRANSMIT) {
            this.nextTimeout = this.nextTimeout * 2;
            this.transmissionCount++;
            return true;
        }
        return false;
    }

    public String getId() {
        return id;
    }

    public CoapMessage getMessage() {
        return message;
    }

    public ScheduledFuture<?> getRetryFuture() {
        return retryFuture;
    }

    public void setRetryFuture(ScheduledFuture<?> retryFuture) {
        this.retryFuture = retryFuture;
    }

    public long getNextTimeout() {
        return nextTimeout;
    }

    /**
     * @return the unique identifier for a given message in a session.
     */
    public static String uniqueId(IoSession session, CoapMessage message) {
        return session.getId() + "#" + message.requestId();
    }

}
