/*
 *   @(#) $Id$
 *
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.mina.io;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IdleStatus;

/**
 * A filter which intercepts {@link IoHandler} events like Servlet filters.
 * Filters can be used for these purposes:
 * <ul>
 *   <li>Event logging,</li>
 *   <li>Performance measurement,</li>
 *   <li>Data transformation (e.g. SSL support),</li>
 *   <li>Firewalling,</li>
 *   <li>and many more.</li>
 * </ul>
 * <p>
 * Please refer to <a href="../../../../../xref/org/apache/mina/io/filter/BlacklistFilter.html"><code>BlacklistFilter</code></a>
 * example.
 * <p>
 * <strong>Please NEVER implement your filters to wrap
 * {@link IoSession}s.</strong> Users can cache the reference to the session,
 * which might malfunction if any filters are added or removed later.
 * Please implement {@link #filterWrite(IoSession,ByteBuffer)} method to
 * override {@link IoSession#write(ByteBuffer, Object)} method.
 * 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 * 
 * @see IoHandlerFilterAdapter
 */
public interface IoHandlerFilter
{
    /**
     * Filters {@link IoHandler#sessionOpened(IoSession)} event.
     */
    void sessionOpened( IoHandler nextHandler, IoSession session );

    /**
     * Filters {@link IoHandler#sessionClosed(IoSession)} event.
     */
    void sessionClosed( IoHandler nextHandler, IoSession session );

    /**
     * Filters {@link IoHandler#sessionIdle(IoSession, IdleStatus)} event.
     */
    void sessionIdle( IoHandler nextHandler, IoSession session,
                     IdleStatus status );

    /**
     * Filters {@link IoHandler#exceptionCaught(IoSession, Throwable)} event.
     */
    void exceptionCaught( IoHandler nextHandler, IoSession session,
                         Throwable cause );

    /**
     * Filters {@link IoHandler#dataRead(IoSession, ByteBuffer)} event.
     */
    void dataRead( IoHandler nextHandler, IoSession session, ByteBuffer buf );

    /**
     * Filters {@link IoHandler#dataWritten(IoSession, Object)} event.
     */
    void dataWritten( IoHandler nextHandler, IoSession session, Object marker );

    /**
     * Filters {@link IoSession#write(ByteBuffer, Object)} method invocation.
     */
    ByteBuffer filterWrite( IoSession session, ByteBuffer buf );
}