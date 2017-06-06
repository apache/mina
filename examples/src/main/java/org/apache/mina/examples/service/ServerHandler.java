package org.apache.mina.examples.service;

import java.nio.ByteBuffer;
import java.util.HashMap;

import org.apache.mina.api.IdleStatus;
import org.apache.mina.api.IoHandler;
import org.apache.mina.api.IoService;
import org.apache.mina.api.IoSession;
import org.apache.mina.codec.IoBuffer;
import org.apache.mina.codec.delimited.serialization.JavaNativeMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerHandler implements IoHandler {

	private static final Logger LOG = LoggerFactory.getLogger(ServerHandler.class);

	@Override
	public void sessionOpened(IoSession session) {
		LOG.info("server session opened {" + session + "}");
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
		if (message instanceof ByteBuffer) {
			try {

				JavaNativeMessageDecoder<HashMap> decoder = new JavaNativeMessageDecoder<HashMap>();
				IoBuffer ioBuff = IoBuffer.wrap((ByteBuffer) message);
				HashMap map = decoder.decode(ioBuff);
				LOG.info("server decode value => " + map);
				System.out.println("server decode => " + map);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void messageSent(IoSession session, Object message) {
		LOG.info("send message:" + message.toString());
		System.out.println("server send message:" + message.toString());
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
