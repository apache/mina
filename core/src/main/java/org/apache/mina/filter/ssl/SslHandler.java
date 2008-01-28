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
package org.apache.mina.filter.ssl;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;

import org.apache.mina.common.DefaultWriteFuture;
import org.apache.mina.common.DefaultWriteRequest;
import org.apache.mina.common.IoBuffer;
import org.apache.mina.common.IoEventType;
import org.apache.mina.common.IoFilterEvent;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.WriteFuture;
import org.apache.mina.common.WriteRequest;
import org.apache.mina.common.IoFilter.NextFilter;
import org.apache.mina.util.CircularQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A helper class using the SSLEngine API to decrypt/encrypt data.
 * <p/>
 * Each connection has a SSLEngine that is used through the lifetime of the connection.
 * We allocate buffers for use as the outbound and inbound network buffers.
 * These buffers handle all of the intermediary data for the SSL connection. To make things easy,
 * we'll require outNetBuffer be completely flushed before trying to wrap any more data.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
class SslHandler {
    
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final SslFilter parent;
    private final SSLContext ctx;
    private final IoSession session;
    private final Queue<IoFilterEvent> preHandshakeEventQueue = new CircularQueue<IoFilterEvent>();
    private final Queue<IoFilterEvent> filterWriteEventQueue = new ConcurrentLinkedQueue<IoFilterEvent>();
    private final Queue<IoFilterEvent> messageReceivedEventQueue = new ConcurrentLinkedQueue<IoFilterEvent>();
    private SSLEngine sslEngine;

    /**
     * Encrypted data from the net
     */
    private IoBuffer inNetBuffer;

    /**
     * Encrypted data to be written to the net
     */
    private IoBuffer outNetBuffer;

    /**
     * Applicaton cleartext data to be read by application
     */
    private IoBuffer appBuffer;

    /**
     * Empty buffer used during initial handshake and close operations
     */
    private final IoBuffer emptyBuffer = IoBuffer.allocate(0);

    private SSLEngineResult.HandshakeStatus handshakeStatus;
    private boolean initialHandshakeComplete;
    private boolean handshakeComplete;
    private boolean writingEncryptedData;

    /**
     * Constuctor.
     *
     * @param sslc
     * @throws SSLException
     */
    public SslHandler(SslFilter parent, SSLContext sslc, IoSession session)
            throws SSLException {
        this.parent = parent;
        this.session = session;
        this.ctx = sslc;
        init();
    }

    public void init() throws SSLException {
        if (sslEngine != null) {
            return;
        }

        InetSocketAddress peer = (InetSocketAddress) session
                .getAttribute(SslFilter.PEER_ADDRESS);
        if (peer == null) {
            sslEngine = ctx.createSSLEngine();
        } else {
            sslEngine = ctx.createSSLEngine(peer.getHostName(), peer.getPort());
        }
        sslEngine.setUseClientMode(parent.isUseClientMode());

        if (parent.isWantClientAuth()) {
            sslEngine.setWantClientAuth(true);
        }

        if (parent.isNeedClientAuth()) {
            sslEngine.setNeedClientAuth(true);
        }

        if (parent.getEnabledCipherSuites() != null) {
            sslEngine.setEnabledCipherSuites(parent.getEnabledCipherSuites());
        }

        if (parent.getEnabledProtocols() != null) {
            sslEngine.setEnabledProtocols(parent.getEnabledProtocols());
        }

        sslEngine.beginHandshake();
        handshakeStatus = sslEngine.getHandshakeStatus();
        
        handshakeComplete = false;
        initialHandshakeComplete = false;
        writingEncryptedData = false;
    }

    /**
     * Release allocated buffers.
     */
    public void destroy() {
        if (sslEngine == null) {
            return;
        }

        // Close inbound and flush all remaining data if available.
        try {
            sslEngine.closeInbound();
        } catch (SSLException e) {
            logger.debug(
                    "Unexpected exception from SSLEngine.closeInbound().", e);
        }

        
        if (outNetBuffer != null) {
            outNetBuffer.capacity(sslEngine.getSession().getPacketBufferSize());
        } else {
            createOutNetBuffer(0);
        }
        try {
            do {
                outNetBuffer.clear();
            } while (sslEngine.wrap(emptyBuffer.buf(), outNetBuffer.buf()).bytesProduced() > 0);
        } catch (SSLException e) {
            // Ignore.
        } finally {
            destroyOutNetBuffer();
        }

        sslEngine.closeOutbound();
        sslEngine = null;

        preHandshakeEventQueue.clear();
    }

