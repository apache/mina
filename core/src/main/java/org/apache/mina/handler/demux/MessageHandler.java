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

import org.apache.mina.common.IoSession;

/**
 * A handler interface that {@link DemuxingIoHandler} forwards
 * <code>messageReceived</code> events to.  You have to register your
 * handler with the type of message you want to get notified using
 * {@link DemuxingIoHandler#addMessageHandler(Class, MessageHandler)}.
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public interface MessageHandler<E> {
    /**
     * A {@link MessageHandler} that does nothing.  This is usefule when
     * you want to ignore messages of the specific type silently.
     */
    static MessageHandler<Object> NOOP = new MessageHandler<Object>() {
        public void messageReceived(IoSession session, Object message) {
        }
    };

    /**
     * Invoked when the specific type of message is received from the
     * specified <code>session</code>.
     */
    void messageReceived(IoSession session, E message) throws Exception;
}