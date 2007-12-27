package org.apache.mina.transport.socket.nio;

import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.IoConnector;
import org.apache.mina.transport.AbstractFileRegionTest;

public class NioFileRegionTest extends AbstractFileRegionTest{

    @Override
    protected IoAcceptor createAcceptor() {
        return new NioSocketAcceptor();
    }

    @Override
    protected IoConnector createConnector() {
        return new NioSocketConnector();
    }

}