    private void destroyOutNetBuffer() {
        outNetBuffer.free();
        outNetBuffer = null;
    }

    public SslFilter getParent() {
        return parent;
    }

    public IoSession getSession() {
        return session;
    }

    /**
     * Check we are writing encrypted data.
     */
    public boolean isWritingEncryptedData() {
        return writingEncryptedData;
    }

    /**
     * Check if handshake is completed.
     */
    public boolean isHandshakeComplete() {
        return handshakeComplete;
    }

    public boolean isInboundDone() {
        return sslEngine == null || sslEngine.isInboundDone();
    }

    public boolean isOutboundDone() {
        return sslEngine == null || sslEngine.isOutboundDone();
    }

    /**
     * Check if there is any need to complete handshake.
     */
    public boolean needToCompleteHandshake() {
        return handshakeStatus == SSLEngineResult.HandshakeStatus.NEED_WRAP && !isInboundDone();
    }

    public void schedulePreHandshakeWriteRequest(NextFilter nextFilter,
                                                 WriteRequest writeRequest) {
        preHandshakeEventQueue.add(new IoFilterEvent(nextFilter,
                IoEventType.WRITE, session, writeRequest));
    }

    public void flushPreHandshakeEvents() throws SSLException {
        IoFilterEvent scheduledWrite;

        while ((scheduledWrite = preHandshakeEventQueue.poll()) != null) {
            parent.filterWrite(scheduledWrite.getNextFilter(), session,
                    (WriteRequest) scheduledWrite.getParameter());
        }
    }

    public void scheduleFilterWrite(NextFilter nextFilter, WriteRequest writeRequest) {
        filterWriteEventQueue.add(new IoFilterEvent(nextFilter, IoEventType.WRITE, session, writeRequest));
    }

    public void scheduleMessageReceived(NextFilter nextFilter, Object message) {
        messageReceivedEventQueue.add(new IoFilterEvent(nextFilter, IoEventType.MESSAGE_RECEIVED, session, message));
    }

    public void flushScheduledEvents() {
        // Fire events only when no lock is hold for this handler.
        if (Thread.holdsLock(this)) {
            return;
        }

        IoFilterEvent e;

        // We need synchronization here inevitably because filterWrite can be
        // called simultaneously and cause 'bad record MAC' integrity error.
        synchronized (this) {
            while ((e = filterWriteEventQueue.poll()) != null) {
                e.getNextFilter().filterWrite(session, (WriteRequest) e.getParameter());
            }
        }

        while ((e = messageReceivedEventQueue.poll()) != null) {
            e.getNextFilter().messageReceived(session, e.getParameter());
        }
    }

    /**
     * Call when data read from net. Will perform inial hanshake or decrypt provided
     * Buffer.
     * Decrytpted data reurned by getAppBuffer(), if any.
     *
     * @param buf        buffer to decrypt
     * @param nextFilter Next filter in chain
     * @throws SSLException on errors
     */
    public void messageReceived(NextFilter nextFilter, ByteBuffer buf) throws SSLException {
        // append buf to inNetBuffer
        if (inNetBuffer == null) {
            inNetBuffer = IoBuffer.allocate(buf.remaining()).setAutoExpand(true);
        }
        
        inNetBuffer.put(buf);
        if (!handshakeComplete) {
            handshake(nextFilter);
        } else {
            decrypt(nextFilter);
        }

        if (isInboundDone()) {
            // Rewind the MINA buffer if not all data is processed and inbound is finished.
            int inNetBufferPosition = inNetBuffer == null? 0 : inNetBuffer.position();
            buf.position(buf.position() - inNetBufferPosition);
            inNetBuffer = null;
        }
    }

