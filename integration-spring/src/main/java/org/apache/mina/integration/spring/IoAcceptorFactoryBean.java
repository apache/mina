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

import java.net.SocketAddress;

import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoServiceConfig;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * Spring {@link FactoryBean} which enables the bindings of an {@link IoAcceptor}
 * to be configured using Spring. Example of usage:
 * <p>
 * 
 * <pre>
 *   &lt;!-- This makes it possible to specify java.net.SocketAddress values 
 *        (e.g. :80 below) as Strings.
 *        They will be converted into java.net.InetSocketAddress objects by Spring.  --&gt;
 *   &lt;bean class="org.springframework.beans.factory.config.CustomEditorConfigurer"&gt;
 *     &lt;property name="customEditors"&gt;
 *       &lt;map&gt;
 *         &lt;entry key="java.net.SocketAddress"&gt;
 *           &lt;bean class="org.apache.mina.integration.spring.InetSocketAddressEditor"/&gt;
 *         &lt;/entry&gt;
 *       &lt;/map&gt;
 *     &lt;/property&gt;
 *   &lt;/bean&gt;
 * 
 *   &lt;!-- The IoHandler implementation --&gt;
 *   &lt;bean id="httpHandler" class="com.example.MyHttpHandler"&gt;
 *     ...
 *   &lt;/bean&gt;
 *     
 *   &lt;bean id="filterChainBuilder" 
 *         class="org.apache.mina.integration.spring.DefaultIoFilterChainBuilderFactoryBean"&gt;
 *     &lt;property name="filters"&gt;
 *       &lt;list&gt;
 *         &lt;bean class="org.apache.mina.filter.LoggingFilter"/&gt;
 *       &lt;/list&gt;
 *     &lt;/property&gt;
 *   &lt;/bean&gt;
 *
 *  &lt;!-- By default MINA uses an ExecutorThreadModel. This demonstrates how to 
 *          use your own with some non default settings. The threadModel will 
 *          be set on the SocketAcceptorConfig defined below. To configure a 
 *          ExecutorFilter directly you will have to use the ThreadModel.MANUAL 
 *          ThreadModel instead. --&gt;
 *   &lt;bean id="threadModel" class="org.apache.mina.integration.spring.ExecutorThreadModelFactoryBean"&gt;
 *     &lt;property name="serviceName" value="HttpService"/&gt;
 *     &lt;property name="executor"&gt;
 *       &lt;bean class="org.apache.mina.integration.spring.ThreadPoolExecutorFactoryBean"&gt;
 *         &lt;property name="corePoolSize" value="2"/&gt;
 *         &lt;property name="maxPoolSize" value="10"/&gt;
 *         &lt;property name="keepAliveSeconds" value="30"/&gt;
 *       &lt;/bean&gt;
 *     &lt;/property&gt;
 *   &lt;/bean&gt;
 *
 *   &lt;bean id="ioAcceptor" class="org.apache.mina.integration.spring.IoAcceptorFactoryBean"&gt;
 *     &lt;property name="target"&gt;
 *       &lt;bean class="org.apache.mina.transport.socket.nio.SocketAcceptor"/&gt;
 *     &lt;/property&gt;
 *     &lt;property name="bindings"&gt;
 *       &lt;list&gt;
 *         &lt;bean class="org.apache.mina.integration.spring.Binding"&gt;
 *           &lt;property name="address" value=":80"/&gt;
 *           &lt;property name="handler" ref="httpHandler"/&gt;
 *           &lt;property name="serviceConfig"&gt;
 *             &lt;bean class="org.apache.mina.transport.socket.nio.SocketAcceptorConfig"&gt;
 *               &lt;property name="filterChainBuilder" ref="filterChainBuilder"/&gt;
 *               &lt;property name="reuseAddress" value="true"/&gt;
 *               &lt;property name="threadModel" ref="threadModel"/&gt; 
 *             &lt;/bean&gt;
 *           &lt;/property&gt;
 *         &lt;/bean&gt;
 *       &lt;/list&gt;
 *     &lt;/property&gt;
 *   &lt;/bean&gt;
 * </pre>
 * 
 * </p>
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class IoAcceptorFactoryBean implements FactoryBean, InitializingBean,
        DisposableBean {
    private Binding[] bindings = new Binding[0];

    private IoAcceptor target;

    /**
     * Sets the {@link IoAcceptor} to be configured using this factory bean.
     * 
     * @param target the target {@link IoAcceptor}.
     */
    public void setTarget(IoAcceptor target) {
        this.target = target;
    }

    /**
     * Sets the bindings to be used by the {@link IoAcceptor} configured by this 
     * factory bean.
     * 
     * @param bindings the bindings.
     * @throws IllegalArgumentException if the specified value is 
     *         <code>null</code>.
     * @see IoAcceptor#bind(SocketAddress, IoHandler)
     * @see IoAcceptor#bind(SocketAddress, IoHandler, IoServiceConfig)
     * @see Binding
     */
    public void setBindings(Binding[] bindings) {
        Assert.notNull(bindings, "Property 'bindings' may not be null");
        this.bindings = bindings;
    }

    public Object getObject() throws Exception {
        return target;
    }

    public Class getObjectType() {
        return IoAcceptor.class;
    }

    public boolean isSingleton() {
        return true;
    }

    public void afterPropertiesSet() throws Exception {
        Assert.notNull(target, "Property 'target' may not be null");

        /*
         * Bind all.
         */
        for (int i = 0; i < bindings.length; i++) {
            Binding b = bindings[i];
            if (b.getServiceConfig() != null) {
                target.bind(b.getAddress(), b.getHandler(), b
                        .getServiceConfig());
            } else {
                target.bind(b.getAddress(), b.getHandler());
            }
        }
    }

    public void destroy() throws Exception {
        for (int i = 0; i < bindings.length; i++) {
            Binding b = bindings[i];
            try {
                target.unbind(b.getAddress());
            } catch (Exception ignored) {
            }
        }
    }
}
