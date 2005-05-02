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
 * 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 * 
 * @see IoFilterAdapter
 */
public interface IoFilter
{
    /**
     * Filters {@link IoHandler#sessionOpened(IoSession)} event.
     */
    void sessionOpened( NextFilter nextFilter, IoSession session ) throws Exception;

    /**
     * Filters {@link IoHandler#sessionClosed(IoSession)} event.
     */
    void sessionClosed( NextFilter nextFilter, IoSession session ) throws Exception;

    /**
     * Filters {@link IoHandler#sessionIdle(IoSession, IdleStatus)} event.
     */
    void sessionIdle( NextFilter nextFilter, IoSession session,
                      IdleStatus status ) throws Exception;

    /**
     * Filters {@link IoHandler#exceptionCaught(IoSession, Throwable)} event.
     */
    void exceptionCaught( NextFilter nextFilter, IoSession session,
                          Throwable cause ) throws Exception;

    /**
     * Filters {@link IoHandler#dataRead(IoSession, ByteBuffer)} event.
     */
    void dataRead( NextFilter nextFilter, IoSession session, ByteBuffer buf ) throws Exception;

    /**
     * Filters {@link IoHandler#dataWritten(IoSession, Object)} event.
     */
    void dataWritten( NextFilter nextFilter, IoSession session, Object marker ) throws Exception;

    /**
     * Filters {@link IoSession#write(ByteBuffer, Object)} method invocation.
     */
    void filterWrite( NextFilter nextFilter, IoSession session, ByteBuffer buf, Object marker ) throws Exception;
    
    public interface NextFilter
    {
        void sessionOpened( IoSession session );
        void sessionClosed( IoSession session );
        void sessionIdle( IoSession session, IdleStatus status );
        void exceptionCaught( IoSession session, Throwable cause );
        void dataRead( IoSession session, ByteBuffer buf );
        void dataWritten( IoSession session, Object marker );
        void filterWrite( IoSession session, ByteBuffer buf, Object marker );
    }
}
