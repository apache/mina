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
package org.apache.mina.statemachine.context;

import static org.junit.Assert.assertSame;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

/**
 * Tests {@link AbstractStateContextLookup}.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class AbstractStateContextLookupTest {
    @Test
    public void testLookup() throws Exception {
        Map<String, StateContext> map = new HashMap<String, StateContext>();
        AbstractStateContextLookup lookup = new AbstractStateContextLookup(
                                             new DefaultStateContextFactory()) {
            protected boolean supports(Class<?> c) {
                return Map.class.isAssignableFrom(c);
            }
            @SuppressWarnings("unchecked")
            protected StateContext lookup(Object eventArg) {
                Map<String, StateContext> map = (Map<String, StateContext>) eventArg;
                return map.get("context");
            }
            @SuppressWarnings("unchecked")
            protected void store(Object eventArg, StateContext context) {
                Map<String, StateContext> map = (Map<String, StateContext>) eventArg;
                map.put("context", context);
            }
        };
        Object[] args1 = new Object[] {new Object(), map, new Object()};
        Object[] args2 = new Object[] {map, new Object()};
        StateContext sc = lookup.lookup(args1);
        assertSame(map.get("context"), sc);
        assertSame(map.get("context"), lookup.lookup(args1));
        assertSame(map.get("context"), lookup.lookup(args2));
    }
    
}
