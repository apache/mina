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
package org.apache.mina.example.tennis;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.transport.vmpipe.VmPipeAcceptor;
import org.apache.mina.transport.vmpipe.VmPipeAddress;
import org.apache.mina.transport.vmpipe.VmPipeConnector;

/**
 * (<b>Entry point</b>) An 'in-VM pipe' example which simulates a tennis game
 * between client and server.
 * <ol>
 *   <li>Client connects to server</li>
 *   <li>At first, client sends {@link TennisBall} with TTL value '10'.</li>
 *   <li>Received side (either server or client) decreases the TTL value of the
 *     received ball, and returns it to remote peer.</li>
 *   <li>Who gets the ball with 0 TTL loses.</li>
 * </ol>
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class Main {

    public static void main(String[] args) throws Exception {
        IoAcceptor acceptor = new VmPipeAcceptor();
        VmPipeAddress address = new VmPipeAddress(8080);

        // Set up server
        acceptor.setHandler(new TennisPlayer());
        acceptor.bind(address);

        // Connect to the server.
        VmPipeConnector connector = new VmPipeConnector();
        connector.setHandler(new TennisPlayer());
        ConnectFuture future = connector.connect(address);
        future.awaitUninterruptibly();
        IoSession session = future.getSession();

        // Send the first ping message
        session.write(new TennisBall(10));

        // Wait until the match ends.
        session.getCloseFuture().awaitUninterruptibly();

        acceptor.unbind();
    }
}
