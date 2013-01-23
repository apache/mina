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

import java.nio.ByteBuffer;

import org.apache.directory.shared.ldap.codec.api.LdapApiServiceFactory;
import org.apache.directory.shared.ldap.codec.api.LdapEncoder;
import org.apache.directory.shared.ldap.model.message.Message;
import org.apache.mina.api.IoSession;
import org.apache.mina.codec.ProtocolEncoder;
import org.apache.mina.filterchain.WriteFilterChainController;
import org.apache.mina.session.WriteRequest;

/**
 * A LDAP message encoder. It is based on shared-ldap encoder.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class LdapProtocolEncoder implements ProtocolEncoder {
    /** The stateful encoder */
    private LdapEncoder encoder;

    /**
     * Creates a new instance of LdapProtocolEncoder.
     * 
     * @param codec The LDAP codec service associated with this encoder.
     */
    public LdapProtocolEncoder() {
        this.encoder = new LdapEncoder(LdapApiServiceFactory.getSingleton());
    }

    /**
     * {@inheritDoc}
     */
    public Object encode(IoSession session, WriteRequest writeRequest, WriteFilterChainController controller) {
        try {
            ByteBuffer buffer = encoder.encodeMessage((Message) writeRequest.getOriginalMessage());
            writeRequest.setMessage(buffer);

            controller.callWriteNextFilter(writeRequest);
        } catch (Exception e) {
            return null;
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    public void dispose(IoSession session) throws Exception {
        // Nothing to do
    }
}
