/*
 *   @(#) $Id$
 *
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.mina.io.filter;

import java.nio.ByteBuffer;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;

import org.apache.mina.io.IoSession;
import org.apache.mina.io.IoHandlerFilter.NextFilter;
import org.apache.mina.util.Queue;

/**
 * A helper class using the SSLEngine API to decrypt/encrypt data.
 * <p>
 * Each connection has a SSLEngine that is used through the lifetime of the connection.
 * We allocate byte buffers for use as the outbound and inbound network buffers.
 * These buffers handle all of the intermediary data for the SSL connection. To make things easy,
 * we'll require outNetBuffer be completely flushed before trying to wrap any more data.
 *
 * @author Jan Andersson (janne@minq.se)
 * @version $Rev$, $Date$
 */
class SSLHandler
{
    private final SSLFilter parent;

    private final IoSession session;
    
    private final Queue nextFilterQueue = new Queue();
    
    private final Queue writeBufferQueue = new Queue();
    
    private final Queue writeMarkerQueue = new Queue();

    private SSLEngine sslEngine;

    /**
     * Encrypted data from the net
     */
    private ByteBuffer inNetBuffer;

    /**
     * Encrypted data to be written to the net
     */
    private ByteBuffer outNetBuffer;

    /**
     * Applicaton cleartext data to be read by application
     */
    private ByteBuffer appBuffer;

    /**
     * Empty buffer used during initial handshake and close operations
     */
    private ByteBuffer hsBB = ByteBuffer.allocate( 0 );

    /**
     * Handshake status
     */
    private SSLEngineResult.HandshakeStatus initialHandshakeStatus;

    /**
     * Initial handshake complete?
     */
    private boolean initialHandshakeComplete;

    /**
     * We have received the shutdown request by our caller, and have
     * closed our outbound side.
     */
    private boolean shutdown = false;

    private boolean closed = false;

    private boolean isWritingEncryptedData = false;
    
    /**
     * Constuctor.
     *
     * @param sslc
     * @throws SSLException 
     */
    SSLHandler( SSLFilter parent, SSLContext sslc, IoSession session ) throws SSLException
    {
        this.parent = parent;
        this.session = session;
        sslEngine = sslc.createSSLEngine();
        sslEngine.setUseClientMode( parent.isUseClientMode() );
        sslEngine.setNeedClientAuth( parent.isNeedClientAuth() );
        sslEngine.setWantClientAuth( parent.isWantClientAuth() );
  
        if( parent.getEnabledCipherSuites() != null )
        {
            sslEngine.setEnabledCipherSuites( parent.getEnabledCipherSuites() );
        }
        
        if( parent.getEnabledProtocols() != null )
        {
            sslEngine.setEnabledProtocols( parent.getEnabledProtocols() );
        }

        sslEngine.beginHandshake();   
        initialHandshakeStatus = sslEngine.getHandshakeStatus();//SSLEngineResult.HandshakeStatus.NEED_UNWRAP;
        initialHandshakeComplete = false;
        //SSLSession sslSession = sslEngine.getSession
        
        SSLByteBufferPool.initiate( sslEngine );

        appBuffer = SSLByteBufferPool.getApplicationBuffer();

        inNetBuffer = SSLByteBufferPool.getPacketBuffer();
        outNetBuffer = SSLByteBufferPool.getPacketBuffer();
        outNetBuffer.position( 0 );
        outNetBuffer.limit( 0 );
    }

    /**
     * Indicate that we are writing encrypted data.
     * Only used as a flag by IoSSLFiler
     */
    public void setWritingEncryptedData( boolean flag )
    {
        isWritingEncryptedData = flag;
    }

    /**
     * Check we are writing encrypted data.
     */
    public boolean isWritingEncryptedData()
    {
        return isWritingEncryptedData;
    }

    /**
     * Check if initial handshake is completed.
     */
    public boolean isInitialHandshakeComplete()
    {
        return initialHandshakeComplete;
    }

    /**
     * Check if SSL sesssion closed
     */
    public boolean isClosed()
    {
        return closed;
    }

    /**
     * Check if there is any need to complete initial handshake.
     */
    public boolean needToCompleteInitialHandshake()
    {
        return ( initialHandshakeStatus == SSLEngineResult.HandshakeStatus.NEED_WRAP && !closed );
    }
    
