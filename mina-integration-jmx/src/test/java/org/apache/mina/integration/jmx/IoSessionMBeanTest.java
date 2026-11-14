/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.mina.integration.jmx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.apache.mina.core.session.DummySession;
import org.apache.mina.core.session.IoSession;
import org.junit.Test;

/**
 * Tests that the {@link IoSessionMBean} <code>attributes</code> MBean attribute does not
 * expose session attribute values (per-peer security state) to read-only JMX principals
 * unless the embedder explicitly opts in.
 */
public class IoSessionMBeanTest {
    @Test
    public void testAttributeValuesAreRedactedByDefault() throws Exception {
        IoSession session = new DummySession();
        session.setAttribute("secretToken", "hunter2");
        IoSessionMBean mbean = new IoSessionMBean(session);

        Map<?, ?> attributes = (Map<?, ?>) mbean.getAttribute0("attributes");

        assertTrue(attributes.containsKey("secretToken"));
        assertEquals(IoSessionMBean.REDACTED_VALUE, attributes.get("secretToken"));
        assertFalse(attributes.containsValue("hunter2"));
    }

    @Test
    public void testAttributeValuesCanBeReEnabled() throws Exception {
        System.setProperty(IoSessionMBean.EXPOSE_ATTRIBUTE_VALUES_PROPERTY, "true");

        try {
            IoSession session = new DummySession();
            session.setAttribute("secretToken", "hunter2");
            IoSessionMBean mbean = new IoSessionMBean(session);

            Map<?, ?> attributes = (Map<?, ?>) mbean.getAttribute0("attributes");

            assertEquals("hunter2", attributes.get("secretToken"));
        } finally {
            System.clearProperty(IoSessionMBean.EXPOSE_ATTRIBUTE_VALUES_PROPERTY);
        }
    }
}
