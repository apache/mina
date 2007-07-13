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

import org.apache.mina.common.ExecutorThreadModel;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import java.util.concurrent.Executor;

/**
 * Spring {@link FactoryBean} which makes it possible to set up a MINA
 * {@link ExecutorThreadModel} using Spring. The <code>serviceName</code>
 * property must be set using {@link #setServiceName(String)}.
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class ExecutorThreadModelFactoryBean implements FactoryBean,
        InitializingBean {
    private String serviceName = null;

    private Executor executor = null;

    /**
     * Sets the {@link Executor} to use. If not set a default {@link Executor}
     * will be used by the {@link ExecutorThreadModel} created by this
     * factory bean.
     *
     * @param executor the executor.
     * @throws IllegalArgumentException if the specified value is
     *         <code>null</code>.
     */
    public void setExecutor(Executor executor) {
        Assert.notNull(executor, "Property 'executor' may not be null");
        this.executor = executor;
    }

    /**
     * Sets the name of the service as used in the call to
     * {@link ExecutorThreadModel#getInstance(String)}. This property is
     * required.
     *
     * @param executor the executor.
     * @throws IllegalArgumentException if the specified value is
     *         <code>null</code>.
     */
    public void setServiceName(String serviceName) {
        Assert.notNull(serviceName, "Property 'serviceName' may not be null");
        this.serviceName = serviceName;
    }

    public Class getObjectType() {
        return ExecutorThreadModel.class;
    }

    public Object getObject() throws Exception {
        ExecutorThreadModel model = ExecutorThreadModel
                .getInstance(serviceName);
        if (executor != null) {
            model.setExecutor(executor);
        }
        return model;
    }

    public boolean isSingleton() {
        return true;
    }

    public void afterPropertiesSet() throws Exception {
        Assert.notNull(serviceName, "Property 'serviceName' may not be null");
    }

}