    public synchronized void scheduleWrite( NextFilter nextFilter, org.apache.mina.common.ByteBuffer buf, Object marker )
    {
        nextFilterQueue.push( nextFilter );
        writeBufferQueue.push( buf );
        writeMarkerQueue.push( marker );
    }
    
    public synchronized void flushScheduledWrites()
    {
        NextFilter nextFilter;
        org.apache.mina.common.ByteBuffer scheduledBuf;
        Object scheduledMarker;
        
        while( ( scheduledBuf = ( org.apache.mina.common.ByteBuffer ) writeBufferQueue.pop() ) != null )
        {
            if( parent.debug != null )
            {
                parent.debug.print( parent, "Flushing buffered write request: " + scheduledBuf );
            }
            nextFilter = ( NextFilter ) nextFilterQueue.pop();
            scheduledMarker = writeMarkerQueue.pop();
            parent.filterWrite( nextFilter, session, scheduledBuf, scheduledMarker );
        }
    }

    /**
     * Call when data read from net. Will perform inial hanshake or decrypt provided
     * Buffer.
     * Decrytpted data reurned by getAppBuffer(), if any.
     *
     * @param buf buffer to decrypt
     * @throws SSLException on errors
     */
    public void dataRead( ByteBuffer buf ) throws SSLException
    {
        if ( buf.limit() > inNetBuffer.remaining() ) {
            // We have to expand inNetBuffer
            inNetBuffer = SSLByteBufferPool.expandBuffer( inNetBuffer,
                inNetBuffer.capacity() + ( buf.limit() * 2 ) );
            // We also expand app. buffer (twice the size of in net. buffer)
            appBuffer = SSLByteBufferPool.expandBuffer( appBuffer, inNetBuffer.capacity() * 2);
            appBuffer.position( 0 );
            appBuffer.limit( 0 );
            if( parent.debug != null )
            {
                parent.debug.print( parent,
                                    "expanded inNetBuffer:" + inNetBuffer );
                parent.debug.print( parent, 
                                    "expanded appBuffer:" + appBuffer );
            }
        }

        // append buf to inNetBuffer
        inNetBuffer.put( buf );
        if( !initialHandshakeComplete )
        {
            doHandshake();
        }
        else
        {
            doDecrypt();
        }
    }

    /**
     * Continue initial SSL handshake.
     *
     * @throws SSLException on errors
     */
    public void continueHandshake() throws SSLException
    {
        if( parent.debug != null )
        {
            parent.debug.print( parent, "continueHandshake()" );
        }
        doHandshake();
    }

    /**
     * Get decrypted application data.
     *
     * @return buffer with data
     */
    public ByteBuffer getAppBuffer()
    {
        return appBuffer;
    }

    /**
     * Get encrypted data to be sent.
     *
     * @return buffer with data
     */
    public ByteBuffer getOutNetBuffer()
    {
        return outNetBuffer;
    }

    /**
     * Encrypt provided buffer. Encytpted data reurned by getOutNetBuffer().
     *
     * @param buf data to encrypt
     * @throws SSLException on errors
     */
    public void encrypt( ByteBuffer buf ) throws SSLException
    {
        doEncrypt( buf );
    }

    /**
     * Start SSL shutdown process
     *
     * @throws SSLException on errors
     */
    public void shutdown() throws SSLException
    {
        if( !shutdown )
        {
            doShutdown();
        }
    }

    /**
     * Release allocated ByteBuffers.
     */
    public void release()
    {
        SSLByteBufferPool.release( appBuffer );
        SSLByteBufferPool.release( inNetBuffer );
        SSLByteBufferPool.release( outNetBuffer );
    }

    /**
     * Decrypt in net buffer. Result is stored in app buffer.
     *
     * @throws SSLException
     */
    private void doDecrypt() throws SSLException
    {

        if( !initialHandshakeComplete )
        {
            throw new IllegalStateException();
        }

        if( appBuffer.hasRemaining() )
        {
             if ( parent.debug != null ) {
                 parent.debug.print( parent, "Error: appBuffer not empty!" );
             }
            //still app data in buffer!?
            throw new IllegalStateException();
        }

        unwrap();
    }

    /**
     * @param status
     * @throws SSLException
     */
    private SSLEngineResult.Status checkStatus( SSLEngineResult.Status status ) throws SSLException
    {
        if( status != SSLEngineResult.Status.OK &&
            status != SSLEngineResult.Status.CLOSED &&
            status != SSLEngineResult.Status.BUFFER_UNDERFLOW )
        {
            throw new SSLException( "SSLEngine error during decrypt: " +
                                    status +
                                    " inNetBuffer: " + inNetBuffer + "appBuffer: " + appBuffer);
        }
        
        return status;
    }
    
