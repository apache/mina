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
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class IoAcceptorFactoryBeanTest extends TestCase {
    public void testBindUnbind() throws Exception {
        IoHandler handler1 = new IoHandlerAdapter();
        IoHandler handler2 = new IoHandlerAdapter();
        IoHandler handler3 = new IoHandlerAdapter();
        IoServiceConfig config1 = (IoServiceConfig) MockControl.createControl(
                IoServiceConfig.class).getMock();
        IoServiceConfig config2 = (IoServiceConfig) MockControl.createControl(
                IoServiceConfig.class).getMock();
        MockControl mockIoAcceptor = MockControl
                .createControl(IoAcceptor.class);
        IoAcceptor acceptor = (IoAcceptor) mockIoAcceptor.getMock();

        acceptor.bind(new InetSocketAddress(80), handler1, config1);
        acceptor.bind(new InetSocketAddress("192.168.0.1", 22), handler2,
                config2);
        acceptor.bind(new InetSocketAddress("10.0.0.1", 9876), handler3);
        acceptor.unbind(new InetSocketAddress(80));
        acceptor.unbind(new InetSocketAddress("192.168.0.1", 22));
        acceptor.unbind(new InetSocketAddress("10.0.0.1", 9876));

        mockIoAcceptor.replay();

        IoAcceptorFactoryBean factory = new IoAcceptorFactoryBean();
        factory.setTarget(acceptor);
        factory
                .setBindings(new Binding[] {
                        new Binding(new InetSocketAddress(80), handler1,
                                config1),
                        new Binding(new InetSocketAddress("192.168.0.1", 22),
                                handler2, config2),
                        new Binding(new InetSocketAddress("10.0.0.1", 9876),
                                handler3) });
        factory.afterPropertiesSet();
        factory.destroy();

        mockIoAcceptor.verify();
    }

    public void testIsSingleton() throws Exception {
        assertTrue(new IoAcceptorFactoryBean().isSingleton());
    }

    public void testGetObjectType() throws Exception {
        assertEquals(IoAcceptor.class, new IoAcceptorFactoryBean()
                .getObjectType());
    }

    public void testGetObject() throws Exception {
        IoAcceptor acceptor = (IoAcceptor) MockControl.createControl(
                IoAcceptor.class).getMock();
        IoAcceptorFactoryBean factory = new IoAcceptorFactoryBean();
        factory.setTarget(acceptor);

        assertEquals(acceptor, factory.getObject());
    }

}
