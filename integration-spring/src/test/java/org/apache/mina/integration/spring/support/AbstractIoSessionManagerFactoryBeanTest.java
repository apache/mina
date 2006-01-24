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

import junit.framework.TestCase;

import org.apache.mina.common.DefaultIoFilterChainBuilder;
import org.apache.mina.common.IoFilter;
import org.apache.mina.common.IoSessionManager;
import org.apache.mina.integration.spring.IoFilterMapping;
import org.easymock.MockControl;
import org.easymock.classextension.MockClassControl;

/**
 * Tests
 * {@link org.apache.mina.integration.spring.support.AbstractIoSessionManagerFactoryBean}.
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class AbstractIoSessionManagerFactoryBeanTest extends TestCase
{
    AbstractIoSessionManagerFactoryBean factory;

    protected void setUp() throws Exception
    {
        /*
         * Create the object under test.
         */
        factory = new AbstractIoSessionManagerFactoryBean()
        {
            public Class getObjectType()
            {
                // Don't care
                return null;
            }

            protected Object createInstance() throws Exception
            {
                // Don't care
                return null;
            }
        };
    }

    /**
     * Tests that
     * {@link AbstractIoSessionManagerFactoryBean#initIoSessionManager(IoSessionManager)}
     * initializes the filter chain in the expected order and using auto
     * generated names.
     */
    public void testInitNoExceptionMonitorUnamedFilters() throws Exception
    {
        /*
         * Create EasyMock mocks.
         */
        MockControl mockIoFilterChainBuilder = MockClassControl
                .createStrictControl( DefaultIoFilterChainBuilder.class );
        MockControl mockIoSessionManager = MockControl
                .createControl( IoSessionManager.class );

        IoFilter[] filters = new IoFilter[] {
                ( IoFilter ) MockControl.createControl( IoFilter.class )
                        .getMock(),
                ( IoFilter ) MockControl.createControl( IoFilter.class )
                        .getMock(),
                ( IoFilter ) MockControl.createControl( IoFilter.class )
                        .getMock() };

        IoSessionManager ioSessionManager = 
            ( IoSessionManager ) mockIoSessionManager.getMock();
        DefaultIoFilterChainBuilder ioFilterChainBuilder = 
            ( DefaultIoFilterChainBuilder ) mockIoFilterChainBuilder.getMock();

        /*
         * Record expectations.
         */
        ioFilterChainBuilder.addLast( "managerFilter0", filters[ 0 ] );
        ioFilterChainBuilder.addLast( "managerFilter1", filters[ 1 ] );
        ioFilterChainBuilder.addLast( "managerFilter2", filters[ 2 ] );

        ioSessionManager.getFilterChain();
        mockIoSessionManager.setReturnValue( mockIoFilterChainBuilder.getMock() );

        /*
         * Replay.
         */
        mockIoFilterChainBuilder.replay();
        mockIoSessionManager.replay();

        factory.setFilters( filters );
        factory.initIoSessionManager( ioSessionManager );

        /*
         * Verify.
         */
        mockIoFilterChainBuilder.verify();
        mockIoSessionManager.verify();
    }

    /**
     * Tests that
     * {@link AbstractIoSessionManagerFactoryBean#initIoSessionManager(IoSessionManager)}
     * initializes the filter chain in the expected order and using the
     * specified names.
     */
    public void testInitNoExceptionMonitorNamedFilters() throws Exception
    {
        /*
         * Create EasyMock mocks.
         */
        MockControl mockIoFilterChainBuilder = MockClassControl
                .createStrictControl( DefaultIoFilterChainBuilder.class );
        MockControl mockIoSessionManager = MockControl
                .createControl( IoSessionManager.class );

        IoFilterMapping[] mappings = new IoFilterMapping[] {
                new IoFilterMapping( "first", ( IoFilter ) MockControl
                        .createControl( IoFilter.class ).getMock() ),
                new IoFilterMapping( "second", ( IoFilter ) MockControl
                        .createControl( IoFilter.class ).getMock() ),
                new IoFilterMapping( "third", ( IoFilter ) MockControl
                        .createControl( IoFilter.class ).getMock() ) };

        IoSessionManager ioSessionManager = ( IoSessionManager ) mockIoSessionManager
                .getMock();
        DefaultIoFilterChainBuilder ioFilterChainBuilder = ( DefaultIoFilterChainBuilder ) mockIoFilterChainBuilder
                .getMock();

        /*
         * Record expectations.
         */
        ioFilterChainBuilder.addLast( "first", mappings[ 0 ].getFilter() );
        ioFilterChainBuilder.addLast( "second", mappings[ 1 ].getFilter() );
        ioFilterChainBuilder.addLast( "third", mappings[ 2 ].getFilter() );

        ioSessionManager.getFilterChain();
        mockIoSessionManager.setReturnValue( mockIoFilterChainBuilder.getMock() );

        /*
         * Replay.
         */
        mockIoFilterChainBuilder.replay();
        mockIoSessionManager.replay();

        factory.setFilterMappings( mappings );
        factory.initIoSessionManager( ioSessionManager );

        /*
         * Verify.
         */
        mockIoFilterChainBuilder.verify();
        mockIoSessionManager.verify();
    }

    /**
     * Tests that
     * {@link AbstractIoSessionManagerFactoryBean#initIoSessionManager(IoSessionManager)}
     * sets the configured ExceptionManager on the IoSessionManager.
     */
    public void testInitWithExceptionMonitor() throws Exception
    {
        /*
         * Create EasyMock mocks.
         */
        MockControl mockIoSessionManager = MockControl
                .createControl( IoSessionManager.class );

        IoSessionManager ioSessionManager = ( IoSessionManager ) mockIoSessionManager
                .getMock();
        DefaultIoFilterChainBuilder ioFilterChainBuilder = ( DefaultIoFilterChainBuilder ) MockClassControl
                .createControl( DefaultIoFilterChainBuilder.class ).getMock();

        /*
         * Record expectations.
         */
        ioSessionManager.getFilterChain();
        mockIoSessionManager.setReturnValue( ioFilterChainBuilder );

        /*
         * Replay.
         */
        mockIoSessionManager.replay();

        factory.initIoSessionManager( ioSessionManager );

        /*
         * Verify.
         */
        mockIoSessionManager.verify();
    }

    /**
     * Tests that
     * {@link AbstractIoSessionManagerFactoryBean#destroyIoSessionManager(IoSessionManager)}
     * clears the filter chain of the IoSessionManager.
     */
    public void testDestroyIoSessionManager() throws Exception
    {
        /*
         * Create EasyMock mocks.
         */
        MockControl mockIoFilterChainBuilder = MockClassControl
                .createStrictControl( DefaultIoFilterChainBuilder.class );
        MockControl mockIoSessionManager = MockControl
                .createControl( IoSessionManager.class );

        IoSessionManager ioSessionManager = ( IoSessionManager ) mockIoSessionManager
                .getMock();
        DefaultIoFilterChainBuilder ioFilterChainBuilder = ( DefaultIoFilterChainBuilder ) mockIoFilterChainBuilder
                .getMock();

        /*
         * Record expectations.
         */
        ioSessionManager.getFilterChain();
        mockIoSessionManager.setReturnValue( ioFilterChainBuilder );
        ioFilterChainBuilder.clear();

        /*
         * Replay.
         */
        mockIoFilterChainBuilder.replay();
        mockIoSessionManager.replay();

        factory.destroyIoSessionManager( ioSessionManager );

        /*
         * Verify.
         */
        mockIoFilterChainBuilder.verify();
        mockIoSessionManager.verify();
    }

    /**
     * Tests that
     * {@link AbstractIoSessionManagerFactoryBean#setFilters(IoFilter[])}
     * validates the method arguments.
     */
    public void testSetIoFilters()
    {
        try
        {
            factory.setFilters( null );
            fail( "null filters array set. IllegalArgumentException expected." );
        }
        catch( IllegalArgumentException iae )
        {
        }
    }

    /**
     * Tests that
     * {@link AbstractIoSessionManagerFactoryBean#setFilterMappings(IoFilterMapping[])}
     * validates the method arguments.
     */
    public void testSetIoFilterMappings()
    {
        try
        {
            factory.setFilterMappings( null );
            fail( "null filter mappings array set. IllegalArgumentException expected." );
        }
        catch( IllegalArgumentException iae )
        {
        }
    }
}