    private void doEncrypt( ByteBuffer src ) throws SSLException
    {
        if( !initialHandshakeComplete )
        {
            throw new IllegalStateException();
        }

        // The data buffer is (must be) empty, we can reuse the entire
        // buffer.
        outNetBuffer.clear();

        SSLEngineResult result;

        // Loop until there is no more data in src
        while ( src.hasRemaining() ) {

            if ( src.remaining() > ( ( outNetBuffer.capacity() - outNetBuffer.position() ) / 2 ) ) {
                // We have to expand outNetBuffer
                // Note: there is no way to know the exact size required, but enrypted data
                // shouln't need to be larger than twice the source data size?
                outNetBuffer = SSLByteBufferPool.expandBuffer( outNetBuffer, src.capacity() * 2 );
                if ( parent.debug != null ) {
                    parent.debug.print( parent, "expanded outNetBuffer:" + outNetBuffer );
                }
            }

            result = sslEngine.wrap( src, outNetBuffer );
            if ( parent.debug != null ) {
                parent.debug.print( parent, "Wrap res:" + result );
            }

            if ( result.getStatus() == SSLEngineResult.Status.OK )
            {
                if ( result.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_TASK ) {
                    doTasks();
                }
            }
            else if( result.getStatus() == SSLEngineResult.Status.CLOSED )
            {
                closed = true;
            }
            else
            {
                throw new SSLException( "SSLEngine error during encrypt: "
                        + result.getStatus() +
                        " src: " + src + "outNetBuffer: " + outNetBuffer);
            }
        }

        outNetBuffer.flip();
    }

    /**
     * Perform any handshaking processing.
     */
    synchronized void doHandshake() throws SSLException
    {

        if( parent.debug != null )
        {
            parent.debug.print( parent, "doHandshake()" );
        }

        while( !initialHandshakeComplete )
        {
            if( initialHandshakeStatus == SSLEngineResult.HandshakeStatus.FINISHED )
            {
                if( parent.debug != null )
                {
                    SSLSession sslSession = sslEngine.getSession();
                    parent.debug.print( parent, " initialHandshakeStatus=FINISHED" );
                    parent.debug.print( parent, " sslSession CipherSuite used " + sslSession.getCipherSuite());
                }
                initialHandshakeComplete = true;
                return;
            }
            else if( initialHandshakeStatus == SSLEngineResult.HandshakeStatus.NEED_TASK )
            {
                if( parent.debug != null )
                {
                    parent.debug.print( parent, " initialHandshakeStatus=NEED_TASK" );
                }
                initialHandshakeStatus = doTasks();
            }
            else if( initialHandshakeStatus == SSLEngineResult.HandshakeStatus.NEED_UNWRAP )
            {
                // we need more data read
                if( parent.debug != null )
                {
                    parent.debug.print(
                            parent,
                            " initialHandshakeStatus=NEED_UNWRAP" );
                }
                SSLEngineResult.Status status = unwrapHandshake();
                if( status == SSLEngineResult.Status.BUFFER_UNDERFLOW
                        || closed )
                {
                    // We need more data or the session is closed
                    return;
                }
            }
            else if( initialHandshakeStatus == SSLEngineResult.HandshakeStatus.NEED_WRAP )
            {
                if( parent.debug != null )
                {
                    parent.debug.print( parent, " initialHandshakeStatus=NEED_WRAP" );
                }
                // First make sure that the out buffer is completely empty. Since we
                // cannot call wrap with data left on the buffer
                if( outNetBuffer.hasRemaining() )
                {
                    if( parent.debug != null )
                    {
                        parent.debug.print( parent, " Still data in out buffer!" );
                    }
                    return;
                }
                outNetBuffer.clear();
                SSLEngineResult result = sslEngine.wrap( hsBB, outNetBuffer );
                if( parent.debug != null )
                {
                    parent.debug.print( parent, "Wrap res:" + result );
                }

                outNetBuffer.flip();
                initialHandshakeStatus = result.getHandshakeStatus();
                parent.writeNetBuffer( session, this );
                // return to allow data on out buffer being sent
                // TODO: We might want to send more data immidiatley?
            }
            else
            {
                throw new IllegalStateException( "Invalid Handshaking State"
                        + initialHandshakeStatus );
            }
        }
    }

