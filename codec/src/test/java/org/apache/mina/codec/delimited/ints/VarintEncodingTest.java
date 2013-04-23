package org.apache.mina.codec.delimited.ints;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class VarintEncodingTest extends IntEncodingTest {

    @Override
    public IntDecoder newDecoderInstance() {
        return new VarIntDecoder();
    }

    @Override
    public IntEncoder newEncoderInstance() {
        return new VarIntEncoder();
    }

    @Override
    public Map<Integer, ByteBuffer> getEncodingSamples() {
        Map<Integer, ByteBuffer> map = new HashMap<Integer, ByteBuffer>();

        map.put(0, ByteBuffer.wrap(new byte[] { 0 }));
        map.put(1 | 2 << 7 | 3 << 14 | 4 << 21 | 5 << 28,
                ByteBuffer.wrap(new byte[] { 1 | (byte) 0x80, 2 | (byte) 0x80, 3 | (byte) 0x80, 4 | (byte) 0x80, 5 }));
        return map;
    }

    @Override
    public Iterable<ByteBuffer> getIllegalBuffers() {
        List<ByteBuffer> list = new LinkedList<ByteBuffer>();
        list.add(ByteBuffer.wrap(new byte[] { 0 | (byte) 0x80, 0 | (byte) 0x80, 0 | (byte) 0x80, 0 | (byte) 0x80,
                1 << 4 }));
        return list;
    }

}
