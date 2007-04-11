package org.apache.mina.common;

import java.net.SocketAddress;

/**
 * Represents write request fired by {@link IoSession#write(Object)}.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public interface WriteRequest
{
    /**
     * Returns {@link WriteFuture} that is associated with this write request.
     */
    WriteFuture getFuture();

    /**
     * Returns a message object to be written.
     */
    Object getMessage();
    
    /**
     * Returne the destination of this write request.
     * 
     * @return <tt>null</tt> for the default destination
     */
    SocketAddress getDestination();
}