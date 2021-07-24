package org.apache.mina.filter.ssl2;

import java.util.concurrent.Executor;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilter.NextFilter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.filter.ssl.SslEvent;

public class SSL2HandlerG0 extends SSL2Handler {

	/**
	 * Maximum number of messages waiting acknowledgement
	 */
	static protected final int MAX_UNACK_MESSAGES = 6;

	/**
	 * Indicates whether the first handshake was completed
	 */
	protected boolean mHandshakeComplete = false;

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
		if (this.mEngine.getUseClientMode()) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("{} open() - begin handshaking", toString());
			}
			this.mEngine.beginHandshake();
			this.lwrite(next);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	synchronized public void receive(final NextFilter next, final IoBuffer message) throws SSLException {
		final IoBuffer source = resume_decode_buffer(message);
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("{} receive() - source {}", toString(), source);
		}
		try {
			while (lreceive(next, source) && message.hasRemaining()) {
				// loop until the message is consumed
			}
		} finally {
			save_decode_buffer(source);
		}
	}

	/**
	 * Process a received message
	 * 
	 * @param message received data
	 * @param session user session
	 * @param next    filter
	 * @return {@code true} if some of the message was consumed
	 * @throws SSLException
	 */
	@SuppressWarnings("incomplete-switch")
	protected boolean lreceive(final NextFilter next, final IoBuffer message) throws SSLException {

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("{} lreceive() - source {}", toString(), message);
		}

		final IoBuffer source = message == null ? ZERO : message;
		final IoBuffer dest = allocate_app_buffer(source.remaining());

		final SSLEngineResult result = mEngine.unwrap(source.buf(), dest.buf());

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("{} lreceive() - bytes-consumed {}, bytes-produced {}, status {}", toString(),
					result.bytesConsumed(), result.bytesProduced(), result.getStatus());
		}

		if (result.bytesProduced() == 0) {
			dest.free();
		} else {
			dest.flip();
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("{} lreceive() - result {}", toString(), dest);
			}
			next.messageReceived(this.mSession, dest);
		}

		switch (result.getHandshakeStatus()) {
			case NEED_TASK:
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("{} lreceive() - handshake needs task, scheduling tasks", toString());
				}
				this.schedule_task(next);
				break;
			case NEED_WRAP:
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("{} lreceive() - handshake needs to write a new message", toString());
				}
				this.lwrite(next);
				break;
			case FINISHED:
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("{} lreceive() - handshake finished, flushing pending requests", toString());
				}
				this.lfinish(next);
				this.lflush(next);
				break;
		}

		return result.bytesConsumed() > 0;
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
			this.lflush(next);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	synchronized public void write(final NextFilter next, final WriteRequest request) throws SSLException {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("{} write() - source {}", toString(), request);
		}

		if (this.mEncodeQueue.isEmpty()) {
			if (lwrite(next, request) == false) {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("{} write() - unable to write right now, saving request for later", toString(),
							request);
				}
				this.mEncodeQueue.add(request);
			}
		} else {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("{} write() - unable to write right now, saving request for later", toString(), request);
			}
			this.mEncodeQueue.add(request);
		}
	}

	/**
	 * Attempts to encode the WriteRequest and write the data to the IoSession
	 * 
	 * @param request
	 * @param session
	 * @param next
	 * @return {@code true} if the WriteRequest was fully consumed; otherwise
	 *         {@code false}
	 * @throws SSLException
	 */
	@SuppressWarnings("incomplete-switch")
	synchronized protected boolean lwrite(final NextFilter next, final WriteRequest request) throws SSLException {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("{} lwrite() - source {}", toString(), request);
		}

		final IoBuffer source = IoBuffer.class.cast(request.getMessage());
		final IoBuffer dest = allocate_encode_buffer(source.remaining());

		final SSLEngineResult result = this.mEngine.wrap(source.buf(), dest.buf());

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("{} lwrite() - bytes-consumed {}, bytes-produced {}, status {}, handshake {}", toString(),
					result.bytesConsumed(), result.bytesProduced(), result.getStatus(), result.getHandshakeStatus());
		}

		if (result.bytesProduced() == 0) {
			dest.free();
		} else {
			if (result.bytesConsumed() == 0) {
				EncryptedWriteRequest encrypted = new EncryptedWriteRequest(dest, null);
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("{} lwrite() - result {}", toString(), encrypted);
				}
				next.filterWrite(this.mSession, encrypted);
			} else {
				// then we probably consumed some data
				dest.flip();
				if (source.hasRemaining()) {
					EncryptedWriteRequest encrypted = new EncryptedWriteRequest(dest, null);
					this.mAckQueue.add(encrypted);
					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug("{} lwrite() - result {}", toString(), encrypted);
					}
					next.filterWrite(this.mSession, encrypted);
					if (this.mAckQueue.size() < MAX_UNACK_MESSAGES) {
						return lwrite(next, request); // write additional chunks
					}
					return false;
				} else {
					source.rewind();
					EncryptedWriteRequest encrypted = new EncryptedWriteRequest(dest, request);
					this.mAckQueue.add(encrypted);
					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug("{} lwrite() - result {}", toString(), encrypted);
					}
					next.filterWrite(this.mSession, encrypted);
					return true;
				}
			}
		}

		switch (result.getHandshakeStatus()) {
			case NEED_TASK:
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("{} lwrite() - handshake needs task, scheduling tasks", toString());
				}
				this.schedule_task(next);
				break;
			case NEED_WRAP:
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("{} lwrite() - handshake needs to encode a message", toString());
				}
				return this.lwrite(next, request);
			case FINISHED:
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("{} lwrite() - handshake finished, flushing pending requests", toString());
				}
				this.lfinish(next);
				if (this.lwrite(next, request)) {
					this.lflush(next);
					return true;
				}
				break;
		}

		return false;
	}

	/**
	 * Attempts to generate a handshake message and write the data to the IoSession
	 * 
	 * @param session
	 * @param next
	 * @return {@code true} if a message was generated and written
	 * @throws SSLException
	 */
	@SuppressWarnings("incomplete-switch")
	synchronized protected boolean lwrite(NextFilter next) throws SSLException {

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("{} lwrite() - internal", toString());
		}

		final IoBuffer source = ZERO;
		final IoBuffer dest = allocate_encode_buffer(source.remaining());

		final SSLEngineResult result = this.mEngine.wrap(source.buf(), dest.buf());

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("{} lwrite() - bytes-consumed {}, bytes-produced {}", toString(), result.bytesConsumed(),
					result.bytesProduced());
		}

		if (result.bytesProduced() == 0) {
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
			case NEED_TASK:
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("{} lwrite() - handshake needs task, scheduling tasks", toString());
				}
				this.schedule_task(next);
				break;
			case NEED_WRAP:
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("{} lwrite() - handshake needs to encode a message", toString());
				}
				this.lwrite(next);
				break;
			case FINISHED:
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("{} lwrite() - handshake finished, flushing pending requests", toString());
				}
				this.lfinish(next);
				this.lflush(next);
				break;
		}

		return result.bytesProduced() > 0;
	}

	synchronized protected void lfinish(final NextFilter next) {
		this.mHandshakeComplete = true;
		next.event(this.mSession, SslEvent.SECURED);
	}

	/**
	 * Flushes the encode queue
	 * 
	 * @param next
	 * @throws SSLException
	 */
	synchronized protected void lflush(final NextFilter next) throws SSLException {
		if (this.mEncodeQueue.isEmpty()) {
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
			if (lwrite(next, current) == false) {
				this.mEncodeQueue.addFirst(current);
				break;
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	synchronized public void close(final NextFilter next) throws SSLException {
		if (mEngine.isOutboundDone())
			return;

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("{} close() - closing session", toString());
		}

		mEngine.closeOutbound();
		this.lwrite(next);
	}

	protected void schedule_task(final NextFilter next) {
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

				lwrite(next);
			} catch (SSLException e) {
				e.printStackTrace();
			}
		}
	}
}
