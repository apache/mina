package org.apache.mina.examples.service;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;

import org.apache.mina.api.IoFuture;
import org.apache.mina.api.IoSession;
import org.apache.mina.transport.nio.NioTcpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MyClient {

	static final private Logger LOG = LoggerFactory.getLogger(MyClient.class);

	public static void main(String[] args) {

		LOG.info("starting echo client");
		final NioTcpClient client = new NioTcpClient();
		// client.setFilters();
		client.setIoHandler(new ClientHandler());

		try {
			
			IoFuture<IoSession> future = client.connect(new InetSocketAddress("localhost", 9999));

			try {
				IoSession session = future.get();
				LOG.info("session connected : {" + session + "}");
				session.write("hhhh");
				session.write("2222");
			} catch (ExecutionException e) {
				LOG.error("cannot connect : ", e);
			}

			LOG.debug("Running the client for 10 sec");
			Thread.sleep(10000);
		} catch (InterruptedException e) {
		}
	}
}
