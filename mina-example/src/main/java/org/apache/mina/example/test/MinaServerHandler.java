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
package org.apache.mina.example.test;

import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;

import java.util.logging.Logger;

public class MinaServerHandler extends IoHandlerAdapter {

    private final Logger logger = Logger.getLogger(String.valueOf(getClass()));

    @Override
    public void sessionOpened(IoSession session) {
        session.getConfig().setIdleTime(IdleStatus.BOTH_IDLE, 1000);
        session.setAttribute("Values: ");
    }

    @Override
    public void messageReceived(IoSession session, Object message) {
        logger.info("Message received in the server..");
        System.out.println("message is " + message);
        logger.info("Message is: " + message.toString());
    }

    @Override
    public void sessionIdle(IoSession session, IdleStatus status) {
        System.out.println("session idled.");
        logger.info("Disconnecting the idle.");
        // disconnect an idle client
        session.close();
    }

    @Override
    public void exceptionCaught(IoSession session, Throwable cause) {
        System.out.println("exceptionCaught");
        // close the connection on exceptional situation
        session.close();
    }
}
