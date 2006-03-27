/*
 *   @(#) $Id$
 *
 *   Copyright 2006 The Apache Software Foundation
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

import org.apache.mina.common.DefaultIoFilterChainBuilder;
import org.apache.mina.common.IoFilter;
import org.apache.mina.common.IoFilterChain;
import org.easymock.MockControl;

import junit.framework.TestCase;

/**
 * Tests {@link DefaultIoFilterChainBuilderFactoryBean}.
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class DefaultIoFilterChainBuilderFactoryBeanTest extends TestCase
{
    MockControl mockChain;
    IoFilterChain chain;
    IoFilter[] filters;

    protected void setUp() throws Exception
    {
        super.setUp();
        
        mockChain = MockControl.createControl( IoFilterChain.class );
        chain = ( IoFilterChain ) mockChain.getMock();
        filters = new IoFilter[] {
                ( IoFilter ) MockControl.createControl( IoFilter.class ).getMock(),
                ( IoFilter ) MockControl.createControl( IoFilter.class ).getMock(),
                ( IoFilter ) MockControl.createControl( IoFilter.class ).getMock()
        };        
    }
    
    public void testUnnamedFilters() throws Exception
    {
        chain.addLast( "prefix0", filters[ 0 ] );
        chain.addLast( "prefix1", filters[ 1 ] );
        chain.addLast( "prefix2", filters[ 2 ] );
        
        mockChain.replay();
        
        DefaultIoFilterChainBuilderFactoryBean factory = 
                                   new DefaultIoFilterChainBuilderFactoryBean();
        factory.setFilters( filters );
        factory.setFilterNamePrefix( "prefix" );
        DefaultIoFilterChainBuilder builder = 
                             ( DefaultIoFilterChainBuilder) factory.createInstance();
        builder.buildFilterChain( chain );
        
        mockChain.verify();
    }
    
    public void testnamedAndUnnamedFilters() throws Exception
    {
        IoFilterMapping[] mappings = new IoFilterMapping[] {
                new IoFilterMapping(),
                new IoFilterMapping(),
                new IoFilterMapping()
        };
        mappings[ 0 ].setFilter( filters[ 0 ] );
        mappings[ 0 ].setName( "f0" );
        mappings[ 1 ].setFilter( filters[ 1 ] );
        mappings[ 2 ].setFilter( filters[ 2 ] );
        mappings[ 2 ].setName( "f2" );
        
        chain.addLast( "f0", filters[ 0 ] );
        chain.addLast( "filter1", filters[ 1 ] );
        chain.addLast( "f2", filters[ 2 ] );
        
        mockChain.replay();
        
        DefaultIoFilterChainBuilderFactoryBean factory = 
                                   new DefaultIoFilterChainBuilderFactoryBean();
        factory.setFilterMappings( mappings );
        DefaultIoFilterChainBuilder builder = 
                             ( DefaultIoFilterChainBuilder) factory.createInstance();
        builder.buildFilterChain( chain );
        
        mockChain.verify();
    }    
}
