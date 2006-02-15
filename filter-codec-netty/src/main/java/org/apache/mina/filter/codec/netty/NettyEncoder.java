/*
 * @(#) $Id: NettyEncoder.java 4 2005-04-18 03:04:09Z trustin $
 */
package org.apache.mina.filter.codec.netty;

import net.gleamynode.netty2.Message;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoderException;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

/**
 * A MINA <tt>ProtocolEncoder</tt> that encodes Netty2 {@link Message}s
 * into byte buffers. 
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev: 4 $, $Date: 2005-04-18 12:04:09 +0900 (월, 18  4월 2005) $,
 */
public class NettyEncoder implements org.apache.mina.filter.codec.ProtocolEncoder
{
    /**
     * Creates a new instance.
     */
    public NettyEncoder()
    {
    }

	public void encode(IoSession session, Object message, ProtocolEncoderOutput out) throws Exception {
	       if( ! ( message instanceof Message ) )
	        {
	            throw new ProtocolEncoderException("This encoder can decode only Netty Messages." );
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

	public void dispose(IoSession session) throws Exception {
		
	}
}