    /**
     * Get decrypted application data.
     *
     * @return buffer with data
     */
    public IoBuffer fetchAppBuffer() {
        IoBuffer appBuffer = this.appBuffer.flip();
        this.appBuffer = null;
        return appBuffer;
    }

    /**
     * Get encrypted data to be sent.
     *
     * @return buffer with data
     */
    public IoBuffer fetchOutNetBuffer() {
        IoBuffer answer = outNetBuffer;
        if (answer == null) {
            return emptyBuffer;
        }
        
        outNetBuffer = null;
        return answer.shrink();
    }

    /**
     * Encrypt provided buffer. Encytpted data reurned by getOutNetBuffer().
     *
     * @param src data to encrypt
     * @throws SSLException on errors
     */
    public void encrypt(ByteBuffer src) throws SSLException {
        if (!handshakeComplete) {
            throw new IllegalStateException();
        }
        
        if (!src.hasRemaining()) {
            if (outNetBuffer == null) {
                outNetBuffer = emptyBuffer;
            }
            return;
        }

        createOutNetBuffer(src.remaining());

        // Loop until there is no more data in src
        while (src.hasRemaining()) {

            SSLEngineResult result = sslEngine.wrap(src, outNetBuffer.buf());
            if (result.getStatus() == SSLEngineResult.Status.OK) {
                if (result.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_TASK) {
                    doTasks();
                }
            } else if (result.getStatus() == SSLEngineResult.Status.BUFFER_OVERFLOW) {
                outNetBuffer.capacity(outNetBuffer.capacity() << 1);
                outNetBuffer.limit(outNetBuffer.capacity());
            } else {
                throw new SSLException("SSLEngine error during encrypt: "
                        + result.getStatus() + " src: " + src
                        + "outNetBuffer: " + outNetBuffer);
            }
        }

        outNetBuffer.flip();
    }

    /**
     * Start SSL shutdown process.
     *
     * @return <tt>true</tt> if shutdown process is started.
     *         <tt>false</tt> if shutdown process is already finished.
     * @throws SSLException on errors
     */
    public boolean closeOutbound() throws SSLException {
        if (sslEngine == null || sslEngine.isOutboundDone()) {
            return false;
        }

        sslEngine.closeOutbound();

        createOutNetBuffer(0);
        SSLEngineResult result;
        for (;;) {
            result = sslEngine.wrap(emptyBuffer.buf(), outNetBuffer.buf());
            if (result.getStatus() == SSLEngineResult.Status.BUFFER_OVERFLOW) {
                outNetBuffer.capacity(outNetBuffer.capacity() << 1);
                outNetBuffer.limit(outNetBuffer.capacity());
            } else {
                break;
            }
        }

        if (result.getStatus() != SSLEngineResult.Status.CLOSED) {
            throw new SSLException("Improper close state: " + result);
        }
        outNetBuffer.flip();
        return true;
    }

    /**
     * Decrypt in net buffer. Result is stored in app buffer.
     *
     * @throws SSLException
     */
    private void decrypt(NextFilter nextFilter) throws SSLException {

        if (!handshakeComplete) {
            throw new IllegalStateException();
        }

        unwrap(nextFilter);
    }

    /**
     * @param res
     * @throws SSLException
     */
    private void checkStatus(SSLEngineResult res)
            throws SSLException {

        SSLEngineResult.Status status = res.getStatus();

        /*
        * The status may be:
        * OK - Normal operation
        * OVERFLOW - Should never happen since the application buffer is
        *      sized to hold the maximum packet size.
        * UNDERFLOW - Need to read more data from the socket. It's normal.
        * CLOSED - The other peer closed the socket. Also normal.
        */
        if (status != SSLEngineResult.Status.OK
                && status != SSLEngineResult.Status.CLOSED
                && status != SSLEngineResult.Status.BUFFER_UNDERFLOW) {
            throw new SSLException("SSLEngine error during decrypt: " + status
                    + " inNetBuffer: " + inNetBuffer + "appBuffer: "
                    + appBuffer);
        }
    }

