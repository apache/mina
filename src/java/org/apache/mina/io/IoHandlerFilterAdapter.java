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
 * An abstract adapter class for {@link IoHandlerFilter}.  You can extend this
 * class and selectively override required event filter methods only.  All
 * methods forwards events to the next filter by default.
 * <p>
 * Please refer to
 * <a href="../../../../../xref/org/apache/mina/io/filter/BlacklistFilter.html"><code>BlacklistFilter</code></a>
 * example.
 * 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 */
public class IoHandlerFilterAdapter implements IoHandlerFilter
{
    public void sessionOpened( IoHandler nextHandler, IoSession session )
    {
        nextHandler.sessionOpened( session );
    }

    public void sessionClosed( IoHandler nextHandler, IoSession session )
    {
        nextHandler.sessionClosed( session );
    }

    public void sessionIdle( IoHandler nextHandler, IoSession session,
                            IdleStatus status )
    {
        nextHandler.sessionIdle( session, status );
    }

    public void exceptionCaught( IoHandler nextHandler, IoSession session,
                                Throwable cause )
    {
        nextHandler.exceptionCaught( session, cause );
    }

    public void dataRead( IoHandler nextHandler, IoSession session,
                         ByteBuffer buf )
    {
        nextHandler.dataRead( session, buf );
    }

    public void dataWritten( IoHandler nextHandler, IoSession session,
                            Object marker )
    {
        nextHandler.dataWritten( session, marker );
    }

    public ByteBuffer filterWrite( IoSession session, ByteBuffer buf )
    {
        return buf;
    }
}