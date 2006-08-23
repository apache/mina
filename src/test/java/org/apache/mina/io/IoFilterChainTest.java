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
package org.apache.mina.io;

import java.net.SocketAddress;
import java.util.Iterator;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.mina.common.BaseSession;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.SessionConfig;
import org.apache.mina.common.TransportType;

/**
 * Tests {@link AbstractIoFilterChain}.
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$ 
 */
public class IoFilterChainTest extends TestCase
{
    private IoFilterChainImpl chain;
    private IoSession session;
    private String result;

    public void setUp()
    {
        chain = new IoFilterChainImpl();
        session = new TestSession();
        result = "";
    }
    
    public void tearDown()
    {
    }
    
    public void testAdd() throws Exception
    {
        chain.addFirst( "A", new TestFilter( 'A' ) );
        chain.addLast( "B", new TestFilter( 'B' ) );
        chain.addFirst( "C", new TestFilter( 'C' ) );
        chain.addLast( "D", new TestFilter( 'D' ) );
        chain.addBefore( "B", "E", new TestFilter( 'E' ) );
        chain.addBefore( "C", "F", new TestFilter( 'F' ) );
        chain.addAfter( "B", "G", new TestFilter( 'G' ) );
        chain.addAfter( "D", "H", new TestFilter( 'H' ) );
        
        String actual = "";
        for( Iterator i = chain.getChildren().iterator(); i.hasNext(); ) 
        {
            TestFilter f = ( TestFilter ) i.next();
            actual += f.id;
        }
        
        Assert.assertEquals( "FCAEBGDH", actual );
    }
    
    public void testRemove() throws Exception
    {
        chain.addLast( "A", new TestFilter( 'A' ) );
        chain.addLast( "B", new TestFilter( 'B' ) );
        chain.addLast( "C", new TestFilter( 'C' ) );
        chain.addLast( "D", new TestFilter( 'D' ) );
        chain.addLast( "E", new TestFilter( 'E' ) );
        
        chain.remove( "A" );
        chain.remove( "E" );
        chain.remove( "C" );
        chain.remove( "B" );
        chain.remove( "D" );
        
        Assert.assertEquals( 0, chain.getChildren().size() );
    }
    
    public void testClear() throws Exception
    {
        chain.addLast( "A", new TestFilter( 'A' ) );
        chain.addLast( "B", new TestFilter( 'B' ) );
        chain.addLast( "C", new TestFilter( 'C' ) );
        chain.addLast( "D", new TestFilter( 'D' ) );
        chain.addLast( "E", new TestFilter( 'E' ) );
        
        chain.clear();
        
        Assert.assertEquals( 0, chain.getChildren().size() );
    }
    
    public void testDefault()
    {
        run( "HSO HDR HDW HSI HEC HSC" );
    }
    
    public void testChained()
    {
        chain.addLast( "A", new TestFilter( 'A' ) );
        chain.addLast( "B", new TestFilter( 'B' ) );
        run( "ASO BSO HSO" +
             "ADR BDR HDR" +
             "BFW AFW ADW BDW HDW" +
             "ASI BSI HSI" +
             "AEC BEC HEC" +
             "ASC BSC HSC" );
    }
    
    private void run( String expectedResult )
    {
        chain.sessionOpened( session );
        chain.dataRead( session, ByteBuffer.allocate( 16 ) );
        chain.filterWrite( session, ByteBuffer.allocate( 16 ), null );
        chain.sessionIdle( session, IdleStatus.READER_IDLE );
        chain.exceptionCaught( session, new Exception() );
        chain.sessionClosed( session );
        
        result = formatResult( result );
        expectedResult = formatResult( expectedResult );
        
        System.out.println( "Expected: " + expectedResult );
        System.out.println( "Actual:   " + result );
        Assert.assertEquals( expectedResult, result );
    }
    
    private String formatResult( String result )
    {
        result = result.replaceAll( "\\s", "" );
        StringBuffer buf = new StringBuffer( result.length() * 4 / 3 );
        for( int i = 0; i < result.length(); i++ )
        {
            buf.append( result.charAt( i ) );
            if( i % 3 == 2 )
            {
                buf.append(' ');
            }
        }
        
        return buf.toString();
    }

    private class TestSession extends BaseSession implements IoSession
    {
        private IoHandler handler = new IoHandlerAdapter()
        {
            public void sessionOpened(IoSession session) {
                result += "HSO ";
            }

            public void dataRead(IoSession session, ByteBuffer buf) {
                result += "HDR ";
            }

            public void dataWritten(IoSession session, Object marker) {
                result += "HDW ";
            }
            
            public void sessionIdle(IoSession session, IdleStatus status) {
                result += "HSI ";
            }

            public void exceptionCaught(IoSession session, Throwable cause) {
                result += "HEC ";
                if( cause.getClass() != Exception.class )
                {
                    cause.printStackTrace( System.out );
                }
            }

            public void sessionClosed(IoSession session) {
                result += "HSC ";
            }
        };

        public IoHandler getHandler()
        {
            return handler;
        }

        public void close( boolean wait )
        {
        }

        public void write(ByteBuffer buf, Object marker)
        {
        }

        public int getScheduledWriteRequests()
        {
            return 0;
        }

        public TransportType getTransportType()
        {
            return null;
        }

        public boolean isConnected()
        {
            return false;
        }

        public SessionConfig getConfig()
        {
            return null;
        }

        public SocketAddress getRemoteAddress()
        {
            return null;
        }

        public SocketAddress getLocalAddress()
        {
            return null;
        }

        public IoFilterChain getFilterChain()
        {
            return null;
        }
    }

    private class TestFilter implements IoFilter
    {
        private final char id;

        private TestFilter( char id )
        {
            this.id = id;
        }
        
        public void sessionOpened(NextFilter nextFilter, IoSession session) {
            result += id + "SO ";
            nextFilter.sessionOpened( session );
        }

        public void sessionClosed(NextFilter nextFilter, IoSession session) {
            result += id + "SC ";
            nextFilter.sessionClosed( session );
        }

        public void sessionIdle(NextFilter nextFilter, IoSession session, IdleStatus status) {
            result += id + "SI ";
            nextFilter.sessionIdle( session, status );
        }

        public void exceptionCaught(NextFilter nextFilter, IoSession session, Throwable cause) {
            result += id + "EC ";
            nextFilter.exceptionCaught( session, cause );
        }

        public void dataRead(NextFilter nextFilter, IoSession session, ByteBuffer buf) {
            result += id + "DR ";
            nextFilter.dataRead( session, buf );
        }

        public void dataWritten(NextFilter nextFilter, IoSession session, Object marker) {
            result += id + "DW ";
            nextFilter.dataWritten( session, marker );
        }

        public void filterWrite(NextFilter nextFilter, IoSession session, ByteBuffer buf, Object marker) {
            result += id + "FW ";
            nextFilter.filterWrite( session, buf, marker );
        }
    }

    private static class IoFilterChainImpl extends AbstractIoFilterChain
    {
        protected IoFilterChainImpl()
        {
        }

        protected void doWrite(IoSession session, ByteBuffer buffer, Object marker)
        {
            dataWritten( session, marker );
        }
    }
    
}
