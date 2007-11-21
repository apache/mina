package org.apache.mina.example.imagine.step1.codec;

import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.CumulativeProtocolDecoder;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.IoBuffer;
import org.apache.mina.example.imagine.step1.ImageResponse;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public class ImageResponseDecoder extends CumulativeProtocolDecoder {

    private static final String DECODER_STATE_KEY = ImageResponseDecoder.class.getName() + ".STATE";

    public static final int MAX_IMAGE_SIZE = 5 * 1024 * 1024;

    private static class DecoderState {
        BufferedImage image1;
    }

    protected boolean doDecode(IoSession session, IoBuffer in, ProtocolDecoderOutput out) throws Exception {
        DecoderState decoderState = (DecoderState) session.getAttribute(DECODER_STATE_KEY);
        if (decoderState == null) {
            decoderState = new DecoderState();
            session.setAttribute(DECODER_STATE_KEY, decoderState);
        }
        if (decoderState.image1 == null) {
            // try to read first image
            if (in.prefixedDataAvailable(4, MAX_IMAGE_SIZE)) {
                decoderState.image1 = readImage(in);
            } else {
                // not enough data available to read first image
                return false;
            }
        }
        if (decoderState.image1 != null) {
            // try to read second image
            if (in.prefixedDataAvailable(4, MAX_IMAGE_SIZE)) {
                BufferedImage image2 = readImage(in);
                ImageResponse imageResponse = new ImageResponse(decoderState.image1, image2);
                out.write(imageResponse);
                decoderState.image1 = null;
                return true;
            } else {
                // not enough data available to read second image
                return false;
            }
        }
        return false;
    }

    private BufferedImage readImage(IoBuffer in) throws IOException {
        int length = in.getInt();
        byte[] bytes = new byte[length];
        in.get(bytes);
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        return ImageIO.read(bais);
    }
    
}
