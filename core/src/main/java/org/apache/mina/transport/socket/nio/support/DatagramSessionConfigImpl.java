/*
 * @(#) $Id$
 */
package org.apache.mina.transport.socket.nio.support;

import java.net.DatagramSocket;
import java.net.SocketException;

import org.apache.mina.common.RuntimeIOException;
import org.apache.mina.transport.socket.nio.DatagramSessionConfig;

public class DatagramSessionConfigImpl implements DatagramSessionConfig
{
    private boolean broadcast;
    private boolean reuseAddress;
    private int receiveBufferSize;
    private int sendBufferSize;
    private int trafficClass;

    /**
     * Creates a new instance.
     * 
     * @throws RuntimeIOException if failed to get the default configuration
     */
    public DatagramSessionConfigImpl()
    {
        DatagramSocket s = null;
        try
        {
            s = new DatagramSocket();
            broadcast = s.getBroadcast();
            reuseAddress = s.getReuseAddress();
            receiveBufferSize = s.getReceiveBufferSize();
            sendBufferSize = s.getSendBufferSize();
            trafficClass = s.getTrafficClass();
        }
        catch( SocketException e )
        {
            throw new RuntimeIOException( "Failed to get the default configuration.", e );
        }
        finally
        {
            if( s != null )
            {
                s.close();
            }
        }
    }

    /**
     * @see DatagramSocket#getBroadcast()
     */
    public boolean isBroadcast()
    {
        return broadcast;
    }
    
    /**
     * @see DatagramSocket#setBroadcast(boolean)
     */
    public void setBroadcast( boolean broadcast )
    {
        this.broadcast = broadcast;
    }
    
    /**
     * @see DatagramSocket#getReuseAddress()
     */
    public boolean isReuseAddress()
    {
        return reuseAddress;
    }
    
    /**
     * @see DatagramSocket#setReuseAddress(boolean)
     */
    public void setReuseAddress( boolean reuseAddress )
    {
        this.reuseAddress = reuseAddress;
    }

    /**
     * @see DatagramSocket#getReceiveBufferSize()
     */
    public int getReceiveBufferSize()
    {
        return receiveBufferSize;
    }

    /**
     * @see DatagramSocket#setReceiveBufferSize(int)
     */
    public void setReceiveBufferSize( int receiveBufferSize )
    {
        this.receiveBufferSize = receiveBufferSize;
    }

    /**
     * @see DatagramSocket#getSendBufferSize()
     */
    public int getSendBufferSize()
    {
        return sendBufferSize;
    }

    /**
     * @see DatagramSocket#setSendBufferSize(int)
     */
    public void setSendBufferSize( int sendBufferSize )
    {
        this.sendBufferSize = sendBufferSize;
    }

    /**
     * @see DatagramSocket#getTrafficClass()
     */
    public int getTrafficClass()
    {
        return trafficClass;
    }

    /**
     * @see DatagramSocket#setTrafficClass(int)
     */
    public void setTrafficClass( int trafficClass )
    {
        this.trafficClass = trafficClass;
    }
}