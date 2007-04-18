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
package org.apache.mina.transport.serial;

import java.util.Queue;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoFilterChain;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.WriteRequest;
import org.apache.mina.common.support.AbstractIoFilterChain;

/**
 * An {@link IoFilterChain} for serial communication transport.
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev: 529590 $, $Date: 2007-04-17 15:14:17 +0200 (mar., 17 avr. 2007) $
 */
public class SerialFilterChain extends AbstractIoFilterChain {

    protected SerialFilterChain(IoSession session) {
        super(session);
    }

    @Override
    protected void doClose(IoSession session) throws Exception {
        ((SerialSession) session).closeSerialPort();
    }

    @Override
    protected void doWrite(IoSession session, WriteRequest writeRequest)
            throws Exception {
        SerialSession s = (SerialSession) session;
        Queue<WriteRequest> queue = s.getWriteRequestQueue();

        // SocketIoProcessor.doFlush() will reset it after write is finished
        // because the buffer will be passed with messageSent event. 
        ((ByteBuffer) writeRequest.getMessage()).mark();
        synchronized (queue) {
            queue.offer(writeRequest);
            if (queue.size() == 1 && session.getTrafficMask().isWritable()) {
                // Notify serial session worker only when writeRequestQueue was empty.
                s.notifyWriteWorker();
            }
        }
    }

}
