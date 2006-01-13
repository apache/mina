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
package org.apache.mina.filter.support;

import java.nio.ByteBuffer;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSession;

import org.apache.mina.common.IoSession;
import org.apache.mina.common.WriteFuture;
import org.apache.mina.common.IoFilter.NextFilter;
import org.apache.mina.common.IoFilter.WriteRequest;
import org.apache.mina.filter.SSLFilter;
import org.apache.mina.util.Queue;
import org.apache.mina.util.SessionLog;

/**
 * A helper class using the SSLEngine API to decrypt/encrypt data.
 * <p>
 * Each connection has a SSLEngine that is used through the lifetime of the connection.
 * We allocate byte buffers for use as the outbound and inbound network buffers.
 * These buffers handle all of the intermediary data for the SSL connection. To make things easy,
 * we'll require outNetBuffer be completely flushed before trying to wrap any more data.
 *
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class SSLHandler
{
    private final SSLFilter parent;
    private final SSLContext ctx;
    private final IoSession session;
    private final Queue scheduledWrites = new Queue();

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
    private final ByteBuffer hsBB = ByteBuffer.allocate( 0 );

    /**
     * Handshake status
     */
    private SSLEngineResult.HandshakeStatus initialHandshakeStatus;

    /**
     * Initial handshake complete?
     */
    private boolean initialHandshakeComplete;

    private boolean writingEncryptedData;
    
    /**
     * Constuctor.
     *
     * @param sslc
     * @throws SSLException 
     */
    public SSLHandler( SSLFilter parent, SSLContext sslc, IoSession session ) throws SSLException
    {
        this.parent = parent;
        this.session = session;
        this.ctx = sslc;
        init();
    }

    public void init() throws SSLException
    {
        if( sslEngine != null )
        {
            return;
        }

        sslEngine = ctx.createSSLEngine();
        sslEngine.setUseClientMode( parent.isUseClientMode() );

        if ( parent.isWantClientAuth() )
        {
            sslEngine.setWantClientAuth( true );
        }

        if ( parent.isNeedClientAuth() )
        {
            sslEngine.setNeedClientAuth( true );
        }
  
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
        
        SSLByteBufferPool.initiate( sslEngine );

        appBuffer = SSLByteBufferPool.getApplicationBuffer();

        inNetBuffer = SSLByteBufferPool.getPacketBuffer();
        outNetBuffer = SSLByteBufferPool.getPacketBuffer();
        outNetBuffer.position( 0 );
        outNetBuffer.limit( 0 );
        
        writingEncryptedData = false;
    }
    
    /**
     * Release allocated ByteBuffers.
     */
    public void destroy()
    {
        if( sslEngine == null )
        {
            return;
        }

        // Close inbound and flush all remaining data if available.
        try
        {
            sslEngine.closeInbound();
        }
        catch( SSLException e )
        {
            SessionLog.debug(
                    session,
                    "Unexpected exception from SSLEngine.closeInbound().",
                    e );
        }
        
        try
        {
            do
            {
                outNetBuffer.clear();
            }
            while( sslEngine.wrap( hsBB, outNetBuffer ).bytesProduced() > 0 );
        }
        catch( SSLException e )
        {
            SessionLog.debug(
                    session,
                    "Unexpected exception from SSLEngine.wrap().",
                    e );
        }
        sslEngine.closeOutbound();
        sslEngine = null;
        
        SSLByteBufferPool.release( appBuffer );
        SSLByteBufferPool.release( inNetBuffer );
        SSLByteBufferPool.release( outNetBuffer );
        scheduledWrites.clear();
    }

    public SSLFilter getParent()
    {
        return parent;
    }
    
    public IoSession getSession()
    {
        return session;
    }

    /**
     * Check we are writing encrypted data.
     */
    public boolean isWritingEncryptedData()
    {
        return writingEncryptedData;
    }

    /**
     * Check if initial handshake is completed.
     */
    public boolean isInitialHandshakeComplete()
    {
        return initialHandshakeComplete;
    }

    public boolean isInboundDone()
    {
        return sslEngine == null || sslEngine.isInboundDone();
    }

    public boolean isOutboundDone()
    {
        return sslEngine == null || sslEngine.isOutboundDone();
    }

    /**
     * Check if there is any need to complete initial handshake.
     */
    public boolean needToCompleteInitialHandshake()
    {
        return ( initialHandshakeStatus == SSLEngineResult.HandshakeStatus.NEED_WRAP && !isInboundDone() );
    }
    
    public void scheduleWrite( NextFilter nextFilter, WriteRequest writeRequest )
    {
        scheduledWrites.push( new ScheduledWrite( nextFilter, writeRequest ) );
    }
    
    public void flushScheduledWrites() throws SSLException
    {
        ScheduledWrite scheduledWrite;
        
        while( ( scheduledWrite = ( ScheduledWrite ) scheduledWrites.pop() ) != null )
        {
            if( SessionLog.isDebugEnabled( session ) )
            {
                SessionLog.debug( session, " Flushing buffered write request: " + scheduledWrite.writeRequest );
            }
            parent.filterWrite( scheduledWrite.nextFilter, session, scheduledWrite.writeRequest );
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
    public void messageReceived( NextFilter nextFilter, ByteBuffer buf ) throws SSLException
    {
        if ( buf.limit() > inNetBuffer.remaining() ) {
            // We have to expand inNetBuffer
            inNetBuffer = SSLByteBufferPool.expandBuffer( inNetBuffer,
                inNetBuffer.capacity() + ( buf.limit() * 2 ) );
            // We also expand app. buffer (twice the size of in net. buffer)
            appBuffer = SSLByteBufferPool.expandBuffer( appBuffer, inNetBuffer.capacity() * 2);
            appBuffer.position( 0 );
            appBuffer.limit( 0 );
            if( SessionLog.isDebugEnabled( session ) )
            {
                SessionLog.debug( session, 
                                    " expanded inNetBuffer:" + inNetBuffer );
                SessionLog.debug( session, 
                                    " expanded appBuffer:" + appBuffer );
            }
        }

        // append buf to inNetBuffer
        inNetBuffer.put( buf );
        if( !initialHandshakeComplete )
        {
            handshake( nextFilter );
        }
        else
        {
            decrypt();
        }

        if( isInboundDone() )
        {
            // Rewind the MINA buffer if not all data is processed and inbound is finished.
            buf.position( buf.position() - inNetBuffer.position() );
            inNetBuffer.clear();
        }
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
     * @param src data to encrypt
     * @throws SSLException on errors
     */
    public void encrypt( ByteBuffer src ) throws SSLException
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
                if ( SessionLog.isDebugEnabled( session ) ) {
                    SessionLog.debug( session, " expanded outNetBuffer:" + outNetBuffer );
                }
            }

            result = sslEngine.wrap( src, outNetBuffer );
            if ( SessionLog.isDebugEnabled( session ) ) {
                SessionLog.debug( session, " Wrap res:" + result );
            }

            if ( result.getStatus() == SSLEngineResult.Status.OK ) {
                if ( result.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_TASK ) {
                    doTasks();
                }
            } else {
                throw new SSLException( "SSLEngine error during encrypt: "
                        + result.getStatus() +
                        " src: " + src + "outNetBuffer: " + outNetBuffer);
            }
        }

        outNetBuffer.flip();
    }

    /**
     * Start SSL shutdown process.
     * 
     * @return <tt>true</tt> if shutdown process is started.
     *         <tt>false</tt> if shutdown process is already finished.
     *
     * @throws SSLException on errors
     */
    public boolean closeOutbound() throws SSLException
    {
        if( sslEngine == null || sslEngine.isOutboundDone() )
        {
            return false;
        }
        
        sslEngine.closeOutbound();

        // By RFC 2616, we can "fire and forget" our close_notify
        // message, so that's what we'll do here.
        outNetBuffer.clear();
        SSLEngineResult result = sslEngine.wrap( hsBB, outNetBuffer );
        if( result.getStatus() != SSLEngineResult.Status.CLOSED )
        {
            throw new SSLException( "Improper close state: " + result );
        }
        outNetBuffer.flip();
        return true;
    }

    /**
     * Decrypt in net buffer. Result is stored in app buffer.
     *
     * @throws SSLException
     */
    private void decrypt() throws SSLException
    {

        if( !initialHandshakeComplete )
        {
            throw new IllegalStateException();
        }

        if( appBuffer.hasRemaining() )
        {
             if ( SessionLog.isDebugEnabled( session ) ) {
                 SessionLog.debug( session, " Error: appBuffer not empty!" );
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
    
    /**
     * Perform any handshaking processing.
     */
    public void handshake( NextFilter nextFilter ) throws SSLException
    {
        if( SessionLog.isDebugEnabled( session ) )
        {
            SessionLog.debug( session, " doHandshake()" );
        }
        
        while( !initialHandshakeComplete )
        {
            if( initialHandshakeStatus == SSLEngineResult.HandshakeStatus.FINISHED )
            {
                session.setAttribute( SSLFilter.SSL_SESSION, sslEngine.getSession() );
                if( SessionLog.isDebugEnabled( session ) )
                {
                    SSLSession sslSession = sslEngine.getSession();
                    SessionLog.debug( session, "  initialHandshakeStatus=FINISHED" );
                    SessionLog.debug( session, "  sslSession CipherSuite used " + sslSession.getCipherSuite() );
                }
                initialHandshakeComplete = true;
                if( session.containsAttribute( SSLFilter.USE_NOTIFICATION ) )
                {
                    nextFilter.messageReceived( session, SSLFilter.SESSION_SECURED );
                }
                break;
            }
            else if( initialHandshakeStatus == SSLEngineResult.HandshakeStatus.NEED_TASK )
            {
                if( SessionLog.isDebugEnabled( session ) )
                {
                    SessionLog.debug( session, "  initialHandshakeStatus=NEED_TASK" );
                }
                initialHandshakeStatus = doTasks();
            }
            else if( initialHandshakeStatus == SSLEngineResult.HandshakeStatus.NEED_UNWRAP )
            {
                // we need more data read
                if( SessionLog.isDebugEnabled( session ) )
                {
                    SessionLog.debug( session, "  initialHandshakeStatus=NEED_UNWRAP" );
                }
                SSLEngineResult.Status status = unwrapHandshake();
                if( ( initialHandshakeStatus != SSLEngineResult.HandshakeStatus.FINISHED 
                        && status == SSLEngineResult.Status.BUFFER_UNDERFLOW )
                        || isInboundDone() )
                {
                    // We need more data or the session is closed
                    break;
                }
            }
            else if( initialHandshakeStatus == SSLEngineResult.HandshakeStatus.NEED_WRAP )
            {
                if( SessionLog.isDebugEnabled( session ) )
                {
                    SessionLog.debug( session, "  initialHandshakeStatus=NEED_WRAP" );
                }
                // First make sure that the out buffer is completely empty. Since we
                // cannot call wrap with data left on the buffer
                if( outNetBuffer.hasRemaining() )
                {
                    if( SessionLog.isDebugEnabled( session ) )
                    {
                        SessionLog.debug( session, "  Still data in out buffer!" );
                    }
                    break;
                }
                outNetBuffer.clear();
                SSLEngineResult result = sslEngine.wrap( hsBB, outNetBuffer );
                if( SessionLog.isDebugEnabled( session ) )
                {
                    SessionLog.debug( session, " Wrap res:" + result );
                }

                outNetBuffer.flip();
                initialHandshakeStatus = result.getHandshakeStatus();
                writeNetBuffer( nextFilter );
            }
            else
            {
                throw new IllegalStateException( "Invalid Handshaking State"
                        + initialHandshakeStatus );
            }
        }
    }

    public WriteFuture writeNetBuffer( NextFilter nextFilter ) throws SSLException
    {
        // Check if any net data needed to be writen
        if( !getOutNetBuffer().hasRemaining() )
        {
            // no; bail out
            return WriteFuture.newNotWrittenFuture();
        }
        
        WriteFuture writeFuture = null;
        
        // write net data
        
        // set flag that we are writing encrypted data
        // (used in SSLFilter.filterWrite())
        writingEncryptedData = true;
        
        try
        {
            if( SessionLog.isDebugEnabled( session ) )
            {
                SessionLog.debug( session, " write outNetBuffer: " + getOutNetBuffer() );
            }
            org.apache.mina.common.ByteBuffer writeBuffer = copy( getOutNetBuffer() );
            if( SessionLog.isDebugEnabled( session ) )
            {
                SessionLog.debug( session, " session write: " + writeBuffer );
            }
            //debug("outNetBuffer (after copy): {0}", sslHandler.getOutNetBuffer());
            
            writeFuture = new WriteFuture();
            parent.filterWrite( nextFilter, session, new WriteRequest( writeBuffer, writeFuture ) );

            // loop while more writes required to complete handshake
            while( needToCompleteInitialHandshake() )
            {
                try
                {
                    handshake( nextFilter );
                }
                catch( SSLException ssle )
                {
                    SSLException newSSLE = new SSLHandshakeException(
                            "Initial SSL handshake failed." );
                    newSSLE.initCause( ssle );
                    throw newSSLE;
                }
                if( getOutNetBuffer().hasRemaining() )
                {
                    if( SessionLog.isDebugEnabled( session ) )
                    {
                        SessionLog.debug( session, " write outNetBuffer2: " + getOutNetBuffer() );
                    }
                    org.apache.mina.common.ByteBuffer writeBuffer2 = copy( getOutNetBuffer() );
                    writeFuture = new WriteFuture();
                    parent.filterWrite( nextFilter, session, new WriteRequest( writeBuffer2, writeFuture ) );
                }
            }
        }
        finally
        {
            writingEncryptedData = false;
        }
        
        if( writeFuture != null )
        {
            return writeFuture;
        }
        else
        {
            return WriteFuture.newNotWrittenFuture();
        }
    }
    
    
    private SSLEngineResult.Status unwrap() throws SSLException
    {
        if( SessionLog.isDebugEnabled( session ) )
        {
            SessionLog.debug( session, " unwrap()" );
        }
        // Prepare the application buffer to receive decrypted data
        appBuffer.clear();

        // Prepare the net data for reading.
        inNetBuffer.flip();

        SSLEngineResult res;
        do
        {
            if( SessionLog.isDebugEnabled( session ) )
            {
                SessionLog.debug( session, "   inNetBuffer: " + inNetBuffer );
                SessionLog.debug( session, "   appBuffer: " + appBuffer );
            }
            res = sslEngine.unwrap( inNetBuffer, appBuffer );
            if( SessionLog.isDebugEnabled( session ) )
            {
                SessionLog.debug( session, " Unwrap res:" + res );
            }
        }
        while( res.getStatus() == SSLEngineResult.Status.OK );

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
        if( SessionLog.isDebugEnabled( session ) )
        {
            SessionLog.debug( session, " unwrapHandshake()" );
        }
        // Prepare the application buffer to receive decrypted data
        appBuffer.clear();

        // Prepare the net data for reading.
        inNetBuffer.flip();

        SSLEngineResult res;
        do
        {
            if( SessionLog.isDebugEnabled( session ) )
            {
                SessionLog.debug( session, "   inNetBuffer: " + inNetBuffer );
                SessionLog.debug( session, "   appBuffer: " + appBuffer );
            }
            res = sslEngine.unwrap( inNetBuffer, appBuffer );
            if( SessionLog.isDebugEnabled( session ) )
            {
                SessionLog.debug( session, " Unwrap res:" + res );
            }

        }
        while( res.getStatus() == SSLEngineResult.Status.OK &&
               res.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_UNWRAP );

        initialHandshakeStatus = res.getHandshakeStatus();
    
        // If handshake finished, no data was produced, and the status is still ok,
        // try to unwrap more
        if (initialHandshakeStatus == SSLEngineResult.HandshakeStatus.FINISHED
                && appBuffer.position() == 0
                && res.getStatus() == SSLEngineResult.Status.OK
                && inNetBuffer.hasRemaining()) {
            do {
                if (SessionLog.isDebugEnabled( session )) {
                    SessionLog.debug( session, "  extra handshake unwrap" );
                    SessionLog.debug( session, "   inNetBuffer: " + inNetBuffer );
                    SessionLog.debug( session, "   appBuffer: " + appBuffer );
                }
                res = sslEngine.unwrap(inNetBuffer, appBuffer);
                if (SessionLog.isDebugEnabled( session )) {
                    SessionLog.debug( session, " Unwrap res:" + res );
                }
            } while (res.getStatus() == SSLEngineResult.Status.OK);
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
        //initialHandshakeStatus = res.getHandshakeStatus();
        return checkStatus( res.getStatus() );
    }

    /**
     * Do all the outstanding handshake tasks in the current Thread.
     */
    private SSLEngineResult.HandshakeStatus doTasks()
    {
        if( SessionLog.isDebugEnabled( session ) )
        {
            SessionLog.debug( session, "   doTasks()" );
        }

        /*
         * We could run this in a separate thread, but I don't see the need
         * for this when used from SSLFilter. Use thread filters in MINA instead?
         */
        Runnable runnable;
        while( ( runnable = sslEngine.getDelegatedTask() ) != null )
        {
            if( SessionLog.isDebugEnabled( session ) )
            {
                SessionLog.debug( session, "    doTask: " + runnable );
            }
            runnable.run();
        }
        if( SessionLog.isDebugEnabled( session ) )
        {
            SessionLog.debug( session, "   doTasks(): "
                    + sslEngine.getHandshakeStatus() );
        }
        return sslEngine.getHandshakeStatus();
    }

    private static class ScheduledWrite
    {
        private final NextFilter nextFilter;
        private final WriteRequest writeRequest;
        
        public ScheduledWrite( NextFilter nextFilter, WriteRequest writeRequest )
        {
            this.nextFilter = nextFilter;
            this.writeRequest = writeRequest;
        }
    }
    
    /**
     * Creates a new Mina byte buffer that is a deep copy of the remaining bytes
     * in the given buffer (between index buf.position() and buf.limit())
     *
     * @param src the buffer to copy
     * @return the new buffer, ready to read from
     */
    public static org.apache.mina.common.ByteBuffer copy( java.nio.ByteBuffer src )
    {
        org.apache.mina.common.ByteBuffer copy = org.apache.mina.common.ByteBuffer.allocate( src.remaining() );
        copy.put( src );
        copy.flip();
        return copy;
    }
}
