package org.apache.mina.example.imagine.step1.codec;

import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.common.IoSession;

public class ImageCodecFactory implements ProtocolCodecFactory {
    private ProtocolEncoder encoder;
    private ProtocolDecoder decoder;

    public ImageCodecFactory(boolean client) {
        if (client) {
            encoder = new ImageRequestEncoder();
            decoder = new ImageResponseDecoder();
        } else {
            encoder = new ImageResponseEncoder();
            decoder = new ImageRequestDecoder();
        }
    }

    public ProtocolEncoder getEncoder(IoSession ioSession) throws Exception {
        return encoder;
    }

    public ProtocolDecoder getDecoder(IoSession ioSession) throws Exception {
        return decoder;
    }
}
