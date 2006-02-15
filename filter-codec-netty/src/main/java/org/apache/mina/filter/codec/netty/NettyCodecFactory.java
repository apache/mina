/*
 * @(#) $Id: NettyCodecFactory.java 4 2005-04-18 03:04:09Z trustin $
 */
package org.apache.mina.filter.codec.netty;

import net.gleamynode.netty2.Message;
import net.gleamynode.netty2.MessageRecognizer;

/**
 * A MINA <tt>ProtocolCodecFactory</tt> that provides encoder and decoder
 * for Netty2 {@link Message}s and {@link MessageRecognizer}s.
 * <p>
 * Please note that this codec factory assumes one {@link MessageRecognizer}
 * can be used for multiple sessions.  If not, you'll have to create your
 * own factory after this factory.
 *
 * (Julien Vermillard) Migrated to 0.9
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev: 4 $, $Date: 2005-04-18 12:04:09 +0900 (월, 18  4월 2005) $,
 */
public class NettyCodecFactory implements org.apache.mina.filter.codec.ProtocolCodecFactory {

    private static final NettyEncoder ENCODER = new NettyEncoder();

    private final MessageRecognizer recognizer;
    
    public NettyCodecFactory(MessageRecognizer recognizer) {
        this.recognizer = recognizer;
    }


	public org.apache.mina.filter.codec.ProtocolEncoder getEncoder() {
		return ENCODER;
	}

	public org.apache.mina.filter.codec.ProtocolDecoder getDecoder() {
		return new NettyDecoder(recognizer);
	}
}
