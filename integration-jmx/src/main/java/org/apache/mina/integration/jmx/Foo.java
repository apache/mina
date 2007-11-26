package org.apache.mina.integration.jmx;

import java.net.InetSocketAddress;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.IoFilter;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

public class Foo {
    public static void main(String[] args) throws Exception {
        final MBeanServer server = MBeanServerFactory.findMBeanServer(null).get(0);
        IoAcceptor service = new NioSocketAcceptor();
        service.setHandler(new IoHandlerAdapter());
        service.setLocalAddress(new InetSocketAddress(8080));
        service.bind();
        
        IoFilter loggingFilter = new LoggingFilter();
        
        server.registerMBean(
                new IoServiceMBean(service),
                new ObjectName("org.apache.mina:type=service,name=myService"));
        
        server.registerMBean(
                new IoFilterMBean(loggingFilter),
                new ObjectName("org.apache.mina:type=filter,name=loggingFilter"));
    }
}
