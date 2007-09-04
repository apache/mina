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
package org.apache.mina.transport.socket.nio;

import java.io.IOException;

import org.apache.mina.common.AbstractIoFilterChain;
import org.apache.mina.common.IoFilterChain;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.WriteRequest;

/**
 * An {@link IoFilterChain} for socket transport (TCP/IP).
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
class SocketFilterChain extends AbstractIoFilterChain {

    SocketFilterChain(IoSession parent) {
        super(parent);
    }

    @Override
    protected void doWrite(IoSession session, WriteRequest writeRequest) {
        SocketSessionImpl s = (SocketSessionImpl) session;
        s.queueWriteRequest(writeRequest);
    }

    @Override
    protected void doClose(IoSession session) throws IOException {
        SocketSessionImpl s = (SocketSessionImpl) session;
        s.getIoProcessor().remove(s);
    }
}
