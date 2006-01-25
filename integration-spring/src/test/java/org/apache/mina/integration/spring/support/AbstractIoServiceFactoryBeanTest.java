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
import org.apache.mina.common.IoService;
import org.apache.mina.integration.spring.IoFilterMapping;
import org.easymock.MockControl;
import org.easymock.classextension.MockClassControl;

/**
 * Tests
 * {@link org.apache.mina.integration.spring.support.AbstractIoServiceFactoryBean}.
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class AbstractIoServiceFactoryBeanTest extends TestCase
{
    AbstractIoServiceFactoryBean factory;

    protected void setUp() throws Exception
    {
        /*
         * Create the object under test.
         */
        factory = new AbstractIoServiceFactoryBean()
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
     * {@link AbstractIoServiceFactoryBean#initIoService(IoService)}
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
        MockControl mockIoService = MockControl
                .createControl( IoService.class );

        IoFilter[] filters = new IoFilter[] {
                ( IoFilter ) MockControl.createControl( IoFilter.class )
                        .getMock(),
                ( IoFilter ) MockControl.createControl( IoFilter.class )
                        .getMock(),
                ( IoFilter ) MockControl.createControl( IoFilter.class )
                        .getMock() };

        IoService ioSessionManager = 
            ( IoService ) mockIoService.getMock();
        DefaultIoFilterChainBuilder ioFilterChainBuilder = 
            ( DefaultIoFilterChainBuilder ) mockIoFilterChainBuilder.getMock();

        /*
         * Record expectations.
         */
        ioFilterChainBuilder.addLast( "managerFilter0", filters[ 0 ] );
        ioFilterChainBuilder.addLast( "managerFilter1", filters[ 1 ] );
        ioFilterChainBuilder.addLast( "managerFilter2", filters[ 2 ] );

        ioSessionManager.getFilterChain();
        mockIoService.setReturnValue( mockIoFilterChainBuilder.getMock() );

        /*
         * Replay.
         */
        mockIoFilterChainBuilder.replay();
        mockIoService.replay();

        factory.setFilters( filters );
        factory.initIoService( ioSessionManager );

        /*
         * Verify.
         */
        mockIoFilterChainBuilder.verify();
        mockIoService.verify();
    }

    /**
     * Tests that
     * {@link AbstractIoServiceFactoryBean#initIoService(IoService)}
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
        MockControl mockIoService = MockControl
                .createControl( IoService.class );

        IoFilterMapping[] mappings = new IoFilterMapping[] {
                new IoFilterMapping( "first", ( IoFilter ) MockControl
                        .createControl( IoFilter.class ).getMock() ),
                new IoFilterMapping( "second", ( IoFilter ) MockControl
                        .createControl( IoFilter.class ).getMock() ),
                new IoFilterMapping( "third", ( IoFilter ) MockControl
                        .createControl( IoFilter.class ).getMock() ) };

        IoService ioSessionManager = ( IoService ) mockIoService
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
        mockIoService.setReturnValue( mockIoFilterChainBuilder.getMock() );

        /*
         * Replay.
         */
        mockIoFilterChainBuilder.replay();
        mockIoService.replay();

        factory.setFilterMappings( mappings );
        factory.initIoService( ioSessionManager );

        /*
         * Verify.
         */
        mockIoFilterChainBuilder.verify();
        mockIoService.verify();
    }

    /**
     * Tests that
     * {@link AbstractIoServiceFactoryBean#initIoService(IoService)}
     * sets the configured ExceptionManager on the IoService.
     */
    public void testInitWithExceptionMonitor() throws Exception
    {
        /*
         * Create EasyMock mocks.
         */
        MockControl mockIoService = MockControl
                .createControl( IoService.class );

        IoService ioSessionManager = ( IoService ) mockIoService
                .getMock();
        DefaultIoFilterChainBuilder ioFilterChainBuilder = ( DefaultIoFilterChainBuilder ) MockClassControl
                .createControl( DefaultIoFilterChainBuilder.class ).getMock();

        /*
         * Record expectations.
         */
        ioSessionManager.getFilterChain();
        mockIoService.setReturnValue( ioFilterChainBuilder );

        /*
         * Replay.
         */
        mockIoService.replay();

        factory.initIoService( ioSessionManager );

        /*
         * Verify.
         */
        mockIoService.verify();
    }

    /**
     * Tests that
     * {@link AbstractIoServiceFactoryBean#destroyIoService(IoService)}
     * clears the filter chain of the IoService.
     */
    public void testDestroyIoService() throws Exception
    {
        /*
         * Create EasyMock mocks.
         */
        MockControl mockIoFilterChainBuilder = MockClassControl
                .createStrictControl( DefaultIoFilterChainBuilder.class );
        MockControl mockIoService = MockControl
                .createControl( IoService.class );

        IoService ioSessionManager = ( IoService ) mockIoService
                .getMock();
        DefaultIoFilterChainBuilder ioFilterChainBuilder = ( DefaultIoFilterChainBuilder ) mockIoFilterChainBuilder
                .getMock();

        /*
         * Record expectations.
         */
        ioSessionManager.getFilterChain();
        mockIoService.setReturnValue( ioFilterChainBuilder );
        ioFilterChainBuilder.clear();

        /*
         * Replay.
         */
        mockIoFilterChainBuilder.replay();
        mockIoService.replay();

        factory.destroyIoService( ioSessionManager );

        /*
         * Verify.
         */
        mockIoFilterChainBuilder.verify();
        mockIoService.verify();
    }

    /**
     * Tests that
     * {@link AbstractIoServiceFactoryBean#setFilters(IoFilter[])}
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
     * {@link AbstractIoServiceFactoryBean#setFilterMappings(IoFilterMapping[])}
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
