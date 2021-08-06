package org.apache.mina.filter.ssl2;

import java.nio.BufferOverflowException;
import java.util.ArrayList;
import java.util.concurrent.Executor;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilter.NextFilter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRejectedException;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.filter.ssl.SslEvent;

public class SSL2HandlerG0 extends SSL2Handler {

	/**
	 * Maximum number of queued messages waiting for encoding
	 */
	static protected final int MAX_QUEUED_MESSAGES = 64;

	/**
	 * Maximum number of messages waiting acknowledgement
	 */
	static protected final int MAX_UNACK_MESSAGES = 6;

	/**
	 * Enable aggregation of handshake messages
	 */
	static protected final boolean ENABLE_FAST_HANDSHAKE = true;

	/**
	 * Enable asynchronous tasks
	 */
	static protected final boolean ENABLE_ASYNC_TASKS = true;

	/**
	 * Indicates whether the first handshake was completed
	 */
	protected boolean mHandshakeComplete = false;

	/**
	 * Indicated whether the first handshake was started
	 */
	protected boolean mHandshakeStarted = false;

	/**
	 * Indicates that the outbound is closing
	 */
	protected boolean mOutboundClosing = false;

	/**
	 * Indicates that previously queued messages should be written before closing
	 */
	protected boolean mOutboundLinger = false;

	/**
	 * Holds the decoder thread reference; used for recursion detection
	 */
	protected Thread mDecodeThread = null;

	/**
	 * Instantiates a new handler
	 * 
	 * @param p engine
	 * @param e executor
	 * @param s session
	 */
	public SSL2HandlerG0(SSLEngine p, Executor e, IoSession s) {
		super(p, e, s);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isOpen() {
		return this.mEngine.isOutboundDone() == false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isConnected() {
		return this.mHandshakeComplete && isOpen();
	}

	/**
	 * {@inheritDoc}
	 */
	synchronized public void open(final NextFilter next) throws SSLException {
		if (this.mHandshakeStarted == false) {
			this.mHandshakeStarted = true;
			if (this.mEngine.getUseClientMode()) {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("{} open() - begin handshaking", toString());
				}
				this.mEngine.beginHandshake();
				this.write(next);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	synchronized public void receive(final NextFilter next, final IoBuffer message) throws SSLException {
		if (this.mDecodeThread == null) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("{} receive() - message {}", toString(), message);
			}
			this.mDecodeThread = Thread.currentThread();
			final IoBuffer source = resume_decode_buffer(message);
			try {
				this.qreceive(next, source);
			} finally {
				suspend_decode_buffer(source);
				this.mDecodeThread = null;
			}
		} else {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("{} receive() - recursion", toString());
			}
			this.qreceive(next, this.mDecodeBuffer);
		}
	}

	/**
	 * Process a received message
	 * 
	 * @param next
	 * @param message
	 * 
	 * @throws SSLException
	 */
	protected void qreceive(final NextFilter next, final IoBuffer message) throws SSLException {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("{} qreceive() - source {}", toString(), message);
		}

		final IoBuffer source = message;
		final IoBuffer dest = allocate_app_buffer(source.remaining());

		final SSLEngineResult result = mEngine.unwrap(source.buf(), dest.buf());

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("{} qreceive() - bytes-consumed {}, bytes-produced {}, status {}, handshake {}", toString(),
					result.bytesConsumed(), result.bytesProduced(), result.getStatus(), result.getHandshakeStatus());
		}

