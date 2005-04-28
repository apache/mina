package org.apache.mina.io;

import org.apache.mina.common.ByteBuffer;

/**
 * An {@link IoHandlerFilterChain} for datagram transport (UDP/IP).
 * 
 * @author The Apache Directory Project
 */
public class IoSessionFilterChain extends AbstractIoHandlerFilterChain {

    private final IoSessionManagerFilterChain prevChain;

    public IoSessionFilterChain( IoSessionManagerFilterChain prevChain )
    {
        this.prevChain = prevChain;
    }

    protected void doWrite( IoSession session, ByteBuffer buf, Object marker )
    {
        prevChain.filterWrite( session, buf, marker );
    }
}
