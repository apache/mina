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
package org.apache.mina.ldap;

import static org.apache.mina.session.AttributeKey.createKey;

import java.nio.ByteBuffer;

import org.apache.directory.shared.ldap.codec.api.LdapApiService;
import org.apache.directory.shared.ldap.codec.api.LdapApiServiceFactory;
import org.apache.directory.shared.ldap.codec.api.LdapMessageContainer;
import org.apache.directory.shared.ldap.codec.api.MessageDecorator;
import org.apache.directory.shared.ldap.model.message.AddResponse;
import org.apache.directory.shared.ldap.model.message.BindResponse;
import org.apache.directory.shared.ldap.model.message.CompareResponse;
import org.apache.directory.shared.ldap.model.message.DeleteResponse;
import org.apache.directory.shared.ldap.model.message.ExtendedResponse;
import org.apache.directory.shared.ldap.model.message.IntermediateResponse;
import org.apache.directory.shared.ldap.model.message.Message;
import org.apache.directory.shared.ldap.model.message.ModifyDnResponse;
import org.apache.directory.shared.ldap.model.message.ModifyResponse;
import org.apache.directory.shared.ldap.model.message.SearchResultDone;
import org.apache.directory.shared.ldap.model.message.SearchResultEntry;
import org.apache.directory.shared.ldap.model.message.SearchResultReference;
import org.apache.mina.api.IdleStatus;
import org.apache.mina.api.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filterchain.WriteFilterChainController;
import org.apache.mina.session.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A LDAP message codec.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class LdapCodec extends ProtocolCodecFilter {
    private static final Logger LOG = LoggerFactory.getLogger(LdapCodec.class);

    /** The LDAP decoder instance */
    private static ProtocolDecoder ldapDecoder = new LdapProtocolDecoder();

    /** The LDAP encoder instance */
    private static ProtocolEncoder ldapEncoder = new LdapProtocolEncoder();

    /** The codec */
    private static LdapApiService codec = LdapApiServiceFactory.getSingleton();

    /** The Message Container attribute */
    public static final AttributeKey<LdapMessageContainer> MESSAGE_CONTAINER_AT = createKey(LdapMessageContainer.class,
            "internal_messageContainer");

    public LdapCodec() {
        super(ldapEncoder, ldapDecoder);
    }

    @Override
    public void sessionOpened(final IoSession session) {
        final LdapMessageContainer<MessageDecorator<? extends Message>> container = new LdapMessageContainer<MessageDecorator<? extends Message>>(
                codec);
        session.setAttribute(MESSAGE_CONTAINER_AT, container);
    }

    @Override
    public void sessionClosed(final IoSession session) {
        session.removeAttribute(MESSAGE_CONTAINER_AT);
    }

    @Override
    public void sessionIdle(final IoSession session, final IdleStatus status) {
        // TODO Auto-generated method stub
    }

    @Override
    public void messageWriting(final IoSession session, final Object message,
            final WriteFilterChainController controller) {
        if (message instanceof AddResponse) {
            ldapEncoder.encode(session, message, controller);
        } else if (message instanceof BindResponse) {
            ldapEncoder.encode(session, message, controller);
        } else if (message instanceof DeleteResponse) {
            ldapEncoder.encode(session, message, controller);
        } else if (message instanceof CompareResponse) {
            ldapEncoder.encode(session, message, controller);
        } else if (message instanceof ExtendedResponse) {
            ldapEncoder.encode(session, message, controller);
        } else if (message instanceof IntermediateResponse) {
            ldapEncoder.encode(session, message, controller);
        } else if (message instanceof ModifyResponse) {
            ldapEncoder.encode(session, message, controller);
        } else if (message instanceof ModifyDnResponse) {
            ldapEncoder.encode(session, message, controller);
        } else if (message instanceof SearchResultDone) {
            ldapEncoder.encode(session, message, controller);
        } else if (message instanceof SearchResultEntry) {
            ldapEncoder.encode(session, message, controller);
        } else if (message instanceof SearchResultReference) {
            ldapEncoder.encode(session, message, controller);
        } else if (message instanceof ByteBuffer) {
            controller.callWriteNextFilter(message);
        }
    }
}