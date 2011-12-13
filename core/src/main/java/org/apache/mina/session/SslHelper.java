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

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Queue;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLEngineResult.Status;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;

import org.apache.mina.api.IoClient;
import org.apache.mina.api.IoSession;
import org.apache.mina.api.IoSession.SessionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An helper class used to manage everything related to SSL/TLS establishment
 * and management.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class SslHelper
{
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
    public static final String PEER_ADDRESS = "internal_peerAddress";
    
    public static final String WANT_CLIENT_AUTH = "internal_wantClientAuth";

    public static final String NEED_CLIENT_AUTH = "internal_needClientAuth";

    /** Incoming buffer accumulating bytes read from the channel */
    private ByteBuffer accBuffer;
    
    /** An empty buffer used during the handshake phase */
    private static final ByteBuffer EMPTY_BUFFER = ByteBuffer.allocate(0);

    /** An empty buffer used during the handshake phase */
    private static final ByteBuffer HANDSHAKE_BUFFER = ByteBuffer.allocate(1024);

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
    /* no qualifier */ IoSession getSession() {
        return session;
    }
    
    
    /**
     * @return The associated SSLEngine
     */
    /* no qualifier */ SSLEngine getEngine() {
        return sslEngine;
    }

    /**
     * Initialize the SSL handshake.
     *
     * @throws SSLException If the underlying SSLEngine handshake initialization failed
     */
    /* no qualifier */ void init() throws SSLException {
        if (sslEngine != null) {
            // We already have a SSL engine created, no need to create a new one
            return;
        }

        LOGGER.debug("{} Initializing the SSL Helper", session);

        InetSocketAddress peer = (InetSocketAddress) session.getAttribute(PEER_ADDRESS);

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
            Boolean needClientAuth = session.<Boolean>getAttribute(NEED_CLIENT_AUTH);
            Boolean wantClientAuth = session.<Boolean>getAttribute(WANT_CLIENT_AUTH);

            // The WantClientAuth supersede the NeedClientAuth, if set.
            if ((needClientAuth != null) && (needClientAuth)) {
                sslEngine.setNeedClientAuth(true);
            }
            
            if ((wantClientAuth != null) && (wantClientAuth)) {
                sslEngine.setWantClientAuth(true);
            }
        }

        if ( LOGGER.isDebugEnabled()) {
            LOGGER.debug("{} SSL Handler Initialization done.", session);
        }
    }

    /**
     * A helper method used to accumulate some ByteBuffer in the inner buffer.
     * 
     * @param buffer The buffer to add.
     */
    private void addInBuffer(ByteBuffer buffer) {
        if (accBuffer.capacity() - accBuffer.limit() < buffer.remaining()) {
            // Increase the internal buffer
            ByteBuffer newBuffer = ByteBuffer.allocate(accBuffer.capacity() + buffer.remaining());
            
            // Copy the two buffers
            newBuffer.put(accBuffer);
            newBuffer.put(buffer);
            
            // And reset the position to the original position
            newBuffer.flip();
            
            accBuffer = newBuffer;
        } else {
            accBuffer.put(buffer);
            accBuffer.flip();
        }
    }
    
    /**
     * Process the NEED_TASK action.
     * 
     * @param engine The SSLEngine instance
     * @return The resulting HandshakeStatus
     * @throws SSLException If we've got an error while processing the tasks
     */
    private HandshakeStatus processTasks(SSLEngine engine) throws SSLException {
        Runnable runnable;
        
        while ((runnable = engine.getDelegatedTask()) != null) {
            // TODO : we may have to use a thread pool here to improve the
            // performances
            runnable.run();
        }

        HandshakeStatus hsStatus = engine.getHandshakeStatus();
        
        return hsStatus;
    }
    
    /**
     * Process the NEED_UNWRAP action. We have to read the incoming buffer, and to feed
     * the application buffer.
     */
    private SSLEngineResult unwrap(ByteBuffer inBuffer, ByteBuffer appBuffer) throws SSLException {
        ByteBuffer tempBuffer = null;
        
        // First work with either the new incoming buffer, or the accumulating buffer
        if ((accBuffer != null) && (accBuffer.remaining() > 0)) {
            // Add the new incoming data into the local buffer
            addInBuffer(inBuffer);
            tempBuffer = this.accBuffer;
        } else {
            tempBuffer = inBuffer;
        }
        
        // Loop until we have processed the entire incoming buffer,
        // or until we have to stop
        while (true) {
            // Do the unwrapping
            SSLEngineResult result = sslEngine.unwrap(tempBuffer, appBuffer);

            switch (result.getStatus()) {
                case OK :
                    // Ok, we have unwrapped a message, return.
                    accBuffer = null;
                    
                    return result;
                    
                case BUFFER_UNDERFLOW :
                    // We need to read some more data from the channel.
                    if (this.accBuffer == null) {
                        this.accBuffer = ByteBuffer.allocate(tempBuffer.capacity() + 4096);
                        this.accBuffer.put(inBuffer);
                    }
                    
                    inBuffer.clear();
                    
                    return result;
    
                case CLOSED :
                    accBuffer = null;

                    // We have received a Close message, we can exit now
                    if (session.isConnectedSecured()) {
                        return result;
                    } else {
                        throw new IllegalStateException();
                    }
    
                case BUFFER_OVERFLOW :
                    // We have to increase the appBuffer size. In any case
                    // we aren't processing an handshake here. Read again.
                    appBuffer = ByteBuffer.allocate(appBuffer.capacity() + 4096 );
            }
        }
    }
    
    /**
     * Process a read ByteBuffer over a secured connection, or during the SSL/TLS
     * Handshake.
     * 
     * @param session The session we are processing a read for
     * @param readBuffer The data we get from the channel
     * @throws SSLException If the unwrapping or handshaking failed
     */
    public void processRead(IoSession session, ByteBuffer readBuffer) throws SSLException {
        if (session.isConnectedSecured()) {
            // Unwrap the incoming data
            processUnwrap(session, readBuffer);
        } else {
            // Process the SSL handshake now
            processHandShake(session, readBuffer);
        }
    }
    

    /**
     * Unwrap a SSL/TLS message. The message might not be encrypted (if we are processing
     * a Handshake message or an Alert message).
     */
    private void processUnwrap(IoSession session, ByteBuffer inBuffer) throws SSLException {
        // Blind guess : once uncompressed, the resulting buffer will be 3 times bigger
        ByteBuffer appBuffer = ByteBuffer.allocate(inBuffer.limit() * 3);
        SSLEngineResult result = unwrap(inBuffer, appBuffer );

        switch (result.getStatus()) {
            case OK :
                // Ok, go through the chain now
                appBuffer.flip();
                session.getFilterChain().processMessageReceived(session, appBuffer);
                break;
                
            case CLOSED :
                // This was a Alert Closure message. Process it
                processClosed( result);
                
                break;
        }
    }
    
    /**
     * Process the SSL/TLS Alert Closure message
     */
    private void processClosed(SSLEngineResult result) throws SSLException {
        // We have received a Alert_CLosure message, we will have to do a wrap
        HandshakeStatus hsStatus = result.getHandshakeStatus();
        
        if (hsStatus == HandshakeStatus.NEED_WRAP) {
            // We need to send back the Alert Closure message
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("{} processing the NEED_WRAP state", session);
            }

            int capacity = sslEngine.getSession().getPacketBufferSize();
            ByteBuffer outBuffer = ByteBuffer.allocate(capacity);
            session.changeState( SessionState.CONNECTED );

            // Loop until the SSLEngine has nothing more to produce
            while (!sslEngine.isOutboundDone()) {
                sslEngine.wrap(EMPTY_BUFFER, outBuffer);
                outBuffer.flip();

                // Get out of the Connected state
                session.enqueueWriteRequest(outBuffer);
            }
        }
    }
    
    /**
     * Process the SLL/TLS Handshake. We may enter in this method more than once,
     * as the handshake is a dialogue between the client and the server.
     */
    private void processHandShake(IoSession session, ByteBuffer inBuffer) throws SSLException {
        // Start the Handshake if we aren't already processing a HandShake
        // and switch to the SECURING state
        HandshakeStatus hsStatus = sslEngine.getHandshakeStatus();
        
        // Initilize the session status when we enter into the Handshake process.
        // Not that we don't call the SSLEngine.beginHandshake() method  :
        // It's implicitely done internally by the unwrap() method.
        if ( hsStatus == HandshakeStatus.NOT_HANDSHAKING) {
            session.changeState(SessionState.SECURING);
        }

        SSLEngineResult result = null;

        // If the SSLEngine has not be started, then the status will be NOT_HANDSHAKING
        // We loop until we reach the FINISHED state
        while (hsStatus != HandshakeStatus.FINISHED) {
            if (hsStatus == HandshakeStatus.NEED_TASK) {
                hsStatus = processTasks(sslEngine);
            } else if (hsStatus == HandshakeStatus.NEED_WRAP) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("{} processing the NEED_WRAP state", session);
                }

                // Create an insanely wide buffer, as the SSLEngine requires it
                int capacity = sslEngine.getSession().getPacketBufferSize();
                ByteBuffer outBuffer = ByteBuffer.allocate(capacity);

                boolean completed = false;
                
                // Loop until we are able to wrap the message (we may have
                // to increase the buffer size more than once.
                while (!completed) {
                    result = sslEngine.wrap(EMPTY_BUFFER, outBuffer);

                    switch (result.getStatus()) {
                        case OK :
                        case CLOSED :
                            completed = true;
                            break;
                            
                        case BUFFER_OVERFLOW :
                            // Increase the target buffer size
                            outBuffer = ByteBuffer.allocate(outBuffer.capacity() + 4096);
                            break;
                    }
                }

                // Done. We can now push this buffer into the write queue.
                outBuffer.flip();
                session.enqueueWriteRequest(outBuffer);
                hsStatus = result.getHandshakeStatus();
                
                // Nothing more to wrap : get out.
                // Note to self : we can probably use only one ByteBuffer for the
                // multiple wrapped messages. (see https://issues.apache.org/jira/browse/DIRMINA-878)
                if (hsStatus != HandshakeStatus.NEED_WRAP) {
                    break;
                }
            } else if ((hsStatus == HandshakeStatus.NEED_UNWRAP) || (hsStatus == HandshakeStatus.NOT_HANDSHAKING)) {
                // We cover the ongoing handshake (NEED_UNWRAP) and
                // the initial call to the handshake (NOT_HANDSHAKING)
                result = unwrap(inBuffer, HANDSHAKE_BUFFER);

                if (result.getStatus() == Status.BUFFER_UNDERFLOW) {
                    // Read more data
                    break;
                } else {
                    hsStatus = result.getHandshakeStatus();
                }
            }
        }

        if (hsStatus == HandshakeStatus.FINISHED) {
            // The handshake has been completed. We can change the session's state.
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("{} processing the FINISHED state", session);
            }
            
            session.changeState(SessionState.SECURED);
        }
    }
    
    /**
     * Process the application data encryption for a session.
     * @param session The session sending encrypted data to the peer.
     * @param message The message to encrypt
     * @param writeQueue The queue in which the encrypted buffer will be written
     * @return The written WriteRequest
     */
    /** No qualifier */ WriteRequest processWrite(IoSession session, Object message, Queue<WriteRequest> writeQueue) {
        ByteBuffer buf = (ByteBuffer)message;
        ByteBuffer appBuffer = ByteBuffer.allocate(sslEngine.getSession().getPacketBufferSize());
        
        try {
            while (true) {
                // Encypt the message
                SSLEngineResult result = sslEngine.wrap(buf, appBuffer);
                
                switch (result.getStatus()) {
                    case BUFFER_OVERFLOW :
                        // Increase the buffer size as needed
                        appBuffer = ByteBuffer.allocate(appBuffer.capacity() + 4096);
                        break;
                        
                    case BUFFER_UNDERFLOW :
                    case CLOSED :
                        break;
                        
                    case OK :
                        // We are done. Flip the buffer and push it to the write queue.
                        appBuffer.flip();
                        WriteRequest request = new DefaultWriteRequest(appBuffer);

                        writeQueue.add(request);
                        
                        return request;
                }
            }
        } catch (SSLException se) {
            throw new IllegalStateException(se.getMessage());
        }
    }
}