    SSLEngineResult.Status unwrap() throws SSLException
    {
        if( parent.debug != null )
        {
            parent.debug.print( parent, "unwrap()" );
        }
        // Prepare the application buffer to receive decrypted data
        appBuffer.clear();

        // Prepare the net data for reading.
        inNetBuffer.flip();

        SSLEngineResult res;
        do
        {
            if( parent.debug != null )
            {
                parent.debug.print( parent, "  inNetBuffer: " + inNetBuffer );
                parent.debug.print( parent, "  appBuffer: " + appBuffer );
            }
            res = sslEngine.unwrap( inNetBuffer, appBuffer );
            if( parent.debug != null )
            {
                parent.debug.print( parent, "Unwrap res:" + res );
            }
        }
        while( res.getStatus() == SSLEngineResult.Status.OK );

        // If we are CLOSED, set flag
        if( res.getStatus() == SSLEngineResult.Status.CLOSED )
        {
            closed = true;
        }

        // prepare to be written again
        inNetBuffer.compact();
        // prepare app data to be read
        appBuffer.flip();

        /*
         * The status may be:
         * OK - Normal operation
         * OVERFLOW - Should never happen since the application buffer is
         *      sized to hold the maximum packet size.
         * UNDERFLOW - Need to read more data from the socket. It's normal.
         * CLOSED - The other peer closed the socket. Also normal.
         */
        return checkStatus( res.getStatus() );
    }

    private SSLEngineResult.Status unwrapHandshake() throws SSLException
    {
        if( parent.debug != null )
        {
            parent.debug.print( parent, "unwrapHandshake()" );
        }
        // Prepare the application buffer to receive decrypted data
        appBuffer.clear();

        // Prepare the net data for reading.
        inNetBuffer.flip();

        SSLEngineResult res;
        do
        {
            if( parent.debug != null )
            {
                parent.debug.print( parent, "  inNetBuffer: " + inNetBuffer );
                parent.debug.print( parent, "  appBuffer: " + appBuffer );
            }
            res = sslEngine.unwrap( inNetBuffer, appBuffer );
            if( parent.debug != null )
            {
                parent.debug.print( parent, "Unwrap res:" + res );
            }

        }
        while( res.getStatus() == SSLEngineResult.Status.OK &&
               res.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_UNWRAP );

        // If we are CLOSED, set flag
        if( res.getStatus() == SSLEngineResult.Status.CLOSED )
        {
            closed = true;
        }
        
        // prepare to be written again
        inNetBuffer.compact();

        // prepare app data to be read
        appBuffer.flip();

        /*
         * The status may be:
         * OK - Normal operation
         * OVERFLOW - Should never happen since the application buffer is
         *      sized to hold the maximum packet size.
         * UNDERFLOW - Need to read more data from the socket. It's normal.
         * CLOSED - The other peer closed the socket. Also normal.
         */
        initialHandshakeStatus = res.getHandshakeStatus();
        return checkStatus( res.getStatus() );
    }

    /**
     * Do all the outstanding handshake tasks in the current Thread.
     */
    private SSLEngineResult.HandshakeStatus doTasks()
    {
        if( parent.debug != null )
        {
            parent.debug.print( parent, "  doTasks()" );
        }

        /*
         * We could run this in a separate thread, but I don't see the need
         * for this when used from IoSSLFilter.Use thread filters in Mina instead?
         */
        Runnable runnable;
        while( ( runnable = sslEngine.getDelegatedTask() ) != null )
        {
            if( parent.debug != null )
            {
                parent.debug.print( parent, "   doTask: " + runnable );
            }
            runnable.run();
        }
        if( parent.debug != null )
        {
            parent.debug.print( parent, "  doTasks(): "
                    + sslEngine.getHandshakeStatus() );
        }
        return sslEngine.getHandshakeStatus();
    }

    /**
     * Begin the shutdown process.
     */
    void doShutdown() throws SSLException
    {

        if( !shutdown )
        {
            sslEngine.closeOutbound();
            shutdown = true;
        }

        // By RFC 2616, we can "fire and forget" our close_notify
        // message, so that's what we'll do here.

        outNetBuffer.clear();
        SSLEngineResult result = sslEngine.wrap( hsBB, outNetBuffer );
        if( result.getStatus() != SSLEngineResult.Status.CLOSED )
        {
            throw new SSLException( "Improper close state: " + result );
        }
        outNetBuffer.flip();
    }
}
