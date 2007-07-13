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

import junit.framework.TestCase;

import org.apache.mina.common.IoSession;
import org.apache.mina.handler.demux.DemuxingIoHandler;
import org.apache.mina.handler.demux.MessageHandler;
import org.easymock.MockControl;

/**
 * Tests {@link org.apache.mina.handler.demux.DemuxingIoHandler}.
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class DemuxingIoHandlerTest extends TestCase {
    MockControl mockHandler1;

    MockControl mockHandler2;

    MockControl mockHandler3;

    MessageHandler handler1;

    MessageHandler handler2;

    MessageHandler handler3;

    IoSession session;

    Object[] msg;

    @SuppressWarnings("unchecked")
    protected void setUp() throws Exception {
        super.setUp();

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
        mockHandler1 = MockControl.createControl(MessageHandler.class);
        mockHandler2 = MockControl.createControl(MessageHandler.class);
        mockHandler3 = MockControl.createControl(MessageHandler.class);

        handler1 = (MessageHandler) mockHandler1.getMock();
        handler2 = (MessageHandler) mockHandler2.getMock();
        handler3 = (MessageHandler) mockHandler3.getMock();

        session = (IoSession) MockControl.createControl(IoSession.class)
                .getMock();
    }

    @SuppressWarnings("unchecked")
    public void testFindHandlerByClass() throws Exception {
        /*
         * Record expectations.
         */
        handler1.messageReceived(session, msg[0]);
        handler1.messageReceived(session, msg[1]);
        handler1.messageReceived(session, msg[2]);
        handler1.messageReceived(session, msg[3]);
        handler2.messageReceived(session, msg[4]);
        handler2.messageReceived(session, msg[5]);
        handler1.messageReceived(session, msg[6]);
        handler2.messageReceived(session, msg[7]);
        handler3.messageReceived(session, msg[8]);

        /*
         * Replay.
         */
        mockHandler1.replay();
        mockHandler2.replay();
        mockHandler3.replay();

        DemuxingIoHandler ioHandler = new DemuxingIoHandler();

        /*
         * First round. All messages should be handled by handler1
         */
        ioHandler.addMessageHandler(C1.class, (MessageHandler) mockHandler1
                .getMock());
        ioHandler.messageReceived(session, msg[0]);
        ioHandler.messageReceived(session, msg[1]);
        ioHandler.messageReceived(session, msg[2]);

        /*
         * Second round. C1 messages should be handled by handler1. C2 and C3
         * messages should be handled by handler2.
         */
        ioHandler.addMessageHandler(C2.class, (MessageHandler) mockHandler2
                .getMock());
        ioHandler.messageReceived(session, msg[3]);
        ioHandler.messageReceived(session, msg[4]);
        ioHandler.messageReceived(session, msg[5]);

        /*
         * Third round. C1 messages should be handled by handler1, C2 by 
         * handler2 and C3 by handler3.
         */
        ioHandler.addMessageHandler(C3.class, (MessageHandler) mockHandler3
                .getMock());
        ioHandler.messageReceived(session, msg[6]);
        ioHandler.messageReceived(session, msg[7]);
        ioHandler.messageReceived(session, msg[8]);

        /*
         * Verify.
         */
        mockHandler1.verify();
        mockHandler2.verify();
        mockHandler3.verify();
    }

    @SuppressWarnings("unchecked")
    public void testFindHandlerByInterface() throws Exception {
        /*
         * Record expectations.
         */
        handler1.messageReceived(session, msg[0]);
        handler1.messageReceived(session, msg[1]);
        handler1.messageReceived(session, msg[2]);
        handler1.messageReceived(session, msg[3]);
        handler2.messageReceived(session, msg[4]);
        handler1.messageReceived(session, msg[5]);
        handler3.messageReceived(session, msg[6]);
        handler2.messageReceived(session, msg[7]);
        handler3.messageReceived(session, msg[8]);

        /*
         * Replay.
         */
        mockHandler1.replay();
        mockHandler2.replay();
        mockHandler3.replay();

        DemuxingIoHandler ioHandler = new DemuxingIoHandler();

        /*
         * First round. All messages should be handled by handler1
         */
        ioHandler.addMessageHandler(I4.class, (MessageHandler) mockHandler1
                .getMock());
        ioHandler.messageReceived(session, msg[0]);
        ioHandler.messageReceived(session, msg[1]);
        ioHandler.messageReceived(session, msg[2]);

        /*
         * Second round. C1 and C3 messages should be handled by handler1. C2
         * messages should be handled by handler2.
         */
        ioHandler.addMessageHandler(I6.class, (MessageHandler) mockHandler2
                .getMock());
        ioHandler.messageReceived(session, msg[3]);
        ioHandler.messageReceived(session, msg[4]);
        ioHandler.messageReceived(session, msg[5]);

        /*
         * Third round. C1 and C3 messages should be handled by handler3. C2
         * messages should be handled by handler2.
         */
        ioHandler.addMessageHandler(I3.class, (MessageHandler) mockHandler3
                .getMock());
        ioHandler.messageReceived(session, msg[6]);
        ioHandler.messageReceived(session, msg[7]);
        ioHandler.messageReceived(session, msg[8]);

        /*
         * Verify.
         */
        mockHandler1.verify();
        mockHandler2.verify();
        mockHandler3.verify();
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
    }

    public interface I2 extends I3 {
    }

    public interface I3 {
    }

    public interface I4 {
    }

    public static class C1 implements I1, I2, I4 {
    }

    public interface I5 {
    }

    public interface I6 {
    }

    public static class C2 extends C1 implements I5, I6 {
    }

    public interface I7 extends I8 {
    }

    public interface I8 {
    }

    public interface I9 extends I3, I4 {
    }

    public static class C3 extends C2 implements I7, I9 {
    }
}
