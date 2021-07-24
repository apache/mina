package org.apache.mina.filter.ssl2;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.service.IoConnector;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.DefaultWriteRequest;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.filter.ssl.SslDIRMINA937Test;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SSL2SimpleTest {

	public static void main(String[] args) throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException,
			UnrecoverableKeyException, CertificateException, IOException {
		// System.setProperty("javax.net.debug", "all");

		KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());

		KeyStore ks = KeyStore.getInstance("JKS");
		KeyStore ts = KeyStore.getInstance("JKS");

		ks.load(SslDIRMINA937Test.class.getResourceAsStream("keystore.sslTest"), "password".toCharArray());
		ts.load(SslDIRMINA937Test.class.getResourceAsStream("truststore.sslTest"), "password".toCharArray());

		kmf.init(ks, "password".toCharArray());
		tmf.init(ts);

		final SSLContext context = SSLContext.getInstance("TLS");
		context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());

		final SSL2Filter filter = new SSL2Filter(context);
		filter.setEnabledCipherSuites(new String[] { "TLS_AES_128_GCM_SHA256", "TLS_AES_256_GCM_SHA384" });
		filter.setEnabledProtocols(new String[] { "TLSv1.3" });

		final IoAcceptor socket_acceptor = new NioSocketAcceptor();

		socket_acceptor.getFilterChain().addFirst("ssl", filter);
		socket_acceptor.setHandler(new DebugFilter());

		final IoConnector socket_connector = new NioSocketConnector();

		socket_connector.getFilterChain().addFirst("ssl", filter);
		socket_connector.setHandler(new DebugFilter());

		final InetSocketAddress server_address = new InetSocketAddress("0.0.0.0", 53301);
		socket_acceptor.bind(server_address);

		final IoFuture connect_future = socket_connector.connect(server_address);
		connect_future.awaitUninterruptibly();

		final IoSession client_socket = connect_future.getSession();

//		try {
//			Thread.sleep(1000);
//		} catch (InterruptedException e) {
//
//		}

		client_socket.write(createWriteRequest());

		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {

		}

		client_socket.closeNow();

		socket_connector.dispose();

		socket_acceptor.unbind();
		socket_acceptor.dispose();
	}

	public static class DebugFilter extends IoHandlerAdapter {
		protected static final Logger LOGGER = LoggerFactory.getLogger(DebugFilter.class);

		@Override
		public void messageReceived(IoSession session, Object message) throws Exception {

			IoBuffer b = IoBuffer.class.cast(message);
			LOGGER.debug("received clear-text message\n" + b.getHexDump(true));
		}
	}

	public static IoBuffer createWriteRequest() {
		// HTTP request
		StringBuilder http = new StringBuilder();
		http.append("GET / HTTP/1.0\r\n");
		http.append("Connection: close\r\n");
		http.append("\r\n");

		IoBuffer message = IoBuffer.allocate(1024);
		message.put(http.toString().getBytes());
		message.flip();

		return message;
	}
}
