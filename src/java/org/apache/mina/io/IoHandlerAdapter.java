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
 * An abstract adapter class for {@link IoHandler}.  You can extend this class
 * and selectively override required event handler methods only.  All methods
 * do nothing by default.
 * <p>
 * Please refer to
 * <a href="../../../../../xref-examples/org/apache/mina/examples/netcat/NetCatProtocolHandler.html"><code>NetCatProtocolHandler</code></a>
 * example. 
 * 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 */
public class IoHandlerAdapter implements IoHandler
{

    public void sessionOpened( IoSession session )
    {
    }

    public void sessionClosed( IoSession session )
    {
    }

    public void sessionIdle( IoSession session, IdleStatus status )
    {
    }

    public void exceptionCaught( IoSession session, Throwable cause )
    {
    }

    public void dataRead( IoSession session, ByteBuffer buf )
    {
    }

    public void dataWritten( IoSession session, Object marker )
    {
    }
}