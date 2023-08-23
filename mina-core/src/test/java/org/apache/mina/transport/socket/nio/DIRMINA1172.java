
package org.apache.mina.transport.socket.nio;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.service.AbstractIoService;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.FilterEvent;
import org.apache.mina.filter.logging.LoggingFilter;
import org.junit.Before;
import org.junit.Test;

public class DIRMINA1172
{
    private static DatagramSocket socket;
    private static InetAddress address;
    private static byte[] buf;

    @Before
    public void init()
    {
        AbstractIoService inputSource1 = new NioDatagramAcceptor();
        ((NioDatagramAcceptor) inputSource1).getSessionConfig().setReuseAddress(true);
        DefaultIoFilterChainBuilder filterChainBuilderUDP = ((NioDatagramAcceptor)inputSource1).getFilterChain();
        filterChainBuilderUDP.addLast("logger", new LoggingFilter());

        ((NioDatagramAcceptor) inputSource1).getSessionConfig().setIdleTime(IdleStatus.READER_IDLE, 100000);
        ((NioDatagramAcceptor) inputSource1).setHandler( new IoHandler()
        {
            
            @Override
            public void sessionOpened( IoSession session ) throws Exception
            {
                // TODO Auto-generated method stub
                
            }
            
            
            @Override
            public void sessionIdle( IoSession session, IdleStatus status ) throws Exception
            {
                // TODO Auto-generated method stub
                
            }
            
            
            @Override
            public void sessionCreated( IoSession session ) throws Exception
            {
                // TODO Auto-generated method stub
                
            }
            
            
            @Override
            public void sessionClosed( IoSession session ) throws Exception
            {
                // TODO Auto-generated method stub
                
            }
            
            
            @Override
            public void messageSent( IoSession session, Object message ) throws Exception
            {
                // TODO Auto-generated method stub
                
            }
            
            
            @Override
            public void messageReceived( IoSession session, Object message ) throws Exception
            {
                // TODO Auto-generated method stub
                System.out.println( "1"+session );
                
            }
            
            
            @Override
            public void inputClosed( IoSession session ) throws Exception
            {
                // TODO Auto-generated method stub
                
            }
            
            
            @Override
            public void exceptionCaught( IoSession session, Throwable cause ) throws Exception
            {
                // TODO Auto-generated method stub
                
            }
            
            
            @Override
            public void event( IoSession session, FilterEvent event ) throws Exception
            {
                // TODO Auto-generated method stub
                
            }
        });

        AbstractIoService inputSource2 = new NioDatagramAcceptor();
        ((NioDatagramAcceptor) inputSource2).getSessionConfig().setReuseAddress(true);
        DefaultIoFilterChainBuilder filterChainBuilderUDP2 = ((NioDatagramAcceptor)inputSource2).getFilterChain();
        filterChainBuilderUDP2.addLast("logger", new LoggingFilter());

        ((NioDatagramAcceptor) inputSource2).getSessionConfig().setIdleTime(IdleStatus.READER_IDLE, 100000);
        ((NioDatagramAcceptor) inputSource2).setHandler( new IoHandler()
        {
            
            @Override
            public void sessionOpened( IoSession session ) throws Exception
            {
                // TODO Auto-generated method stub
                
            }
            
            
            @Override
            public void sessionIdle( IoSession session, IdleStatus status ) throws Exception
            {
                // TODO Auto-generated method stub
                
            }
            
            
            @Override
            public void sessionCreated( IoSession session ) throws Exception
            {
                // TODO Auto-generated method stub
                
            }
            
            
            @Override
            public void sessionClosed( IoSession session ) throws Exception
            {
                // TODO Auto-generated method stub
                
            }
            
            
            @Override
            public void messageSent( IoSession session, Object message ) throws Exception
            {
                // TODO Auto-generated method stub
                
            }
            
            
            @Override
            public void messageReceived( IoSession session, Object message ) throws Exception
            {
                // TODO Auto-generated method stub
                System.out.println( "2:"+session );
                
            }
            
            
            @Override
            public void inputClosed( IoSession session ) throws Exception
            {
                // TODO Auto-generated method stub
                
            }
            
            
            @Override
            public void exceptionCaught( IoSession session, Throwable cause ) throws Exception
            {
                // TODO Auto-generated method stub
                
            }
            
            
            @Override
            public void event( IoSession session, FilterEvent event ) throws Exception
            {
                // TODO Auto-generated method stub
                
            }
        });

        try {
            ((NioDatagramAcceptor)inputSource1).bind(new InetSocketAddress(9800));
            ((NioDatagramAcceptor)inputSource2).bind(new InetSocketAddress(9801));
        } catch (IOException e) {
            //log.error("Failed to connect {}", e);
        }
    }

    @Test
    public void test() throws InterruptedException, IOException
    {
        socket = new DatagramSocket();
        address = InetAddress.getByName("localhost");
        
        int[] ports = new int[]{9800, 9801};

        while(true) {
            
            for (int port : ports ) {
                String msg = "TEST_" + port + " " + String.valueOf(System.currentTimeMillis());
                buf = msg.getBytes();
                DatagramPacket packet = new DatagramPacket(buf, buf.length, address, port);
                socket.send(packet);
                System.out.println("Send: " + msg);
            }
            
            Thread.sleep(5000);
        }
    }

}
