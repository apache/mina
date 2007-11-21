package org.apache.mina.example.imagine.step1.codec;

import org.apache.mina.common.IoBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.example.imagine.step1.ImageRequest;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

public class ImageRequestEncoder implements ProtocolEncoder {

    public void encode(IoSession session, Object message, ProtocolEncoderOutput out) throws Exception {
        ImageRequest request = (ImageRequest) message;
        IoBuffer buffer = IoBuffer.allocate(12, false);
        buffer.putInt(request.getWidth());
        buffer.putInt(request.getHeight());
        buffer.putInt(request.getNumberOfCharacters());
        buffer.flip();
        out.write(buffer);
    }

    public void dispose(IoSession session) throws Exception {
        // nothing to dispose
    }
}
