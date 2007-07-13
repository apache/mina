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

import junit.framework.TestCase;

import org.apache.mina.common.ExecutorThreadModel;

import java.util.concurrent.Executor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Tests {@link ExecutorThreadModelFactoryBean}.
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class ExecutorThreadModelFactoryBeanTest extends TestCase {
    public void testSuccessfulCreationWithExecutor() throws Exception {
        Executor executor = new ThreadPoolExecutor(1, 10, 3600,
                TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
        ExecutorThreadModelFactoryBean factory = new ExecutorThreadModelFactoryBean();
        factory.setServiceName("foo");
        factory.setExecutor(executor);
        factory.afterPropertiesSet();
        ExecutorThreadModel threadModel = (ExecutorThreadModel) factory
                .getObject();
        assertSame(executor, threadModel.getExecutor());
    }

    public void testSuccessfulCreationWithoutExecutor() throws Exception {
        ExecutorThreadModelFactoryBean factory = new ExecutorThreadModelFactoryBean();
        factory.setServiceName("foo");
        factory.afterPropertiesSet();
        ExecutorThreadModel threadModel = (ExecutorThreadModel) factory
                .getObject();
        assertTrue(threadModel.getExecutor() instanceof ThreadPoolExecutor);
    }

    public void testUnsuccessfulCreation() throws Exception {
        ExecutorThreadModelFactoryBean factory = new ExecutorThreadModelFactoryBean();
        try {
            factory.afterPropertiesSet();
            fail("No serviceName set. IllegalArgumentException expected.");
        } catch (IllegalArgumentException iae) {
        }
    }
}
