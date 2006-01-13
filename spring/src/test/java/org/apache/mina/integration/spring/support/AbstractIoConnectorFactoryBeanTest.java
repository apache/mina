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

import junit.framework.TestCase;

import org.apache.mina.common.IoConnector;
import org.apache.mina.common.IoSessionManager;
import org.easymock.MockControl;
import org.easymock.classextension.MockClassControl;

/**
 * Tests
 * {@link org.apache.mina.integration.spring.support.AbstractIoConnectorFactoryBean}.
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class AbstractIoConnectorFactoryBeanTest extends TestCase
{
    MockControl mockFactory;

    AbstractIoConnectorFactoryBean factory;

    MockControl mockIoConnector;

    IoConnector ioConnector;

    protected void setUp() throws Exception
    {
        /*
         * Create the object to be tested. We're using EasyMock to mock some of
         * the methods in the super class since we're already testing those in
         * AbstractIoSessionManagerFactoryBeanTest and we don't want to test
         * them in this test again.
         */
        mockFactory = MockClassControl
                .createControl(
                        TestIoConnectorFactoryBean.class,
                        new Method[] {
                                TestIoConnectorFactoryBean.class
                                        .getDeclaredMethod(
                                                "createIoConnector",
                                                new Class[ 0 ] ),
                                AbstractIoSessionManagerFactoryBean.class
                                        .getDeclaredMethod(
                                                "initIoSessionManager",
                                                new Class[] { IoSessionManager.class } ),
                                AbstractIoSessionManagerFactoryBean.class
                                        .getDeclaredMethod(
                                                "destroyIoSessionManager",
                                                new Class[] { IoSessionManager.class } ) } );

        factory = ( AbstractIoConnectorFactoryBean ) mockFactory.getMock();

        /*
         * Create other EasyMock mocks.
         */
        mockIoConnector = MockControl.createControl( IoConnector.class );
        ioConnector = ( IoConnector ) mockIoConnector.getMock();
    }

    public void testCreateInstance() throws Exception
    {
        /*
         * Record expectations.
         */
        factory.createIoConnector();
        mockFactory.setReturnValue( ioConnector );
        factory.initIoSessionManager( ioConnector );
        ioConnector.setConnectTimeout( 30 );

        /*
         * Replay.
         */
        mockIoConnector.replay();
        mockFactory.replay();

        factory.setConnectTimeout( 30 );
        Object o = factory.createInstance();

        /*
         * Verify.
         */
        mockIoConnector.verify();
        mockFactory.verify();

        assertSame( ioConnector, o );
    }

    public void testDestroyInstance() throws Exception
    {
        /*
         * Record expectations.
         */
        factory.destroyIoSessionManager( ioConnector );

        /*
         * Replay.
         */
        mockFactory.replay();

        factory.destroyInstance( ioConnector );

        /*
         * Verify.
         */
        mockFactory.verify();
    }

    public void testGetObjectType() throws Exception
    {
        AbstractIoConnectorFactoryBean factory = new TestIoConnectorFactoryBean();
        assertSame( IoConnector.class, factory.getObjectType() );
    }

    /*
     * We need a concrete class to test.
     */
    public static class TestIoConnectorFactoryBean extends
            AbstractIoConnectorFactoryBean
    {
        protected IoConnector createIoConnector()
        {
            // Don't care. This method will be mocked.
            return null;
        }
    }
}
