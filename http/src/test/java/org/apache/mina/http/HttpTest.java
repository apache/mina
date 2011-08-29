package org.apache.mina.http;

import java.net.InetSocketAddress;

import org.apache.mina.HttpServerCodec;
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
        acceptor.setFilters(new LoggingFilter("INCOMING"), new HttpServerCodec(), new LoggingFilter("DECODED"));

        acceptor.bind(new InetSocketAddress(8080));
        Thread.sleep(20000);
        acceptor.unbindAll();

    }
}
