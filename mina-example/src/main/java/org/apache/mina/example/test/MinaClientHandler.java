package org.apache.mina.example.test;

import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;

import java.util.logging.Logger;

public class MinaClientHandler extends IoHandlerAdapter {
    private final Logger logger = Logger.getLogger(String.valueOf(getClass()));
    private final String values;
    private boolean finished;

    public MinaClientHandler(String values) {
        this.values = values;
    }

    public boolean isFinished() {
        return finished;
    }
    @Override
    public void sessionOpened(IoSession session) {
        session.write(values);
    }
    @Override
    public void messageReceived(IoSession session, Object message) {
        logger.info("Message received in the client..");
        logger.info("Message is: " + message.toString());
    }

    @Override
    public void exceptionCaught(IoSession session, Throwable cause) {
        session.close();
    }
}