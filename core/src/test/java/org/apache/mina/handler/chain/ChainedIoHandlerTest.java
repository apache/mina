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
package org.apache.mina.handler.chain;

import org.apache.mina.core.session.DummySession;
import org.apache.mina.core.session.IoSession;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * A test case for {@link ChainedIoHandler}.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class ChainedIoHandlerTest {
    @Test
    public void testChainedCommand() throws Exception {
        IoHandlerChain chain = new IoHandlerChain();
        StringBuilder buf = new StringBuilder();
        chain.addLast("A", new TestCommand(buf, 'A'));
        chain.addLast("B", new TestCommand(buf, 'B'));
        chain.addLast("C", new TestCommand(buf, 'C'));

        new ChainedIoHandler(chain).messageReceived(new DummySession(), null);

        assertEquals("ABC", buf.toString());
    }

    private class TestCommand implements IoHandlerCommand {
        private final StringBuilder buf;

        private final char ch;

        public TestCommand(StringBuilder buf, char ch) {
            this.buf = buf;
            this.ch = ch;
        }

        public void execute(NextCommand next, IoSession session, Object message)
                throws Exception {
            buf.append(ch);
            next.execute(session, message);
        }
    }
}
