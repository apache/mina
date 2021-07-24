package org.apache.mina.filter.ssl2;

import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.write.DefaultWriteRequest;
import org.apache.mina.core.write.WriteRequest;

public class EncryptedWriteRequest extends DefaultWriteRequest {

	// The original message
	private WriteRequest parentRequest;

	public EncryptedWriteRequest(Object encodedMessage, WriteRequest parent) {
		super(encodedMessage, null);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isEncoded() {
		return true;
	}

	public WriteRequest getParentRequest() {
		return this.parentRequest;
	}

	@Override
	public WriteFuture getFuture() {
		return (this.getParentRequest() != null) ? this.getParentRequest().getFuture() : super.getFuture();
	}
}