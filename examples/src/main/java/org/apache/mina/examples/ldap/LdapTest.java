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

import javax.naming.ldap.ExtendedRequest;

import org.apache.directory.shared.ldap.model.message.AbandonRequest;
import org.apache.directory.shared.ldap.model.message.AddRequest;
import org.apache.directory.shared.ldap.model.message.BindRequest;
import org.apache.directory.shared.ldap.model.message.BindResponse;
import org.apache.directory.shared.ldap.model.message.CompareRequest;
import org.apache.directory.shared.ldap.model.message.DeleteRequest;
import org.apache.directory.shared.ldap.model.message.ModifyDnRequest;
import org.apache.directory.shared.ldap.model.message.ModifyRequest;
import org.apache.directory.shared.ldap.model.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.model.message.SearchRequest;
import org.apache.directory.shared.ldap.model.message.UnbindRequest;
import org.apache.mina.api.AbstractIoFilter;
import org.apache.mina.api.IoSession;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.filterchain.ReadFilterChainController;
import org.apache.mina.ldap.LdapCodec;
import org.apache.mina.transport.nio.tcp.NioTcpServer;

/**
 * A simple LDAP server class used to test theLDAP encoder/decoder. It only deal with the BindRequest message.
 */
public class LdapTest {

    public static void main(String[] args) throws Exception {
        LdapTest ldapServer = new LdapTest();
        NioTcpServer acceptor = new NioTcpServer();
        acceptor.setFilters(new LoggingFilter("INCOMING"), new LdapCodec(), new LoggingFilter("DECODED"),
                ldapServer.new DummyLdapServer());

        acceptor.bind(new InetSocketAddress(10389));

        // run for 20 seconds
        Thread.sleep(200000);
        acceptor.unbind();

    }

    private class DummyLdapServer extends AbstractIoFilter {
        @Override
        public void messageReceived(IoSession session, Object message, ReadFilterChainController controller) {
            if (message instanceof AbandonRequest) {
                handle(session, (AbandonRequest) message);
            } else if (message instanceof AddRequest) {
                handle(session, (AddRequest) message);
            } else if (message instanceof BindRequest) {
                handle(session, (BindRequest) message);
            } else if (message instanceof CompareRequest) {
                handle(session, (CompareRequest) message);
            } else if (message instanceof DeleteRequest) {
                handle(session, (DeleteRequest) message);
            } else if (message instanceof ExtendedRequest) {
                handle(session, (ExtendedRequest) message);
            } else if (message instanceof ModifyRequest) {
                handle(session, (ModifyRequest) message);
            } else if (message instanceof ModifyDnRequest) {
                handle(session, (ModifyDnRequest) message);
            } else if (message instanceof SearchRequest) {
                handle(session, (SearchRequest) message);
            } else if (message instanceof UnbindRequest) {
                handle(session, (UnbindRequest) message);
            }
        }

        /**
         * Process the AbandonRequest message
         */
        private void handle(IoSession session, AbandonRequest abandonRequest) {
            // Do nothing
        }

        /**
         * Process the AddRequest message
         */
        private void handle(IoSession session, AddRequest addRequest) {
            // Do nothing
        }

        /**
         * Process the BindRequest message
         */
        private void handle(IoSession session, BindRequest bindRequest) {
            // Build a faked BindResponse
            BindResponse response = bindRequest.getResultResponse();
            response.getLdapResult().setResultCode(ResultCodeEnum.SUCCESS);

            session.write(response);
        }

        /**
         * Process the CompareRequest message
         */
        private void handle(IoSession session, CompareRequest compareRequest) {
            // Do nothing
        }

        /**
         * Process the DeleteRequest message
         */
        private void handle(IoSession session, DeleteRequest deleteRequest) {
            // Do nothing
        }

        /**
         * Process the ExtendedRequest message
         */
        private void handle(IoSession session, ExtendedRequest extendedRequest) {
            // Do nothing
        }

        /**
         * Process the ModifyRequest message
         */
        private void handle(IoSession session, ModifyRequest modifyRequest) {
            // Do nothing
        }

        /**
         * Process the ModifyDnRequest message
         */
        private void handle(IoSession session, ModifyDnRequest modifyDnRequest) {
            // Do nothing
        }

        /**
         * Process the SearchRequest message
         */
        private void handle(IoSession session, SearchRequest searchRequest) {
            // Do nothing
        }

        /**
         * Process the UnbindRequest message
         */
        private void handle(IoSession session, UnbindRequest unbindRequest) {
            // Do nothing
        }
    }
}
