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
package org.apache.mina.protocol;

import org.apache.mina.common.IdleStatus;

/**
 * An abstract adapter class for {@link ProtocolHandler}.  You can extend this
 * class and selectively override required event handler methods only.  All
 * methods do nothing by default. 
 * 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 */
public class ProtocolHandlerAdapter implements ProtocolHandler
{

    public void sessionOpened( ProtocolSession session )
    {
    }

    public void sessionClosed( ProtocolSession session )
    {
    }

    public void sessionIdle( ProtocolSession session, IdleStatus status )
    {
    }

    public void exceptionCaught( ProtocolSession session, Throwable cause )
    {
    }

    public void messageReceived( ProtocolSession session, Object message )
    {
    }

    public void messageSent( ProtocolSession session, Object message )
    {
    }
}