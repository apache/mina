/*
 * @(#) $Id$
 */
package org.apache.mina.filter.codec.asn1;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;

import org.apache.asn1.codec.EncoderException;
import org.apache.asn1.codec.stateful.EncoderCallback;
import org.apache.asn1.codec.stateful.StatefulEncoder;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

/**
 * Adapts {@link StatefulEncoder} to MINA <tt>ProtocolEncoder</tt>
 * 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$, 
 */
public class Asn1CodecEncoder implements ProtocolEncoder
{
    private final StatefulEncoder encoder;

    private final EncoderCallbackImpl callback = new EncoderCallbackImpl();

    public Asn1CodecEncoder( StatefulEncoder encoder )
    {
        encoder.setCallback( callback );
        this.encoder = encoder;
    }

    public void encode( IoSession session, Object message,
                        ProtocolEncoderOutput out ) throws EncoderException
    {
        callback.encOut = out;
        encoder.encode( message );
    }

    public void dispose( IoSession session ) throws Exception
    {
    }

    private class EncoderCallbackImpl implements EncoderCallback
    {
        private ProtocolEncoderOutput encOut;

        public void encodeOccurred( StatefulEncoder codec, Object encoded )
        {
            if( encoded instanceof java.nio.ByteBuffer )
            {
                java.nio.ByteBuffer buf = ( java.nio.ByteBuffer ) encoded;
                ByteBuffer wrappedBuf = ByteBuffer.wrap( buf );
                wrappedBuf.acquire();  // acquire once more to prvent leak
                encOut.write( wrappedBuf );
            }
            else if( encoded instanceof Object[] )
            {
                Object[] bufArray = ( Object[] ) encoded;
                for( int i = 0; i < bufArray.length; i ++ )
                {
                    this.encodeOccurred( codec, bufArray[ i ] );
                }

                encOut.mergeAll();
            }
            else if( encoded instanceof Iterator )
            {
                Iterator it = ( Iterator ) encoded;
                while( it.hasNext() )
                {
                    this.encodeOccurred( codec, it.next() );
                }
                
                encOut.mergeAll();
            }
            else if( encoded instanceof Collection )
            {
                Iterator it = ( ( Collection ) encoded ).iterator();
                while( it.hasNext() )
                {
                    this.encodeOccurred( codec, it.next() );
                }
                
                encOut.mergeAll();
            }
            else if( encoded instanceof Enumeration )
            {
                Enumeration e = ( Enumeration ) encoded;
                while( e.hasMoreElements() )
                {
                    this.encodeOccurred( codec, e.nextElement() );
                }
                
                encOut.mergeAll();
            }
            else
            {
                throw new IllegalArgumentException(
                        "Encoded result is not a ByteBuffer: " +
                        encoded.getClass() );
            }
        }
    }
}
