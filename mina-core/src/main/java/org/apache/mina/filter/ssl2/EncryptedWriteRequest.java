package org.apache.mina.filter.ssl2;

import org.apache.mina.core.write.DefaultWriteRequest;
import org.apache.mina.core.write.WriteRequest;

public class EncryptedWriteRequest extends DefaultWriteRequest {

	// The original message
	private WriteRequest originalRequest;

	public EncryptedWriteRequest(Object encodedMessage, WriteRequest parent) {
		super(encodedMessage, parent != null ? parent.getFuture() : null);
		this.originalRequest = parent != null ? parent : this;
	}

	public WriteRequest getOriginalRequest() {
		return this.originalRequest;
	}
}