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
package org.apache.mina.filterchain;

import java.nio.ByteBuffer;

import org.apache.mina.api.IoFilter;
import org.apache.mina.api.IoFuture;
import org.apache.mina.api.IoService;
import org.apache.mina.api.IoSession;
import org.apache.mina.session.DefaultWriteRequest;
import org.apache.mina.session.WriteRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The default implementation of the {@link IoFilterController}
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class DefaultIoFilterController implements IoFilterController, ReadFilterChainController,
        WriteFilterChainController {
    /** A logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger(DefaultIoFilterController.class);

    /** the current position n the write chain for this thread */
    private int writeChainPosition;

    /** the current position in the read chain for this thread */
    private int readChainPosition;

    /** hold the last WriteRequest created for the high level message currently written (can be null) */
    private WriteRequest lastWriteRequest;

    /**
     * The list of {@link IoFilter} implementing this chain.
     */
    private final IoFilter[] chain;

    /**
     * The instance of {@link DefaultIoFilterController} with the {@link IoService} chain.
     */
    public DefaultIoFilterController(IoFilter[] chain) {
        if (chain == null) {
            throw new IllegalArgumentException("chain");
        }
        
        this.chain = chain;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void processSessionCreated(IoSession session) {
        LOG.debug("processing session created event for session {}", session);
        
        for (IoFilter filter : chain) {
            filter.sessionCreated(session);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void processSessionOpened(IoSession session) {
        LOG.debug("processing session open event");
        
        for (IoFilter filter : chain) {
            filter.sessionOpened(session);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void processSessionClosed(IoSession session) {
        LOG.debug("processing session closed event");
        
        for (IoFilter filter : chain) {
            filter.sessionClosed(session);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void processMessageReceived(IoSession session, Object message) {
        LOG.debug("processing message '{}' received event ", message);
        
        if (chain.length < 1) {
            LOG.debug("Nothing to do, the chain is empty");
        } else {
            readChainPosition = 0;
            // we call the first filter, it's supposed to call the next ones using the filter chain controller
            chain[readChainPosition].messageReceived(session, message, this);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void processMessageWriting(IoSession session, Object message, IoFuture<Void> future) {
        LOG.debug("processing message '{}' writing event ", message);

        lastWriteRequest = null;

        if (chain.length < 1) {
            enqueueFinalWriteMessage(session, message);
        } else {
            writeChainPosition = chain.length - 1;
            // we call the first filter, it's supposed to call the next ones using the filter chain controller
            int position = writeChainPosition;
            IoFilter nextFilter = chain[position];
            nextFilter.messageWriting(session, message, this);
        }
        
        // put the future in the last write request
        if (future != null) {
            WriteRequest request = lastWriteRequest;
            
            
            if (request != null) {
                ((DefaultWriteRequest) request).setFuture(future);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void callWriteNextFilter(IoSession session, Object message) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("calling next filter for writing for message '{}' position : {}", message,
                    writeChainPosition);
        }
        
        writeChainPosition--;
        
        if (writeChainPosition < 0 || chain.length == 0) {
            // end of chain processing
            enqueueFinalWriteMessage(session, message);
        } else {
            chain[writeChainPosition].messageWriting(session, message, this);
        }
        
        writeChainPosition++;;
    }

    /**
     * At the end of write chain processing, enqueue final encoded {@link ByteBuffer} message in the session
     */
    private void enqueueFinalWriteMessage(IoSession session, Object message) {
        LOG.debug("end of write chan we enqueue the message in the session : {}", message);
        lastWriteRequest = session.enqueueWriteRequest(message);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void callReadNextFilter(IoSession session, Object message) {
        readChainPosition++;
        
        if (readChainPosition >= chain.length) {
            // end of chain processing
        } else {
            chain[readChainPosition].messageReceived(session, message, this);
        }
        
        readChainPosition--;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder bldr = new StringBuilder("IoFilterChain {");
        int index = 0;
        
        for (IoFilter filter : chain) {
            bldr.append(index).append(":").append(filter).append(", ");
        }
        
        return bldr.append("}").toString();
    }
}
