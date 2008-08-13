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
package org.apache.mina.proxy.handlers.socks;

import java.net.InetSocketAddress;

import org.apache.mina.proxy.handlers.ProxyRequest;

/**
 * SocksProxyRequest.java - Wrapper class for SOCKS requests.
 * 
 * @author Edouard De Oliveira <a href="mailto:doe_wanted@yahoo.fr">doe_wanted@yahoo.fr</a>
 * @version $Id: $
 */
public class SocksProxyRequest extends ProxyRequest {

    private byte protocolVersion;

    private byte commandCode;

    private String userName;

    private String password;

    private String host;

    private int port;

    private String serviceKerberosName;

    /**
     * Constructor used when making a SOCKS4/SOCKS5 request.
     */
    public SocksProxyRequest(byte protocolVersion, byte commandCode,
            InetSocketAddress endpointAddress, String userName) {
        super(endpointAddress);
        this.protocolVersion = protocolVersion;
        this.commandCode = commandCode;
        this.userName = userName;
    }

    /**
     * Constructor used when making a SOCKS4a request.
     */
    public SocksProxyRequest(byte commandCode, String host, int port,
            String userName) {
        this.protocolVersion = SocksProxyConstants.SOCKS_VERSION_4;
        this.commandCode = commandCode;
        this.userName = userName;
        this.host = host;
        this.port = port;
    }

    public byte[] getIpAddress() {
        if (getEndpointAddress() == null)
            return SocksProxyConstants.FAKE_IP;
        else
            return getEndpointAddress().getAddress().getAddress();
    }

    public byte[] getPort() {
        byte[] port = new byte[2];
        int p = (int) (getEndpointAddress() == null ? this.port
                : getEndpointAddress().getPort());
        port[1] = (byte) p;
        port[0] = (byte) (p >> 8);
        return port;
    }

    public byte getCommandCode() {
        return commandCode;
    }

    public byte getProtocolVersion() {
        return protocolVersion;
    }

    public String getUserName() {
        return userName;
    }

    public synchronized final String getHost() {
        if (host == null) {
            if (getEndpointAddress() != null) {
                host = getEndpointAddress().getHostName();
            }
        }

        return host;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getServiceKerberosName() {
        return serviceKerberosName;
    }

    public void setServiceKerberosName(String serviceKerberosName) {
        this.serviceKerberosName = serviceKerberosName;
    }
}