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
package org.apache.mina.transport.socket.nio.support;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoFilterChain;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.IoFilter.WriteRequest;
import org.apache.mina.common.support.AbstractIoFilterChain;
import org.apache.mina.util.Queue;

/**
 * An {@link IoFilterChain} for datagram transport (UDP/IP).
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 */
class DatagramFilterChain extends AbstractIoFilterChain {

    DatagramFilterChain(IoSession parent) {
        super(parent);
    }

    protected void doWrite(IoSession session, WriteRequest writeRequest) {
        DatagramSessionImpl s = (DatagramSessionImpl) session;
        Queue writeRequestQueue = s.getWriteRequestQueue();

        // SocketIoProcessor.doFlush() will reset it after write is finished
        // because the buffer will be passed with messageSent event. 
        ByteBuffer buffer = (ByteBuffer) writeRequest.getMessage();
        buffer.mark();
        
        int remaining = buffer.remaining();
        if (remaining == 0) {
            s.increaseScheduledWriteRequests();            
        } else {
            s.increaseScheduledWriteBytes(buffer.remaining());
        }

        synchronized (writeRequestQueue) {
            writeRequestQueue.push(writeRequest);
        }
        
        if (session.getTrafficMask().isWritable()) {
            s.getManagerDelegate().flushSession(s);
        }
    }

    protected void doClose(IoSession session) {
        DatagramSessionImpl s = (DatagramSessionImpl) session;
        DatagramService manager = s.getManagerDelegate();
        if (manager instanceof DatagramConnectorDelegate) {
            ((DatagramConnectorDelegate) manager).closeSession(s);
        } else {
            ((DatagramAcceptorDelegate) manager).getListeners()
                    .fireSessionDestroyed(session);
            session.getCloseFuture().setClosed();
        }
    }
}
