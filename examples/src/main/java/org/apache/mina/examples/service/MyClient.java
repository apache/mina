package org.apache.mina.examples.service;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import org.apache.mina.api.IoFuture;
import org.apache.mina.api.IoSession;
import org.apache.mina.codec.delimited.serialization.JavaNativeMessageEncoder;
import org.apache.mina.transport.nio.NioTcpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyClient {

	static final private Logger LOG = LoggerFactory.getLogger(MyClient.class);

	public static void main(String[] args) {

		LOG.info("starting echo client");
		final NioTcpClient client = new NioTcpClient();
		client.setIoHandler(new ClientHandler());

		try {

			IoFuture<IoSession> future = client.connect(new InetSocketAddress("localhost", 9999));

			try {
				IoSession session = future.get();
				LOG.info("session connected : {" + session + "}");

				HashMap<String, String> m = new HashMap<String, String>();
				m.put("1", "1");
				
				// encode
				JavaNativeMessageEncoder<HashMap> in = new JavaNativeMessageEncoder<HashMap>();
				ByteBuffer encode = in.encode(m);
				session.write(encode);

			} catch (ExecutionException e) {
				LOG.error("cannot connect : ", e);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
