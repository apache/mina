package org.apache.mina.codec.delimited;

import java.nio.ByteBuffer;

import org.apache.mina.codec.ProtocolDecoder;
import org.apache.mina.codec.ProtocolDecoderException;
import org.apache.mina.codec.StatelessProtocolDecoder;

public class SizePrefixedDecoder
		implements
		ProtocolDecoder<ByteBuffer, ByteBuffer, SizePrefixedDecoder.IntRef> {

	final static protected class IntRef {
		private Integer value = null;

		public Integer get() {
			return value;
		}

		public void reset() {
			value = null;
		}

		public boolean isDefined() {
			return value != null;
		}

		public void set(Integer value) {
			this.value = value;
		}
	}

	final private StatelessProtocolDecoder<ByteBuffer, Integer> intDecoder;

	public SizePrefixedDecoder(
			StatelessProtocolDecoder<ByteBuffer, Integer> intDecoder) {
		super();
		this.intDecoder = intDecoder;
	}

	@Override
	public IntRef createDecoderState() {
		return new IntRef();
	}

	@Override
	public ByteBuffer decode(ByteBuffer input, IntRef nextBlockSize)
			throws ProtocolDecoderException {
		ByteBuffer output = null;
		if (nextBlockSize.get() == null) {
			nextBlockSize.set(intDecoder.decode(input, null));
		}

		if (nextBlockSize.isDefined()) {
			if (input.remaining() >= nextBlockSize.get()) {
				output = input.slice();
				output.limit(output.position() + nextBlockSize.get());
				nextBlockSize.reset();
			}
		}
		return output;
	}

	@Override
	public void finishDecode(IntRef context) {
		//
	}

}