    /**
     * Perform any handshaking processing.
     */
    public void handshake(NextFilter nextFilter) throws SSLException {
        for (; ;) {
            if (handshakeStatus == SSLEngineResult.HandshakeStatus.FINISHED) {
                session.setAttribute(
                        SslFilter.SSL_SESSION, sslEngine.getSession());
                handshakeComplete = true;
                if (!initialHandshakeComplete
                        && session.containsAttribute(SslFilter.USE_NOTIFICATION)) {
                    // SESSION_SECURED is fired only when it's the first handshake.
                    // (i.e. renegotiation shouldn't trigger SESSION_SECURED.)
                    initialHandshakeComplete = true;
                    scheduleMessageReceived(nextFilter,
                            SslFilter.SESSION_SECURED);
                }
                break;
            } else if (handshakeStatus == SSLEngineResult.HandshakeStatus.NEED_TASK) {
                handshakeStatus = doTasks();
            } else if (handshakeStatus == SSLEngineResult.HandshakeStatus.NEED_UNWRAP) {
                // we need more data read
                SSLEngineResult.Status status = unwrapHandshake(nextFilter);
                if (status == SSLEngineResult.Status.BUFFER_UNDERFLOW
                        || isInboundDone()) {
                    // We need more data or the session is closed
                    break;
                }
            } else if (handshakeStatus == SSLEngineResult.HandshakeStatus.NEED_WRAP) {
                // First make sure that the out buffer is completely empty. Since we
                // cannot call wrap with data left on the buffer
                if (outNetBuffer != null && outNetBuffer.hasRemaining()) {
                    break;
                }
                
                SSLEngineResult result;
                createOutNetBuffer(0);
                for (;;) {
                    result = sslEngine.wrap(emptyBuffer.buf(), outNetBuffer.buf());
                    if (result.getStatus() == SSLEngineResult.Status.BUFFER_OVERFLOW) {
                        outNetBuffer.capacity(outNetBuffer.capacity() << 1);
                        outNetBuffer.limit(outNetBuffer.capacity());
                    } else {
                        break;
                    }
                }

                outNetBuffer.flip();
                handshakeStatus = result.getHandshakeStatus();
                writeNetBuffer(nextFilter);
            } else {
                throw new IllegalStateException("Invalid Handshaking State"
                        + handshakeStatus);
            }
        }
    }

    private void createOutNetBuffer(int expectedRemaining) {
        // SSLEngine requires us to allocate unnecessarily big buffer
        // even for small data.  *Shrug*
        int capacity = Math.max(
                expectedRemaining,
                sslEngine.getSession().getPacketBufferSize());
        
        if (outNetBuffer != null) {
            outNetBuffer.capacity(capacity);
        } else {
            outNetBuffer = IoBuffer.allocate(capacity).minimumCapacity(0);
        }
    }

    public WriteFuture writeNetBuffer(NextFilter nextFilter)
            throws SSLException {
        // Check if any net data needed to be writen
        if (outNetBuffer == null || !outNetBuffer.hasRemaining()) {
            // no; bail out
            return null;
        }

        // set flag that we are writing encrypted data
        // (used in SSLFilter.filterWrite())
        writingEncryptedData = true;

        // write net data
        WriteFuture writeFuture = null;

        try {
            IoBuffer writeBuffer = fetchOutNetBuffer();
            writeFuture = new DefaultWriteFuture(session);
            parent.filterWrite(nextFilter, session, new DefaultWriteRequest(
                    writeBuffer, writeFuture));

            // loop while more writes required to complete handshake
            while (needToCompleteHandshake()) {
                try {
                    handshake(nextFilter);
                } catch (SSLException ssle) {
                    SSLException newSsle = new SSLHandshakeException(
                            "SSL handshake failed.");
                    newSsle.initCause(ssle);
                    throw newSsle;
                }
                
                IoBuffer outNetBuffer = fetchOutNetBuffer();
                if (outNetBuffer != null && outNetBuffer.hasRemaining()) {
                    writeFuture = new DefaultWriteFuture(session);
                    parent.filterWrite(nextFilter, session,
                            new DefaultWriteRequest(outNetBuffer, writeFuture));
                }
            }
        } finally {
            writingEncryptedData = false;
        }

        return writeFuture;
    }

