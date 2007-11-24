package org.apache.mina.integration.jmx;

import java.net.InetSocketAddress;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

public class Foo {
    public static void main(String[] args) throws Exception {
        final MBeanServer server = MBeanServerFactory.findMBeanServer(null).get(0);
        IoAcceptor service = new NioSocketAcceptor();
        service.setHandler(new IoHandlerAdapter() {
            @Override
            public void sessionOpened(IoSession session) throws Exception {
                server.registerMBean(
                        new DefaultModelMBean(session),
                        new ObjectName("org.apache.mina:type=session,name=" + session.getId()));
            }
            
            @Override
            public void sessionClosed(IoSession session) throws Exception {
                server.unregisterMBean(new ObjectName(
                        "org.apache.mina:type=session,name=" + session.getId()));
            }
        });
        service.setLocalAddress(new InetSocketAddress(8080));
        service.bind();
        
        server.registerMBean(
                new DefaultModelMBean(service),
                new ObjectName("org.apache.mina:type=service,name=myService"));
        
        Thread.sleep(1000000);
    }
}
