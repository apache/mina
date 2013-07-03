package org.apache.mina.codec.delimited.ints;

import org.apache.mina.codec.delimited.ByteBufferEncoder;
import org.apache.mina.codec.delimited.IoBufferDecoder;

public interface IntTranscoder {
    IoBufferDecoder<Integer> getDecoder();

    ByteBufferEncoder<Integer> getEncoder();
}
