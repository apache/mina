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
import org.apache.mina.io.DefaultExceptionMonitor;
import org.apache.mina.io.IoHandler;
import org.apache.mina.io.IoHandlerFilterAdapter;
import org.apache.mina.io.IoSession;

/**
 * An SSL filter that encrypts and decrypts the data exchanged in the session.
 * This filter uses an {@link SSLEngine} which was introduced in Java 5, so 
 * Java version 5 or above is mandatory to use this filter. And please note that
 * this filter only works for TCP/IP connections.
 * <p>
 * Jan Andersson kindly contributed this filter for the Apache Directory team.
 * We thank him a lot for his significant contribution.
 * 
 * @author Jan Andersson (janne@minq.se)
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 */
public class SSLFilter extends IoHandlerFilterAdapter
{
    // SSL Context
    private SSLContext sslContext;

    // Map used to map SSLHandler objects per session (key is IoSession)
    private Map sslSessionHandlerMap = new IdentityHashMap();

    /** debug interface */
    Debug debug = null;

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

    // IoHandlerFilter impl.

    public void sessionOpened( IoHandler nextHandler, IoSession session )
    {
        nextHandler.sessionOpened( session );
        // Create an SSL handler
        createSSLSessionHandler( session );
    }

    public void sessionClosed( IoHandler nextHandler, IoSession session )
    {
        SSLHandler sslHandler = getSSLSessionHandler( session );
        if( debug != null )
        {
            debug.print( "Closed: " + sslHandler );
        }
        if( sslHandler != null )
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
                nextHandler.exceptionCaught( session, ssle );
            }
            finally
            {
                // notify closed session
                nextHandler.sessionClosed( session );

                // release buffers
                sslHandler.release();
                removeSSLSessionHandler( session );
            }
        }
    }

    public void dataRead( IoHandler nextHandler, IoSession session,
                         ByteBuffer buf )
    {
        SSLHandler sslHandler = getSSLSessionHandler( session );
        if( sslHandler != null )
        {
            if( debug != null )
            {
                debug.print( "Data Read: " + sslHandler + " ("
                             + buf.getHexDump() + ')' );
            }
            synchronized( sslHandler )
            {
                try
                {
                    // forward read encrypted data to SSL handler
                    sslHandler.dataRead( buf.buf() );

                    // Handle data to be forwarded to application or written to net
                    handleSSLData( nextHandler, session, sslHandler );

                    if( sslHandler.isClosed() )
                    {
                        if( debug != null )
                        {
                            debug
                                    .print( "SSL Session closed. Closing connection.." );
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

                    nextHandler.exceptionCaught( session, ssle );
                }
            }
        }
        else
        {
            nextHandler.dataRead( session, buf );
        }
    }

    public void dataWritten( IoHandler nextHandler, IoSession session,
                            Object marker )
    {
        nextHandler.dataWritten( session, marker );
    }

    public ByteBuffer filterWrite( IoSession session, ByteBuffer buf )
    {

        SSLHandler sslHandler = getSSLSessionHandler( session );
        if( debug != null )
        {
            debug.print( "Filtered Write: " + sslHandler );
        }
        if( sslHandler != null )
        {
            synchronized( sslHandler )
            {
                if( sslHandler.isWritingEncryptedData() )
                {
                    // data already encrypted; simply return buffer
                    if( debug != null )
                    {
                        debug.print( "  already encrypted: " + buf );
                    }
                    return buf;
                }
                if( sslHandler.isInitialHandshakeComplete() )
                {
                    // SSL encrypt
                    try
                    {
                        if( debug != null )
                        {
                            debug.print( "encrypt: " + buf );
                        }
                        sslHandler.encrypt( buf.buf() );
                        ByteBuffer encryptedBuffer = copy( sslHandler
                                .getOutNetBuffer() );

                        if( debug != null )
                        {
                            debug.print( "encrypted data: "
                                    + encryptedBuffer.getHexDump() );
                        }
                        return encryptedBuffer;
                    }
                    catch( SSLException ssle )
                    {
                        throw new RuntimeException(
                                "Unexpected SSLException.", ssle );
                    }
                }
            }
        }
        return buf;
    }

    // Utiliities

    private void handleSSLData( IoHandler nextHandler, IoSession session,
                               SSLHandler sslHandler ) throws SSLException
    {
        // First write encrypted data to be written (if any)
        writeNetBuffer( session, sslHandler );
        // handle app. data read (if any)
        handleAppDataRead( nextHandler, session, sslHandler );
    }

    private void handleAppDataRead( IoHandler nextHandler, IoSession session,
                                   SSLHandler sslHandler )
    {
        if( debug != null )
        {
            debug.print( "appBuffer: " + sslHandler.getAppBuffer() );
        }
        if( sslHandler.getAppBuffer().hasRemaining() )
        {
            // forward read app data
            ByteBuffer readBuffer = copy( sslHandler.getAppBuffer() );
            if( debug != null )
            {
                debug.print( "app data read: " + readBuffer + " (" + readBuffer.getHexDump() + ')' );
            }
            nextHandler.dataRead( session, readBuffer );
        }
    }

    private void writeNetBuffer( IoSession session, SSLHandler sslHandler )
            throws SSLException
    {
        // first check if any net data needed to be writen
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
                debug.print( "write outNetBuffer: "
                        + sslHandler.getOutNetBuffer() );
            }
            ByteBuffer writeBuffer = copy( sslHandler.getOutNetBuffer() );
            if( debug != null )
            {
                debug.print( "session write: " + writeBuffer );
            }
            //debug("outNetBuffer (after copy): {0}", sslHandler.getOutNetBuffer());
            session.write( writeBuffer, null );

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
                        debug.print( "write outNetBuffer2: "
                                + sslHandler.getOutNetBuffer() );
                    }
                    ByteBuffer writeBuffer2 = copy( sslHandler
                            .getOutNetBuffer() );
                    session.write( writeBuffer2, null );
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
    private ByteBuffer copy( java.nio.ByteBuffer src )
    {
        ByteBuffer copy = ByteBuffer.allocate( src.remaining() );
        copy.put( src );
        copy.flip();
        return copy;
    }

    // Utilities to mainpulate SSLHandler based on IoSession

    private SSLHandler createSSLSessionHandler( IoSession session )
    {
        SSLHandler sslHandler = new SSLHandler( this, sslContext );
        synchronized( sslSessionHandlerMap )
        {
            sslSessionHandlerMap.put( session, sslHandler );
        }
        return sslHandler;
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
        void print( String message );
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

        public void print( String message )
        {
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
        public void print( String message )
        {
            // do nothing
        }
    }
}
