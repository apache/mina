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
package org.apache.mina.handler.demux;

import org.apache.mina.core.session.IoSession;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests {@link org.apache.mina.handler.demux.DemuxingIoHandler}.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
@SuppressWarnings("unchecked")
public class DemuxingIoHandlerTest {
    MessageHandler handler1;

    MessageHandler handler2;

    MessageHandler handler3;

    IoSession session;

    Object[] msg;

    @Before
    public void setUp() throws Exception {
        /*
         * Create the messages.
         */
        msg = new Object[9];
        msg[0] = new C1();
        msg[1] = new C2();
        msg[2] = new C3();
        msg[3] = new C1();
        msg[4] = new C2();
        msg[5] = new C3();
        msg[6] = new C1();
        msg[7] = new C2();
        msg[8] = new C3();

        /*
         * Create mocks.
         */
        handler1 = EasyMock.createMock(MessageHandler.class);
        handler2 = EasyMock.createMock(MessageHandler.class);
        handler3 = EasyMock.createMock(MessageHandler.class);

        session = EasyMock.createMock( IoSession.class);
    }

    @Test
    public void testFindHandlerByClass() throws Exception {
        /*
         * Record expectations.
         */
        handler1.handleMessage(session, msg[0]);
        handler1.handleMessage(session, msg[1]);
        handler1.handleMessage(session, msg[2]);
        handler1.handleMessage(session, msg[3]);
        handler2.handleMessage(session, msg[4]);
        handler2.handleMessage(session, msg[5]);
        handler1.handleMessage(session, msg[6]);
        handler2.handleMessage(session, msg[7]);
        handler3.handleMessage(session, msg[8]);

        /*
         * Replay.
         */
        EasyMock.replay(handler1);
        EasyMock.replay(handler2);
        EasyMock.replay(handler3);

        DemuxingIoHandler ioHandler = new DemuxingIoHandler();

        /*
         * First round. All messages should be handled by handler1
         */
        ioHandler.addReceivedMessageHandler(C1.class, handler1);
        ioHandler.messageReceived(session, msg[0]);
        ioHandler.messageReceived(session, msg[1]);
        ioHandler.messageReceived(session, msg[2]);

        /*
         * Second round. C1 messages should be handled by handler1. C2 and C3
         * messages should be handled by handler2.
         */
        ioHandler.addReceivedMessageHandler(C2.class, handler2);
        ioHandler.messageReceived(session, msg[3]);
        ioHandler.messageReceived(session, msg[4]);
        ioHandler.messageReceived(session, msg[5]);

        /*
         * Third round. C1 messages should be handled by handler1, C2 by
         * handler2 and C3 by handler3.
         */
        ioHandler.addReceivedMessageHandler(C3.class, handler3);
        ioHandler.messageReceived(session, msg[6]);
        ioHandler.messageReceived(session, msg[7]);
        ioHandler.messageReceived(session, msg[8]);

        /*
         * Verify.
         */
        EasyMock.verify(handler1);
        EasyMock.verify(handler2);
        EasyMock.verify(handler3);
    }

    @Test
    public void testFindHandlerByInterface() throws Exception {
        /*
         * Record expectations.
         */
        handler1.handleMessage(session, msg[0]);
        handler1.handleMessage(session, msg[1]);
        handler1.handleMessage(session, msg[2]);
        handler1.handleMessage(session, msg[3]);
        handler2.handleMessage(session, msg[4]);
        handler1.handleMessage(session, msg[5]);
        handler3.handleMessage(session, msg[6]);
        handler2.handleMessage(session, msg[7]);
        handler3.handleMessage(session, msg[8]);

        /*
         * Replay.
         */
        EasyMock.replay(handler1);
        EasyMock.replay(handler2);
        EasyMock.replay(handler3);

        DemuxingIoHandler ioHandler = new DemuxingIoHandler();

        /*
         * First round. All messages should be handled by handler1
         */
        ioHandler.addReceivedMessageHandler(I4.class, handler1);
        ioHandler.messageReceived(session, msg[0]);
        ioHandler.messageReceived(session, msg[1]);
        ioHandler.messageReceived(session, msg[2]);

        /*
         * Second round. C1 and C3 messages should be handled by handler1. C2
         * messages should be handled by handler2.
         */
        ioHandler.addReceivedMessageHandler(I6.class, handler2);
        ioHandler.messageReceived(session, msg[3]);
        ioHandler.messageReceived(session, msg[4]);
        ioHandler.messageReceived(session, msg[5]);

        /*
         * Third round. C1 and C3 messages should be handled by handler3. C2
         * messages should be handled by handler2.
         */
        ioHandler.addReceivedMessageHandler(I3.class, handler3);
        ioHandler.messageReceived(session, msg[6]);
        ioHandler.messageReceived(session, msg[7]);
        ioHandler.messageReceived(session, msg[8]);

        /*
         * Verify.
         */
        EasyMock.verify(handler1);
        EasyMock.verify(handler2);
        EasyMock.verify(handler3);
    }

    /*
     * Define some interfaces and classes used when testing the findHandler
     * method. This is what the hierarchy looks like:
     *
     * C3 - I7 - I9
     *  |    |   /\
     *  |   I8  I3 I4
     *  |
     * C2 - I5 - I6
     *  |
     * C1 - I1 - I2 - I4
     *            |
     *           I3
     */

    public interface I1 {
        // Do nothing
    }

    public interface I2 extends I3 {
        // Do nothing
    }

    public interface I3 {
        // Do nothing
    }

    public interface I4 {
        // Do nothing
    }

    public static class C1 implements I1, I2, I4 {
        // Do nothing
    }

    public interface I5 {
        // Do nothing
    }

    public interface I6 {
        // Do nothing
    }

    public static class C2 extends C1 implements I5, I6 {
        // Do nothing
    }

    public interface I7 extends I8 {
        // Do nothing
    }

    public interface I8 {
        // Do nothing
    }

    public interface I9 extends I3, I4 {
        // Do nothing
    }

    public static class C3 extends C2 implements I7, I9 {
        // Do nothing
    }
}
