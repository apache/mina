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
package org.apache.mina.transport.socket.nio.support;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

import org.apache.mina.common.ExceptionMonitor;
import org.apache.mina.common.IoConnectorConfig;
import org.apache.mina.common.RuntimeIOException;
import org.apache.mina.common.support.BaseIoSessionConfig;
import org.apache.mina.transport.socket.nio.SocketConnector;
import org.apache.mina.transport.socket.nio.SocketSessionConfig;

/**
 * An {@link IoConnectorConfig} for {@link SocketConnector}.
 *
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class SocketSessionConfigImpl extends BaseIoSessionConfig implements SocketSessionConfig
{
    private boolean reuseAddress;
    private int receiveBufferSize;
    private int sendBufferSize;
    private int trafficClass;
    private boolean keepAlive;
    private boolean oobInline;
    private int soLinger;
    private boolean tcpNoDelay;

    /**
     * Creates a new instance.
     * 
     * @throws RuntimeIOException if failed to get the default configuration
     */
    public SocketSessionConfigImpl()
    {
        Socket s = null;
        try
        {
            s = new Socket();
            reuseAddress = s.getReuseAddress();
            receiveBufferSize = s.getReceiveBufferSize();
            sendBufferSize = s.getSendBufferSize();
            trafficClass = s.getTrafficClass();
            keepAlive = s.getKeepAlive();
            oobInline = s.getOOBInline();
            soLinger = s.getSoLinger();
            tcpNoDelay = s.getTcpNoDelay();
        }
        catch( SocketException e )
        {
            throw new RuntimeIOException( "Failed to get the default configuration.", e );
        }
        finally
        {
            if( s != null )
            {
                try
                {
                    s.close();
                }
                catch( IOException e )
                {
                    ExceptionMonitor.getInstance().exceptionCaught( e );
                }
            }
        }
    }

    public boolean isReuseAddress()
    {
        return reuseAddress;
    }
    
    public void setReuseAddress( boolean reuseAddress )
    {
        this.reuseAddress = reuseAddress;
    }

    public int getReceiveBufferSize()
    {
        return receiveBufferSize;
    }

    public void setReceiveBufferSize( int receiveBufferSize )
    {
        this.receiveBufferSize = receiveBufferSize;
    }

    public int getSendBufferSize()
    {
        return sendBufferSize;
    }

    public void setSendBufferSize( int sendBufferSize )
    {
        this.sendBufferSize = sendBufferSize;
    }

    public int getTrafficClass()
    {
        return trafficClass;
    }

    public void setTrafficClass( int trafficClass )
    {
        this.trafficClass = trafficClass;
    }

    public boolean isKeepAlive()
    {
        return keepAlive;
    }

    public void setKeepAlive( boolean keepAlive )
    {
        this.keepAlive = keepAlive;
    }

    public boolean isOobInline()
    {
        return oobInline;
    }

    public void setOobInline( boolean oobInline )
    {
        this.oobInline = oobInline;
    }

    public int getSoLinger()
    {
        return soLinger;
    }

    public void setSoLinger( int soLinger )
    {
        this.soLinger = soLinger;
    }

    public boolean isTcpNoDelay()
    {
        return tcpNoDelay;
    }

    public void setTcpNoDelay( boolean tcpNoDelay )
    {
        this.tcpNoDelay = tcpNoDelay;
    }
}
