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
 * {@link ProtocolProvider} for SumUp server.
 * 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$, 
 */
public class ServerProtocolProvider implements ProtocolProvider
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
                                                                 SumUpMessageRecognizer.SERVER_MODE ) );
        }
    };

    private static final ProtocolHandler HANDLER = new ServerSessionHandler();

    public ServerProtocolProvider()
    {
    }

    public ProtocolCodecFactory getCodecFactory()
    {
        return CODEC_FACTORY;
    }

    public ProtocolHandler getHandler()
    {
        return HANDLER;
    }
}