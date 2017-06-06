package org.apache.mina.examples.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.apache.mina.api.IdleStatus;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.transport.nio.NioTcpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MyServer {

	private static final Logger LOG = LoggerFactory.getLogger(MyServer.class);

	public static void main(String[] args) {
		LOG.info("start server...");
		final NioTcpServer acceptor = new NioTcpServer();
		acceptor.setFilters(new LoggingFilter("LoggingFilter1"));
		acceptor.setIoHandler(new ServerHandler());

		try {
			
			final SocketAddress address = new InetSocketAddress(9999);
			acceptor.bind(address);
			new BufferedReader(new InputStreamReader(System.in)).readLine();
			acceptor.unbind();
		} catch (final IOException e) {
			LOG.error("Interrupted exception", e);
		}
	}

}
