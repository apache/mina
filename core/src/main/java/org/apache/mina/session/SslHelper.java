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
package org.apache.mina.session;

import static org.apache.mina.session.AttributeKey.createKey;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Queue;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;

import org.apache.mina.api.IoClient;
import org.apache.mina.api.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An helper class used to manage everything related to SSL/TLS establishment
 * and management.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class SslHelper {
    /** A logger for this class */
    private final static Logger LOGGER = LoggerFactory.getLogger(SslHelper.class);

    /** The SSL engine instance */
    private SSLEngine sslEngine;

    /** The SSLContext instance */
    private final SSLContext sslContext;

    /** The current session */
    private final IoSession session;

    /**
     * A session attribute key that should be set to an {@link InetSocketAddress}.
     * Setting this attribute causes
     * {@link SSLContext#createSSLEngine(String, int)} to be called passing the
     * hostname and port of the {@link InetSocketAddress} to get an
     * {@link SSLEngine} instance. If not set {@link SSLContext#createSSLEngine()}
     * will be called.<br/>
     * Using this feature {@link SSLSession} objects may be cached and reused
     * when in client mode.
     *
     * @see SSLContext#createSSLEngine(String, int)
     */
    public static final AttributeKey<InetSocketAddress> PEER_ADDRESS = createKey(InetSocketAddress.class,
            "internal_peerAddress");

    public static final AttributeKey<Boolean> WANT_CLIENT_AUTH = createKey(Boolean.class, "internal_wantClientAuth");

    public static final AttributeKey<Boolean> NEED_CLIENT_AUTH = createKey(Boolean.class, "internal_needClientAuth");

    /** Incoming buffer accumulating bytes read from the channel */
    /** An empty buffer used during the handshake phase */
    private static final ByteBuffer EMPTY_BUFFER = ByteBuffer.allocate(0);

    /** An empty buffer used during the handshake phase */
    private static final ByteBuffer HANDSHAKE_BUFFER = ByteBuffer.allocate(1024);
    
    private ByteBuffer previous = null;

    /**
     * Create a new SSL Handler.
     *
     * @param session The associated session
     */
    public SslHelper(IoSession session, SSLContext sslContext) {
        this.session = session;
        this.sslContext = sslContext;
    }

    /**
     * @return The associated session
     */
    /* no qualifier */IoSession getSession() {
        return session;
    }

    /**
     * @return The associated SSLEngine
     */
    /* no qualifier */SSLEngine getEngine() {
        return sslEngine;
    }

    boolean isHanshaking() {
        return sslEngine.getHandshakeStatus() != HandshakeStatus.NOT_HANDSHAKING;
    }
    
    /**
     * Initialize the SSL handshake.
     *
     * @throws SSLException If the underlying SSLEngine handshake initialization failed
     */
    /* no qualifier */void init() throws SSLException {
        if (sslEngine != null) {
            // We already have a SSL engine created, no need to create a new one
            return;
        }

        LOGGER.debug("{} Initializing the SSL Helper", session);

        InetSocketAddress peer = session.getAttribute(PEER_ADDRESS, null);

        // Create the SSL engine here
        if (peer == null) {
            sslEngine = sslContext.createSSLEngine();
        } else {
            sslEngine = sslContext.createSSLEngine(peer.getHostName(), peer.getPort());
        }

        // Initialize the engine in client mode if necessary
        sslEngine.setUseClientMode(session.getService() instanceof IoClient);

        // Initialize the different SslEngine modes
        if (!sslEngine.getUseClientMode()) {
            // Those parameters are only valid when in server mode
            boolean needClientAuth = session.getAttribute(NEED_CLIENT_AUTH, false);
            boolean wantClientAuth = session.getAttribute(WANT_CLIENT_AUTH, false);

            // The WantClientAuth supersede the NeedClientAuth, if set.
            if (needClientAuth) {
                sslEngine.setNeedClientAuth(true);
            }

            if (wantClientAuth) {
                sslEngine.setWantClientAuth(true);
            }
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("{} SSL Handler Initialization done.", session);
        }
    }

    /**
     * Duplicate a byte buffer for storing it into this context for future
     * use.
     * 
     * @param buffer the buffer to duplicate
     * @return the newly allocated buffer
     */
    private ByteBuffer duplicate(ByteBuffer buffer) {
        ByteBuffer newBuffer = ByteBuffer.allocateDirect(buffer.remaining() * 2);
        newBuffer.put(buffer);
        newBuffer.flip();
        return newBuffer;
    }
    
    /**
     * Accumulate the given buffer into the current context. Allocation is performed only
     * if needed.
     * 
     * @param buffer the buffer to accumulate
     * @return the accumulated buffer
     */
    private ByteBuffer accumulate(ByteBuffer buffer) {
        if (previous.capacity() - previous.remaining() > buffer.remaining()) {
            int oldPosition = previous.position();
            previous.position(previous.limit());
            previous.limit(previous.limit() + buffer.remaining());
            previous.put(buffer);
            previous.position(oldPosition);
        } else {
            ByteBuffer newPrevious = ByteBuffer.allocateDirect((previous.remaining() + buffer.remaining() ) * 2);
            newPrevious.put(previous);
            newPrevious.put(buffer);
            newPrevious.flip();
            previous = newPrevious;
        }
        return previous;
    }
    
    /**
     * Process a read ByteBuffer over a secured connection, or during the SSL/TLS
     * Handshake.
     * 
     * @param session The session we are processing a read for
     * @param readBuffer The data we get from the channel
     * @throws SSLException If the unwrapping or handshaking failed
     */
    public void processRead(AbstractIoSession session, ByteBuffer readBuffer) throws SSLException {
        ByteBuffer tempBuffer;
        
        if (previous != null) {
            tempBuffer = accumulate(readBuffer);
        } else {
            tempBuffer = readBuffer;
        }
        
        
        boolean done = false;
        SSLEngineResult result;
        ByteBuffer appBuffer = ByteBuffer.allocateDirect(sslEngine.getSession().getApplicationBufferSize());
        
        HandshakeStatus handshakeStatus = sslEngine.getHandshakeStatus();
        while (!done) {
            switch (handshakeStatus) {
            case NEED_UNWRAP:
            case NOT_HANDSHAKING:
            case FINISHED:
              result = sslEngine.unwrap(tempBuffer, appBuffer);
              handshakeStatus = result.getHandshakeStatus();

              switch (result.getStatus()) {
              case BUFFER_UNDERFLOW:
                  /* we need more data */
                  done = true;
                  break;
              case BUFFER_OVERFLOW:
                  /* resize output buffer */
                  appBuffer = ByteBuffer.allocateDirect(appBuffer.capacity() * 2);
                  break;
              case OK:
                  if ((handshakeStatus == HandshakeStatus.NOT_HANDSHAKING) &&
                      (result.bytesProduced() > 0)) {
                      appBuffer.flip();
                      session.processMessageReceived(appBuffer);
                  }
              }
              break;
            case NEED_TASK:
                Runnable task;
                
                while ((task = sslEngine.getDelegatedTask()) != null) {
                    task.run();
                }
                handshakeStatus = sslEngine.getHandshakeStatus();
                break;
            case NEED_WRAP:
                result = sslEngine.wrap(EMPTY_BUFFER, appBuffer);
                handshakeStatus = result.getHandshakeStatus();
                switch (result.getStatus()) {
                case  BUFFER_OVERFLOW:
                    appBuffer = ByteBuffer.allocateDirect(appBuffer.capacity() * 2);
                    break;
                case BUFFER_UNDERFLOW:
                    done = true;
                    break;
                case CLOSED:
                case OK:
                    appBuffer.flip();
                    WriteRequest writeRequest = new DefaultWriteRequest(readBuffer);
                    writeRequest.setMessage(appBuffer);
                    session.enqueueWriteRequest(writeRequest);
                    break;
                }
            }
        }
        if (tempBuffer.remaining() > 0) {
            previous = duplicate(tempBuffer);
        } else {
            previous = null;
        }
        readBuffer.clear();
     }

    /**
     * Process the application data encryption for a session.
     * @param session The session sending encrypted data to the peer.
     * @param message The message to encrypt
     * @param writeQueue The queue in which the encrypted buffer will be written
     * @return The written WriteRequest
     */
    /** No qualifier */
    WriteRequest processWrite(IoSession session, Object message, Queue<WriteRequest> writeQueue) {
        ByteBuffer buf = (ByteBuffer) message;
        ByteBuffer appBuffer = ByteBuffer.allocate(sslEngine.getSession().getPacketBufferSize());

        try {
            while (true) {
                // Encrypt the message
                SSLEngineResult result = sslEngine.wrap(buf, appBuffer);

                switch (result.getStatus()) {
                case BUFFER_OVERFLOW:
                    // Increase the buffer size as needed
                    appBuffer = ByteBuffer.allocate(appBuffer.capacity() + 4096);
                    break;

                case BUFFER_UNDERFLOW:
                case CLOSED:
                    break;

                case OK:
                    // We are done. Flip the buffer and push it to the write queue.
                    appBuffer.flip();
                    WriteRequest request = new DefaultWriteRequest(appBuffer);

                    return request;
                }
            }
        } catch (SSLException se) {
            throw new IllegalStateException(se.getMessage());
        }
    }
}
