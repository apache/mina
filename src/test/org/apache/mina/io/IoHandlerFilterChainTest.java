package org.apache.mina.io;

import java.net.SocketAddress;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.SessionConfig;
import org.apache.mina.common.TransportType;

public class IoHandlerFilterChainTest extends TestCase
{
    private IoHandlerFilterChain chain;
    private IoSession session;
    private String result;

    public void setUp()
    {
        chain = new IoHandlerFilterChainImpl( true );
        session = new TestSession();
        result = "";
    }
    
    public void tearDown()
    {
    }
    
    public void testDefault()
    {
        run( "HSO HDR HDW HSI HEC HSC" );
    }
    
    public void testSimpleChain()
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
    
    public void testNestedChain()
    {
        IoHandlerFilterChainImpl childChain = new IoHandlerFilterChainImpl( false );

        chain.addLast( "A", new TestFilter( 'A' ) );
        chain.addLast( "child", childChain );
        chain.addLast( "B", new TestFilter( 'B' ) );
        childChain.addFirst( "C", new TestFilter( 'C' ) );
        childChain.addLast( "D", new TestFilter( 'D' ) );
        
        run( "ASO CSO BSO HSO DSO" +
             "ADR CDR BDR HDR DDR" +
             "BFW DFW AFW ADW CDW BDW HDW DDW CFW" +
             "ASI CSI BSI HSI DSI" +
             "AEC CEC BEC HEC DEC" +
             "ASC CSC BSC HSC DSC" );
    }
    
    private void run( String expectedResult )
    {
        chain.sessionOpened( null, session );
        chain.dataRead( null, session, ByteBuffer.allocate( 16 ) );
        chain.filterWrite( null, session, ByteBuffer.allocate( 16 ), null );
        chain.sessionIdle( null, session, IdleStatus.READER_IDLE );
        chain.exceptionCaught( null, session, new Exception() );
        chain.sessionClosed( null, session );
        
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

    private class TestSession implements IoSession
    {
        private IoHandler handler = new IoHandler()
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

        public void close()
        {
        }

        public void write(ByteBuffer buf, Object marker)
        {
        }

        public Object getAttachment()
        {
            return null;
        }

        public void setAttachment(Object attachment)
        {
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

        public long getReadBytes()
        {
            return 0;
        }

        public long getWrittenBytes()
        {
            return 0;
        }

        public long getLastIoTime()
        {
            return 0;
        }

        public long getLastReadTime()
        {
            return 0;
        }

        public long getLastWriteTime()
        {
            return 0;
        }

        public boolean isIdle(IdleStatus status)
        {
            return false;
        }
    }

    private class TestFilter implements IoHandlerFilter
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

    private static class IoHandlerFilterChainImpl extends AbstractIoHandlerFilterChain
    {
        protected IoHandlerFilterChainImpl(boolean root) {
            super( root );
        }

        protected void doWrite(IoSession session, ByteBuffer buffer, Object marker)
        {
            getRoot().dataWritten( null, session, marker );
        }
    }
    
}
