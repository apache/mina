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

import java.net.SocketAddress;

import org.apache.mina.common.DefaultIoFilterChainBuilder;
import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.IoFilterChainBuilder;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoService;
import org.apache.mina.integration.spring.Binding;
import org.apache.mina.integration.spring.IoFilterMapping;
import org.springframework.util.Assert;

/**
 * Abstract Spring FactoryBean which creates {@link IoAcceptor} instances and 
 * enables their bindings and filters to be configured using Spring.
 * <p>
 * NOTE: Do NOT call {@link IoAcceptor#bind(SocketAddress, IoHandler)} on the
 * created {@link IoAcceptor}. If you do add additional bindings that way
 * {@link #destroyInstance(Object)} will not be able to unbind all bindings of
 * the {@link IoAcceptor} and it will not be fully shut down when the Spring 
 * BeanFactory is closed.
 * </p>
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public abstract class AbstractIoAcceptorFactoryBean extends
        AbstractIoServiceFactoryBean
{

    protected Binding[] bindings = new Binding[ 0 ];
    protected boolean disconnectClientsOnUnbind = true;

    /**
     * Creates the {@link IoAcceptor} configured by this factory bean.
     * 
     * @return the {@link IoAcceptor}.
     * @throws Exception on errors.
     */
    protected abstract IoAcceptor createIoAcceptor() throws Exception;

    /**
     * Creates a new {@link IoAcceptor}. Calls {@link #createIoAcceptor()} to 
     * get the new {@link IoAcceptor} instance and then calls
     * {@link AbstractIoServiceFactoryBean#initIoService(IoService)}
     * followed by {@link #initIoAcceptor(IoAcceptor)}.
     * 
     * @return the {@link IoAcceptor} instance.
     */
    protected Object createInstance() throws Exception
    {
        IoAcceptor acceptor = createIoAcceptor();

        acceptor.setDisconnectClientsOnUnbind( disconnectClientsOnUnbind );
        
        initIoService( acceptor );
        initIoAcceptor( acceptor );

        return acceptor;
    }

    /**
     * Initializes an {@link IoAcceptor} configured by this factory bean by
     * calling {@link IoAcceptor#bind(SocketAddress, IoHandler, IoFilterChainBuilder)}
     * for all bindings configured using {@link #setBindings(Binding[])}.
     * 
     * @param acceptor the {@link IoAcceptor}.
     * @throws Exception on errors.
     */
    protected void initIoAcceptor( IoAcceptor acceptor ) throws Exception
    {

        /*
         * Bind all.
         */
        for( int i = 0; i < bindings.length; i++ )
        {
            Binding b = bindings[ i ];
            DefaultIoFilterChainBuilder chainBuilder = 
                new DefaultIoFilterChainBuilder();
            
            IoFilterMapping[] fm = b.getFilterMappings();
            for( int j = 0; j < fm.length; j++ )
            {
                chainBuilder.addLast( fm[ j ].getName(), fm[ j ].getFilter() );
            }
            
            SocketAddress address = parseSocketAddress( b.getAddress() );
            acceptor.bind( address, b.getHandler(), chainBuilder );
        }
    }

    /**
     * Destroys an {@link IoAcceptor} created by the factory bean by calling
     * {@link #destroyIoAcceptor(IoAcceptor)} and then
     * {@link AbstractIoServiceFactoryBean#destroyIoService(IoService)}.
     * This method may be overridden by extending classes if additional calls
     * are necessary to shutdown the {@link IoAcceptor} or if the sequence of 
     * calls should be different.
     * <p>
     * This method will be called by Spring when the BeanFactory creating this
     * instance is closed. Spring will NOT call this method if this factory bean
     * has been configured for non-singleton use.
     * </p>
     * 
     * @param instance the {@link IoAcceptor} instance to be destroyed.
     */
    protected void destroyInstance( Object instance ) throws Exception
    {
        IoAcceptor acceptor = ( IoAcceptor ) instance;
        destroyIoAcceptor( acceptor );
        destroyIoService( acceptor );
    }

    /**
     * Destroys an {@link IoAcceptor} created by the factory bean by unbinding all
     * bindings set through {@link #setBindings(Binding[])}.
     * 
     * @param acceptor the {@link IoAcceptor} instance to be destroyed.
     */
    protected void destroyIoAcceptor( IoAcceptor acceptor ) throws Exception
    {
        /*
         * Unbind all.
         */
        for( int i = 0; i < bindings.length; i++ )
        {
            Binding b = bindings[ i ];
            try
            {
                acceptor.unbind( parseSocketAddress( b.getAddress() ) );
            }
            catch( Exception ignored )
            {
            }
        }
    }

    public Class getObjectType()
    {
        return IoAcceptor.class;
    }

    /**
     * Sets the bindings to be used by the {@link IoAcceptor} created by this 
     * factory bean.
     * 
     * @param bindings the bindings.
     * @throws IllegalArgumentException if the specified value is 
     *         <code>null</code>.
     * @see IoAcceptor#bind(SocketAddress, IoHandler, IoFilterChainBuilder)
     * @see #parseSocketAddress(String)
     * @see Binding
     */
    public void setBindings( Binding[] bindings )
    {
        Assert.notNull( bindings, "Property 'bindings' may not be null" );
        this.bindings = bindings;
    }

    /**
     * Sets the <code>disconnectClientsOnUnbind</code> property of the
     * {@link IoAcceptor} this factory bean will create.
     * 
     * @param disconnectClientsOnUnbind the property value.
     * @see IoAcceptor#setDisconnectClientsOnUnbind(boolean)
     */
    public void setDisconnectClientsOnUnbind( boolean disconnectClientsOnUnbind )
    {
        this.disconnectClientsOnUnbind = disconnectClientsOnUnbind;
    }

    /**
     * Parses the specified string and returns the corresponding 
     * {@link SocketAddress}.
     * 
     * @param s the string to parse.
     * @return the {@link SocketAddress}.
     */
    protected abstract SocketAddress parseSocketAddress( String s );
}
