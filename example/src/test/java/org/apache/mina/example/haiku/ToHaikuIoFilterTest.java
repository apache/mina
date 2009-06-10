/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.mina.example.haiku;

import java.util.Collections;
import java.util.List;

import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.session.IoSession;
import org.jmock.Mock;
import org.jmock.MockObjectTestCase;

/**
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class ToHaikuIoFilterTest extends MockObjectTestCase {
    private IoFilter filter;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        filter = new ToHaikuIoFilter();
    }

    public void testThreeStringsMakesAHaiku() throws Exception {
        Mock list = mock(List.class);
        list.expects(once()).method("add").with(eq("two")).will(
                returnValue(true));
        list.expects(once()).method("add").with(eq("three")).will(
                returnValue(true));
        list.expects(once()).method("toArray").with(isA(String[].class)).will(
                returnValue(new String[] { "one", "two", "three" }));
        list.expects(exactly(2)).method("size").will(
                onConsecutiveCalls(returnValue(2), returnValue(3)));

        Mock session = mock(IoSession.class);
        session.expects(exactly(3)).method("getAttribute").with(eq("phrases"))
                .will(
                        onConsecutiveCalls(returnValue(null), returnValue(list
                                .proxy()), returnValue(list.proxy()),
                                returnValue(list.proxy())));
        session.expects(exactly(1)).method("setAttribute").with(eq("phrases"),
                eq(Collections.emptyList()));
        session.expects(exactly(1)).method("removeAttribute").with(
                eq("phrases"));

        IoSession sessionProxy = (IoSession) session.proxy();

        Mock nextFilter = mock(IoFilter.NextFilter.class);
        nextFilter.expects(once()).method("messageReceived").with(
                eq(sessionProxy), eq(new Haiku("one", "two", "three")));

        IoFilter.NextFilter nextFilterProxy = (IoFilter.NextFilter) nextFilter
                .proxy();

        filter.messageReceived(nextFilterProxy, sessionProxy, "one");
        filter.messageReceived(nextFilterProxy, sessionProxy, "two");
        filter.messageReceived(nextFilterProxy, sessionProxy, "three");
    }

}