    private void unwrap(NextFilter nextFilter) throws SSLException {
        // Prepare the net data for reading.
        if (inNetBuffer != null) {
            inNetBuffer.flip();
        }

        if (inNetBuffer == null || !inNetBuffer.hasRemaining()) {
            return;
        }

        SSLEngineResult res = unwrap0();

        // prepare to be written again
        if (inNetBuffer.hasRemaining()) {
            inNetBuffer.compact();
        } else {
            inNetBuffer = null;
        }

        checkStatus(res);

        renegotiateIfNeeded(nextFilter, res);
    }

    private SSLEngineResult.Status unwrapHandshake(NextFilter nextFilter) throws SSLException {
        // Prepare the net data for reading.
        if (inNetBuffer != null) {
            inNetBuffer.flip();
        }
        
        if (inNetBuffer == null || !inNetBuffer.hasRemaining()) {
            // Need more data.
            return SSLEngineResult.Status.BUFFER_UNDERFLOW;
        }

        SSLEngineResult res = unwrap0();
        handshakeStatus = res.getHandshakeStatus();

        checkStatus(res);

        // If handshake finished, no data was produced, and the status is still ok,
        // try to unwrap more
        if (handshakeStatus == SSLEngineResult.HandshakeStatus.FINISHED
                && res.getStatus() == SSLEngineResult.Status.OK
                && inNetBuffer.hasRemaining()) {
            res = unwrap0();

            // prepare to be written again
            if (inNetBuffer.hasRemaining()) {
                inNetBuffer.compact();
            } else {
                inNetBuffer = null;
            }

            renegotiateIfNeeded(nextFilter, res);
        } else {
            // prepare to be written again
            if (inNetBuffer.hasRemaining()) {
                inNetBuffer.compact();
            } else {
                inNetBuffer = null;
            }
        }

        return res.getStatus();
    }

    private void renegotiateIfNeeded(NextFilter nextFilter, SSLEngineResult res)
            throws SSLException {
        if (res.getStatus() != SSLEngineResult.Status.CLOSED
                && res.getStatus() != SSLEngineResult.Status.BUFFER_UNDERFLOW
                && res.getHandshakeStatus() != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
            // Renegotiation required.
            handshakeComplete = false;
            handshakeStatus = res.getHandshakeStatus();
            handshake(nextFilter);
        }
    }

    private SSLEngineResult unwrap0() throws SSLException {
        if (appBuffer == null) {
            appBuffer = IoBuffer.allocate(inNetBuffer.remaining());
        } else {
            appBuffer.expand(inNetBuffer.remaining());
        }
        
        SSLEngineResult res;
        do {
            res = sslEngine.unwrap(inNetBuffer.buf(), appBuffer.buf());
            if (res.getStatus() == SSLEngineResult.Status.BUFFER_OVERFLOW) {
                appBuffer.capacity(appBuffer.capacity() << 1);
                appBuffer.limit(appBuffer.capacity());
                continue;
            }
        } while ((res.getStatus() == SSLEngineResult.Status.OK || res.getStatus() == SSLEngineResult.Status.BUFFER_OVERFLOW) &&
                 (handshakeComplete && res.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING ||
                  res.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_UNWRAP));

        return res;
    }

    /**
     * Do all the outstanding handshake tasks in the current Thread.
     */
    private SSLEngineResult.HandshakeStatus doTasks() {
        /*
         * We could run this in a separate thread, but I don't see the need
         * for this when used from SSLFilter. Use thread filters in MINA instead?
         */
        Runnable runnable;
        while ((runnable = sslEngine.getDelegatedTask()) != null) {
            runnable.run();
        }
        return sslEngine.getHandshakeStatus();
    }

    /**
     * Creates a new MINA buffer that is a deep copy of the remaining bytes
     * in the given buffer (between index buf.position() and buf.limit())
     *
     * @param src the buffer to copy
     * @return the new buffer, ready to read from
     */
    public static IoBuffer copy(ByteBuffer src) {
        IoBuffer copy = IoBuffer.allocate(src.remaining());
        copy.put(src);
        copy.flip();
        return copy;
    }
}
