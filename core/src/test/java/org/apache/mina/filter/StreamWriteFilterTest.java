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
package org.apache.mina.filter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.MessageDigest;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.DefaultWriteRequest;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoFilterChain;
import org.apache.mina.common.IoFutureListener;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoService;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.IoSessionConfig;
import org.apache.mina.common.TransportType;
import org.apache.mina.common.WriteFuture;
import org.apache.mina.common.WriteRequest;
import org.apache.mina.common.IoFilter.NextFilter;
import org.apache.mina.common.support.BaseIoSession;
import org.apache.mina.transport.socket.nio.SocketAcceptor;
import org.apache.mina.transport.socket.nio.SocketConnector;
import org.apache.mina.util.AvailablePortFinder;
import org.easymock.AbstractMatcher;
import org.easymock.MockControl;

/**
 * Tests {@link StreamWriteFilter}.
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class StreamWriteFilterTest extends TestCase {
    private MockControl mockNextFilter;
    private IoSession session;
    private NextFilter nextFilter;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        /*
         * Create the mocks.
         */
        session = new DummySession();
        mockNextFilter = MockControl.createControl( NextFilter.class );
        nextFilter = ( NextFilter ) mockNextFilter.getMock();
    }
    
    @Override
    protected void tearDown() throws Exception
    {
        assertFalse(session.containsAttribute(StreamWriteFilter.CURRENT_STREAM));
        assertFalse(session.containsAttribute(StreamWriteFilter.CURRENT_WRITE_REQUEST));
        assertFalse(session.containsAttribute(StreamWriteFilter.WRITE_REQUEST_QUEUE));
    }

    public void testWriteEmptyStream() throws Exception
    {
        StreamWriteFilter filter = new StreamWriteFilter();
        
        InputStream stream = new ByteArrayInputStream( new byte[ 0 ] );
        WriteRequest writeRequest = new DefaultWriteRequest( stream, new DummyWriteFuture() );
        
        /*
         * Record expectations
         */
        nextFilter.messageSent( session, writeRequest );
        
        /*
         * Replay.
         */
        mockNextFilter.replay();
        
        filter.filterWrite( nextFilter, session, writeRequest );
        
        /*
         * Verify.
         */
        mockNextFilter.verify();
        
        assertTrue( writeRequest.getFuture().isWritten() );
    }

    /**
     * Tests that the filter just passes objects which aren't InputStreams
     * through to the next filter.
     */
    public void testWriteNonStreamMessage() throws Exception
    {
        StreamWriteFilter filter = new StreamWriteFilter();
        
        Object message = new Object();
        WriteRequest writeRequest = new DefaultWriteRequest( message, new DummyWriteFuture() );
        
        /*
         * Record expectations
         */
        nextFilter.filterWrite( session, writeRequest );
        nextFilter.messageSent( session, writeRequest );
        
        /*
         * Replay.
         */
        mockNextFilter.replay();
        
        filter.filterWrite( nextFilter, session, writeRequest );
        filter.messageSent( nextFilter, session, writeRequest );
        
        /*
         * Verify.
         */
        mockNextFilter.verify();
    }
    
    /**
     * Tests when the contents of the stream fits into one write buffer.
     */
    public void testWriteSingleBufferStream() throws Exception
    {
        StreamWriteFilter filter = new StreamWriteFilter();
        
        byte[] data = new byte[] { 1, 2, 3, 4 };
        
        InputStream stream = new ByteArrayInputStream( data );
        WriteRequest writeRequest = new DefaultWriteRequest( stream, new DummyWriteFuture() );
        
        /*
         * Record expectations
         */
        nextFilter.filterWrite( session, new DefaultWriteRequest( ByteBuffer.wrap( data ) ) );
        mockNextFilter.setMatcher( new WriteRequestMatcher() );
        nextFilter.messageSent( session, writeRequest );
        
        /*
         * Replay.
         */
        mockNextFilter.replay();
        
        filter.filterWrite( nextFilter, session, writeRequest );
        filter.messageSent( nextFilter, session, writeRequest );
        
        /*
         * Verify.
         */
        mockNextFilter.verify();
        
        assertTrue( writeRequest.getFuture().isWritten() );
    }

    /**
     * Tests when the contents of the stream doesn't fit into one write buffer.
     */
    public void testWriteSeveralBuffersStream() throws Exception
    {
        StreamWriteFilter filter = new StreamWriteFilter();
        filter.setWriteBufferSize( 4 );
        
        byte[] data = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        byte[] chunk1 = new byte[] { 1, 2, 3, 4 };
        byte[] chunk2 = new byte[] { 5, 6, 7, 8 };
        byte[] chunk3 = new byte[] { 9, 10 };
        
        InputStream stream = new ByteArrayInputStream( data );
        WriteRequest writeRequest = new DefaultWriteRequest( stream, new DummyWriteFuture() );
        
        WriteRequest chunk1Request = new DefaultWriteRequest( ByteBuffer.wrap( chunk1 ) );
        WriteRequest chunk2Request = new DefaultWriteRequest( ByteBuffer.wrap( chunk2 ) );
        WriteRequest chunk3Request = new DefaultWriteRequest( ByteBuffer.wrap( chunk3 ) );

        /*
         * Record expectations
         */
        nextFilter.filterWrite( session, chunk1Request );
        mockNextFilter.setMatcher( new WriteRequestMatcher() );
        nextFilter.filterWrite( session, chunk2Request );
        nextFilter.filterWrite( session, chunk3Request );
        nextFilter.messageSent( session, writeRequest );
        
        /*
         * Replay.
         */
        mockNextFilter.replay();
        
        filter.filterWrite( nextFilter, session, writeRequest );
        filter.messageSent( nextFilter, session, chunk1Request );
        filter.messageSent( nextFilter, session, chunk2Request );
        filter.messageSent( nextFilter, session, chunk3Request );
        
        /*
         * Verify.
         */
        mockNextFilter.verify();
        
        assertTrue( writeRequest.getFuture().isWritten() );
    }
    
    public void testWriteWhileWriteInProgress() throws Exception
    {
        StreamWriteFilter filter = new StreamWriteFilter();
        
        Queue<WriteRequest> queue = new LinkedList<WriteRequest>();
        InputStream stream = new ByteArrayInputStream( new byte[ 5 ] );

        /*
         * Make up the situation.
         */
        session.setAttribute( StreamWriteFilter.CURRENT_STREAM, stream );
        session.setAttribute( StreamWriteFilter.WRITE_REQUEST_QUEUE, queue );

        /*
         * Replay.  (We recorded *nothing* because nothing should occur.)
         */
        mockNextFilter.replay();

        WriteRequest wr = new DefaultWriteRequest( new Object(), new DummyWriteFuture() );
        filter.filterWrite( nextFilter, session, wr );
        assertEquals( 1, queue.size() );
        assertSame( wr, queue.poll() );
        
        /*
         * Verify.
         */
        mockNextFilter.verify();
        
        session.removeAttribute(StreamWriteFilter.CURRENT_STREAM);
        session.removeAttribute(StreamWriteFilter.WRITE_REQUEST_QUEUE);
    }
    
    public void testWritesWriteRequestQueueWhenFinished() throws Exception
    {
        StreamWriteFilter filter = new StreamWriteFilter();

        WriteRequest wrs[] = new WriteRequest[] { 
                new DefaultWriteRequest( new Object(), new DummyWriteFuture() ),
                new DefaultWriteRequest( new Object(), new DummyWriteFuture() ),
                new DefaultWriteRequest( new Object(), new DummyWriteFuture() )
        };
        Queue<WriteRequest> queue = new LinkedList<WriteRequest>();
        queue.offer( wrs[ 0 ] );
        queue.offer( wrs[ 1 ] );
        queue.offer( wrs[ 2 ] );
        InputStream stream = new ByteArrayInputStream( new byte[ 0 ] );
        
        /*
         * Make up the situation.
         */
        session.setAttribute( StreamWriteFilter.CURRENT_STREAM, stream );
        session.setAttribute( StreamWriteFilter.CURRENT_WRITE_REQUEST, new DefaultWriteRequest(stream));
        session.setAttribute( StreamWriteFilter.WRITE_REQUEST_QUEUE, queue );

        /*
         * Record expectations
         */
        nextFilter.filterWrite( session, wrs[ 0 ] );
        nextFilter.filterWrite( session, wrs[ 1 ] );
        nextFilter.filterWrite( session, wrs[ 2 ] );
        nextFilter.messageSent( session, new DefaultWriteRequest(stream) );
        mockNextFilter.setMatcher(new WriteRequestMatcher());
        
        /*
         * Replay.
         */
        mockNextFilter.replay();

        filter.messageSent( nextFilter, session, new DefaultWriteRequest(new Object()) );
        assertEquals( 0, queue.size() );
        
        /*
         * Verify.
         */
        mockNextFilter.verify();
    }    
    
    /**
     * Tests that {@link StreamWriteFilter#setWriteBufferSize(int)} checks the
     * specified size.
     */
    public void testSetWriteBufferSize() throws Exception
    {
        StreamWriteFilter filter = new StreamWriteFilter();
        
        try
        {
            filter.setWriteBufferSize( 0 );
            fail( "0 writeBuferSize specified. IllegalArgumentException expected." );
        }
        catch ( IllegalArgumentException iae )
        {
        }
        
        try
        {
            filter.setWriteBufferSize( -100 );
            fail( "Negative writeBuferSize specified. IllegalArgumentException expected." );
        }
        catch ( IllegalArgumentException iae )
        {
        }

        filter.setWriteBufferSize( 1 );
        assertEquals( 1, filter.getWriteBufferSize() );
        filter.setWriteBufferSize( 1024 );
        assertEquals( 1024, filter.getWriteBufferSize() );
    }
    
    public void testWriteUsingSocketTransport() throws Exception
    {
        SocketAcceptor acceptor = new SocketAcceptor();
        acceptor.setReuseAddress( true );
        SocketAddress address = new InetSocketAddress( "localhost", AvailablePortFinder.getNextAvailable() );

        SocketConnector connector = new SocketConnector();
        
        FixedRandomInputStream stream = new FixedRandomInputStream( 4 * 1024 * 1024 );
        
        SenderHandler sender = new SenderHandler( stream );
        ReceiverHandler receiver = new ReceiverHandler( stream.size );
        
        acceptor.setLocalAddress( address );
        acceptor.setHandler( sender );
        
        connector.setHandler( receiver );

        acceptor.bind();
        
        synchronized( sender.lock )
        {
            synchronized( receiver.lock )
            {
                connector.connect( address );
                
                sender.lock.wait();
                receiver.lock.wait();
            }
        }
        
        acceptor.unbind();
        
        assertEquals( stream.bytesRead, receiver.bytesRead );
        assertEquals( stream.size, receiver.bytesRead );
        byte[] expectedMd5 = stream.digest.digest();
        byte[] actualMd5 = receiver.digest.digest();
        assertEquals( expectedMd5.length, actualMd5.length );
        for( int i = 0; i < expectedMd5.length; i++ )
        {
            assertEquals( expectedMd5[ i ], actualMd5[ i ] );
        }
    }

    private static class FixedRandomInputStream extends InputStream
    {
        long size;
        long bytesRead = 0;
        Random random = new Random();
        MessageDigest digest;

        public FixedRandomInputStream( long size ) throws Exception
        {
            this.size = size;
            digest = MessageDigest.getInstance( "MD5" );
        }

        @Override
        public int read() throws IOException
        {
            if ( isAllWritten() ) {
                return -1;
            }
            bytesRead++;
            byte b = ( byte ) random.nextInt( 255 );
            digest.update( b );
            return b;
        }
        
        public long getBytesRead()
        {
            return bytesRead;
        }

        public long getSize()
        {
            return size;
        }

        public boolean isAllWritten()
        {
            return bytesRead >= size;
        }
    }

    private static class SenderHandler extends IoHandlerAdapter
    {
        Object lock = new Object();
        InputStream inputStream;
        StreamWriteFilter streamWriteFilter = new StreamWriteFilter();

        public SenderHandler( InputStream inputStream )
        {
            this.inputStream = inputStream;
        }

        @Override
        public void sessionCreated( IoSession session ) throws Exception {
            super.sessionCreated( session );
            session.getFilterChain().addLast( "codec", streamWriteFilter );
        }

        @Override
        public void sessionOpened( IoSession session ) throws Exception {
            session.write( inputStream );
        }

        @Override
        public void exceptionCaught( IoSession session, Throwable cause ) throws Exception
        {
            synchronized( lock )
            {
                lock.notifyAll();
            }
        }

        @Override
        public void sessionClosed( IoSession session ) throws Exception
        {
            synchronized( lock )
            {
                lock.notifyAll();
            }
        }

        @Override
        public void sessionIdle( IoSession session, IdleStatus status ) throws Exception
        {
            synchronized( lock )
            {
                lock.notifyAll();
            }
        }

        @Override
        public void messageSent( IoSession session, Object message ) throws Exception
        {
            if( message == inputStream )
            {
                synchronized( lock )
                {
                    lock.notifyAll();
                }
            }
        }
    }

    private static class ReceiverHandler extends IoHandlerAdapter
    {
        Object lock = new Object();
        long bytesRead = 0;
        long size = 0;
        MessageDigest digest;

        public ReceiverHandler( long size ) throws Exception
        {
            this.size = size;
            digest = MessageDigest.getInstance( "MD5" );
        }

        @Override
        public void sessionCreated( IoSession session ) throws Exception
        {
            super.sessionCreated(session);
            
            session.setIdleTime( IdleStatus.READER_IDLE, 5 );
        }

        @Override
        public void sessionIdle( IoSession session, IdleStatus status ) throws Exception
        {
            session.close();
        }

        @Override
        public void exceptionCaught( IoSession session, Throwable cause ) throws Exception
        {
            synchronized( lock )
            {
                lock.notifyAll();
            }
        }
        
        @Override
        public void sessionClosed( IoSession session ) throws Exception
        {
            synchronized( lock )
            {
                lock.notifyAll();
            }
        }

        @Override
        public void messageReceived( IoSession session, Object message ) throws Exception
        {
            ByteBuffer buf = ( ByteBuffer ) message;
            while( buf.hasRemaining() )
            {
                digest.update( buf.get() );
                bytesRead++;
            }
            if( bytesRead >= size )
            {
                session.close();
            }
        }
    }
    
    public static class WriteRequestMatcher extends AbstractMatcher
    {
        @Override
        protected boolean argumentMatches( Object expected, Object actual )
        {
            if( expected instanceof WriteRequest && actual instanceof WriteRequest )
            {
                WriteRequest w1 = ( WriteRequest ) expected;
                WriteRequest w2 = ( WriteRequest ) actual;
                
                return w1.getMessage().equals( w2.getMessage() ) 
                    && w1.getFuture().isWritten() == w2.getFuture().isWritten();
            }
            return super.argumentMatches( expected, actual );
        }
    }
    
    private static class DummySession extends BaseIoSession {

        @Override
        protected void updateTrafficMask() {
        }

        public IoSessionConfig getConfig() {
            return null;
        }

        public IoFilterChain getFilterChain() {
            return null;
        }

        public IoHandler getHandler() {
            return null;
        }

        public SocketAddress getLocalAddress() {
            return null;
        }

        public SocketAddress getRemoteAddress() {
            return null;
        }

        public int getScheduledWriteBytes() {
            return 0;
        }

        public int getScheduledWriteMessages() {
            return 0;
        }

        public IoService getService() {
            return null;
        }

        public TransportType getTransportType() {
            return null;
        }
    }
    
    private static class DummyWriteFuture implements WriteFuture
    {
        private boolean written;
        
        public boolean isWritten()
        {
            return written;
        }

        public void setWritten( boolean written )
        {
            this.written = written;
        }
        
        public IoSession getSession()
        {
            return null;
        }

        public Object getLock()
        {
            return this;
        }

        public void join()
        {
        }

        public boolean join( long timeoutInMillis )
        {
            return true;
        }

        public boolean isReady()
        {
            return true;
        }

        public void addListener( IoFutureListener listener )
        {
        }

        public void removeListener( IoFutureListener listener )
        {
        }

        public void await() throws InterruptedException {
            
        }

        public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
            return true;
        }

        public boolean await(long timeoutMillis) throws InterruptedException {
            return true;
        }

        public void awaitUninterruptibly() {
        }

        public boolean awaitUninterruptibly(long timeout, TimeUnit unit) {
            return true;
        }

        public boolean awaitUninterruptibly(long timeoutMillis) {
            return true;
        }
    }
}
