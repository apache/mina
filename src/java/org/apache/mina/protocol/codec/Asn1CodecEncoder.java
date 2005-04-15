/*
 * @(#) $Id$
 */
package org.apache.mina.protocol.codec;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;

import org.apache.asn1.codec.EncoderException;
import org.apache.asn1.codec.stateful.EncoderCallback;
import org.apache.asn1.codec.stateful.StatefulEncoder;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.protocol.ProtocolEncoder;
import org.apache.mina.protocol.ProtocolEncoderOutput;
import org.apache.mina.protocol.ProtocolSession;
import org.apache.mina.protocol.ProtocolViolationException;

/**
 * A wrapper for {@link StatefulEncoder} from 
 * <a href="http://incubator.apache.org/directory/subprojects/asn1/codec-stateful/">Apache ASN.1 Codec</a>.
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

    public void encode( ProtocolSession session, Object message,
                       ProtocolEncoderOutput out )
            throws ProtocolViolationException
    {
        callback.encOut = out;
        try
        {
            encoder.encode( message );
        }
        catch( EncoderException e )
        {
            throw new ProtocolViolationException( "Encoding failed.", e );
        }
    }

    private class EncoderCallbackImpl implements EncoderCallback
    {
        private ProtocolEncoderOutput encOut;

        public void encodeOccurred( StatefulEncoder codec, Object encoded )
        {
            if( encoded instanceof java.nio.ByteBuffer )
            {
                java.nio.ByteBuffer buf = ( java.nio.ByteBuffer ) encoded;
                encOut.write( ByteBuffer.wrap( buf ) );
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
