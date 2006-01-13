/*
 * @(#) $Id$
 */
package org.apache.asn1.codec.mina;

import org.apache.asn1.codec.DecoderException;
import org.apache.asn1.codec.stateful.DecoderCallback;
import org.apache.asn1.codec.stateful.StatefulDecoder;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;

/**
 * Adapts {@link StatefulDecoder} to MINA <tt>ProtocolDecoder</tt>
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

    public void decode( IoSession session, ByteBuffer in,
                        ProtocolDecoderOutput out ) throws DecoderException
    {
        callback.decOut = out;
        decoder.decode( in.buf() );
    }

    public void dispose( IoSession session ) throws Exception
    {
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