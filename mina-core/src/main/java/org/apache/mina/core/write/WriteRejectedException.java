package org.apache.mina.core.write;

import java.util.Collection;

public class WriteRejectedException extends WriteException {
	private static final long serialVersionUID = 6272160412793858438L;

	/**
	 * Create a new WriteRejectedException instance
	 * 
	 * @param requests The {@link WriteRequest} which has been rejected
	 * @param message  The error message
	 */
	public WriteRejectedException(WriteRequest requests, String message) {
		super(requests, message);
	}

	/**
	 * Create a new WriteRejectedException instance
	 * 
	 * @param requests The {@link WriteRequest} which has been rejected
	 */
	public WriteRejectedException(WriteRequest requests) {
		super(requests);
	}

	/**
	 * Create a new WriteRejectedException instance
	 * 
	 * @param requests The {@link WriteRequest} which has been rejected
	 */
	public WriteRejectedException(Collection<WriteRequest> requests) {
		super(requests);
	}
	
	/**
	 * Create a new WriteRejectedException instance
	 * 
	 * @param requests The {@link WriteRequest} which has been rejected
	 */
	public WriteRejectedException(Collection<WriteRequest> requests, String message) {
		super(requests, message);
	}
}
