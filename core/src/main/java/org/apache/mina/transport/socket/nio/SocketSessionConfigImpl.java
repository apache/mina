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
package org.apache.mina.transport.socket.nio;

import org.apache.mina.common.ExceptionMonitor;
import org.apache.mina.common.IoConnectorConfig;
import org.apache.mina.common.support.BaseIoSessionConfig;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

/**
 * An {@link IoConnectorConfig} for {@link SocketConnector}.
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class SocketSessionConfigImpl extends BaseIoSessionConfig implements SocketSessionConfig
{
    private static boolean SET_RECEIVE_BUFFER_SIZE_AVAILABLE = false;
    private static boolean SET_SEND_BUFFER_SIZE_AVAILABLE = false;
    private static boolean GET_TRAFFIC_CLASS_AVAILABLE = false;
    private static boolean SET_TRAFFIC_CLASS_AVAILABLE = false;

    private static boolean DEFAULT_REUSE_ADDRESS;
    private static int DEFAULT_RECEIVE_BUFFER_SIZE;
    private static int DEFAULT_SEND_BUFFER_SIZE;
    private static int DEFAULT_TRAFFIC_CLASS;
    private static boolean DEFAULT_KEEP_ALIVE;
    private static boolean DEFAULT_OOB_INLINE;
    private static int DEFAULT_SO_LINGER;
    private static boolean DEFAULT_TCP_NO_DELAY;
    
    static
    {
        initialize();
    }
    
    private static void initialize()
    {
        Socket socket = null;
        
        socket = new Socket();

        try
        {
            DEFAULT_REUSE_ADDRESS = socket.getReuseAddress();
            DEFAULT_RECEIVE_BUFFER_SIZE = socket.getReceiveBufferSize();
            DEFAULT_SEND_BUFFER_SIZE = socket.getSendBufferSize();
            DEFAULT_KEEP_ALIVE = socket.getKeepAlive();
            DEFAULT_OOB_INLINE = socket.getOOBInline();
            DEFAULT_SO_LINGER = socket.getSoLinger();
            DEFAULT_TCP_NO_DELAY = socket.getTcpNoDelay();
            
            // Check if setReceiveBufferSize is supported.
            try
            {
                socket.setReceiveBufferSize(DEFAULT_RECEIVE_BUFFER_SIZE);
                SET_RECEIVE_BUFFER_SIZE_AVAILABLE = true;
            }
            catch( SocketException e )
            {
                SET_RECEIVE_BUFFER_SIZE_AVAILABLE = false;
            }
            
            // Check if setSendBufferSize is supported.
            try
            {
                socket.setSendBufferSize(DEFAULT_SEND_BUFFER_SIZE);
                SET_SEND_BUFFER_SIZE_AVAILABLE = true;
            }
            catch( SocketException e )
            {
                SET_SEND_BUFFER_SIZE_AVAILABLE = false;
            }

            // Check if getTrafficClass is supported.
            try
            {
                DEFAULT_TRAFFIC_CLASS = socket.getTrafficClass();
                GET_TRAFFIC_CLASS_AVAILABLE = true;
            }
            catch( SocketException e )
            {
                GET_TRAFFIC_CLASS_AVAILABLE = false;
                DEFAULT_TRAFFIC_CLASS = 0;
            }
        }
        catch( SocketException e )
        {
            throw new ExceptionInInitializerError(e);
        }
        finally
        {
            if( socket != null )
            {
                try
                {
                    socket.close();
                }
                catch( IOException e )
                {
                    ExceptionMonitor.getInstance().exceptionCaught(e);
                }
            }
        }
    }
    
    public static boolean isSetReceiveBufferSizeAvailable() {
        return SET_RECEIVE_BUFFER_SIZE_AVAILABLE;
    }
    
    public static boolean isSetSendBufferSizeAvailable() {
        return SET_SEND_BUFFER_SIZE_AVAILABLE;
    }
    
    public static boolean isGetTrafficClassAvailable() {
        return GET_TRAFFIC_CLASS_AVAILABLE;
    }
    
    public static boolean isSetTrafficClassAvailable() {
        return SET_TRAFFIC_CLASS_AVAILABLE;
    }
    
    private boolean reuseAddress = DEFAULT_REUSE_ADDRESS;
    private int receiveBufferSize = DEFAULT_RECEIVE_BUFFER_SIZE;
    private int sendBufferSize = DEFAULT_SEND_BUFFER_SIZE;
    private int trafficClass = DEFAULT_TRAFFIC_CLASS;
    private boolean keepAlive = DEFAULT_KEEP_ALIVE;
    private boolean oobInline = DEFAULT_OOB_INLINE;
    private int soLinger = DEFAULT_SO_LINGER;
    private boolean tcpNoDelay = DEFAULT_TCP_NO_DELAY;

    /**
     * Creates a new instance.
     */
    public SocketSessionConfigImpl()
    {
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
