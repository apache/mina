package org.apache.mina.example.gettingstarted.timeserver;

import org.apache.mina.core.service.IoConnector;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.TextLineCodecFactory;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.transport.socket.nio.NioSocketConnector;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MinaTimerClient {

    public static void main(String[] args) {
        final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
        IoConnector connector = new NioSocketConnector();
        // Add two filters : a logger and a codec
//        connector.getFilterChain().addLast( "logger", new LoggingFilter() );
        connector.getFilterChain().addLast( "codec", new ProtocolCodecFilter( new TextLineCodecFactory( StandardCharsets.UTF_8)));

        connector.setHandler(new IoHandler() {
            @Override
            public void sessionCreated(IoSession session) throws Exception {
                System.out.println("sessionCreated, session=" + session);
            }

            @Override
            public void sessionOpened(final IoSession session) throws Exception {
                System.out.println("sessionOpened, session=" + session);

                executorService.scheduleAtFixedRate(new Runnable() {
                    @Override
                    public void run() {
                        session.write("what is the time");
                    }
                },1, 1,TimeUnit.SECONDS);
            }

            @Override
            public void sessionClosed(IoSession session) throws Exception {
                System.out.println("sessionClosed, session=" + session);
            }

            @Override
            public void sessionIdle(IoSession session, IdleStatus status) throws Exception {
                System.out.println("sessionIdle, session=" + session);
            }

            @Override
            public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
                System.out.println("exceptionCaught, session=" + session);
            }

            @Override
            public void messageReceived(IoSession session, Object message) throws Exception {
                System.out.println("messageReceived, session=" + session + ", message=" + message);
            }

            @Override
            public void messageSent(IoSession session, Object message) throws Exception {
                System.out.println("messageSent, session=" + session + ", message=" + message);
            }

            @Override
            public void inputClosed(IoSession session) throws Exception {
                System.out.println("inputClosed, session=" + session);
            }
        });
        connector.connect(new InetSocketAddress(MinaTimeServer.PORT));
    }
}
