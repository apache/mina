/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.apache.mina.example.proxy.telnet;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.proxy.AbstractProxyIoHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TelnetSessionHandler.java - Telnet session handler.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * @since MINA 2.0.0-M3
 */
public class TelnetSessionHandler extends AbstractProxyIoHandler {
    private final static Logger logger = LoggerFactory
            .getLogger(TelnetSessionHandler.class);

    /**
     * Default constructor.
     */
    public TelnetSessionHandler() {
    }

    /**
     * {@inheritDoc} 
     */
    @Override
    public void sessionCreated(IoSession session) throws Exception {
        logger.debug("CLIENT - Session created: " + session);
    }

    /**
     * {@inheritDoc} 
     */
    @Override
    public void proxySessionOpened(IoSession session) throws Exception {
        logger.debug("CLIENT - Session opened: " + session);
        final IoSession _session = session;
        // Enter typing loop
        new Thread(new Runnable() {
            public void run() {
                InputStreamReader isr = new InputStreamReader(System.in);
                BufferedReader br = new BufferedReader(isr);

                while (!_session.isClosing()) {
                    try {
                        String line = br.readLine();
                        if (line != null) {
                            _session.write(line);
                        }
                    } catch (Exception e) {
                        break;
                    }
                }

                _session.close(true);
            }

        }).start();
    }

    /**
     * {@inheritDoc} 
     */
    @Override
    public void messageReceived(IoSession session, Object message) {
        //System.out.println((String) message);
    }

    /**
     * {@inheritDoc} 
     */
    @Override
    public void sessionClosed(IoSession session) throws Exception {
        logger.debug("CLIENT - Session closed");
    }

    /**
     * {@inheritDoc} 
     */
    @Override
    public void exceptionCaught(IoSession session, Throwable cause) {
        logger.debug("CLIENT - Exception caught");
        //cause.printStackTrace();
        session.close(true);
    }
}