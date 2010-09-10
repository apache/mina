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
package org.apache.mina.core;

import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.filterchain.IoFilterChain.Entry;
import org.apache.mina.filter.util.NoopFilter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/**
 * Tests {@link DefaultIoFilterChainBuilder}.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class DefaultIoFilterChainBuilderTest {
    @Before
    public void setUp() throws Exception {
        // Do nothing
    }

    @After
    public void tearDown() throws Exception {
        // Do nothing
    }

    @Test
    public void testAdd() throws Exception {
        DefaultIoFilterChainBuilder builder = new DefaultIoFilterChainBuilder();

        builder.addFirst("A", new NoopFilter());
        builder.addLast("B", new NoopFilter());
        builder.addFirst("C", new NoopFilter());
        builder.addLast("D", new NoopFilter());
        builder.addBefore("B", "E", new NoopFilter());
        builder.addBefore("C", "F", new NoopFilter());
        builder.addAfter("B", "G", new NoopFilter());
        builder.addAfter("D", "H", new NoopFilter());

        String actual = "";
        for (Entry e : builder.getAll()) {
            actual += e.getName();
        }

        assertEquals("FCAEBGDH", actual);
    }

    @Test
    public void testGet() throws Exception {
        DefaultIoFilterChainBuilder builder = new DefaultIoFilterChainBuilder();

        IoFilter filterA = new NoopFilter();
        IoFilter filterB = new NoopFilter();
        IoFilter filterC = new NoopFilter();
        IoFilter filterD = new NoopFilter();

        builder.addFirst("A", filterA);
        builder.addLast("B", filterB);
        builder.addBefore("B", "C", filterC);
        builder.addAfter("A", "D", filterD);

        assertSame(filterA, builder.get("A"));
        assertSame(filterB, builder.get("B"));
        assertSame(filterC, builder.get("C"));
        assertSame(filterD, builder.get("D"));
    }

    @Test
    public void testRemove() throws Exception {
        DefaultIoFilterChainBuilder builder = new DefaultIoFilterChainBuilder();

        builder.addLast("A", new NoopFilter());
        builder.addLast("B", new NoopFilter());
        builder.addLast("C", new NoopFilter());
        builder.addLast("D", new NoopFilter());
        builder.addLast("E", new NoopFilter());

        builder.remove("A");
        builder.remove("E");
        builder.remove("C");
        builder.remove("B");
        builder.remove("D");

        assertEquals(0, builder.getAll().size());
    }

    @Test
    public void testClear() throws Exception {
        DefaultIoFilterChainBuilder builder = new DefaultIoFilterChainBuilder();

        builder.addLast("A", new NoopFilter());
        builder.addLast("B", new NoopFilter());
        builder.addLast("C", new NoopFilter());
        builder.addLast("D", new NoopFilter());
        builder.addLast("E", new NoopFilter());

        builder.clear();

        assertEquals(0, builder.getAll().size());
    }

    @Test
    public void testToString() {
        DefaultIoFilterChainBuilder builder = new DefaultIoFilterChainBuilder();

        // When the chain is empty
        assertEquals("{ empty }", builder.toString());

        // When there's one filter
        builder.addLast("A", new IoFilterAdapter() {
            @Override
            public String toString() {
                return "B";
            }
        });
        assertEquals("{ (A:B) }", builder.toString());

        // When there are two
        builder.addLast("C", new IoFilterAdapter() {
            @Override
            public String toString() {
                return "D";
            }
        });
        assertEquals("{ (A:B), (C:D) }", builder.toString());
    }
}
