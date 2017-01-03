package org.apache.mina.examples.service;

import java.nio.ByteBuffer;

import org.apache.mina.api.IdleStatus;
import org.apache.mina.api.IoHandler;
import org.apache.mina.api.IoService;
import org.apache.mina.api.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerHandler implements IoHandler {

	private static final Logger LOG = LoggerFactory.getLogger(ServerHandler.class);

	@Override
	public void sessionOpened(IoSession session) {
		
		LOG.info("cccsession opened {" + session + "}");

		final String welcomeStr = "welcome\n";
		final ByteBuffer bf = ByteBuffer.allocate(welcomeStr.length());
		bf.put(welcomeStr.getBytes());
		bf.flip();
		session.write(bf);
	}

	@Override
	public void sessionClosed(IoSession session) {
		LOG.info("IP:" + session.getRemoteAddress().toString() + " close");
	}

	@Override
	public void sessionIdle(IoSession session, IdleStatus status) {

	}

	@Override
	public void messageReceived(IoSession session, Object message) {
		// if (message != null) {
		// LOG.info("server message => " + message);
		// session.write(message);
		// }

		if (message != null) {
			LOG.info("echoing");
//			System.out.println("server echoing");
			session.write(message);
		}
	}

	@Override
	public void messageSent(IoSession session, Object message) {
		LOG.info("send message:" + message.toString());
//		System.out.println("server send message:" + message.toString());
	}

	@Override
	public void serviceActivated(IoService service) {

	}

	@Override
	public void serviceInactivated(IoService service) {

	}

	@Override
	public void exceptionCaught(IoSession session, Exception cause) {

	}

	@Override
	public void handshakeStarted(IoSession abstractIoSession) {

	}

	@Override
	public void handshakeCompleted(IoSession session) {

	}

	@Override
	public void secureClosed(IoSession session) {

	}

}
