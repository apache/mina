package org.apache.mina.codec.delimited;

import java.nio.ByteBuffer;

import org.apache.mina.codec.delimited.ints.VarIntTranscoder;
import org.junit.Test;

public class TestSizePrefixedEncoder {
    @Test
    public void test() {
        SizePrefixedEncoder spe = new SizePrefixedEncoder(new VarIntTranscoder());
        spe.encode(ByteBuffer.wrap("array".getBytes()),null);
    }
}
