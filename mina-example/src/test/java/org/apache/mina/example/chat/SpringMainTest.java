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
package org.apache.mina.example.chat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.mina.core.service.IoService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * TODO Add documentation
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class SpringMainTest {

    private ConfigurableApplicationContext appContext;

    @Before
    public void tearDown() throws Exception {
        if (appContext != null) {
            appContext.close();
        }
    }

    @Test
    public void testContext() {
        appContext = SpringMain.getApplicationContext();
        IoService service = (IoService) appContext.getBean("ioAcceptor");
        IoService ioAcceptorWithSSL = (IoService) appContext.getBean("ioAcceptorWithSSL");
        assertTrue(service.isActive());
        assertTrue(ioAcceptorWithSSL.isActive());
        appContext.close();
        assertFalse(service.isActive());
        assertFalse(ioAcceptorWithSSL.isActive());
    }
}
