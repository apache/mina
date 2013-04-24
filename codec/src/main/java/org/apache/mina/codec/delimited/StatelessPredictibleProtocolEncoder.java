package org.apache.mina.codec.delimited;

import java.nio.ByteBuffer;

import org.apache.mina.codec.StatelessProtocolEncoder;

public interface StatelessPredictibleProtocolEncoder<INPUT> extends StatelessProtocolEncoder<INPUT, ByteBuffer> {

    public int getEncodedSize(INPUT message);
    
    public void encodeTo(INPUT message, ByteBuffer buffer);
}
