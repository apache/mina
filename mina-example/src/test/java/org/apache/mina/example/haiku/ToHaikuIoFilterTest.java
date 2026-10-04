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

import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.session.IoSession;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class ToHaikuIoFilterTest {
    private IoFilter filter;

    @Before
    public void setUp() {
        filter = new ToHaikuIoFilter();
    }

    @Test
    public void testThreeStringsMakesAHaiku() throws Exception {
        IoSession session = mock(IoSession.class);

        List<String> phrases = new ArrayList<>();
        doReturn(phrases).when(session).getAttribute("phrases");

        IoFilter.NextFilter nextFilter = mock(IoFilter.NextFilter.class);

        filter.messageReceived(nextFilter, session, "one");
        filter.messageReceived(nextFilter, session, "two");
        filter.messageReceived(nextFilter, session, "three");

        verify(nextFilter).messageReceived(eq(session), eq(new Haiku("one", "two", "three")));
        verify(session).removeAttribute(eq("phrases"));
    }
}
