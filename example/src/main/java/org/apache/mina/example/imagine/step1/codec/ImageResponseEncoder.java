package org.apache.mina.example.imagine.step1.codec;

import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.apache.mina.filter.codec.ProtocolEncoderAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.IoBuffer;
import org.apache.mina.example.imagine.step1.ImageResponse;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ImageResponseEncoder extends ProtocolEncoderAdapter {

    public void encode(IoSession session, Object message, ProtocolEncoderOutput out) throws Exception {
        ImageResponse imageResponse = (ImageResponse) message;
        byte[] bytes1 = getBytes(imageResponse.getImage1());
        byte[] bytes2 = getBytes(imageResponse.getImage2());
        int capacity = bytes1.length + bytes2.length + 8;
        IoBuffer buffer = IoBuffer.allocate(capacity, false);
        buffer.setAutoExpand(true);
        buffer.putInt(bytes1.length);
        buffer.put(bytes1);
        buffer.putInt(bytes2.length);
        buffer.put(bytes2);
        buffer.flip();
        out.write(buffer);
    }

    private byte[] getBytes(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", baos);
        return baos.toByteArray();
    }
}
