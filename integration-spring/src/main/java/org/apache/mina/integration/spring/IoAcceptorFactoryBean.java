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
 *   &lt;!-- POP3 server IoHandler implementation--&gt;
 *   &lt;bean id=&quot;pop3Handler&quot; class=&quot;com.foo.bar.MyPop3Handler&quot;&gt;
 *       ...
 *   &lt;!-- IMAP server IoHandler implementation --&gt;
 *   &lt;bean id=&quot;imapHandler&quot; class=&quot;com.foo.bar.MyImapHandler&quot;&gt;
 *       ...
 *   &lt;/bean&gt;
 *   &lt;!-- Telnet based admin console handler. Should only be 
 *        accessible from localhost --&gt;
 *   &lt;bean id=&quot;adminHandler&quot; class=&quot;com.foo.bar.MyAdminHandler&quot;&gt;
 *       ...
 *   &lt;/bean&gt;
 *   
 *   &lt;!-- Create a thread pool filter --&gt;
 *   &lt;bean id=&quot;threadPoolFilter&quot; 
 *         class=&quot;org.apache.mina.filter.ThreadPoolFilter&quot;&gt;
 *     &lt;!-- Threads will be named IoWorker-1, IoWorker-2, etc --&gt;
 *     &lt;constructor-arg value=&quot;IoWorker&quot;/&gt;
 *     &lt;property name=&quot;maximumPoolSize&quot; value=&quot;10&quot;/&gt;
 *   &lt;/bean&gt;
 *   
 *   &lt;!-- Create an SSL filter to be used for POP3 over SSL --&gt;
 *   &lt;bean id=&quot;sslFilter&quot; class=&quot;org.apache.mina.filter.SSLFilter&quot;&gt;
 *         ...
 *   &lt;/bean&gt;
 *   
 *   &lt;!-- Create the SocketAcceptor --&gt;
 *   &lt;bean id=&quot;socketAcceptor&quot; 
 *        class=&quot;org.apache.mina.integration.spring.SocketAcceptorFactoryBean&quot;&gt;
 *     &lt;property name=&quot;filters&quot;&gt;
 *       &lt;list&gt;
 *         &lt;ref local=&quot;threadPoolFilter&quot;/&gt;
 *       &lt;/list&gt;
 *     &lt;/property&gt;
 *     &lt;property name=&quot;bindings&quot;&gt;
 *       &lt;list&gt;
 *         &lt;bean class=&quot;org.apache.mina.integration.spring.Binding&quot;&gt;
 *           &lt;property name=&quot;address&quot; value=&quot;:110&quot;/&gt;
 *           &lt;property name=&quot;handler&quot; ref=&quot;pop3Handler&quot;/&gt;
 *         &lt;/bean&gt;
 *         &lt;bean class=&quot;org.apache.mina.integration.spring.Binding&quot;&gt;
 *           &lt;property name=&quot;address&quot; value=&quot;:995&quot;/&gt;
 *           &lt;property name=&quot;handler&quot; ref=&quot;pop3Handler&quot;/&gt;
 *           &lt;property name=&quot;filters&quot;&gt;
 *             &lt;list&gt;
 *               &lt;ref local=&quot;sslFilter&quot;/&gt;
 *             &lt;/list&gt;
 *           &lt;/property&gt;
 *         &lt;/bean&gt;
 *         &lt;bean class=&quot;org.apache.mina.integration.spring.Binding&quot;&gt;
 *           &lt;property name=&quot;address&quot; value=&quot;:143&quot;/&gt;
 *           &lt;property name=&quot;handler&quot; ref=&quot;imapHandler&quot;/&gt;
 *         &lt;/bean&gt;
 *         &lt;bean class=&quot;org.apache.mina.integration.spring.Binding&quot;&gt;
 *           &lt;property name=&quot;address&quot; value=&quot;127.0.0.1:60987&quot;/&gt;
 *           &lt;property name=&quot;handler&quot; ref=&quot;adminHandler&quot;/&gt;
 *         &lt;/bean&gt;
 *       &lt;/list&gt;
 *     &lt;/property&gt;
 *   &lt;/bean&gt;
 * </pre>
 * 
 * </p>
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class IoAcceptorFactoryBean implements FactoryBean, InitializingBean, DisposableBean
{
    private Binding[] bindings = new Binding[ 0 ];
    private IoAcceptor target;

    /**
     * Sets the {@link IoAcceptor} to be configured using this factory bean.
     * 
     * @param target the target {@link IoAcceptor}.
     */
    public void setTarget( IoAcceptor target )
    {
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
    public void setBindings( Binding[] bindings )
    {
        Assert.notNull( bindings, "Property 'bindings' may not be null" );
        this.bindings = bindings;
    }
    
    public Object getObject() throws Exception
    {
        return target;
    }

    public Class getObjectType()
    {
        return IoAcceptor.class;
    }

    public boolean isSingleton()
    {
        return true;
    }

    public void afterPropertiesSet() throws Exception
    {
        Assert.notNull( target, "Property 'target' may not be null" );
        
        /*
         * Bind all.
         */
        for( int i = 0; i < bindings.length; i++ )
        {
            Binding b = bindings[ i ];
            if( b.getServiceConfig() != null )
            {
                target.bind( b.getAddress(), b.getHandler(), b.getServiceConfig() );
            }
            else
            {
                target.bind( b.getAddress(), b.getHandler() );
            }
        }
    }

    public void destroy() throws Exception
    {
        for( int i = 0; i < bindings.length; i++ )
        {
            Binding b = bindings[ i ];
            try
            {
                target.unbind( b.getAddress() );
            }
            catch( Exception ignored )
            {
            }
        }        
    }
}
