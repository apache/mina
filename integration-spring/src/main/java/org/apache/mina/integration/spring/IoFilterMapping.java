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

import org.apache.mina.common.IoFilter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * Associates a name with an {@link IoFilter}. This makes it possible to configure
 * named filters using Spring.
 * <p> 
 * Use this class when you want to configure the
 * filters added to the filter chain of all sessions created from a particular
 * {@link org.apache.mina.common.IoService} created using one of the
 * {@link org.apache.mina.integration.spring.IoAcceptorFactoryBean}
 * sub-classes but you don't want the names to be generated automatically.
 * </p>
 * <p>
 * This class can also be used when creating {@link Binding} objects. This lets
 * one configure per-port filters. These filters will only be added to the
 * filter chain of sessions for incoming connections on the port specified by
 * the {@link Binding}. Note that {@link Binding} can also be configured to 
 * generate filter names automatically. In that case you add the {@link IoFilter}
 * instances directly to the {@link Binding}.
 * </p>
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 * 
 * @see org.apache.mina.integration.spring.IoAcceptorFactoryBean
 * @see org.apache.mina.integration.spring.Binding
 */
public class IoFilterMapping implements InitializingBean {
    private String name = null;

    private IoFilter filter = null;

    /**
     * Creates a new empty instance.
     */
    public IoFilterMapping() {
    }

    /**
     * Creates a new instance using the specified name and filter.
     * 
     * @param name the name.
     * @param filter the filter.
     * @throws IllegalArgumentException if any of the arguments are 
     *         <code>null</code>.
     */
    public IoFilterMapping(String name, IoFilter filter) {
        Assert.notNull(name, "Argument 'name' may not be null");
        Assert.notNull(filter, "Argument 'filter' may not be null");

        this.name = name;
        this.filter = filter;
    }

    /**
     * Returns the filter of this mapping.
     * 
     * @return the filter.
     */
    public IoFilter getFilter() {
        return filter;
    }

    /**
     * Returns the name associated with the filter.
     * 
     * @return the name.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the filter of this mapping.
     * 
     * @param filter the filter.
     * @throws IllegalArgumentException if the specified value is 
     *         <code>null</code>.
     */
    public void setFilter(IoFilter filter) {
        Assert.notNull(filter, "Argument 'filter' may not be null");
        this.filter = filter;
    }

    /**
     * Sets the name associated with the filter.
     * 
     * @param name the name.
     * @throws IllegalArgumentException if the specified value is 
     *         <code>null</code>.
     */
    public void setName(String name) {
        Assert.notNull(name, "Argument 'name' may not be null");
        this.name = name;
    }

    public void afterPropertiesSet() throws Exception {
        Assert.notNull(name, "Argument 'name' may not be null");
        Assert.notNull(filter, "Argument 'filter' may not be null");
    }
}
