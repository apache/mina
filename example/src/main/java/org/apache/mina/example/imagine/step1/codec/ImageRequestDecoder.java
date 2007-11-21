package org.apache.mina.example.imagine.step1.codec;

import org.apache.mina.common.IoBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.example.imagine.step1.ImageRequest;
import org.apache.mina.filter.codec.CumulativeProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;

public class ImageRequestDecoder extends CumulativeProtocolDecoder {
    
    protected boolean doDecode(IoSession session, IoBuffer in, ProtocolDecoderOutput out) throws Exception {
        if (in.remaining() >= 12) {
            int width = in.getInt();
            int height = in.getInt();
            int numberOfCharachters = in.getInt();
            ImageRequest request = new ImageRequest(width, height, numberOfCharachters);
            out.write(request);
            return true;
        } else {
            return false;
        }
    }
}
