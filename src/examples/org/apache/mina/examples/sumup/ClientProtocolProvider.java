/*
 * @(#) $Id$
 */
package org.apache.mina.examples.sumup;

import org.apache.mina.protocol.ProtocolCodecFactory;
import org.apache.mina.protocol.ProtocolDecoder;
import org.apache.mina.protocol.ProtocolEncoder;
import org.apache.mina.protocol.ProtocolHandler;
import org.apache.mina.protocol.ProtocolProvider;
import org.apache.mina.protocol.codec.NettyDecoder;
import org.apache.mina.protocol.codec.NettyEncoder;

/**
 * {@link ProtocolProvider} for SumUp client.
 * 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$, 
 */
public class ClientProtocolProvider implements ProtocolProvider
{

    private static final ProtocolCodecFactory CODEC_FACTORY = new ProtocolCodecFactory()
    {

        public ProtocolEncoder newEncoder()
        {
            return new NettyEncoder();
        }

        public ProtocolDecoder newDecoder()
        {
            return new NettyDecoder(
                                     new SumUpMessageRecognizer(
                                                                 SumUpMessageRecognizer.CLIENT_MODE ) );
        }
    };

    private final ProtocolHandler handler;

    public ClientProtocolProvider( int[] values )
    {
        handler = new ClientSessionHandler( values );
    }

    public ProtocolCodecFactory getCodecFactory()
    {
        return CODEC_FACTORY;
    }

    public ProtocolHandler getHandler()
    {
        return handler;
    }
}