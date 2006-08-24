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

import org.apache.mina.common.DefaultIoFilterChainBuilder;
import org.apache.mina.common.IoFilter;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.util.Assert;

/**
 * Spring {@link org.springframework.beans.factory.FactoryBean}
 * which creates {@link DefaultIoFilterChainBuilder} instances. This
 * factory bean makes it possible to configure the filters to be added to all the
 * sessions created by an {@link org.apache.mina.common.IoAcceptor} 
 * or {@link org.apache.mina.common.IoConnector} using Spring.
 * <p>
 * The filters may be set up in two ways. By creating
 * {@link IoFilterMapping} objects which associate a name with an {@link IoFilter}
 * instance and set them using {@link #setFilterMappings(IoFilterMapping[])} or
 * by using {@link #setFilters(IoFilter[])} directly which assigns automatically
 * generated names to each {@link IoFilter}. Use the 
 * {@link #setFilterNamePrefix(String)} method to set the prefix used for
 * auto generated names.
 * </p>
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class DefaultIoFilterChainBuilderFactoryBean extends AbstractFactoryBean
{
    private IoFilterMapping[] filterMappings = new IoFilterMapping[ 0 ];
    private String prefix = "filter";

    protected Object createInstance() throws Exception
    {
        DefaultIoFilterChainBuilder builder = new DefaultIoFilterChainBuilder();
        for( int i = 0; i < filterMappings.length; i++ )
        {
            String name = filterMappings[ i ].getName();
            if( name == null )
            {
                name = prefix + i;
            }
            builder.addLast( name, filterMappings[ i ].getFilter() );
        }
        
        return builder;
    }

    public Class getObjectType()
    {
        return DefaultIoFilterChainBuilder.class;
    }

    /**
     * Sets the prefix used to create the names for automatically named filters
     * added using {@link #setFilters(IoFilter[])}. The default prefix is 
     * <tt>filter</tt>.
     * 
     * @param prefix the prefix.
     * @throws IllegalArgumentException if the specified value is 
     *         <code>null</code>.
     */
    public void setFilterNamePrefix( String prefix )
    {
        Assert.notNull( prefix, "Property 'filterNamePrefix' may not be null" );
        this.prefix = prefix;
    }

    /**
     * Sets a number of unnamed filters which will be added to the filter
     * chain created by this factory bean. The filters will be assigned 
     * automatically generated names (<code>&lt;filterNamePrefix&gt;0</code>, 
     * <code>&lt;filterNamePrefix&gt;1</code>, etc).
     * 
     * @param filters the filters.
     * @throws IllegalArgumentException if the specified value is 
     *         <code>null</code>.
     * @see #setFilterNamePrefix(String)
     */
    public void setFilters( IoFilter[] filters )
    {
        Assert.notNull( filters, "Property 'filters' may not be null" );
        this.filterMappings = new IoFilterMapping[ filters.length ];

        for( int i = 0; i < filters.length; i++ )
        {
            this.filterMappings[ i ] = new IoFilterMapping();
            this.filterMappings[ i ].setFilter( filters[ i ] );
        }
    }

    /**
     * Sets a number of named filters which will be added to the filter
     * chain created by this factory bean. {@link IoFilterMapping} objects
     * set using this method which haven't had their name set will be assigned
     * automatically generated names derived from the prefix set using
     * {@link #setFilterNamePrefix(String)} and the position in the specified
     * array (i.e. <code>&lt;filterNamePrefix&gt;&lt;pos&gt;</code>).
     * 
     * @param filterMappings the name to filter mappings.
     * @throws IllegalArgumentException if the specified value is 
     *         <code>null</code>.
     * @see #setFilterNamePrefix(String)
     */
    public void setFilterMappings( IoFilterMapping[] filterMappings )
    {
        Assert.notNull( filterMappings, "Property 'filterMappings' may not be null" );
        this.filterMappings = filterMappings;
    }    
}
