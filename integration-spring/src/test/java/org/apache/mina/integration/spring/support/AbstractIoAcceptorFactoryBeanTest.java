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
package org.apache.mina.integration.spring.support;

import java.lang.reflect.Method;
import java.net.SocketAddress;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

import org.apache.mina.common.DefaultIoFilterChainBuilder;
import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.IoFilter;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoSessionManager;
import org.apache.mina.common.IoFilterChain.Entry;
import org.apache.mina.integration.spring.Binding;
import org.easymock.AbstractMatcher;
import org.easymock.MockControl;
import org.easymock.classextension.MockClassControl;

/**
 * Tests
 * {@link org.apache.mina.integration.spring.support.AbstractIoAcceptorFactoryBean}.
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class AbstractIoAcceptorFactoryBeanTest extends TestCase
{
    MockControl mockFactory;
    AbstractIoAcceptorFactoryBean factory;
    MockControl mockIoAcceptor;
    IoAcceptor ioAcceptor;
    Binding[] bindings;
    IoHandler popHandler;
    IoHandler sshHandler;
    IoHandler httpHandler;
    IoFilter filter1;
    IoFilter filter2;

    protected void setUp() throws Exception
    {
        /*
         * Create the object to be tested. We're using EasyMock to mock some of
         * the methods in the super class since we're already testing those in
         * AbstractIoSessionManagerFactoryBeanTest and we don't want to test
         * them in this test again.
         */
        mockFactory = MockClassControl
                .createNiceControl(
                        TestIoAcceptorFactoryBean.class,
                        new Class[ 0 ], new Object[ 0 ],
                        new Method[] {
                                TestIoAcceptorFactoryBean.class
                                        .getDeclaredMethod( "createIoAcceptor",
                                                new Class[ 0 ] ),
                                AbstractIoSessionManagerFactoryBean.class
                                        .getDeclaredMethod(
                                                "initIoSessionManager",
                                                new Class[] { IoSessionManager.class } ),
                                AbstractIoSessionManagerFactoryBean.class
                                        .getDeclaredMethod(
                                                "destroyIoSessionManager",
                                                new Class[] { IoSessionManager.class } ) } );

        factory = ( AbstractIoAcceptorFactoryBean ) mockFactory.getMock();

        /*
         * Create other EasyMock mocks.
         */
        mockIoAcceptor = MockControl.createControl( IoAcceptor.class );
        ioAcceptor = ( IoAcceptor ) mockIoAcceptor.getMock();

        /*
         * Create some IoHandlers.
         */
        popHandler = ( IoHandler ) MockControl.createControl( IoHandler.class ).getMock();
        sshHandler = ( IoHandler ) MockControl.createControl( IoHandler.class ).getMock();
        httpHandler = ( IoHandler ) MockControl.createControl( IoHandler.class ).getMock();

        /*
         * Create the filters
         */
        filter1 = ( IoFilter ) MockControl.createControl( IoFilter.class ).getMock();
        filter2 = ( IoFilter ) MockControl.createControl( IoFilter.class ).getMock();
        
        /*
         * Create the bindings to use.
         */
        bindings = new Binding[ 3 ];
        bindings[ 0 ] = new Binding( ":110", popHandler, new IoFilter[] { filter1 } );
        bindings[ 1 ] = new Binding( "127.0.0.1:22", sshHandler, new IoFilter[] { filter1, filter2 } );
        bindings[ 2 ] = new Binding( "192.168.0.1:80", httpHandler );
    }

    public void testCreateInstance() throws Exception
    {
        
        DefaultIoFilterChainBuilder builder1 = new DefaultIoFilterChainBuilder();
        builder1.addLast( "portFilter0", filter1 );
        DefaultIoFilterChainBuilder builder2 = new DefaultIoFilterChainBuilder();
        builder2.addLast( "portFilter0", filter1 );
        builder2.addLast( "portFilter1", filter2 );
        
        /*
         * Record expectations.
         */
        factory.createIoAcceptor();
        mockFactory.setReturnValue( ioAcceptor );
        ioAcceptor.setDisconnectClientsOnUnbind( true );
        factory.initIoSessionManager( ioAcceptor );
        ioAcceptor.bind( new DummySocketAddress( ":110" ), popHandler, builder1 );
        mockIoAcceptor.setMatcher( new IoAcceptorBindArgumentsMatcher() );
        ioAcceptor.bind( new DummySocketAddress( "127.0.0.1:22" ), sshHandler, builder2 );
        ioAcceptor.bind( new DummySocketAddress( "192.168.0.1:80" ),
                httpHandler, new DefaultIoFilterChainBuilder() );

        /*
         * Replay.
         */
        mockIoAcceptor.replay();
        mockFactory.replay();

        factory.setDisconnectClientsOnUnbind( true );
        factory.setBindings( bindings );
        Object o = factory.createInstance();

        /*
         * Verify.
         */
        mockIoAcceptor.verify();
        mockFactory.verify();

        assertSame( ioAcceptor, o );
    }

    public void testDestroyInstance() throws Exception
    {

        /*
         * Record expectations.
         */
        ioAcceptor.unbind( new DummySocketAddress( ":110" ) );
        ioAcceptor.unbind( new DummySocketAddress( "127.0.0.1:22" ) );
        // Make this unbind call throw an exception. The exception should be
        // ignored.
        mockIoAcceptor.setThrowable( new NullPointerException() );
        ioAcceptor.unbind( new DummySocketAddress( "192.168.0.1:80" ) );
        factory.destroyIoSessionManager( ioAcceptor );

        /*
         * Replay.
         */
        mockIoAcceptor.replay();
        mockFactory.replay();

        factory.setBindings( bindings );
        factory.destroyInstance( ioAcceptor );

        /*
         * Verify.
         */
        mockIoAcceptor.verify();
        mockFactory.verify();
    }

    public void testGetObjectType() throws Exception
    {
        AbstractIoAcceptorFactoryBean factory = new TestIoAcceptorFactoryBean();
        assertSame( IoAcceptor.class, factory.getObjectType() );
    }

    /*
     * We need a concrete class to test.
     */
    public static class TestIoAcceptorFactoryBean extends
            AbstractIoAcceptorFactoryBean
    {
        protected IoAcceptor createIoAcceptor() throws Exception
        {
            // Don't care. This method will be mocked.
            return null;
        }

        protected SocketAddress parseSocketAddress( String s )
        {
            return new DummySocketAddress( s );
        }
    }

    public static class DummySocketAddress extends SocketAddress
    {
        private static final long serialVersionUID = -4369202604535464701L;

        private String s = null;

        public DummySocketAddress( String s )
        {
            this.s = s;
        }

        public boolean equals( Object o )
        {
            if( !( o instanceof DummySocketAddress ) )
            {
                return false;
            }
            DummySocketAddress that = ( DummySocketAddress ) o;
            return this.s.equals( that.s );
        }
    }
    
    public static class IoAcceptorBindArgumentsMatcher extends AbstractMatcher
    {
        protected boolean argumentMatches( Object expected, Object actual )
        {
            if( expected instanceof DefaultIoFilterChainBuilder && 
                actual instanceof DefaultIoFilterChainBuilder )
            {
                DefaultIoFilterChainBuilder b1 = ( DefaultIoFilterChainBuilder ) expected;
                DefaultIoFilterChainBuilder b2 = ( DefaultIoFilterChainBuilder ) actual;
                
                List l1 = b1.getAll();
                List l2 = b2.getAll();
                if( l1.size() != l2.size() )
                    return false;
                
                Iterator it1 = l1.iterator();
                Iterator it2 = l2.iterator();
                while( it1.hasNext() && it2.hasNext() )
                {
                    Entry e1 = ( Entry ) it1.next();
                    Entry e2 = ( Entry ) it2.next();
                    
                    if( !e1.getName().equals( e2.getName() ) )
                        return false;                    
                    if( e1.getFilter() != e2.getFilter() )
                        return false;                    
                }
                
                return true;
            }
            return super.argumentMatches( expected, actual );
        }

    }
}
