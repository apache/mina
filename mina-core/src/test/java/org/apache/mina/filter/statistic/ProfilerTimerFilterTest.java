package org.apache.mina.filter.statistic;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoEventType;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests {@link ProfilerTimerFilter}.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class ProfilerTimerFilterTest {

	private NioSocketAcceptor acceptor;
	private ProfilerTimerFilter filter;

	@Before
	public void setUp() {
		acceptor = new NioSocketAcceptor();
		filter = new ProfilerTimerFilter();
	}

	@After
	public void tearDown() {
		acceptor.dispose(true);
	}

	@Test
	public void shouldNotThrowArithmeticExceptionWhenTheStatisticsIsEmpty() throws IOException {
		acceptor.getFilterChain().addFirst("statistics", filter);
		acceptor.setHandler(new IoHandlerAdapter());
		acceptor.bind();
		
		filter.getAverageTime(IoEventType.MESSAGE_RECEIVED);
	}

	@Test
	public void shouldGetAverageGreaterThan0() throws IOException, InterruptedException {
		acceptor.getFilterChain().addFirst("statistics", filter);
		 
		final CountDownLatch countDownReceivedMessage = new CountDownLatch(1);
		
		 acceptor.setHandler(new IoHandlerAdapter() {
			@Override
			public void messageReceived(IoSession session, Object message) throws Exception {
				Thread.sleep(100);
				countDownReceivedMessage.countDown();
			}
		});
		acceptor.bind();

		connectAndWrite(acceptor.getLocalAddress());
		
		countDownReceivedMessage.await();
		
		double average = filter.getAverageTime(IoEventType.MESSAGE_RECEIVED);
		assertTrue("Average should be greater than 0", average > 0.0);
	}

	private void connectAndWrite(InetSocketAddress address) {
		NioSocketConnector connector = new NioSocketConnector();
		connector.setHandler(new IoHandlerAdapter());
		ConnectFuture connectFuture = connector.connect(address);
		connectFuture.awaitUninterruptibly(5000);
		
		IoBuffer message = IoBuffer.allocate(4).putInt(65000).flip();
		IoSession session = connectFuture.getSession();
		
		session.write(message).awaitUninterruptibly(5000);
	}

}
