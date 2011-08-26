package org.apache.mina.http;

import java.net.InetSocketAddress;

import org.apache.mina.HttpProtocol;
import org.apache.mina.api.IoService;
import org.apache.mina.api.IoServiceListener;
import org.apache.mina.api.IoSession;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.service.OneThreadSelectorStrategy;
import org.apache.mina.service.SelectorFactory;
import org.apache.mina.transport.tcp.NioSelectorProcessor;
import org.apache.mina.transport.tcp.nio.NioTcpServer;
import org.junit.Test;

public class HttpTest {

    @Test
    public void simpleServer() throws Exception {

        OneThreadSelectorStrategy strategy = new OneThreadSelectorStrategy(new SelectorFactory(
                NioSelectorProcessor.class));
        NioTcpServer acceptor = new NioTcpServer(strategy);

        acceptor.addListener(new IoServiceListener() {

            @Override
            public void sessionDestroyed(IoSession session) {
            }

            @Override
            public void sessionCreated(IoSession session) {
                session.getFilterChain().getChain().add(new HttpProtocol());
                session.getFilterChain().getChain().add(new LoggingFilter("Logging"));
            }

            @Override
            public void serviceInactivated(IoService service) {
            }

            @Override
            public void serviceActivated(IoService service) {
            }
        });

        acceptor.bind(new InetSocketAddress(8080));
        Thread.sleep(20000);
        acceptor.unbindAll();

    }
}
