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
package org.apache.mina.common;

import java.util.Iterator;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.mina.common.IoFilterChain.Entry;

/**
 * Tests {@link DefaultIoFilterChainBuilder}.
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class DefaultIoFilterChainBuilderTest extends TestCase {
    public static void main(String[] args) {
        junit.textui.TestRunner.run(DefaultIoFilterChainBuilderTest.class);
    }

    protected void setUp() throws Exception {
    }

    protected void tearDown() throws Exception {
    }

    public void testAdd() throws Exception {
        DefaultIoFilterChainBuilder builder = new DefaultIoFilterChainBuilder();

        builder.addFirst("A", new IoFilterAdapter());
        builder.addLast("B", new IoFilterAdapter());
        builder.addFirst("C", new IoFilterAdapter());
        builder.addLast("D", new IoFilterAdapter());
        builder.addBefore("B", "E", new IoFilterAdapter());
        builder.addBefore("C", "F", new IoFilterAdapter());
        builder.addAfter("B", "G", new IoFilterAdapter());
        builder.addAfter("D", "H", new IoFilterAdapter());

        String actual = "";
        for (Iterator i = builder.getAll().iterator(); i.hasNext();) {
            Entry e = (Entry) i.next();
            actual += e.getName();
        }

        Assert.assertEquals("FCAEBGDH", actual);
    }

    public void testGet() throws Exception {
        DefaultIoFilterChainBuilder builder = new DefaultIoFilterChainBuilder();

        IoFilter filterA = new IoFilterAdapter();
        IoFilter filterB = new IoFilterAdapter();
        IoFilter filterC = new IoFilterAdapter();
        IoFilter filterD = new IoFilterAdapter();

        builder.addFirst("A", filterA);
        builder.addLast("B", filterB);
        builder.addBefore("B", "C", filterC);
        builder.addAfter("A", "D", filterD);

        Assert.assertSame(filterA, builder.get("A"));
        Assert.assertSame(filterB, builder.get("B"));
        Assert.assertSame(filterC, builder.get("C"));
        Assert.assertSame(filterD, builder.get("D"));
    }

    public void testRemove() throws Exception {
        DefaultIoFilterChainBuilder builder = new DefaultIoFilterChainBuilder();

        builder.addLast("A", new IoFilterAdapter());
        builder.addLast("B", new IoFilterAdapter());
        builder.addLast("C", new IoFilterAdapter());
        builder.addLast("D", new IoFilterAdapter());
        builder.addLast("E", new IoFilterAdapter());

        builder.remove("A");
        builder.remove("E");
        builder.remove("C");
        builder.remove("B");
        builder.remove("D");

        Assert.assertEquals(0, builder.getAll().size());
    }

    public void testClear() throws Exception {
        DefaultIoFilterChainBuilder builder = new DefaultIoFilterChainBuilder();

        builder.addLast("A", new IoFilterAdapter());
        builder.addLast("B", new IoFilterAdapter());
        builder.addLast("C", new IoFilterAdapter());
        builder.addLast("D", new IoFilterAdapter());
        builder.addLast("E", new IoFilterAdapter());

        builder.clear();

        Assert.assertEquals(0, builder.getAll().size());
    }

    public void testToString() {
        DefaultIoFilterChainBuilder builder = new DefaultIoFilterChainBuilder();

        // When the chain is empty
        Assert.assertEquals("{ empty }", builder.toString());

        // When there's one filter
        builder.addLast("A", new IoFilterAdapter() {
            public String toString() {
                return "B";
            }
        });
        Assert.assertEquals("{ (A:B) }", builder.toString());

        // When there are two
        builder.addLast("C", new IoFilterAdapter() {
            public String toString() {
                return "D";
            }
        });
        Assert.assertEquals("{ (A:B), (C:D) }", builder.toString());
    }
}
