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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoFilter;
import org.apache.mina.common.IoFilterAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.util.SessionLog;

/**
 * A {@link IoFilter} which blocks connections from blacklisted remote
 * address.
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class BlacklistFilter extends IoFilterAdapter {
    private final Set<InetAddress> blacklist = new CopyOnWriteArraySet<InetAddress>();

    /**
     * Sets the addresses to be blacklisted.
     *
     * NOTE: this call will remove any previously blacklisted addresses.
     *
     * @param addresses an array of addresses to be blacklisted.
     */
    public void setBlacklist(InetAddress... addresses) {
        if (addresses == null)
            throw new NullPointerException("addresses");
        blacklist.clear();
        for (int i = 0; i < addresses.length; i++) {
            InetAddress addr = addresses[i];
            block(addr, "addresses[" + i + ']');
        }
    }

    /**
     * Sets the addresses to be blacklisted.
     *
     * NOTE: this call will remove any previously blacklisted addresses.
     *
     * @param addresses a collection of InetAddress objects representing the
     *        addresses to be blacklisted.
     * @throws IllegalArgumentException if the specified collections contains
     *         non-{@link InetAddress} objects.
     */
    public void setBlacklist(Collection<InetAddress> addresses) {
        if (addresses == null)
            throw new NullPointerException("addresses");

        InetAddress[] inetAddresses = new InetAddress[addresses.size()];
        try {
            setBlacklist(addresses.toArray(inetAddresses));
        } catch (ArrayStoreException ase) {
            IllegalArgumentException iae = new IllegalArgumentException(
                    "Collection of addresses must contain only InetAddress instances.");
            iae.initCause(ase);
            throw iae;
        }
    }

    /**
     * Blocks the specified endpoint.
     */
    public void block(InetAddress address, String error_string) {
        if (address == null)
            throw new NullPointerException(error_string);
        blacklist.add(address);
    }

    /**
     * Blocks the specified endpoint.
     */
    public void block(InetAddress address) {
        block(address, "address");
    }

    /**
     * Unblocks the specified endpoint.
     */
    public void unblock(InetAddress address) {
        if (address == null)
            throw new NullPointerException("address");
        blacklist.remove(address);
    }

    @Override
    public void sessionCreated(NextFilter nextFilter, IoSession session) {
        if (!isBlocked(session)) {
            // forward if not blocked
            nextFilter.sessionCreated(session);
        } else {
            blockSession(session);
        }
    }

    @Override
    public void sessionOpened(NextFilter nextFilter, IoSession session)
            throws Exception {
        if (!isBlocked(session)) {
            // forward if not blocked
            nextFilter.sessionOpened(session);
        } else {
            blockSession(session);
        }
    }

    @Override
    public void sessionClosed(NextFilter nextFilter, IoSession session)
            throws Exception {
        if (!isBlocked(session)) {
            // forward if not blocked
            nextFilter.sessionClosed(session);
        } else {
            blockSession(session);
        }
    }

    @Override
    public void sessionIdle(NextFilter nextFilter, IoSession session,
            IdleStatus status) throws Exception {
        if (!isBlocked(session)) {
            // forward if not blocked
            nextFilter.sessionIdle(session, status);
        } else {
            blockSession(session);
        }
    }

    @Override
    public void messageReceived(NextFilter nextFilter, IoSession session,
            Object message) {
        if (!isBlocked(session)) {
            // forward if not blocked
            nextFilter.messageReceived(session, message);
        } else {
            blockSession(session);
        }
    }

    @Override
    public void messageSent(NextFilter nextFilter, IoSession session,
            Object message) throws Exception {
        if (!isBlocked(session)) {
            // forward if not blocked
            nextFilter.messageSent(session, message);
        } else {
            blockSession(session);
        }
    }

    private void blockSession(IoSession session) {
        SessionLog.info(session, "Remote address in the blacklist; closing.");
        session.close();
    }

    private boolean isBlocked(IoSession session) {
        SocketAddress remoteAddress = session.getRemoteAddress();
        if (remoteAddress instanceof InetSocketAddress) {
            if (blacklist.contains(((InetSocketAddress) remoteAddress)
                    .getAddress())) {
                return true;
            }
        }

        return false;
    }
}
