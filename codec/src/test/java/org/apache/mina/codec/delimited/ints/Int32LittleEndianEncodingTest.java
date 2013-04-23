package org.apache.mina.codec.delimited.ints;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.apache.mina.codec.delimited.ints.Endianness;
import org.apache.mina.codec.delimited.ints.Int32Decoder;
import org.apache.mina.codec.delimited.ints.Int32Encoder;
import org.apache.mina.codec.delimited.ints.IntDecoder;
import org.apache.mina.codec.delimited.ints.IntEncoder;

public class Int32LittleEndianEncodingTest extends IntEncodingTest {

	@Override
	public IntDecoder newDecoderInstance() {
		return new Int32Decoder(Endianness.LITTLE);
	}

	@Override
	public IntEncoder newEncoderInstance() {
		return new Int32Encoder(Endianness.LITTLE);
	}

	@Override
	public Map<Integer, ByteBuffer> getEncodingSamples() {
		Map<Integer, ByteBuffer> map = new HashMap<Integer, ByteBuffer>();

		map.put(0, ByteBuffer.wrap(new byte[] { 0, 0, 0, 0 }));
		map.put(1 | 2 << 8 | 3 << 16 | 4 << 24,
				ByteBuffer.wrap(new byte[] { 1, 2, 3, 4 }));
		return map;
	}

}
