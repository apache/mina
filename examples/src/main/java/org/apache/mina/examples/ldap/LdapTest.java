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
package org.apache.mina.examples.ldap;

import java.net.InetSocketAddress;

import org.apache.directory.shared.ldap.model.message.BindRequest;
import org.apache.directory.shared.ldap.model.message.BindResponse;
import org.apache.directory.shared.ldap.model.message.ResultCodeEnum;
import org.apache.mina.api.IdleStatus;
import org.apache.mina.api.IoFilter;
import org.apache.mina.api.IoSession;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.filterchain.ReadFilterChainController;
import org.apache.mina.filterchain.WriteFilterChainController;
import org.apache.mina.ldap.LdapCodec;
import org.apache.mina.service.OneThreadSelectorStrategy;
import org.apache.mina.service.SelectorFactory;
import org.apache.mina.transport.tcp.NioSelectorProcessor;
import org.apache.mina.transport.tcp.nio.NioTcpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple LDAP server class used to test theLDAP encoder/decoder. It only deal with the 
 * BindRequest message.
 */
public class LdapTest {
    private static final Logger LOG = LoggerFactory.getLogger(LdapTest.class);

    public static void main(String[] args) throws Exception {

        OneThreadSelectorStrategy strategy = new OneThreadSelectorStrategy(new SelectorFactory(
                NioSelectorProcessor.class));
        NioTcpServer acceptor = new NioTcpServer(strategy);
        acceptor.setFilters(new LoggingFilter("INCOMING"), new LdapCodec(), new LoggingFilter("DECODED"),
                new DummyLdapSever());

        acceptor.bind(new InetSocketAddress(10389));

        // run for 20 seconds
        Thread.sleep(200000);
        acceptor.unbindAll();

    }

    private static class DummyLdapSever implements IoFilter {

        @Override
        public void sessionCreated(IoSession session) {
            System.out.println("Session created");
        }

        @Override
        public void sessionOpened(IoSession session) {
            // TODO Auto-generated method stub

        }

        @Override
        public void sessionClosed(IoSession session) {
            // TODO Auto-generated method stub

        }

        @Override
        public void sessionIdle(IoSession session, IdleStatus status) {
            // TODO Auto-generated method stub

        }

        @Override
        public void messageReceived(IoSession session, Object message, ReadFilterChainController controller) {
            if (message instanceof BindRequest) {
                BindRequest bindRequest = (BindRequest)message;
                
                BindResponse response = (BindResponse) bindRequest.getResultResponse();
                response.getLdapResult().setResultCode(ResultCodeEnum.SUCCESS);

                session.write(response);
            }
        }
        
        @Override
        public void messageWriting(IoSession session, Object message, WriteFilterChainController controller) {
            // we just push the message in the chain
            controller.callWriteNextFilter(session, message);
        }
    }
}
