package org.apache.mina.example.rce;

import org.apache.mina.core.buffer.matcher.FullClassNameMatcher;
import org.apache.mina.core.buffer.matcher.RegexpClassNameMatcher;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.serialization.ObjectSerializationCodecFactory;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import java.io.IOException;
import java.net.InetSocketAddress;

public class MinaServer {
    private static final int PORT = 9123;

    public static void main(String[] args) throws IOException {
        IoAcceptor acceptor = new NioSocketAcceptor();
        ObjectSerializationCodecFactory codec = new ObjectSerializationCodecFactory();
        codec.accept(new RegexpClassNameMatcher("java.util.Collections.*"));
        codec.accept(new RegexpClassNameMatcher("org.apache.commons.collections4.*"));
        codec.accept(new RegexpClassNameMatcher("java.lang.*"));

        codec.accept(new FullClassNameMatcher(
            "javax.management.BadAttributeValueExpException",
            "java.util.ArrayList",
            "java.util.HashMap"));

        /*
        codec.accept(new FullClassNameMatcher(
            "javax.management.BadAttributeValueExpException",
            "java.lang.Exception",
            "java.lang.Throwable",
            "java.lang.StackTraceElement",
            "java.util.Collections$UnmodifiableList",
            "java.util.Collections$UnmodifiableCollection",
            "java.util.ArrayList",
            "org.apache.commons.collections4.keyvalue.TiedMapEntry",
            "org.apache.commons.collections4.map.LazyMap",
            "org.apache.commons.collections4.functors.ChainedTransformer",
            "org.apache.commons.collections4.functors.ConstantTransformer",
            "org.apache.commons.collections4.functors.InvokerTransformer",
            "java.lang.String",
            "java.lang.Integer",
            "java.lang.Number",
            "java.util.HashMap"));
             */
        
        acceptor.getFilterChain().addLast("codec", new ProtocolCodecFilter(codec));
        acceptor.setHandler(new ServerHandler());
        acceptor.bind(new InetSocketAddress(PORT));
        System.out.println("Mina Server started on port " + PORT);
    }
    
    
    private static class ServerHandler extends IoHandlerAdapter {
        @Override
        public void messageReceived(IoSession session, Object message) {
            System.out.println("Received: " + message);
            session.write("Server Response: " + message);
        } 
    }
}