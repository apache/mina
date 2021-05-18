package org.apache.mina.filter.codec;

import static org.junit.Assert.assertTrue;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.serialization.ObjectSerializationCodecFactory;
import org.apache.mina.filter.codec.textline.TextLineCodecFactory;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.junit.Test;

public class ParallelProtocolEncoderTest {
	private NioSocketConnector connector = null;
	private NioSocketAcceptor acceptor = null;
	private static int LOOP = 1000;
	private static int THREAD = 3;

	private static Logger logger = LogManager.getLogger(ParallelProtocolEncoderTest.class);
	private static ExecutorService executorService = Executors.newFixedThreadPool(THREAD);

	@Test
	public void missingMessageTest() throws Exception {
		String host = "localhost";
		int port = 28_000;

		// server
		acceptor = new NioSocketAcceptor();
		acceptor.getFilterChain().addFirst("codec", new ProtocolCodecFilter(new ObjectSerializationCodecFactory()));
		ServerHandler serverHandler = new ServerHandler();
		acceptor.setHandler(serverHandler);
		acceptor.bind(new InetSocketAddress(host, port));

		// client
		connector = new NioSocketConnector(1);
		connector.getFilterChain().addLast("codec", new ProtocolCodecFilter(new ObjectSerializationCodecFactory()));
		ClientHandler clientHandler = new ClientHandler();
		connector.setHandler(clientHandler);
		ConnectFuture connectFuture = connector.connect(new InetSocketAddress(host, 28_000));
		connectFuture.awaitUninterruptibly();

		final IoSession ioSession = connectFuture.getSession();

		logger.info("missingMessageTest.begin with " + LOOP + " messages and " + THREAD + " threads");

		for (int i = 1; i <= LOOP; i++) {
			final String message = "Message:" + i;
			executorService.submit(new Runnable() {

				@Override
				public void run() {

					logger.info("missingMessageTest.client.write "+message);

					final WriteFuture future = ioSession.write(message);
					if (future != null) {
						future.addListener(new IoFutureListener<WriteFuture>() {
							@Override
							public void operationComplete(WriteFuture writeFuture) {
								if (!future.isWritten()) {
									logger.error("writeFuture: " + writeFuture.getException());
								}
							}
						});
					}
				}
			});

		}
		logger.info("missingMessageTest.end");

		int maxSleep = 5_000;
		int time = 1000;
		int sleep = 0;
		while ((!clientHandler.isFinished() || !serverHandler.isFinished()) && maxSleep > sleep) {
			sleep += time;
			logger.info("missingMessageTest.sleep... " + sleep);
			Thread.sleep(time);
		}

		logger.info("missingMessageTest.close");

		ioSession.closeNow();
		connector.dispose();
		acceptor.dispose();

		if (!serverHandler.isFinished()) {
			Set<String> missingMessages = clientHandler.getMessages();
			missingMessages.removeAll(serverHandler.getMessages());
			logger.error("missing <" + missingMessages.size() + "> messages : " + missingMessages);
		}

		assertTrue(serverHandler.isFinished());
		assertTrue(clientHandler.isFinished());
	}

	private static class ServerHandler extends IoHandlerAdapter {
		private Set<String> messages = new HashSet<>(LOOP);
		private AtomicInteger count = new AtomicInteger(0);

		@Override
		public void messageReceived(IoSession session, Object message) throws Exception {

			String messageString = (String) message;
			count.incrementAndGet();

			if (messages.contains(messageString)) {
				logger.error("messageReceived: message <" + messageString + "> already received");
			}
			messages.add(messageString);

			// logger.info("messageReceived: <"+message+">, count="+count);

			if (isFinished()) {
				logger.info("messageReceived: finish");
			}

			super.messageReceived(session, message);
		}

		public boolean isFinished() {
			return count.get() == LOOP;
		}

		/**
		 * Get the messages.
		 *
		 * @return the messages
		 */
		public Set<String> getMessages() {
			return messages;
		}
	}

	private static class ClientHandler extends IoHandlerAdapter {
		private Set<String> messages = new HashSet<>(LOOP);
		private AtomicInteger count = new AtomicInteger(0);

		@Override
		public void messageSent(IoSession session, Object message) throws Exception {

			logger.info("messageSent " + message);
			
			count.incrementAndGet();
			String messageString = (String) message;
			if (messages.contains(messageString)) {
				logger.error("messageSent: message <" + messageString + "> already sent");
			}
			messages.add(messageString);

			// logger.info("messageSent: <"+message+">, count="+count);

			if (isFinished()) {
				logger.info("messageSent: finish");
			}
			super.messageSent(session, message);
		}

		public boolean isFinished() {
			return count.get() == LOOP;
		}

		/**
		 * Get the messages.
		 *
		 * @return the messages
		 */
		public Set<String> getMessages() {
			return messages;
		}
	}
}
