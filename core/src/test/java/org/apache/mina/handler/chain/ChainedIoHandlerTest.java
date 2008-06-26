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

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.mina.core.session.DummySession;
import org.apache.mina.core.session.IoSession;

/**
 * A test case for {@link ChainedIoHandler}.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class ChainedIoHandlerTest extends TestCase {
    public static void main(String[] args) {
        junit.textui.TestRunner.run(ChainedIoHandlerTest.class);
    }

    public void testChainedCommand() throws Exception {
        IoHandlerChain chain = new IoHandlerChain();
        StringBuffer buf = new StringBuffer();
        chain.addLast("A", new TestCommand(buf, 'A'));
        chain.addLast("B", new TestCommand(buf, 'B'));
        chain.addLast("C", new TestCommand(buf, 'C'));

        new ChainedIoHandler(chain).messageReceived(new DummySession(), null);

        Assert.assertEquals("ABC", buf.toString());
    }

    private class TestCommand implements IoHandlerCommand {
        private final StringBuffer buf;

        private final char ch;

        private TestCommand(StringBuffer buf, char ch) {
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
