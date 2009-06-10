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
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * @since MINA 2.0.0-M3
 */
public class SocksProxyRequest extends ProxyRequest {

    /**
     * The SOCKS protocol version.
     */
    private byte protocolVersion;

    /**
     * The command code.
     */
    private byte commandCode;

    /**
     * The user name used when authenticating to the proxy server. 
     */
    private String userName;

    /**
     * The user's password used when authenticating to the proxy server.
     */
    private String password;

    /**
     * The SOCKS server host name.
     */
    private String host;

    /**
     * The SOCKS server port.
     */
    private int port;

    /**
     * The Kerberos service name used in GSSAPI authentication mode.
     */
    private String serviceKerberosName;

    /**
     * Constructor used when building a SOCKS4 request.
     * 
     * @param protocolVersion the protocol version
     * @param commandCode the command code
     * @param endpointAddress the endpoint address
     * @param userName the user name
     */
    public SocksProxyRequest(byte protocolVersion, byte commandCode,
            InetSocketAddress endpointAddress, String userName) {
        super(endpointAddress);
        this.protocolVersion = protocolVersion;
        this.commandCode = commandCode;
        this.userName = userName;
    }

    /**
     * Constructor used when building a SOCKS4a request.
     * 
     * @param commandCode the command code
     * @param host the server host name
     * @param port the server port
     * @param userName the user name
     */
    public SocksProxyRequest(byte commandCode, String host, int port,
            String userName) {
        this.protocolVersion = SocksProxyConstants.SOCKS_VERSION_4;
        this.commandCode = commandCode;
        this.userName = userName;
        this.host = host;
        this.port = port;
    }

    /**
     * Returns the endpoint address resulting from the {@link #getEndpointAddress()}. 
     * If not set, it will return the {@link SocksProxyConstants#FAKE_IP} constant 
     * value which will be ignored in a SOCKS v4 request.
     *   
     * @return the endpoint address
     */
    public byte[] getIpAddress() {
        if (getEndpointAddress() == null) {
            return SocksProxyConstants.FAKE_IP;
        }
        
        return getEndpointAddress().getAddress().getAddress();
    }

    /**
     * Return the server port as a byte array.
     * 
     * @return the server port
     */
    public byte[] getPort() {
        byte[] port = new byte[2];
        int p = (getEndpointAddress() == null ? this.port
                : getEndpointAddress().getPort());
        port[1] = (byte) p;
        port[0] = (byte) (p >> 8);
        return port;
    }

    /**
     * Return the command code.
     * 
     * @return the command code
     */
    public byte getCommandCode() {
        return commandCode;
    }

    /**
     * Return the protocol version.
     * 
     * @return the protocol version
     */
    public byte getProtocolVersion() {
        return protocolVersion;
    }

    /**
     * Return the user name.
     * 
     * @return the user name
     */
    public String getUserName() {
        return userName;
    }

    /**
     * Return the server host name.
     * 
     * @return the server host name
     */
    public synchronized final String getHost() {
        if (host == null) {
            InetSocketAddress adr = getEndpointAddress();
            
            if ( adr != null && !adr.isUnresolved()) {
                host = getEndpointAddress().getHostName();
            }
        }

        return host;
    }

    /**
     * Return the user password.
     * 
     * @return the user password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Set the user password
     * 
     * @param password the user password value
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Return the Kerberos service name.
     * 
     * @return the Kerberos service name
     */
    public String getServiceKerberosName() {
        return serviceKerberosName;
    }

    /**
     * Set the Kerberos service name.
     * 
     * @param serviceKerberosName the Kerberos service name
     */
    public void setServiceKerberosName(String serviceKerberosName) {
        this.serviceKerberosName = serviceKerberosName;
    }
}