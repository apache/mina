package org.apache.mina.filter.ssl;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.Security;
import java.util.Deque;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLEngineResult.Status;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;

import org.apache.mina.core.buffer.IoBuffer;
import org.junit.Ignore;
import org.junit.Test;

public class SSLEngineTest {
	private BlockingDeque<ByteBuffer> clientQueue = new LinkedBlockingDeque<>();
	private BlockingDeque<ByteBuffer> serverQueue = new LinkedBlockingDeque<>();

	private class Handshaker implements Runnable {
		private SSLEngine sslEngine;
		private ByteBuffer workBuffer;
		private ByteBuffer emptyBuffer = ByteBuffer.allocate(0);

		private void push(Deque<ByteBuffer> queue, ByteBuffer buffer) {
			ByteBuffer result = ByteBuffer.allocate(buffer.capacity());
			result.put(buffer);
			queue.addFirst(result);
		}

		public void run() {
			HandshakeStatus handshakeStatus = sslEngine.getHandshakeStatus();
			SSLEngineResult result;

			try {
				while (handshakeStatus != HandshakeStatus.FINISHED) {
					switch (handshakeStatus) {
						case NEED_TASK:
							break;

						case NEED_UNWRAP:
							// The SSLEngine waits for some input.
							// We may have received too few data (TCP fragmentation)
							//
							ByteBuffer data = serverQueue.takeLast();
							result = sslEngine.unwrap(data, workBuffer);

							while (result.getStatus() == SSLEngineResult.Status.BUFFER_UNDERFLOW) {
								// We need more data, until then, wait.
								// ByteBuffer data = serverQueue.takeLast();
								result = sslEngine.unwrap(data, workBuffer);
							}

							handshakeStatus = sslEngine.getHandshakeStatus();
							break;

						case NEED_WRAP:
						case NOT_HANDSHAKING:
							result = sslEngine.wrap(emptyBuffer, workBuffer);

							workBuffer.flip();

							if (workBuffer.hasRemaining()) {
								push(clientQueue, workBuffer);
								workBuffer.clear();
							}

							handshakeStatus = result.getHandshakeStatus();

							break;

						case FINISHED:

					}
				}
			} catch (SSLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		public Handshaker(SSLEngine sslEngine) {
			this.sslEngine = sslEngine;
			int packetBufferSize = sslEngine.getSession().getPacketBufferSize();
			workBuffer = ByteBuffer.allocate(packetBufferSize);
		}
	}

	/** A JVM independant KEY_MANAGER_FACTORY algorithm */
	private static final String KEY_MANAGER_FACTORY_ALGORITHM;

	static {
		String algorithm = Security.getProperty("ssl.KeyManagerFactory.algorithm");
		if (algorithm == null) {
			algorithm = KeyManagerFactory.getDefaultAlgorithm();
		}

		KEY_MANAGER_FACTORY_ALGORITHM = algorithm;
	}

	/** App data buffer for the client SSLEngine */
	private IoBuffer inNetBufferClient;

	/** Net data buffer for the client SSLEngine */
	private IoBuffer outNetBufferClient;

	/** App data buffer for the server SSLEngine */
	private IoBuffer inNetBufferServer;

	/** Net data buffer for the server SSLEngine */
	private IoBuffer outNetBufferServer;

	private final IoBuffer emptyBuffer = IoBuffer.allocate(0);

	private static SSLContext createSSLContext() throws IOException, GeneralSecurityException {
		char[] passphrase = "password".toCharArray();

		SSLContext ctx = SSLContext.getInstance("TLS");
		KeyManagerFactory kmf = KeyManagerFactory.getInstance(KEY_MANAGER_FACTORY_ALGORITHM);
		TrustManagerFactory tmf = TrustManagerFactory.getInstance(KEY_MANAGER_FACTORY_ALGORITHM);

		KeyStore ks = KeyStore.getInstance("JKS");
		KeyStore ts = KeyStore.getInstance("JKS");

		ks.load(SSLEngineTest.class.getResourceAsStream("keystore.jks"), passphrase);
		ts.load(SSLEngineTest.class.getResourceAsStream("truststore.jks"), passphrase);

		kmf.init(ks, passphrase);
		tmf.init(ts);
		ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

		return ctx;
	}

	/**
	 * Decrypt the incoming buffer and move the decrypted data to an application
	 * buffer.
	 */
	private SSLEngineResult unwrap(SSLEngine sslEngine, IoBuffer inBuffer, IoBuffer outBuffer) throws SSLException {
		// We first have to create the application buffer if it does not exist
		if (outBuffer == null) {
			outBuffer = IoBuffer.allocate(inBuffer.remaining());
		} else {
			// We already have one, just add the new data into it
			outBuffer.expand(inBuffer.remaining());
		}

		SSLEngineResult res;
		Status status;
		HandshakeStatus localHandshakeStatus;

		do {
			// Decode the incoming data
			res = sslEngine.unwrap(inBuffer.buf(), outBuffer.buf());
			status = res.getStatus();

			// We can be processing the Handshake
			localHandshakeStatus = res.getHandshakeStatus();

			if (status == SSLEngineResult.Status.BUFFER_OVERFLOW) {
				// We have to grow the target buffer, it's too small.
				// Then we can call the unwrap method again
				int newCapacity = sslEngine.getSession().getApplicationBufferSize();

				if (inBuffer.remaining() >= newCapacity) {
					// The buffer is already larger than the max buffer size suggested by the SSL
					// engine.
					// Raising it any more will not make sense and it will end up in an endless
					// loop. Throwing an error is safer
					throw new SSLException("SSL buffer overflow");
				}

				inBuffer.expand(newCapacity);
				continue;
			}
		} while (((status == SSLEngineResult.Status.OK) || (status == SSLEngineResult.Status.BUFFER_OVERFLOW))
				&& ((localHandshakeStatus == SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING)
						|| (localHandshakeStatus == SSLEngineResult.HandshakeStatus.NEED_UNWRAP)));

		return res;
	}

	private SSLEngineResult.Status unwrapHandshake(SSLEngine sslEngine, IoBuffer appBuffer, IoBuffer netBuffer)
			throws SSLException {
		// Prepare the net data for reading.
		if ((appBuffer == null) || !appBuffer.hasRemaining()) {
			// Need more data.
			return SSLEngineResult.Status.BUFFER_UNDERFLOW;
		}

		SSLEngineResult res = unwrap(sslEngine, appBuffer, netBuffer);
		HandshakeStatus handshakeStatus = res.getHandshakeStatus();

		// checkStatus(res);

		// If handshake finished, no data was produced, and the status is still
		// ok, try to unwrap more
		if ((handshakeStatus == SSLEngineResult.HandshakeStatus.FINISHED)
				&& (res.getStatus() == SSLEngineResult.Status.OK) && appBuffer.hasRemaining()) {
			res = unwrap(sslEngine, appBuffer, netBuffer);

			// prepare to be written again
			if (appBuffer.hasRemaining()) {
				appBuffer.compact();
			} else {
				appBuffer.free();
				appBuffer = null;
			}
		} else {
			// prepare to be written again
			if (appBuffer.hasRemaining()) {
				appBuffer.compact();
			} else {
				appBuffer.free();
				appBuffer = null;
			}
		}

		return res.getStatus();
	}

	/* no qualifier */boolean isInboundDone(SSLEngine sslEngine) {
		return sslEngine == null || sslEngine.isInboundDone();
	}

	/* no qualifier */boolean isOutboundDone(SSLEngine sslEngine) {
		return sslEngine == null || sslEngine.isOutboundDone();
	}

	/**
	 * Perform any handshaking processing.
	 */
	/* no qualifier */HandshakeStatus handshake(SSLEngine sslEngine, IoBuffer appBuffer, IoBuffer netBuffer)
			throws SSLException {
		SSLEngineResult result;
		HandshakeStatus handshakeStatus = sslEngine.getHandshakeStatus();

		for (;;) {
			switch (handshakeStatus) {
				case FINISHED:
					// handshakeComplete = true;
					return handshakeStatus;

				case NEED_TASK:
					// handshakeStatus = doTasks();
					break;

				case NEED_UNWRAP:
					// we need more data read
					SSLEngineResult.Status status = unwrapHandshake(sslEngine, appBuffer, netBuffer);
					handshakeStatus = sslEngine.getHandshakeStatus();

					return handshakeStatus;

				case NEED_WRAP:
					result = sslEngine.wrap(emptyBuffer.buf(), netBuffer.buf());

					while (result.getStatus() == SSLEngineResult.Status.BUFFER_OVERFLOW) {
						netBuffer.capacity(netBuffer.capacity() << 1);
						netBuffer.limit(netBuffer.capacity());

						result = sslEngine.wrap(emptyBuffer.buf(), netBuffer.buf());
					}

					netBuffer.flip();
					return result.getHandshakeStatus();

				case NOT_HANDSHAKING:
					result = sslEngine.wrap(emptyBuffer.buf(), netBuffer.buf());

					while (result.getStatus() == SSLEngineResult.Status.BUFFER_OVERFLOW) {
						netBuffer.capacity(netBuffer.capacity() << 1);
						netBuffer.limit(netBuffer.capacity());

						result = sslEngine.wrap(emptyBuffer.buf(), netBuffer.buf());
					}

					netBuffer.flip();
					handshakeStatus = result.getHandshakeStatus();
					return handshakeStatus;

				default:
					throw new IllegalStateException("error");
			}
		}
	}

	/**
	 * Do all the outstanding handshake tasks in the current Thread.
	 */
	private SSLEngineResult.HandshakeStatus doTasks(SSLEngine sslEngine) {
		/*
		 * We could run this in a separate thread, but I don't see the need for this
		 * when used from SSLFilter. Use thread filters in MINA instead?
		 */
		Runnable runnable;
		while ((runnable = sslEngine.getDelegatedTask()) != null) {
			// Thread thread = new Thread(runnable);
			// thread.start();
			runnable.run();
		}
		return sslEngine.getHandshakeStatus();
	}

	private HandshakeStatus handshake(SSLEngine sslEngine, HandshakeStatus expected, IoBuffer inBuffer,
			IoBuffer outBuffer, boolean dumpBuffer) throws SSLException {
		HandshakeStatus handshakeStatus = handshake(sslEngine, inBuffer, outBuffer);

		if (handshakeStatus != expected) {
			fail();
		}

		if (dumpBuffer) {
			System.out.println("Message:" + outBuffer);
		}

		return handshakeStatus;
	}

	@Test
	@Ignore
	public void testSSL() throws Exception {
		// Initialise the client SSLEngine
		SSLContext sslContextClient = createSSLContext();
		SSLEngine sslEngineClient = sslContextClient.createSSLEngine();
		int packetBufferSize = sslEngineClient.getSession().getPacketBufferSize();
		inNetBufferClient = IoBuffer.allocate(packetBufferSize).setAutoExpand(true);
		outNetBufferClient = IoBuffer.allocate(packetBufferSize).setAutoExpand(true);

		sslEngineClient.setUseClientMode(true);

		// Initialise the Server SSLEngine
		SSLContext sslContextServer = createSSLContext();
		SSLEngine sslEngineServer = sslContextServer.createSSLEngine();
		packetBufferSize = sslEngineServer.getSession().getPacketBufferSize();
		inNetBufferServer = IoBuffer.allocate(packetBufferSize).setAutoExpand(true);
		outNetBufferServer = IoBuffer.allocate(packetBufferSize).setAutoExpand(true);

		sslEngineServer.setUseClientMode(false);

		Handshaker handshakerClient = new Handshaker(sslEngineClient);
		Handshaker handshakerServer = new Handshaker(sslEngineServer);

		handshakerServer.run();

		HandshakeStatus handshakeStatusClient = sslEngineClient.getHandshakeStatus();
		HandshakeStatus handshakeStatusServer = sslEngineServer.getHandshakeStatus();

		// <<< Server
		// Start the server
		handshakeStatusServer = handshake(sslEngineServer, HandshakeStatus.NEED_UNWRAP, null, outNetBufferServer,
				false);

		// >>> Client
		// Now start the client, which will generate a CLIENT_HELLO,
		// stored into the outNetBufferClient
		handshakeStatusClient = handshake(sslEngineClient, HandshakeStatus.NEED_UNWRAP, null, outNetBufferClient, true);

		// <<< Server
		// Process the CLIENT_HELLO on the server
		handshakeStatusServer = handshake(sslEngineServer, HandshakeStatus.NEED_TASK, outNetBufferClient,
				outNetBufferServer, false);

		// Process the tasks on the server, prepare the SERVER_HELLO message
		handshakeStatusServer = doTasks(sslEngineServer);

		// We should be ready to generate the SERVER_HELLO message
		if (handshakeStatusServer != HandshakeStatus.NEED_WRAP) {
			fail();
		}

		// Get the SERVER_HELLO message, with all the associated messages
		// ([Certificate], [ServerKeyExchange], [CertificateRequest], ServerHelloDone)
		outNetBufferServer.clear();
		handshakeStatusServer = handshake(sslEngineServer, HandshakeStatus.NEED_UNWRAP, null, outNetBufferServer, true);

		// >>> Client
		// Process the SERVER_HELLO message on the client
		handshakeStatusClient = handshake(sslEngineClient, HandshakeStatus.NEED_TASK, outNetBufferServer,
				inNetBufferClient, false);

		// Prepare the client response
		handshakeStatusClient = doTasks(sslEngineClient);

		// We should get back the Client messages ([Certificate],
		// ClientKeyExchange, [CertificateVerify])
		if (handshakeStatusClient != HandshakeStatus.NEED_WRAP) {
			fail();
		}

		// Generate the [Certificate], ClientKeyExchange, [CertificateVerify] messages
		outNetBufferClient.clear();
		handshakeStatusClient = handshake(sslEngineClient, HandshakeStatus.NEED_WRAP, null, outNetBufferClient, true);

		// <<< Server
		// Process the CLIENT_KEY_EXCHANGE on the server
		outNetBufferServer.clear();
		handshakeStatusServer = handshake(sslEngineServer, HandshakeStatus.NEED_TASK, outNetBufferClient,
				outNetBufferServer, false);

		// Do the controls
		handshakeStatusServer = doTasks(sslEngineServer);

		// The server is waiting for more
		if (handshakeStatusServer != HandshakeStatus.NEED_UNWRAP) {
			fail();
		}

		// >>> Client
		// The CHANGE_CIPHER_SPEC message generation
		outNetBufferClient.clear();
		handshakeStatusClient = handshake(sslEngineClient, HandshakeStatus.NEED_WRAP, null, outNetBufferClient, true);

		// <<< Server
		// Process the CHANGE_CIPHER_SPEC on the server
		outNetBufferServer.clear();
		handshakeStatusServer = handshake(sslEngineServer, HandshakeStatus.NEED_UNWRAP, outNetBufferClient,
				outNetBufferServer, false);

		// >>> Client
		// Generate the FINISHED message on thee client
		outNetBufferClient.clear();
		handshakeStatusClient = handshake(sslEngineClient, HandshakeStatus.NEED_UNWRAP, null, outNetBufferClient, true);

		// <<< Server
		// Process the client FINISHED message
		outNetBufferServer.clear();
		handshakeStatusServer = handshake(sslEngineServer, HandshakeStatus.NEED_WRAP, outNetBufferClient,
				outNetBufferServer, false);

		// Generate the CHANGE_CIPHER_SPEC message on the server
		handshakeStatusServer = handshake(sslEngineServer, HandshakeStatus.NEED_WRAP, null, outNetBufferServer, true);

		// >>> Client
		// Process the server CHANGE_SCIPHER_SPEC message on the client
		outNetBufferClient.clear();
		handshakeStatusClient = handshake(sslEngineClient, HandshakeStatus.NEED_UNWRAP, outNetBufferServer,
				outNetBufferClient, false);

		// <<< Server
		// Generate the server FINISHED message
		outNetBufferServer.clear();
		handshakeStatusServer = handshake(sslEngineServer, HandshakeStatus.FINISHED, null, outNetBufferServer, true);

		// >>> Client
		// Process the server FINISHED message on the client
		outNetBufferClient.clear();
		handshakeStatusClient = handshake(sslEngineClient, HandshakeStatus.NOT_HANDSHAKING, outNetBufferServer,
				outNetBufferClient, false);
	}
}
