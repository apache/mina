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
 * An abstract adapter class for {@link IoFilter}.  You can extend this
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
public class IoFilterAdapter implements IoFilter
{
    public void sessionOpened( NextFilter nextFilter, IoSession session ) throws Exception
    {
        nextFilter.sessionOpened( session );
    }

    public void sessionClosed( NextFilter nextFilter, IoSession session ) throws Exception
    {
        nextFilter.sessionClosed( session );
    }

    public void sessionIdle( NextFilter nextFilter, IoSession session,
                            IdleStatus status ) throws Exception
    {
        nextFilter.sessionIdle( session, status );
    }

    public void exceptionCaught( NextFilter nextFilter, IoSession session,
                                Throwable cause ) throws Exception
    {
        nextFilter.exceptionCaught( session, cause );
    }

    public void dataRead( NextFilter nextFilter, IoSession session,
                         ByteBuffer buf ) throws Exception
    {
        nextFilter.dataRead( session, buf );
    }

    public void dataWritten( NextFilter nextFilter, IoSession session,
                            Object marker ) throws Exception
    {
        nextFilter.dataWritten( session, marker );
    }

    public void filterWrite( NextFilter nextFilter, IoSession session,
                             ByteBuffer buf, Object marker ) throws Exception
    {
        nextFilter.filterWrite( session, buf, marker );
    }
}