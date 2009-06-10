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

import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IoSession;
import org.jmock.Mock;
import org.jmock.MockObjectTestCase;

/**
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class HaikuValidatorIoHandlerTest extends MockObjectTestCase {
    private IoHandler handler;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        handler = new HaikuValidatorIoHandler();
    }

    public void testValidHaiku() throws Exception {
        Mock session = mock(IoSession.class);
        session.expects(once()).method("write").with(eq("HAIKU!"));
        IoSession sessionProxy = (IoSession) session.proxy();

        handler.messageReceived(sessionProxy, new Haiku(
                "Oh, I drank too much.", "Why, oh why did I sign up",
                "For an eight thirty?"));
    }

    public void testInvalidHaiku() throws Exception {
        Mock session = mock(IoSession.class);
        session.expects(once()).method("write").with(
                eq("NOT A HAIKU: phrase 1, 'foo' had 1 syllables, not 5"));
        IoSession sessionProxy = (IoSession) session.proxy();

        handler.messageReceived(sessionProxy,
                new Haiku("foo", "a haiku", "poo"));
    }
}
