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

import org.apache.mina.common.IoFilter;
import org.apache.mina.common.IoHandler;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * Defines an address to {@link org.apache.mina.common.IoHandler} binding.
 * This is used when specifying the addresses to accept new connections on when
 * creating {@link org.apache.mina.common.IoAcceptor} objects using one of the
 * {@link org.apache.mina.integration.spring.support.AbstractIoAcceptorFactoryBean}
 * sub-classes.
 * <p>
 * This class also allows for an optional list of filters to be associated with
 * the address. The {@link org.apache.mina.common.IoAcceptor} will add
 * all these filters to the filter chain of sessions created for incoming
 * connections on the address specified by this binding. This makes it possible
 * to specify different filters depending on the port the client is connecting
 * on (e.g. using an {@link org.apache.mina.filter.SSLFilter} when connecting
 * on port 443 but not on port 80). 
 * </p>
 *
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class Binding implements InitializingBean
{
    private String address = null;
    private IoHandler handler = null;
    private IoFilterMapping[] filterMappings = new IoFilterMapping[ 0 ];

    /**
     * Creates a new empty instance.
     */
    public Binding()
    {
    }

    /**
     * Creates a new instance using the specified values.
     * 
     * @param address the address. See {@link #setAddress(String)} for 
     *        information on the format.
     * @param handler the handler.
     * @throws IllegalArgumentException if the any of the specified values are 
     *         <code>null</code>.
     */
    public Binding( String address, IoHandler handler )
    {
        setAddress( address );
        setHandler( handler );
    }
    
    /**
     * Creates a new instance using the specified values.
     * 
     * @param address the address. See {@link #setAddress(String)} for 
     *        information on the format.
     * @param handler the handler.
     * @param filterMappings the filter mappigns.
     * @throws IllegalArgumentException if the any of the specified values are 
     *         <code>null</code>.
     */
    public Binding( String address, IoHandler handler, 
                    IoFilterMapping[] filterMappings )
    {
        setAddress( address );
        setHandler( handler );
        setFilterMappings( filterMappings );
    }

    /**
     * Creates a new instance using the specified values.
     * 
     * @param address the address. See {@link #setAddress(String)} for 
     *        information on the format.
     * @param handler the handler.
     * @param filters the filters.
     * @throws IllegalArgumentException if the any of the specified values are 
     *         <code>null</code>.
     */
    public Binding( String address, IoHandler handler, 
                    IoFilter[] filters )
    {
        setAddress( address );
        setHandler( handler );
        setFilters( filters );
    }
    
    /**
     * Returns the address the handler of this object will be bound to.
     * See {@link #setAddress(String)} for more information on the format of this
     * string.
     *  
     * @return the textual representation of the transport type specific address.
     */
    public String getAddress()
    {
        return address;
    }

    /**
     * Sets the address the handler of this object will be bound to.
     * The format of this address depends on the transport type of the
     * {@link org.apache.mina.common.IoAcceptor} this binding will be used for.
     * When creating a {@link org.apache.mina.transport.socket.nio.SocketAcceptor}
     * using {@link SocketAcceptorFactoryBean} the format looks like
     * <code>[&lt;interface&gt;:]port</code>, e.g. <code>127.0.0.1:8080</code>.
     * {@link #getAddress()}
     * 
     * @param address the textual representation of the transport type specific 
     *        address.
     * @throws IllegalArgumentException if the specified value is 
     *         <code>null</code>.
     */
    public void setAddress( String address )
    {
        Assert.notNull( address, "Property 'address' may not be null" );
        this.address = address;
    }

    /**
     * Returns the handler of this binding object.
     * 
     * @return the handler.
     */
    public IoHandler getHandler()
    {
        return handler;
    }

    /**
     * Sets the handler of this binding object.
     *
     * @param handler the handler.
     * @throws IllegalArgumentException if the specified value is 
     *         <code>null</code>.
     */
    public void setHandler( IoHandler handler )
    {
        Assert.notNull( handler, "Property 'handler' may not be null" );
        this.handler = handler;
    }

    /**
     * Sets a number of unnamed filters. These will be added to the filter chain
     * of sessions created when a connection is made on the address specified by
     * this binding. The filters will be assigned automatically generated names 
     * (<code>portFilter0</code>, <code>portFilter1</code>, etc).
     * 
     * @param filters the filters.
     * @throws IllegalArgumentException if the specified value is 
     *         <code>null</code>.
     */
    public void setFilters( IoFilter[] filters )
    {
        Assert.notNull( filters, "Property 'filters' may not be null" );
        this.filterMappings = new IoFilterMapping[ filters.length ];

        for( int i = 0; i < filters.length; i++ )
        {
            this.filterMappings[ i ] = new IoFilterMapping();
            this.filterMappings[ i ].setName( "portFilter" + i );
            this.filterMappings[ i ].setFilter( filters[ i ] );
        }
    }

    /**
     * Sets a number of named filters. These will be added to the filter chain
     * of sessions created when a connection is made on the address specified by
     * this binding.
     * 
     * @param filterMappings the name to filter mappings.
     * @throws IllegalArgumentException if the specified value is 
     *         <code>null</code>.
     */
    public void setFilterMappings( IoFilterMapping[] filterMappings )
    {
        Assert.notNull( filterMappings, "Property 'filterMappings' may not be null" );
        this.filterMappings = filterMappings;
    }    
    
    /**
     * Returns the array of {@link IoFilterMapping} objects configured for this
     * binding.
     * 
     * @return the mappings.
     */
    public IoFilterMapping[] getFilterMappings()
    {
        return filterMappings;
    }

    public void afterPropertiesSet() throws Exception
    {
        Assert.notNull( address, "Property 'address' may not be null" );
        Assert.notNull( handler, "Property 'handler' may not be null" );
    }

}
