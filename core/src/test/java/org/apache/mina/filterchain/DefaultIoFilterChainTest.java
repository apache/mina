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
package org.apache.mina.filterchain;

import org.apache.mina.api.IdleStatus;
import org.apache.mina.api.IoFilter;
import org.apache.mina.api.IoSession;

public class DefaultIoFilterChainTest {

    private static class DummyFilter implements IoFilter {

        String id;

        public DummyFilter(String id) {
            this.id = id;
        }

        @Override
        public void sessionCreated(IoSession session) {
        }

        @Override
        public void sessionOpened(IoSession session) {
        }

        @Override
        public void sessionClosed(IoSession session) {
        }

        @Override
        public void sessionIdle(IoSession session, IdleStatus status) {
        }

        public String toString() {
            return "DummyFilter(" + id + ")";
        }

        @Override
        public void messageReceived(IoSession session, Object message, ReadFilterChainController controller) {
            controller.callReadNextFilter(session, message);
        }

        @Override
        public void messageWriting(IoSession session, Object message, WriteFilterChainController controller) {
            controller.callWriteNextFilter(session, message);
        }
    }

}
