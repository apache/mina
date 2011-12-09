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

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLEngineResult.Status;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;

import org.apache.mina.api.IoClient;
import org.apache.mina.api.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An helper class used to manage everything related to SSL/TLS establishement
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

    /** The Handshake status */
    private SSLEngineResult.HandshakeStatus handshakeStatus;

    /** Application cleartext data to be read by application */
    private ByteBuffer appBuffer;

    /** Incoming buffer accumulating bytes read from the channel */
    private ByteBuffer accBuffer;
    
    /**
     * Create a new SSL Handler.
     *
     * @param session The associated session
     * @throws SSLException
     */
    SslHelper(IoSession session, SSLContext sslContext) throws SSLException {
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
    public SSLEngine getEngine() {
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

            // The WantClientAuth superseed the NeedClientAuth, if set.
            if ((needClientAuth != null) && (needClientAuth)) {
                sslEngine.setNeedClientAuth(true);
            }
            
            if ((wantClientAuth != null) && (wantClientAuth)) {
                sslEngine.setWantClientAuth(true);
            }
        }

        handshakeStatus = sslEngine.getHandshakeStatus();

        if ( LOGGER.isDebugEnabled()) {
            LOGGER.debug("{} SSL Handler Initialization done.", session);
        }
    }

    /**
     * Get the local AppBuffer
     * @return The Application Buffer allocated in the SslHelper, if any
     */
    public ByteBuffer getAppBuffer() {
        return appBuffer;
    }

    public ByteBuffer getSslInBuffer(ByteBuffer inBuffer) {
        if (inBuffer == null) {
            inBuffer = inBuffer;
        } else {
            addInBuffer(inBuffer);
        }
        
        return inBuffer;
    }

    public void setInBuffer(ByteBuffer inBuffer) {
        inBuffer = ByteBuffer.allocate(16*1024);
        inBuffer.put(inBuffer);
    }

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
    
    public void releaseInBuffer() {
        accBuffer = null;
    }
    
    /**
     * Process the NEED_TASK action.
     * 
     * @param engine The SSLEngine instance
     * @return The resulting HandshakeStatus
     * @throws SSLException If we've got an error while processing the tasks
     */
    public HandshakeStatus processTasks(SSLEngine engine) throws SSLException {
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
     * the application buffer. We also have to cover the three special cases : <br/>
     * <ul>
     * <li>The incoming buffer does not contain enough data (then we need to read some
     * more and to accumulate the bytes in a temporary buffer)</li>
     * <li></li>
     * </ul>
     * 
     * @param engine
     * @param inBuffer
     * @param appBuffer
     * @return
     * @throws SSLException
     */
    public Status processUnwrap(SSLEngine engine, ByteBuffer inBuffer, ByteBuffer appBuffer) throws SSLException {
        ByteBuffer tempBuffer = null;
        
        // First work with either the new incoming buffer, or the accumulating buffer
        if ((this.accBuffer != null) && (this.accBuffer.remaining() > 0)) {
            // Add the new incoming data into the local buffer
            addInBuffer(inBuffer);
            tempBuffer = this.accBuffer;
        } else {
            tempBuffer = inBuffer;
        }
        
        // Loop until we have processed the entire incoming buffer,
        // or until we have to stop
        while (true) {
            SSLEngineResult result = engine.unwrap(tempBuffer, appBuffer);

            switch (result.getStatus()) {
                case OK :
                    System.out.println( "------------------> OK" );
                    accBuffer = null;
                    
                    return Status.OK;
                    
                case BUFFER_UNDERFLOW :
                    System.out.println( "------------------> UNDERFLOW" );
                    // We need to read some more data from the channel.
                    if (this.accBuffer == null) {
                        this.accBuffer = ByteBuffer.allocate(tempBuffer.capacity() + 4096);
                        this.accBuffer.put(inBuffer);
                    }
                    
                    inBuffer.clear();
                    
                    return Status.BUFFER_UNDERFLOW;
    
                case CLOSED :
                    System.out.println( "------------------> CLOSED" );
                    // Get out
                    accBuffer = null;
                    throw new IllegalStateException();
    
                case BUFFER_OVERFLOW :
                    System.out.println( "------------------> OVERFLOW" );
                    // We have to increase the appBuffer size. In any case
                    // we aren't processing an handshake here. Read again.
                    appBuffer = ByteBuffer.allocate(appBuffer.capacity() + 4096 );
            }
        }
    }
}
