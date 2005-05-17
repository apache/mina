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

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.io.IoFilterAdapter;
import org.apache.mina.io.IoHandler;
import org.apache.mina.io.IoSession;

/**
 * An SSL filter that encrypts and decrypts the data exchanged in the session.
 * This filter uses an {@link SSLEngine} which was introduced in Java 5, so 
 * Java version 5 or above is mandatory to use this filter. And please note that
 * this filter only works for TCP/IP connections.
 * <p>
 * This filter logs debug information in {@link Level#FINEST} using {@link Logger}.
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class SSLFilter extends IoFilterAdapter
{
    private static final Logger log = Logger.getLogger( SSLFilter.class.getName() );

    /**
     * A marker which is passed with {@link IoHandler#dataWritten(IoSession, Object)}
     * when <tt>SSLFilter</tt> writes data other then user actually requested.
     */
    private static final Object SSL_MARKER = new Object()
    {
        public String toString()
        {
            return "SSL_MARKER";
        }
    };
    
    // SSL Context
    private SSLContext sslContext;

    // Map used to map SSLHandler objects per session (key is IoSession)
    private Map sslSessionHandlerMap = new IdentityHashMap();

    private boolean client;
    private boolean needClientAuth;
    private boolean wantClientAuth;
    private String[] enabledCipherSuites;
    private String[] enabledProtocols;

    /**
     * Creates a new SSL filter using the specified {@link SSLContext}.
     */
    public SSLFilter( SSLContext sslContext )
    {
        if( sslContext == null )
        {
            throw new NullPointerException( "sslContext" );
        }

        this.sslContext = sslContext;
    }

    /**
     * Returns <tt>true</tt> if the engine is set to use client mode
     * when handshaking.
     */
    public boolean isUseClientMode()
    {
        return client;
    }
    
    /**
     * Configures the engine to use client (or server) mode when handshaking.
     */
    public void setUseClientMode( boolean clientMode )
    {
        this.client = clientMode;
    }
    
    /**
     * Returns <tt>true</tt> if the engine will <em>require</em> client authentication.
     * This option is only useful to engines in the server mode.
     */
    public boolean isNeedClientAuth()
    {
        return needClientAuth;
    }

    /**
     * Configures the engine to <em>require</em> client authentication.
     * This option is only useful for engines in the server mode.
     */
    public void setNeedClientAuth( boolean needClientAuth )
    {
        this.needClientAuth = needClientAuth;
    }
    
    
    /**
     * Returns <tt>true</tt> if the engine will <em>request</em> client authentication.
     * This option is only useful to engines in the server mode.
     */
    public boolean isWantClientAuth()
    {
        return wantClientAuth;
    }
    
    /**
     * Configures the engine to <em>request</em> client authentication.
     * This option is only useful for engines in the server mode.
     */
    public void setWantClientAuth( boolean wantClientAuth )
    {
        this.wantClientAuth = wantClientAuth;
    }
    
    /**
     * Returns the list of cipher suites to be enabled when {@link SSLEngine}
     * is initialized.
     * 
     * @return <tt>null</tt> means 'use {@link SSLEngine}'s default.'
     */
    public String[] getEnabledCipherSuites()
    {
        return enabledCipherSuites;
    }
    
    /**
     * Sets the list of cipher suites to be enabled when {@link SSLEngine}
     * is initialized.
     * 
     * @param cipherSuites <tt>null</tt> means 'use {@link SSLEngine}'s default.'
     */
    public void setEnabledCipherSuites( String[] cipherSuites )
    {
        this.enabledCipherSuites = cipherSuites;
    }

    /**
     * Returns the list of protocols to be enabled when {@link SSLEngine}
     * is initialized.
     * 
     * @return <tt>null</tt> means 'use {@link SSLEngine}'s default.'
     */
    public String[] getEnabledProtocols()
    {
        return enabledProtocols;
    }
    
    /**
     * Sets the list of protocols to be enabled when {@link SSLEngine}
     * is initialized.
     * 
     * @param protocols <tt>null</tt> means 'use {@link SSLEngine}'s default.'
     */
    public void setEnabledProtocols( String[] protocols )
    {
        this.enabledProtocols = protocols;
    }

    // IoFilter impl.

    public void sessionOpened( NextFilter nextFilter, IoSession session ) throws SSLException
    {
        // Create an SSL handler
        createSSLSessionHandler( nextFilter, session );
        nextFilter.sessionOpened( session );
    }

    public void sessionClosed( NextFilter nextFilter, IoSession session )
    {
        SSLHandler sslHandler = getSSLSessionHandler( session );
        if( log.isLoggable( Level.FINEST ) )
        {
            log.log( Level.FINEST, session + " Closed: " + sslHandler );
        }
        if( sslHandler != null )
        {
            synchronized( sslHandler )
            {
               // Start SSL shutdown process
               try
               {
                  // shut down
                  sslHandler.shutdown();
                  
                  // there might be data to write out here?
                  writeNetBuffer( nextFilter, session, sslHandler );
               }
               catch( SSLException ssle )
               {
                  nextFilter.exceptionCaught( session, ssle );
               }
               finally
               {
                  // notify closed session
                  nextFilter.sessionClosed( session );
                  
                  // release buffers
                  sslHandler.release();
                  removeSSLSessionHandler( session );
               }
            }
        }
    }
   
    public void dataRead( NextFilter nextFilter, IoSession session,
                          ByteBuffer buf ) throws SSLException
    {
        SSLHandler sslHandler = createSSLSessionHandler( nextFilter, session );
        if( sslHandler != null )
        {
            if( log.isLoggable( Level.FINEST ) )
            {
                log.log( Level.FINEST, session + " Data Read: " + sslHandler + " (" + buf+ ')' );
            }
            synchronized( sslHandler )
            {
                try
                {
                    // forward read encrypted data to SSL handler
                    sslHandler.dataRead( nextFilter, buf.buf() );

                    // Handle data to be forwarded to application or written to net
                    handleSSLData( nextFilter, session, sslHandler );

                    if( sslHandler.isClosed() )
                    {
                        if( log.isLoggable( Level.FINEST ) )
                        {
                            log.log( Level.FINEST,
                                     session + " SSL Session closed. Closing connection.." );
                        }
                        session.close();
                    }
                }
                catch( SSLException ssle )
                {
                    if( !sslHandler.isInitialHandshakeComplete() )
                    {
                        SSLException newSSLE = new SSLHandshakeException(
                                "Initial SSL handshake failed." );
                        newSSLE.initCause( ssle );
                        ssle = newSSLE;
                    }

                    nextFilter.exceptionCaught( session, ssle );
                }
            }
        }
        else
        {
            nextFilter.dataRead( session, buf );
        }
    }

    public void dataWritten( NextFilter nextFilter, IoSession session,
                            Object marker )
    {
        if( marker != SSL_MARKER )
        {
            nextFilter.dataWritten( session, marker );
        }
    }

    public void filterWrite( NextFilter nextFilter, IoSession session, ByteBuffer buf, Object marker ) throws SSLException
    {

        SSLHandler handler = createSSLSessionHandler( nextFilter, session );
        if( log.isLoggable( Level.FINEST ) )
        {
            log.log( Level.FINEST, session + " Filtered Write: " + handler );
        }

        synchronized( handler )
        {
            if( handler.isWritingEncryptedData() )
            {
                // data already encrypted; simply return buffer
                if( log.isLoggable( Level.FINEST ) )
                {
                    log.log( Level.FINEST, session + "   already encrypted: " + buf );
                }
                nextFilter.filterWrite( session, buf, marker );
                return;
            }
            
            if( handler.isInitialHandshakeComplete() )
            {
                // SSL encrypt
                if( log.isLoggable( Level.FINEST ) )
                {
                    log.log( Level.FINEST, session + " encrypt: " + buf );
                }
                handler.encrypt( buf.buf() );
                ByteBuffer encryptedBuffer = copy( handler
                        .getOutNetBuffer() );

                if( log.isLoggable( Level.FINEST ) )
                {
                    log.log( Level.FINEST, session + " encrypted buf: " + encryptedBuffer);
                }
                buf.release();
                nextFilter.filterWrite( session, encryptedBuffer, marker );
                return;
            }
            else
            {
                if( !session.isConnected() )
                {
                    if( log.isLoggable( Level.FINEST ) )
                    {
                        log.log( Level.FINEST, session + " Write request on closed session." );
                    }
                }
                else
                {
                    if( log.isLoggable( Level.FINEST ) )
                    {
                        log.log( Level.FINEST, session + " Handshaking is not complete yet. Buffering write request." );
                    }
                    handler.scheduleWrite( nextFilter, buf, marker );
                }
            }
        }
    }

    // Utiliities

    private void handleSSLData( NextFilter nextFilter, IoSession session,
                               SSLHandler handler ) throws SSLException
    {
        // Flush any buffered write requests occurred before handshaking.
        if( handler.isInitialHandshakeComplete() )
        {
            handler.flushScheduledWrites();
        }

        // Write encrypted data to be written (if any)
        writeNetBuffer( nextFilter, session, handler );

        // handle app. data read (if any)
        handleAppDataRead( nextFilter, session, handler );
    }

    private void handleAppDataRead( NextFilter nextFilter, IoSession session,
                                   SSLHandler sslHandler )
    {
        if( log.isLoggable( Level.FINEST ) )
        {
            log.log( Level.FINEST, session + " appBuffer: " + sslHandler.getAppBuffer() );
        }
        if( sslHandler.getAppBuffer().hasRemaining() )
        {
            // forward read app data
            ByteBuffer readBuffer = copy( sslHandler.getAppBuffer() );
            if( log.isLoggable( Level.FINEST ) )
            {
                log.log( Level.FINEST, session + " app data read: " + readBuffer + " (" + readBuffer.getHexDump() + ')' );
            }
            nextFilter.dataRead( session, readBuffer );
        }
    }

    void writeNetBuffer( NextFilter nextFilter, IoSession session, SSLHandler sslHandler )
            throws SSLException
    {
        // Check if any net data needed to be writen
        if( !sslHandler.getOutNetBuffer().hasRemaining() )
        {
            // no; bail out
            return;
        }

        // write net data

        // set flag that we are writing encrypted data
        // (used in filterWrite() above)
        synchronized( sslHandler )
        {
            sslHandler.setWritingEncryptedData( true );
        }

        try
        {
            if( log.isLoggable( Level.FINEST ) )
            {
                log.log( Level.FINEST, session + " write outNetBuffer: " +
                                   sslHandler.getOutNetBuffer() );
            }
            ByteBuffer writeBuffer = copy( sslHandler.getOutNetBuffer() );
            if( log.isLoggable( Level.FINEST ) )
            {
                log.log( Level.FINEST, session + " session write: " + writeBuffer );
            }
            //debug("outNetBuffer (after copy): {0}", sslHandler.getOutNetBuffer());
            filterWrite( nextFilter, session, writeBuffer, SSL_MARKER );

            // loop while more writes required to complete handshake
            while( sslHandler.needToCompleteInitialHandshake() )
            {
                try
                {
                    sslHandler.continueHandshake( nextFilter );
                }
                catch( SSLException ssle )
                {
                    SSLException newSSLE = new SSLHandshakeException(
                            "Initial SSL handshake failed." );
                    newSSLE.initCause( ssle );
                    throw newSSLE;
                }
                if( sslHandler.getOutNetBuffer().hasRemaining() )
                {
                    if( log.isLoggable( Level.FINEST ) )
                    {
                        log.log( Level.FINEST, session + " write outNetBuffer2: " +
                                           sslHandler.getOutNetBuffer() );
                    }
                    ByteBuffer writeBuffer2 = copy( sslHandler
                            .getOutNetBuffer() );
                    filterWrite( nextFilter, session, writeBuffer2, SSL_MARKER );
                }
            }
        }
        finally
        {
            synchronized( sslHandler )
            {
                sslHandler.setWritingEncryptedData( false );
            }
        }
    }

    /**
     * Creates a new Mina byte buffer that is a deep copy of the remaining bytes
     * in the given buffer (between index buf.position() and buf.limit())
     *
     * @param src the buffer to copy
     * @return the new buffer, ready to read from
     */
    private static ByteBuffer copy( java.nio.ByteBuffer src )
    {
        ByteBuffer copy = ByteBuffer.allocate( src.remaining() );
        copy.put( src );
        copy.flip();
        return copy;
    }

    // Utilities to mainpulate SSLHandler based on IoSession

    private SSLHandler createSSLSessionHandler( NextFilter nextFilter, IoSession session ) throws SSLException
    {
        SSLHandler handler = ( SSLHandler ) sslSessionHandlerMap.get( session );
        if( handler == null )
        {
            synchronized( sslSessionHandlerMap )
            {
                handler = ( SSLHandler ) sslSessionHandlerMap.get( session );
                if( handler == null )
                {
                    boolean done = false;
                    try
                    {
                        handler =
                            new SSLHandler( this, sslContext, session );
                        sslSessionHandlerMap.put( session, handler );
                        handler.doHandshake( nextFilter );
                        done = true;
                    }
                    finally 
                    {
                        if( !done )
                        {
                            sslSessionHandlerMap.remove( session );
                        }
                    }
                }
            }
        }
        
        return handler;
    }

    private SSLHandler getSSLSessionHandler( IoSession session )
    {
        return ( SSLHandler ) sslSessionHandlerMap.get( session );
    }

    private void removeSSLSessionHandler( IoSession session )
    {
        synchronized( sslSessionHandlerMap )
        {
            sslSessionHandlerMap.remove( session );
        }
    }
}
