/*
 * @(#) $Id$
 */
package org.apache.mina.protocol.codec;

import net.gleamynode.netty2.Message;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.protocol.ProtocolEncoder;
import org.apache.mina.protocol.ProtocolEncoderOutput;
import org.apache.mina.protocol.ProtocolSession;
import org.apache.mina.protocol.ProtocolViolationException;

/**
 * Encodes Trustin Lee's
 * <a href="http://gleamynode.net/dev/projects/netty2/">Netty</code>
 * {@link Message}s. 
 * 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$,
 */
public class NettyEncoder implements ProtocolEncoder
{

    public NettyEncoder()
    {
    }

    public void encode( ProtocolSession session, Object message,
                       ProtocolEncoderOutput out )
            throws ProtocolViolationException
    {
        if( ! ( message instanceof Message ) )
        {
            throw new ProtocolViolationException(
                                                  "This encoder can decode only Netty Messages." );
        }

        for( ;; )
        {
            ByteBuffer buf = ByteBuffer.allocate( 8192 );
            Message m = ( Message ) message;
            try
            {
                if( m.write( buf.buf() ) )
                {
                    break;
                }
            }
            finally
            {
                buf.flip();
                if( buf.hasRemaining() )
                {
                    out.write( buf );
                }
                else
                {
                    buf.release();
                }
            }
        }
    }
}