		if (result.bytesProduced() == 0) {
			dest.free();
		} else {
			dest.flip();
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("{} qreceive() - result {}", toString(), dest);
			}
			next.messageReceived(this.mSession, dest);
		}

		switch (result.getHandshakeStatus()) {
			case NEED_UNWRAP:
			case NEED_UNWRAP_AGAIN:
				if (result.bytesConsumed() != 0 && message.hasRemaining()) {
					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug("{} qreceive() - handshake needs unwrap, looping", toString());
					}
					this.qreceive(next, message);
				}
				break;
			case NEED_TASK:
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("{} qreceive() - handshake needs task, scheduling", toString());
				}
				this.schedule_task(next);
				break;
			case NEED_WRAP:
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("{} qreceive() - handshake needs wrap, invoking write", toString());
				}
				this.write(next);
				break;
			case FINISHED:
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("{} qreceive() - handshake finished, flushing queue", toString());
				}
				this.lfinish(next);
				break;
			case NOT_HANDSHAKING:
				if ((result.bytesProduced() != 0 || result.bytesConsumed() != 0) && message.hasRemaining()) {
					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug("{} qreceive() - trying to decode more messages, looping", toString());
					}
					this.qreceive(next, message);
				}
				break;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	synchronized public void ack(final NextFilter next, final WriteRequest request) throws SSLException {
		if (this.mAckQueue.remove(request)) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("{} ack() - {}", toString(), request);
			}
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("{} ack() - checking to see if any messages can be flushed", toString(), request);
			}
			this.flush(next);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	synchronized public void write(final NextFilter next, final WriteRequest request)
			throws SSLException, WriteRejectedException {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("{} write() - source {}", toString(), request);
		}

		if (this.mOutboundClosing) {
			throw new WriteRejectedException(request, "closing");
		}

		if (this.mEncodeQueue.isEmpty()) {
			if (qwrite(next, request) == false) {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("{} write() - unable to write right now, saving request for later", toString(),
							request);
				}
				if (this.mEncodeQueue.size() == MAX_QUEUED_MESSAGES) {
					throw new BufferOverflowException();
				}
				this.mEncodeQueue.add(request);
			}
		} else {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("{} write() - unable to write right now, saving request for later", toString(), request);
			}
			if (this.mEncodeQueue.size() == MAX_QUEUED_MESSAGES) {
				throw new BufferOverflowException();
			}
			this.mEncodeQueue.add(request);
		}
	}

	/**
	 * Attempts to encode the WriteRequest and write the data to the IoSession
	 * 
	 * @param next
	 * @param request
	 * 
	 * @return {@code true} if the WriteRequest was fully consumed; otherwise
	 *         {@code false}
	 * 
	 * @throws SSLException
	 */
	@SuppressWarnings("incomplete-switch")
	synchronized protected boolean qwrite(final NextFilter next, final WriteRequest request) throws SSLException {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("{} qwrite() - source {}", toString(), request);
		}

		final IoBuffer source = IoBuffer.class.cast(request.getMessage());
		final IoBuffer dest = allocate_encode_buffer(source.remaining());

		final SSLEngineResult result = this.mEngine.wrap(source.buf(), dest.buf());

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("{} qwrite() - bytes-consumed {}, bytes-produced {}, status {}, handshake {}", toString(),
					result.bytesConsumed(), result.bytesProduced(), result.getStatus(), result.getHandshakeStatus());
		}

		if (result.bytesProduced() == 0) {
			dest.free();
		} else {
			if (result.bytesConsumed() == 0) {
				// an handshaking message must have been produced
				EncryptedWriteRequest encrypted = new EncryptedWriteRequest(dest, null);
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("{} qwrite() - result {}", toString(), encrypted);
				}
				next.filterWrite(this.mSession, encrypted);
				// do not return because we want to enter the handshake switch
			} else {
				// then we probably consumed some data
				dest.flip();
				if (source.hasRemaining()) {
					EncryptedWriteRequest encrypted = new EncryptedWriteRequest(dest, null);
					this.mAckQueue.add(encrypted);
					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug("{} qwrite() - result {}", toString(), encrypted);
					}
					next.filterWrite(this.mSession, encrypted);
					if (this.mAckQueue.size() < MAX_UNACK_MESSAGES) {
						return qwrite(next, request); // write additional chunks
					}
					return false;
				} else {
					EncryptedWriteRequest encrypted = new EncryptedWriteRequest(dest, request);
					this.mAckQueue.add(encrypted);
					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug("{} qwrite() - result {}", toString(), encrypted);
					}
					next.filterWrite(this.mSession, encrypted);
					return true;
				}
				// we return because there is not reason to enter the handshake switch
			}
		}

		switch (result.getHandshakeStatus()) {
			case NEED_TASK:
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("{} qwrite() - handshake needs task, scheduling", toString());
				}
				this.schedule_task(next);
				break;
			case NEED_WRAP:
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("{} qwrite() - handshake needs wrap, looping", toString());
				}
				return this.qwrite(next, request);
			case FINISHED:
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("{} qwrite() - handshake finished, flushing queue", toString());
				}
				this.lfinish(next);
				return this.qwrite(next, request);
		}

		return false;
	}

	/**
	 * Attempts to generate a handshake message and write the data to the IoSession
	 * 
	 * @param next
	 * 
	 * @return {@code true} if a message was generated and written
	 * 
	 * @throws SSLException
	 */
	synchronized public boolean write(NextFilter next) throws SSLException {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("{} write() - internal", toString());
		}

		final IoBuffer source = ZERO;
		final IoBuffer dest = allocate_encode_buffer(source.remaining());
		return lwrite(next, source, dest);
	}

	/**
	 * Attempts to generate a handshake message and write the data to the IoSession.
	 * <p>
	 * If FAST_HANDSHAKE is enabled, this method will recursively loop in order to
	 * combine multiple messages into one buffer.
	 * 
	 * @param next
	 * @param source
	 * @param dest
	 * 
	 * @return {@code true} if a message was generated and written
	 * 
	 * @throws SSLException
	 */
	@SuppressWarnings("incomplete-switch")
	protected boolean lwrite(NextFilter next, IoBuffer source, IoBuffer dest) throws SSLException {
		if (this.mOutboundClosing && this.mEngine.isOutboundDone()) {
			return false;
		}

		final SSLEngineResult result = this.mEngine.wrap(source.buf(), dest.buf());

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("{} lwrite() - bytes-consumed {}, bytes-produced {}, status {}, handshake {}", toString(),
					result.bytesConsumed(), result.bytesProduced(), result.getStatus(), result.getHandshakeStatus());
		}

		if (ENABLE_FAST_HANDSHAKE) {
			/**
			 * Fast handshaking allows multiple handshake messages to be written to a single
			 * buffer. This reduces the number of network messages used during the handshake
			 * process.
			 * 
			 * Additional handshake messages are only written if a message was produced in
			 * the last loop otherwise any additional messages need to be written by
			 * NEED_WRAP will be handled in the standard routine below which allocates a new
			 * buffer.
			 */
			switch (result.getHandshakeStatus()) {
				case NEED_WRAP:
					switch (result.getStatus()) {
						case OK:
							if (LOGGER.isDebugEnabled()) {
								LOGGER.debug("{} lwrite() - handshake needs wrap, fast looping", toString());
							}
							return lwrite(next, source, dest);
					}
					break;
			}
		}

		final boolean success = dest.position() != 0;

		if (success == false) {
			dest.free();
		} else {
			dest.flip();
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("{} lwrite() - result {}", toString(), dest);
			}
			final EncryptedWriteRequest encrypted = new EncryptedWriteRequest(dest, null);
			next.filterWrite(this.mSession, encrypted);
		}

		switch (result.getHandshakeStatus()) {
			case NEED_UNWRAP:
			case NEED_UNWRAP_AGAIN:
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("{} lwrite() - handshake needs unwrap, invoking receive", toString());
				}
				this.receive(next, ZERO);
				break;
			case NEED_WRAP:
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("{} lwrite() - handshake needs wrap, looping", toString());
				}
				this.write(next);
				break;
			case NEED_TASK:
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("{} lwrite() - handshake needs task, scheduling", toString());
				}
				this.schedule_task(next);
				break;
			case FINISHED:
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("{} lwrite() - handshake finished, flushing queue", toString());
				}
				this.lfinish(next);
				break;
		}

		return success;
	}

	/**
	 * Marks the handshake as complete and emits any signals
	 * 
	 * @param next
	 * @throws SSLException
	 */
	synchronized protected void lfinish(final NextFilter next) throws SSLException {
		if (this.mHandshakeComplete == false) {
			this.mHandshakeComplete = true;
			this.mSession.setAttribute(SSL2Filter.SSL_SECURED, this.mEngine.getSession());
			next.event(this.mSession, SslEvent.SECURED);
		}
		/**
		 * There exists a bug in the JDK which emits FINISHED twice instead of once.
		 */
		this.receive(next, ZERO);
		this.flush(next);
	}

	/**
	 * Flushes the encode queue
	 * 
	 * @param next
	 * 
	 * @throws SSLException
	 */
	synchronized public void flush(final NextFilter next) throws SSLException {
		if (this.mOutboundClosing && this.mOutboundLinger == false) {
			return;
		}

		if (this.mEncodeQueue.size() != 0) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("{} flush() - no saved messages", toString());
			}
			return;
		}

		WriteRequest current = null;
		while ((this.mAckQueue.size() < MAX_UNACK_MESSAGES) && (current = this.mEncodeQueue.poll()) != null) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("{} flush() - {}", toString(), current);
			}
			if (qwrite(next, current) == false) {
				this.mEncodeQueue.addFirst(current);
				break;
			}
		}

		if (this.mOutboundClosing && this.mEncodeQueue.size() == 0) {
			this.mEngine.closeOutbound();
			this.write(next);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	synchronized public void close(final NextFilter next, final boolean linger) throws SSLException {
		if (this.mOutboundClosing)
			return;

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("{} close() - closing session", toString());
		}

		this.mOutboundLinger = linger;
		this.mOutboundClosing = true;
		if (linger == false) {
			if (this.mEncodeQueue.size() != 0) {
				next.exceptionCaught(this.mSession,
						new WriteRejectedException(new ArrayList<>(this.mEncodeQueue), "closing"));
				this.mEncodeQueue.clear();
			}
			this.mEngine.closeOutbound();
			this.write(next);
		} else {
			this.flush(next);
		}
	}

	protected void schedule_task(final NextFilter next) {
		if (ENABLE_ASYNC_TASKS) {
			if (this.mExecutor == null) {
				this.execute_task(next);
			} else {
				this.mExecutor.execute(new Runnable() {
					@Override
					public void run() {
						SSL2HandlerG0.this.execute_task(next);
					}
				});
			}
		} else {
			this.execute_task(next);
		}
	}

	synchronized protected void execute_task(final NextFilter next) {
		Runnable t = null;
		while ((t = mEngine.getDelegatedTask()) != null) {
			try {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("{} task() - executing {}", toString(), t);
				}

				t.run();

				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("{} task() - writing handshake messages", toString());
				}

				write(next);
			} catch (SSLException e) {
				e.printStackTrace();
			}
		}
	}
}
