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

import org.apache.mina.common.DefaultIoFilterChainBuilder;
import org.apache.mina.common.IoFilter;
import org.apache.mina.common.IoService;
import org.apache.mina.integration.spring.IoFilterMapping;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.util.Assert;

/**
 * Abstract Spring {@link org.springframework.beans.factory.FactoryBean}
 * which creates {@link org.apache.mina.common.IoService} instances. This
 * factory bean makes it possible to configure the filters to be added to all the
 * sessions created by the {@link org.apache.mina.common.IoService} using
 * Spring.
 * <p>
 * The filters may be set up in two ways. By creating
 * {@link IoFilterMapping} objects which associate a name with an {@link IoFilter}
 * instance and set them using {@link #setFilterMappings(IoFilterMapping[])} or
 * by using {@link #setFilters(IoFilter[])} directly which assigns automatically
 * generated names to each {@link IoFilter}.
 * </p>
 * <p>
 * NOTE: Instances of this class should NOT be configured as non-singletons.
 * This will prevent Spring from calling the <code>destroyInstance()</code>
 * method on BeanFactory shut down.
 * </p>
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public abstract class AbstractIoServiceFactoryBean extends
        AbstractFactoryBean
{
    private IoFilterMapping[] filterMappings = new IoFilterMapping[ 0 ];

    /**
     * Initializes an {@link IoService} configured by this factory bean.
     * 
     * @param sessionManager the {@link IoService}.
     * @throws Exception on errors.
     */
    protected void initIoService( IoService sessionManager )
            throws Exception
    {
        /*
         * Add filters to the end of the filter chain.
         */
        DefaultIoFilterChainBuilder builder = sessionManager.getFilterChain();
        for( int i = 0; i < filterMappings.length; i++ )
        {
            builder.addLast( filterMappings[ i ].getName(),
                             filterMappings[ i ].getFilter() );
        }
    }

    /**
     * Destroys an {@link IoService} created by the factory bean.
     * 
     * @param sessionManager the IoService instance to be destroyed.
     */
    protected void destroyIoService( IoService sessionManager )
            throws Exception
    {

        /*
         * Remove all filters.
         */
        sessionManager.getFilterChain().clear();
    }

    /**
     * Sets a number of unnamed filters which will be added to the filter
     * chain of all sessions created by the {@link IoService} created by 
     * this factory bean. The filters will be assigned automatically generated 
     * names (<code>managerFilter0</code>, <code>managerFilter1</code>, etc).
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
            this.filterMappings[ i ].setName( "managerFilter" + i );
            this.filterMappings[ i ].setFilter( filters[ i ] );
        }
    }

    /**
     * Sets a number of named filters which will be added to the filter
     * chain of all sessions created by the {@link IoService} created by 
     * this factory bean. 
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
}
