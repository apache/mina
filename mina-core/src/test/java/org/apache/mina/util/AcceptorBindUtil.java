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
package org.apache.mina.util;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.apache.mina.core.service.IoAcceptor;

/**
 * A utility class containing a method to bind acceptor with
 * a retry mechanism, in case the port has already been taken when the
 * bind occurs.
 * <p>
 * May be useful to avoid random test failures...
 */
public final class AcceptorBindUtil {
    /** The default number of tries */
    private static final int NB_TRIES = 10;

    private AcceptorBindUtil() {

    }

    /**
     * Binds an {@link IoAcceptor} using a random available port.
     *
     * @param acceptor the acceptor to bind
     * @return the port to which the acceptor was successfully bound
     * @throws RuntimeException if no port can be bound after the default retry count
     */
    public static int tryBind(IoAcceptor acceptor) {
       return tryBind(acceptor, -1);
    }

    /**
     * Binds an {@link IoAcceptor} to a preferred port first, then falls back to
     * random available ports using the default retry count.
     *
     * @param acceptor the acceptor to bind
     * @param preferredPort the port to try first, or {@code -1} to skip the initial fixed-port attempt
     * @return the port to which the acceptor was successfully bound
     * @throws RuntimeException if no port can be bound after the default retry count
     */
    public static int tryBind(IoAcceptor acceptor, int preferredPort) {
        return tryBind(acceptor, preferredPort, NB_TRIES);
    }

    /**
     * Binds an {@link IoAcceptor} to a preferred port first, then falls back to
     * random available ports, and retries up to {@code nbTries} times.
     *
     * @param acceptor the acceptor to bind
     * @param preferredPort the port to try first, or {@code -1} to skip the initial fixed-port attempt
     * @param nbTries the maximum number of retry attempts when random ports are used
     * @return the port to which the acceptor was successfully bound
     * @throws RuntimeException if no port can be bound within {@code nbTries} attempts
     */
    public static int tryBind(IoAcceptor acceptor, int preferredPort, int nbTries) {
        if (preferredPort != -1) {
            try {
                acceptor.bind(new InetSocketAddress(preferredPort));
                return preferredPort;
            } catch (IOException ignored) {
            }
        }

        while (true) {
            int nextAvailable = AvailablePortFinder.getNextAvailable();
            try {
                acceptor.bind(new InetSocketAddress(nextAvailable));
                return nextAvailable;
            } catch (IOException e) {
                nbTries--;

                if (nbTries == 0) {
                    throw new RuntimeException("Failed to bind acceptor after " + nbTries + " retries", e);
                }
            }
        }
    }
}
