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

import java.util.Arrays;
import java.util.LinkedList;

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
public class DefaultIoFilterChainBuilderFactoryBeanTest extends TestCase {
    MockControl mockChain;

    IoFilterChain chain;

    IoFilter[] filters;

    protected void setUp() throws Exception {
        super.setUp();

        mockChain = MockControl.createControl(IoFilterChain.class);
        chain = (IoFilterChain) mockChain.getMock();
        filters = new IoFilter[] {
                (IoFilter) MockControl.createControl(IoFilter.class).getMock(),
                (IoFilter) MockControl.createControl(IoFilter.class).getMock(),
                (IoFilter) MockControl.createControl(IoFilter.class).getMock() };
    }

    public void testUnnamedFilters() throws Exception {
        chain.addLast("prefix0", filters[0]);
        chain.addLast("prefix1", filters[1]);
        chain.addLast("prefix2", filters[2]);

        mockChain.replay();

        DefaultIoFilterChainBuilderFactoryBean factory = new DefaultIoFilterChainBuilderFactoryBean();
        factory.setFilters(Arrays.asList(filters));
        factory.setFilterNamePrefix("prefix");
        DefaultIoFilterChainBuilder builder = (DefaultIoFilterChainBuilder) factory
                .createInstance();
        builder.buildFilterChain(chain);

        mockChain.verify();
    }

    @SuppressWarnings("unchecked")
    public void testIllegalObjectsInFilterList() throws Exception {
        LinkedList mappings = new LinkedList();
        mappings.add(new IoFilterMapping("f0", filters[0]));
        mappings.add(new Object());
        DefaultIoFilterChainBuilderFactoryBean factory = new DefaultIoFilterChainBuilderFactoryBean();
        try {
            factory.setFilters(mappings);
            fail("Illegal object in list of filters. IllegalArgumentException expected.");
        } catch (IllegalArgumentException iae) {
        }
    }

    @SuppressWarnings("unchecked")
    public void testNamedAndUnnamedFilters() throws Exception {
        LinkedList mappings = new LinkedList();
        mappings.add(new IoFilterMapping("f0", filters[0]));
        mappings.add(filters[1]);
        mappings.add(new IoFilterMapping("f2", filters[2]));

        chain.addLast("f0", filters[0]);
        chain.addLast("filter1", filters[1]);
        chain.addLast("f2", filters[2]);

        mockChain.replay();

        DefaultIoFilterChainBuilderFactoryBean factory = new DefaultIoFilterChainBuilderFactoryBean();
        factory.setFilters(mappings);
        DefaultIoFilterChainBuilder builder = (DefaultIoFilterChainBuilder) factory
                .createInstance();
        builder.buildFilterChain(chain);

        mockChain.verify();
    }
}
