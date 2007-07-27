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
package org.apache.mina.example.chat.client;

import org.apache.mina.common.IoFilter;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.example.chat.ChatCommand;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.TextLineCodecFactory;
import org.apache.mina.filter.logging.LoggingFilter;

/**
 * {@link IoHandler} implementation of the client side of the simple chat protocol.
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class SwingChatClientHandler extends IoHandlerAdapter {

    public interface Callback {
        void connected();

        void loggedIn();

        void loggedOut();

        void disconnected();

        void messageReceived(String message);

        void error(String message);
    }

    private static final IoFilter LOGGING_FILTER = new LoggingFilter();

    private static final IoFilter CODEC_FILTER = new ProtocolCodecFilter(
            new TextLineCodecFactory());

    private final Callback callback;

    public SwingChatClientHandler(Callback callback) {
        this.callback = callback;
    }

    @Override
    public void sessionCreated(IoSession session) throws Exception {
        session.getFilterChain().addLast("codec", CODEC_FILTER);
        session.getFilterChain().addLast("logger", LOGGING_FILTER);
    }

    @Override
    public void sessionOpened(IoSession session) throws Exception {
        callback.connected();
    }

    @Override
    public void messageReceived(IoSession session, Object message)
            throws Exception {
        String theMessage = (String) message;
        String[] result = theMessage.split(" ", 3);
        String status = result[1];
        String theCommand = result[0];
        ChatCommand command = ChatCommand.valueOf(theCommand);

        if ("OK".equals(status)) {

            switch (command.toInt()) {

            case ChatCommand.BROADCAST:
                if (result.length == 3) {
                    callback.messageReceived(result[2]);
                }
                break;
            case ChatCommand.LOGIN:
                callback.loggedIn();
                break;

            case ChatCommand.QUIT:
                callback.loggedOut();
                break;
            }

        } else {
            if (result.length == 3) {
                callback.error(result[2]);
            }
        }
    }

    @Override
    public void sessionClosed(IoSession session) throws Exception {
        callback.disconnected();
    }

}
