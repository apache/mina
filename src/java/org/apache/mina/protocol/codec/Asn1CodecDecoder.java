/*
 * @(#) $Id$
 */
package org.apache.mina.protocol.codec;

import org.apache.asn1.codec.DecoderException;
import org.apache.asn1.codec.stateful.DecoderCallback;
import org.apache.asn1.codec.stateful.StatefulDecoder;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.protocol.ProtocolDecoder;
import org.apache.mina.protocol.ProtocolDecoderOutput;
import org.apache.mina.protocol.ProtocolSession;
import org.apache.mina.protocol.ProtocolViolationException;

/**
 * A wrapper for {@link StatefulDecoder} from 
 * <a href="http://directory.apache.org/subprojects/asn1/codec-stateful/">Apache ASN.1 Codec</a>.
 * 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$, 
 */
public class Asn1CodecDecoder implements ProtocolDecoder
{

    private final StatefulDecoder decoder;

    private final DecoderCallbackImpl callback = new DecoderCallbackImpl();

    public Asn1CodecDecoder( StatefulDecoder decoder )
    {
        decoder.setCallback( callback );
        this.decoder = decoder;
    }

    public void decode( ProtocolSession session, ByteBuffer in,
                       ProtocolDecoderOutput out )
            throws ProtocolViolationException
    {
        callback.decOut = out;
        try
        {
            decoder.decode( in.buf() );
        }
        catch( DecoderException e )
        {
            throw new ProtocolViolationException( "Failed to decode.", e );
        }
    }

    private class DecoderCallbackImpl implements DecoderCallback
    {
        private ProtocolDecoderOutput decOut;

        public void decodeOccurred( StatefulDecoder decoder, Object decoded )
        {
            decOut.write( decoded );
        }
    }
}