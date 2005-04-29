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

import java.lang.reflect.Method;
import java.text.DateFormat;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.DefaultExceptionMonitor;
import org.apache.mina.io.IoHandler;
import org.apache.mina.io.IoFilterAdapter;
import org.apache.mina.io.IoSession;

/**
 * An SSL filter that encrypts and decrypts the data exchanged in the session.
 * This filter uses an {@link SSLEngine} which was introduced in Java 5, so 
 * Java version 5 or above is mandatory to use this filter. And please note that
 * this filter only works for TCP/IP connections.
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class SSLFilter extends IoFilterAdapter
{
    /**
     * A marker which is passed with {@link IoHandler#dataWritten(IoSession, Object)}
     * when <tt>SSLFilter</tt> writes data other then user actually requested.
     */
    public static final Object SSL_MARKER = new Object()
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

    /** debug interface */
    Debug debug = null;

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

    /**
     * Sets the debug message auditter.
     */
    public void setDebug( Debug debug )
    {
        if( debug == null )
        {
            throw new NullPointerException( "debug" );
        }

        if( debug == Debug.OFF )
        {
            this.debug = null;
        }
        else
        {
            this.debug = debug;
        }
    }

    /**
     * Gets the debug message auditter.
     */
    public Debug getDebug()
    {
        if( debug == null )
        {
            return Debug.OFF;
        }
        else
        {
            return debug;
        }
    }

    // IoFilter impl.

    public void sessionOpened( NextFilter nextFilter, IoSession session )
    {
        // Create an SSL handler
        createSSLSessionHandler( session );
        nextFilter.sessionOpened( session );
    }

    public void sessionClosed( NextFilter nextFilter, IoSession session )
    {
        SSLHandler sslHandler = getSSLSessionHandler( session );
        if( debug != null )
        {
            debug.print( this, "Closed: " + sslHandler );
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
                  writeNetBuffer( session, sslHandler );
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
                         ByteBuffer buf )
    {
        SSLHandler sslHandler = getSSLSessionHandler( session );
        if( sslHandler != null )
        {
            if( debug != null )
            {
                debug.print( this, "Data Read: " + sslHandler + " (" + buf+ ')' );
            }
            synchronized( sslHandler )
            {
                try
                {
                    // forward read encrypted data to SSL handler
                    sslHandler.dataRead( buf.buf() );

                    // Handle data to be forwarded to application or written to net
                    handleSSLData( nextFilter, session, sslHandler );

                    if( sslHandler.isClosed() )
                    {
                        if( debug != null )
                        {
                            debug.print(
                                    this,
                                    "SSL Session closed. Closing connection.." );
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
        nextFilter.dataWritten( session, marker );
    }

    public void filterWrite( NextFilter nextFilter, IoSession session, ByteBuffer buf, Object marker )
    {

        SSLHandler handler = createSSLSessionHandler( session );
        if( debug != null )
        {
            debug.print( this, "Filtered Write: " + handler );
        }

        synchronized( handler )
        {
            if( handler.isWritingEncryptedData() )
            {
                // data already encrypted; simply return buffer
                if( debug != null )
                {
                    debug.print( this, "  already encrypted: " + buf );
                }
                nextFilter.filterWrite( session, buf, marker );
                return;
            }
            
            if( handler.isInitialHandshakeComplete() )
            {
                // SSL encrypt
                try
                {
                    if( debug != null )
                    {
                        debug.print( this, "encrypt: " + buf );
                    }
                    handler.encrypt( buf.buf() );
                    ByteBuffer encryptedBuffer = copy( handler
                            .getOutNetBuffer() );

                    if( debug != null )
                    {
                        debug.print( this, "encrypted buf: " + encryptedBuffer);
                    }
                    buf.release();
                    nextFilter.filterWrite( session, encryptedBuffer, marker );
                    return;
                }
                catch( SSLException ssle )
                {
                    throw new RuntimeException(
                            "Unexpected SSLException.", ssle );
                }
            }
            else
            {
                if( !session.isConnected() )
                {
                    if( debug != null )
                    {
                        debug.print( this, "Write request on closed session." );
                    }
                }
                else
                {
                    if( debug != null )
                    {
                        debug.print( this, "Handshaking is not complete yet. Buffering write request." );
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
        writeNetBuffer( session, handler );

        // handle app. data read (if any)
        handleAppDataRead( nextFilter, session, handler );
    }

    private void handleAppDataRead( NextFilter nextFilter, IoSession session,
                                   SSLHandler sslHandler )
    {
        if( debug != null )
        {
            debug.print( this, "appBuffer: " + sslHandler.getAppBuffer() );
        }
        if( sslHandler.getAppBuffer().hasRemaining() )
        {
            // forward read app data
            ByteBuffer readBuffer = copy( sslHandler.getAppBuffer() );
            if( debug != null )
            {
                debug.print( this, "app data read: " + readBuffer + " (" + readBuffer.getHexDump() + ')' );
            }
            nextFilter.dataRead( session, readBuffer );
        }
    }

    void writeNetBuffer( IoSession session, SSLHandler sslHandler )
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
            if( debug != null )
            {
                debug.print( this, "write outNetBuffer: " +
                                   sslHandler.getOutNetBuffer() );
            }
            ByteBuffer writeBuffer = copy( sslHandler.getOutNetBuffer() );
            if( debug != null )
            {
                debug.print( this, "session write: " + writeBuffer );
            }
            //debug("outNetBuffer (after copy): {0}", sslHandler.getOutNetBuffer());
            session.write( writeBuffer, SSL_MARKER );

            // loop while more writes required to complete handshake
            while( sslHandler.needToCompleteInitialHandshake() )
            {
                try
                {
                    sslHandler.continueHandshake();
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
                    if( debug != null )
                    {
                        debug.print( this, "write outNetBuffer2: " +
                                           sslHandler.getOutNetBuffer() );
                    }
                    ByteBuffer writeBuffer2 = copy( sslHandler
                            .getOutNetBuffer() );
                    session.write( writeBuffer2, SSL_MARKER );
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

    private SSLHandler createSSLSessionHandler( IoSession session )
    {
        SSLHandler handler = ( SSLHandler ) sslSessionHandlerMap.get( session );
        if( handler == null )
        {
            synchronized( sslSessionHandlerMap )
            {
                handler = ( SSLHandler ) sslSessionHandlerMap.get( session );
                if( handler == null )
                {
                    try
                    {
                        handler =
                            new SSLHandler( this, sslContext, session );
                        sslSessionHandlerMap.put( session, handler );
                        handler.doHandshake();
                    }
                    catch( SSLException e )
                    {
                        sslSessionHandlerMap.remove( session );
                        throw new RuntimeException(
                                "Failed to initialize SSLEngine.", e );
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

    /**
     * An interface that users can log debug messages from an {@link SSLFilter}.
     * 
     * @author Trustin Lee (trustin@apache.org)
     * @version $Rev$, $Date$
     */
    public static interface Debug
    {
        /**
         * This will print out the messages to Commons-Logging or stdout.
         */
        static final Debug ON = new DebugOn();

        /**
         * This will suppress debug messages.
         */
        static final Debug OFF = new DebugOff();

        /**
         * Prints out the specified debug messages.
         */
        void print( SSLFilter filter, String message );
    }

    private static class DebugOn implements Debug
    {
        private static final Object log;

        private static final Method debugMethod;

        static
        {
            Object tempLog = null;
            Method tempDebugMethod = null;

            try
            {
                Class logCls = Class
                        .forName( "org.apache.commons.logging.Log" );
                Class logFactoryCls = Class
                        .forName( "org.apache.commons.logging.LogFactory" );
                Method getLogMethod = logFactoryCls.getMethod( "getLog",
                        new Class[] { String.class } );
                tempLog = getLogMethod.invoke( null,
                        new Object[] { DefaultExceptionMonitor.class
                                .getPackage().getName() } );
                tempDebugMethod = logCls.getMethod( "debug",
                        new Class[] { Object.class } );
            }
            catch( Exception e )
            {
                tempLog = null;
                tempDebugMethod = null;
            }

            log = tempLog;
            debugMethod = tempDebugMethod;
        }

        private final DateFormat df = DateFormat.getDateTimeInstance(
                DateFormat.MEDIUM, DateFormat.MEDIUM );

        private final Date date = new Date();

        public void print( SSLFilter filter, String message )
        {
            if( filter.isUseClientMode() )
            {
                message = "[CLIENT] " + message;
            }
            else
            {
                message = "[SERVER] " + message;
            }

            if( log == null )
            {
                logToStdOut( message );
            }
            else
            {
                logToCommonsLogging( message );
            }
        }

        private void logToCommonsLogging( String message )
        {
            try
            {
                debugMethod.invoke( log, new Object[] { message } );
            }
            catch( Exception e )
            {
                logToStdOut( message );
            }
        }

        private void logToStdOut( String message )
        {
            synchronized( System.out )
            {
                date.setTime( System.currentTimeMillis() );

                System.out.print( '[' );
                System.out.print( df.format( date ) );
                System.out.print( "] [" );
                System.out.print( Thread.currentThread().getName() );
                System.out.print( "] " );
                System.out.println( message );
            }
        }
    }

    private static class DebugOff implements Debug
    {
        public void print( SSLFilter filter, String message )
        {
            // do nothing
        }
    }
}
