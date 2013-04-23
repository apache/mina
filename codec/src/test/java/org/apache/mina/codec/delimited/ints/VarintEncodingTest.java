package org.apache.mina.codec.delimited.ints;

import static org.junit.Assert.fail;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.apache.mina.codec.ProtocolDecoderException;
import org.junit.Test;

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
				ByteBuffer.wrap(new byte[] { 1 | (byte) 0x80, 2 | (byte) 0x80,
						3 | (byte) 0x80, 4 | (byte) 0x80, 5 }));
		return map;
	}

	@Test
	public void testOverflow() {		
		ByteBuffer buffer = ByteBuffer.wrap(new byte[] { 0 | (byte) 0x80,
				0 | (byte) 0x80, 0 | (byte) 0x80, 0 | (byte) 0x80, 1<<4 });

		try {
			decoder.decode(buffer, null);
			fail("Should throw an overflow exception");
		} catch (ProtocolDecoderException e) {
			// fine
		}
	}
}
