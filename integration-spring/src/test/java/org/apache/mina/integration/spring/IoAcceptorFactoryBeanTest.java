/*
 * Copyright (c) 2004-2005, Trillian AB. All Rights Reserved.
 * 
 * Redistribution and use in source and binary forms, with or 
 * without modification, are prohibited without specific prior 
 * written permission from Trillian AB (http://www.trillian.se).
 *
 * This notice and attribution to Trillian AB may not be removed.
 * 
 * Created on 2006-feb-12
 */
package org.apache.mina.integration.spring;

import java.net.InetSocketAddress;

import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoServiceConfig;
import org.easymock.MockControl;

import junit.framework.TestCase;

/**
 * Tests {@link IoAcceptorFactoryBean}.
 *
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class IoAcceptorFactoryBeanTest extends TestCase
{
    public void testBindUnbind() throws Exception
    {
        IoHandler handler1 = new IoHandlerAdapter();
        IoHandler handler2 = new IoHandlerAdapter();
        IoHandler handler3 = new IoHandlerAdapter();
        IoServiceConfig config1 = 
            ( IoServiceConfig ) MockControl.createControl( IoServiceConfig.class ).getMock();
        IoServiceConfig config2 = 
            ( IoServiceConfig ) MockControl.createControl( IoServiceConfig.class ).getMock();
        MockControl mockIoAcceptor = MockControl.createControl( IoAcceptor.class );
        IoAcceptor acceptor = ( IoAcceptor ) mockIoAcceptor.getMock();
        
        acceptor.bind( new InetSocketAddress( 80 ), handler1, config1 );
        acceptor.bind( new InetSocketAddress( "192.168.0.1", 22 ), handler2, config2 );
        acceptor.bind( new InetSocketAddress( "10.0.0.1", 9876 ), handler3 );
        acceptor.unbind( new InetSocketAddress( 80 ) );
        acceptor.unbind( new InetSocketAddress( "192.168.0.1", 22 ) );
        acceptor.unbind( new InetSocketAddress( "10.0.0.1", 9876 ) );
        
        mockIoAcceptor.replay();
        
        IoAcceptorFactoryBean factory = new IoAcceptorFactoryBean();
        factory.setTarget( acceptor );
        factory.setBindings( new Binding[] {
                new Binding( new InetSocketAddress( 80 ), handler1, config1 ),
                new Binding( new InetSocketAddress( "192.168.0.1", 22 ), handler2, config2 ),
                new Binding( new InetSocketAddress( "10.0.0.1", 9876 ), handler3 )
        } );
        factory.afterPropertiesSet();
        factory.destroy();
        
        mockIoAcceptor.verify();
    }
    
    public void testIsSingleton() throws Exception
    {
        assertTrue( new IoAcceptorFactoryBean().isSingleton() );
    }
    
    public void testGetObjectType() throws Exception
    {
        assertEquals( IoAcceptor.class, new IoAcceptorFactoryBean().getObjectType() );
    }
    
    public void testGetObject() throws Exception
    {
        IoAcceptor acceptor = 
            ( IoAcceptor ) MockControl.createControl( IoAcceptor.class ).getMock();
        IoAcceptorFactoryBean factory = new IoAcceptorFactoryBean();
        factory.setTarget( acceptor );

        assertEquals( acceptor, factory.getObject() );
    }
    
}
