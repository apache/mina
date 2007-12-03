package org.apache.mina.integration.jmx;

import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.filter.executor.ExecutorFilter;
import org.apache.mina.filter.executor.IoEventQueueThrottle;
import org.apache.mina.filter.executor.OrderedThreadPoolExecutor;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

public class Foo {
    public static void main(String[] args) throws Exception {
        final MBeanServer server = ManagementFactory.getPlatformMBeanServer();

        IoAcceptor service = new NioSocketAcceptor();
        service.setHandler(new IoHandlerAdapter());
        
        ExecutorFilter executorFilter = new ExecutorFilter(
                new OrderedThreadPoolExecutor(
                        0, 16, 30, TimeUnit.SECONDS,
                        new IoEventQueueThrottle(1048576)));
        
        service.getFilterChain().addLast("executor", executorFilter);
        service.bind(new InetSocketAddress(8080));
        
        server.registerMBean(
                new IoServiceMBean(service),
                new ObjectName("org.apache.mina:type=service,name=myService"));
        
        server.registerMBean(
                new IoFilterMBean(executorFilter),
                new ObjectName("org.apache.mina:type=filter,name=executor"));

        server.registerMBean(
                new IoFilterMBean(new LoggingFilter()),
                new ObjectName("org.apache.mina:type=filter,name=logger"));
    }
}
