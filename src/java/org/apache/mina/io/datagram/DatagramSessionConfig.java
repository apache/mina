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
package org.apache.mina.io.datagram;

import java.net.SocketException;
import java.nio.channels.DatagramChannel;

import org.apache.mina.common.SessionConfig;
import org.apache.mina.util.BasicSessionConfig;

/**
 * A {@link SessionConfig} for datagram transport (UDP/IP).
 * 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$,
 */
public class DatagramSessionConfig extends BasicSessionConfig
{
    private final DatagramChannel ch;

    DatagramSessionConfig( DatagramChannel ch )
    {
        this.ch = ch;
    }

    public boolean getReuseAddress() throws SocketException
    {
        return ch.socket().getReuseAddress();
    }

    public void setReuseAddress( boolean on ) throws SocketException
    {
        ch.socket().setReuseAddress( on );
    }

    public int getTrafficClass() throws SocketException
    {
        return ch.socket().getTrafficClass();
    }

    public void setTrafficClass( int tc ) throws SocketException
    {
        ch.socket().setTrafficClass( tc );
    }
}