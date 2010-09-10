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
package org.apache.mina.handler.demux;

import org.apache.mina.core.session.IoSession;

/**
 * A handler interface that {@link DemuxingIoHandler} forwards
 * <tt>messageReceived</tt> or <tt>messageSent</tt> events to.  You have to
 * register your handler with the type of the message you want to get notified
 * using {@link DemuxingIoHandler#addReceivedMessageHandler(Class, MessageHandler)}
 * or {@link DemuxingIoHandler#addSentMessageHandler(Class, MessageHandler)}.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public interface MessageHandler<E> {
    /**
     * A {@link MessageHandler} that does nothing.  This is useful when
     * you want to ignore a message of a specific type silently.
     */
    static MessageHandler<Object> NOOP = new MessageHandler<Object>() {
        public void handleMessage(IoSession session, Object message) {
            // Do nothing
        }
    };

    /**
     * Invoked when the specific type of message is received from or sent to
     * the specified <code>session</code>.
     * 
     * @param session the associated {@link IoSession}
     * @param message the message to decode. Its type is set by the implementation
     * @throws Exception if there is an error during the message processing
     */
    void handleMessage(IoSession session, E message) throws Exception;
}