package org.apache.mina.example.imagine.step1.client;

import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.RuntimeIoException;
import org.apache.mina.example.imagine.step1.ImageRequest;
import org.apache.mina.example.imagine.step1.ImageResponse;
import org.apache.mina.example.imagine.step1.codec.ImageCodecFactory;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.SocketConnector;
import org.apache.mina.transport.socket.nio.NioSocketConnector;

import java.net.InetSocketAddress;

public class ImageClient extends IoHandlerAdapter {
    public static final int CONNECT_TIMEOUT = 3000;

    private String host;
    private int port;
    private SocketConnector connector;
    private IoSession session;
    private ImageListener imageListener;

    public ImageClient(String host, int port, ImageListener imageListener) {
        this.host = host;
        this.port = port;
        this.imageListener = imageListener;
        connector = new NioSocketConnector();
        connector.getFilterChain().addLast("codec", new ProtocolCodecFilter(new ImageCodecFactory(true)));
        connector.setHandler(this);
    }

    public void connect() {
        ConnectFuture connectFuture = connector.connect(new InetSocketAddress(host, port));
        connectFuture.awaitUninterruptibly(CONNECT_TIMEOUT);
        try {
            session = connectFuture.getSession();
        }
        catch (RuntimeIoException e) {
            imageListener.onException(e);
        }
    }

    public void disconnect() {
        if (session != null) {
            session.close().awaitUninterruptibly(CONNECT_TIMEOUT);
            session = null;
        }
    }

    public void sessionOpened(IoSession session) throws Exception {
        imageListener.sessionOpened();
    }

    public void sessionClosed(IoSession session) throws Exception {
        imageListener.sessionClosed();
    }

    public void sendRequest(ImageRequest imageRequest) {
        if (session == null) {
            //noinspection ThrowableInstanceNeverThrown
            imageListener.onException(new Throwable("not connected"));
        } else {
            session.write(imageRequest);
        }
    }

    public void messageReceived(IoSession session, Object message) throws Exception {
        ImageResponse response = (ImageResponse) message;
        imageListener.onImages(response.getImage1(), response.getImage2());
    }

    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        imageListener.onException(cause);
    }

}
