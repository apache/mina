package org.apache.mina.protocol;

import java.net.SocketAddress;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.SessionConfig;
import org.apache.mina.common.TransportType;

public class ProtocolHandlerFilterChainTest extends TestCase
{
    private ProtocolHandlerFilterChainImpl chain;
    private ProtocolSession session;
    private String result;

    public void setUp()
    {
        chain = new ProtocolHandlerFilterChainImpl();
        session = new TestSession();
        result = "";
    }
    
    public void tearDown()
    {
    }
    
    public void testDefault()
    {
        run( "HSO HMR HMS HSI HEC HSC" );
    }
    
    public void testChained()
    {
        chain.addLast( "A", new TestFilter( 'A' ) );
        chain.addLast( "B", new TestFilter( 'B' ) );
        run( "ASO BSO HSO" +
             "AMR BMR HMR" +
             "BFW AFW AMS BMS HMS" +
             "ASI BSI HSI" +
             "AEC BEC HEC" +
             "ASC BSC HSC" );
    }
    
    private void run( String expectedResult )
    {
        chain.sessionOpened( session );
        chain.messageReceived( session, new Object() );
        chain.filterWrite( session, new Object() );
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

    private class TestSession implements ProtocolSession
    {
        private ProtocolHandler handler = new ProtocolHandler()
        {
            public void sessionOpened(ProtocolSession session) {
                result += "HSO";
            }

            public void sessionClosed(ProtocolSession session) {
                result += "HSC";
            }

            public void sessionIdle(ProtocolSession session, IdleStatus status) {
                result += "HSI";
            }

            public void exceptionCaught(ProtocolSession session, Throwable cause) {
                result += "HEC";
                if( cause.getClass() != Exception.class )
                {
                    cause.printStackTrace( System.out );
                }
            }

            public void messageReceived(ProtocolSession session, Object message) {
                result += "HMR";
            }

            public void messageSent(ProtocolSession session, Object message) {
                result += "HMS";
            }
        };

        public ProtocolHandler getHandler() {
            return handler;
        }

        public ProtocolEncoder getEncoder() {
            return null;
        }

        public ProtocolDecoder getDecoder() {
            return null;
        }

        public void close() {
        }

        public Object getAttachment() {
            return null;
        }

        public void setAttachment(Object attachment) {
        }

        public void write(Object message) {
        }

        public TransportType getTransportType() {
            return null;
        }

        public boolean isConnected() {
            return false;
        }

        public SessionConfig getConfig() {
            return null;
        }

        public SocketAddress getRemoteAddress() {
            return null;
        }

        public SocketAddress getLocalAddress() {
            return null;
        }

        public long getReadBytes() {
            return 0;
        }

        public long getWrittenBytes() {
            return 0;
        }

        public long getLastIoTime() {
            return 0;
        }

        public long getLastReadTime() {
            return 0;
        }

        public long getLastWriteTime() {
            return 0;
        }

        public boolean isIdle(IdleStatus status) {
            return false;
        }
    }

    private class TestFilter implements ProtocolHandlerFilter
    {
        private final char id;

        private TestFilter( char id )
        {
            this.id = id;
        }
        
        public void sessionOpened(NextFilter nextFilter, ProtocolSession session) {
            result += id + "SO";
            nextFilter.sessionOpened( session );
        }

        public void sessionClosed(NextFilter nextFilter, ProtocolSession session) {
            result += id + "SC";
            nextFilter.sessionClosed( session );
        }

        public void sessionIdle(NextFilter nextFilter, ProtocolSession session, IdleStatus status) {
            result += id + "SI";
            nextFilter.sessionIdle( session, status );
        }

        public void exceptionCaught(NextFilter nextFilter, ProtocolSession session, Throwable cause) {
            result += id + "EC";
            nextFilter.exceptionCaught( session, cause );
        }

        public void filterWrite(NextFilter nextFilter, ProtocolSession session, Object message) {
            result += id + "FW";
            nextFilter.filterWrite( session, message );
        }

        public void messageReceived(NextFilter nextFilter, org.apache.mina.protocol.ProtocolSession session, Object message) {
            result += id + "MR";
            nextFilter.messageReceived( session, message );
        }

        public void messageSent(NextFilter nextFilter, org.apache.mina.protocol.ProtocolSession session, Object message) {
            result += id + "MS";
            nextFilter.messageSent( session, message );
        }
    }

    private static class ProtocolHandlerFilterChainImpl extends AbstractProtocolHandlerFilterChain
    {
        protected ProtocolHandlerFilterChainImpl()
        {
        }

        protected void doWrite( ProtocolSession session, Object message )
        {
            messageSent( session, message );
        }
    }
    
}
