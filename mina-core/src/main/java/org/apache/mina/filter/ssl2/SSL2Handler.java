package org.apache.mina.filter.ssl2;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executor;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilter.NextFilter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRejectedException;
import org.apache.mina.core.write.WriteRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SSL2Handler {

	/**
	 * Minimum size of encoder buffer in packets
	 */
	static protected final int MIN_ENCODER_BUFFER_PACKETS = 2;

	/**
	 * Maximum size of encoder buffer in packets
	 */
	static protected final int MAX_ENCODER_BUFFER_PACKETS = 8;

	/**
	 * Zero length buffer used to prime the ssl engine
	 */
	static protected final IoBuffer ZERO = IoBuffer.allocate(0, true);

	/**
	 * Static logger
	 */
	static protected final Logger LOGGER = LoggerFactory.getLogger(SSL2Handler.class);

	/**
	 * Write Requests which are enqueued prior to the completion of the handshaking
	 */
	protected final Deque<WriteRequest> mEncodeQueue = new ConcurrentLinkedDeque<>();

	/**
	 * Requests which have been sent to the socket and waiting acknowledgment
	 */
	protected final Deque<WriteRequest> mAckQueue = new ConcurrentLinkedDeque<>();

	/**
	 * SSL Engine
	 */
	protected final SSLEngine mEngine;

	/**
	 * Task executor
	 */
	protected final Executor mExecutor;

	/**
	 * Socket session
	 */
	protected final IoSession mSession;

	/**
	 * Progressive decoder buffer
	 */
	protected IoBuffer mDecodeBuffer;

	/**
	 * Instantiates a new handler
	 * 
	 * @param p engine
	 * @param e executor
	 * @param s session
	 */
	public SSL2Handler(SSLEngine p, Executor e, IoSession s) {
		this.mEngine = p;
		this.mExecutor = e;
		this.mSession = s;
	}

	/**
	 * {@code true} if the encryption session is open
	 */
	abstract public boolean isOpen();

	/**
	 * {@code true} if the encryption session is connected and secure
	 */
	abstract public boolean isConnected();

	/**
	 * Opens the encryption session, this may include sending the initial handshake
	 * message
	 * 
	 * @param session
	 * @param next
	 * 
	 * @throws SSLException
	 */
	abstract public void open(NextFilter next) throws SSLException;

	/**
	 * Decodes encrypted messages and passes the results to the {@code next} filter.
	 * 
	 * @param message
	 * @param session
	 * @param next
	 * 
	 * @throws SSLException
	 */
	abstract public void receive(NextFilter next, final IoBuffer message) throws SSLException;

	/**
	 * Acknowledge that a {@link WriteRequest} has been successfully written to the
	 * {@link IoSession}
	 * <p>
	 * This functionality is used to enforce flow control by allowing only a
	 * specific number of pending write operations at any moment of time. When one
	 * {@code WriteRequest} is acknowledged, another can be encoded and written.
	 * 
	 * @param request
	 * @param session
	 * @param next
	 * 
	 * @throws SSLException
	 */
	abstract public void ack(NextFilter next, final WriteRequest request) throws SSLException;

	/**
	 * Encrypts and writes the specified {@link WriteRequest} to the
	 * {@link IoSession} or enqueues it to be processed later.
	 * <p>
	 * The encryption session may be currently handshaking preventing application
	 * messages from being written.
	 * 
	 * @param request
	 * @param session
	 * @param next
	 * 
	 * @throws SSLException
	 * @throws WriteRejectedException when the session is closing
	 */
	abstract public void write(NextFilter next, final WriteRequest request) throws SSLException, WriteRejectedException;

	/**
	 * Closes the encryption session and writes any required messages
	 * 
	 * @param next
	 * @param linger if true, write any queued messages before closing
	 * 
	 * @throws SSLException
	 */
	abstract public void close(NextFilter next, final boolean linger) throws SSLException;

	/**
	 * {@inheritDoc}
	 */
	public String toString() {
		StringBuilder b = new StringBuilder();

		b.append(this.getClass().getSimpleName());
		b.append("@");
		b.append(Integer.toHexString(this.hashCode()));
		b.append("[mode=");

		if (this.mEngine.getUseClientMode()) {
			b.append("client");
		} else {
			b.append("server");
		}

		b.append(", connected=");
		b.append(this.isConnected());

		b.append("]");

		return b.toString();
	}

	/**
	 * Combines the received data with any previously received data
	 * 
	 * @param source received data
	 * @return buffer to decode
	 */
	protected IoBuffer resume_decode_buffer(IoBuffer source) {
		if (mDecodeBuffer == null)
			if (source == null) {
				return ZERO;
			} else {
				mDecodeBuffer = source;
				return source;
			}
		else {
			if (source != null && source != ZERO) {
				mDecodeBuffer.expand(source.remaining());
				mDecodeBuffer.put(source);
				source.free();
			}
			mDecodeBuffer.flip();
			return mDecodeBuffer;
		}
	}

	/**
	 * Stores data for later use if any is remaining
	 * 
	 * @param source the buffer previously returned by
	 *               {@link #resume_decode_buffer(IoBuffer)}
	 */
	protected void suspend_decode_buffer(IoBuffer source) {
		if (source.hasRemaining()) {
			if (source.isDerived()) {
				this.mDecodeBuffer = IoBuffer.allocate(source.remaining());
				this.mDecodeBuffer.put(source);
			} else {
				source.compact();
				this.mDecodeBuffer = source;
			}
		} else {
			if (source != ZERO) {
				source.free();
			}
			this.mDecodeBuffer = null;
		}
	}

	/**
	 * Allocates the default encoder buffer for the given source size
	 * 
	 * @param source
	 * @return buffer
	 */
	protected IoBuffer allocate_encode_buffer(int estimate) {
		SSLSession session = this.mEngine.getHandshakeSession();
		if (session == null)
			session = this.mEngine.getSession();
		int packets = Math.max(MIN_ENCODER_BUFFER_PACKETS,
				Math.min(MAX_ENCODER_BUFFER_PACKETS, 1 + (estimate / session.getApplicationBufferSize())));
		return IoBuffer.allocate(packets * session.getPacketBufferSize());
	}

	/**
	 * Allocates the default decoder buffer for the given source size
	 * 
	 * @param source
	 * @return buffer
	 */
	protected IoBuffer allocate_app_buffer(int estimate) {
		SSLSession session = this.mEngine.getHandshakeSession();
		if (session == null)
			session = this.mEngine.getSession();
		int packets = 1 + (estimate / session.getPacketBufferSize());
		return IoBuffer.allocate(packets * session.getApplicationBufferSize());
	}
}
