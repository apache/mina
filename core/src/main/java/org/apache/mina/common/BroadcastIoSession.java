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
package org.apache.mina.common;

import java.net.SocketAddress;

/**
 * An {@link IoSession} for broadcast transports. (e.g. UDP broadcast or multicast)
 * 
 * <h2>Writing Back to the Broadcasting Server</h2>
 * <p>
 * When you implement a client that receives a broadcast message from a server
 * such as DHCP server, the client might need to send a response message for the
 * broadcast message the server sent.  Because the remote address of the session
 * is not the address of the server in case of broadcasting, there should be a
 * way to specify the destination when you write the response message.
 * This interface provides {@link #write(Object, SocketAddress)} method so you
 * can specify the destination.
 * </p>
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public interface BroadcastIoSession extends IoSession {
    /**
     * Writes the specified <tt>message</tt> to the specified <tt>destination</tt>.
     * This operation is asynchronous; {@link IoHandler#messageSent(IoSession, Object)}
     * will be invoked when the message is actually sent to remote peer. You can
     * also wait for the returned {@link WriteFuture} if you want to wait for
     * the message actually written.
     * 
     * @param destination <tt>null</tt> if you want the message sent to the
     *                    default remote address
     */
    WriteFuture write(Object message, SocketAddress destination);
}
