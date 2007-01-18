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
package org.apache.mina.common.support;

import java.net.SocketAddress;

import org.apache.mina.common.IoSessionConfig;
import org.apache.mina.common.TransportType;

/**
 * A default immutable implementation of {@link TransportType}.
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class DefaultTransportType implements TransportType {

    private final String name;
    private final boolean connectionless;
    private final Class<? extends SocketAddress> addressType;
    private final Class<? extends Object> envelopeType;
    private final Class<? extends IoSessionConfig> sessionConfigType;
    
    public DefaultTransportType(
            String name,
            boolean connectionless,
            Class<? extends SocketAddress> addressType,
            Class<? extends Object> envelopeType,
            Class<? extends IoSessionConfig> sessionConfigType) {

        if (name == null) {
            throw new NullPointerException("name");
        }
        
        name = name.trim();
        
        if (name.length() == 0) {
            throw new IllegalArgumentException("name is empty.");
        }
        
        if (addressType == null) {
            throw new NullPointerException("addressType");
        }
        
        if (envelopeType == null) {
            throw new NullPointerException("envelopeType");
        }
        
        if (sessionConfigType == null) {
            throw new NullPointerException("sessionConfigType");
        }
        
        this.name = name;
        this.connectionless = connectionless;
        this.addressType = addressType;
        this.envelopeType = envelopeType;
        this.sessionConfigType = sessionConfigType;
    }
    
    public Class<? extends SocketAddress> getAddressType() {
        return addressType;
    }

    public Class<? extends Object> getEnvelopeType() {
        return envelopeType;
    }
    
    public Class<? extends IoSessionConfig> getSessionConfigType() {
        return sessionConfigType;
    }

    public String getName() {
        return name;
    }

    public boolean isConnectionless() {
        return connectionless;
    }
    
    public String toString() {
        return name;
    }
}
