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

/**
 * SocksProxyConstants.java - SOCKS proxy constants.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * @since MINA 2.0.0-M3
 */
public class SocksProxyConstants {
    /**
     * SOCKS versions field values.
     */
    /** Socks V4 */
    public static final byte SOCKS_VERSION_4 = 0x04;

    /** Socks V5 */
    public static final byte SOCKS_VERSION_5 = 0x05;

    /** terminator */
    public static final byte TERMINATOR = 0x00;

    /**
     * The size of a server to client response in a SOCKS4/4a negotiation.
     */
    public static final int SOCKS_4_RESPONSE_SIZE = 8;

    /**
     * Invalid IP used in SOCKS 4a protocol to specify that the
     * client can't resolve the destination host's domain name.
     */
    public static final byte[] FAKE_IP = new byte[] { 0, 0, 0, 10 };

    /**
     * Command codes. 
     */
    /** TCPIP stream */
    public static final byte ESTABLISH_TCPIP_STREAM = 0x01;

    /** TCPIP bind */
    public static final byte ESTABLISH_TCPIP_BIND = 0x02;

    /** UDP associate */
    public static final byte ESTABLISH_UDP_ASSOCIATE = 0x03;

    /**
     * SOCKS v4/v4a server reply codes.
     */
    /** Request granted */
    public static final byte V4_REPLY_REQUEST_GRANTED = 0x5a;

    /** Request rejected or failed */
    public static final byte V4_REPLY_REQUEST_REJECTED_OR_FAILED = 0x5b;

    /** Request failed not identified */
    public static final byte V4_REPLY_REQUEST_FAILED_NO_IDENTD = 0x5c;

    /** Request failed identity not confirmed */
    public static final byte V4_REPLY_REQUEST_FAILED_ID_NOT_CONFIRMED = 0x5d;

    /**
     * SOCKS v5 server reply codes.
     */
    /** Success */
    public static final byte V5_REPLY_SUCCEEDED = 0x00;

    /** General failure */
    public static final byte V5_REPLY_GENERAL_FAILURE = 0x01;

    /** Not allowed */
    public static final byte V5_REPLY_NOT_ALLOWED = 0x02;

    /** Network unreachable */
    public static final byte V5_REPLY_NETWORK_UNREACHABLE = 0x03;

    /** Host unreachable */
    public static final byte V5_REPLY_HOST_UNREACHABLE = 0x04;

    /** Connection refused */
    public static final byte V5_REPLY_CONNECTION_REFUSED = 0x05;

    /** TTL expired */
    public static final byte V5_REPLY_TTL_EXPIRED = 0x06;

    /** Command not supported */
    public static final byte V5_REPLY_COMMAND_NOT_SUPPORTED = 0x07;

    /** Address type not supported */
    public static final byte V5_REPLY_ADDRESS_TYPE_NOT_SUPPORTED = 0x08;

    /** IPV4 address types */
    public static final byte IPV4_ADDRESS_TYPE = 0x01;

    /** Domain name address type */
    public static final byte DOMAIN_NAME_ADDRESS_TYPE = 0x03;

    /** IPV6 address type */
    public static final byte IPV6_ADDRESS_TYPE = 0x04;

    /**
     * SOCKS v5 handshake steps.
     */
    /** Greeting step */
    public static final int SOCKS5_GREETING_STEP = 0;

    /** Authentication step */
    public static final int SOCKS5_AUTH_STEP = 1;

    /** Request step */
    public static final int SOCKS5_REQUEST_STEP = 2;

    /**
     * SOCKS v5 authentication methods.
     */
    /** No authentication */
    public static final byte NO_AUTH = 0x00;

    /** GSSAPI authentication */
    public static final byte GSSAPI_AUTH = 0x01;

    /** Basic authentication */
    public static final byte BASIC_AUTH = 0x02;

    /** Non acceptable method authentication */
    public static final byte NO_ACCEPTABLE_AUTH_METHOD = (byte) 0xFF;

    /** Supported authentication methods */
    public static final byte[] SUPPORTED_AUTH_METHODS = new byte[] { NO_AUTH, GSSAPI_AUTH, BASIC_AUTH };

    /** Basic authentication subnegociation version */
    public static final byte BASIC_AUTH_SUBNEGOTIATION_VERSION = 0x01;

    /** GSSAPI authentication subnegociation version */
    public static final byte GSSAPI_AUTH_SUBNEGOTIATION_VERSION = 0x01;

    /** GSSAPI message type */
    public static final byte GSSAPI_MSG_TYPE = 0x01;

    /**
     * Kerberos providers OID's.
     */
    /** Kerberos V5 OID */
    public static final String KERBEROS_V5_OID = "1.2.840.113554.1.2.2";

    /** Microsoft Kerberos V5 OID */
    public static final String MS_KERBEROS_V5_OID = "1.2.840.48018.1.2.2";

    /**
     * Microsoft NTLM security support provider.
     */
    public static final String NTLMSSP_OID = "1.3.6.1.4.1.311.2.2.10";

    private SocksProxyConstants() {
    }
    
    /**
     * Return the string associated with the specified reply code.
     * 
     * @param code the reply code
     * @return the reply string
     */
    public static final String getReplyCodeAsString(byte code) {
        switch (code) {
        // v4 & v4a codes
        case V4_REPLY_REQUEST_GRANTED:
            return "Request granted";
        case V4_REPLY_REQUEST_REJECTED_OR_FAILED:
            return "Request rejected or failed";
        case V4_REPLY_REQUEST_FAILED_NO_IDENTD:
            return "Request failed because client is not running identd (or not reachable from the server)";
        case V4_REPLY_REQUEST_FAILED_ID_NOT_CONFIRMED:
            return "Request failed because client's identd could not confirm the user ID string in the request";

            // v5 codes
        case V5_REPLY_SUCCEEDED:
            return "Request succeeded";
        case V5_REPLY_GENERAL_FAILURE:
            return "Request failed: general SOCKS server failure";
        case V5_REPLY_NOT_ALLOWED:
            return "Request failed: connection not allowed by ruleset";
        case V5_REPLY_NETWORK_UNREACHABLE:
            return "Request failed: network unreachable";
        case V5_REPLY_HOST_UNREACHABLE:
            return "Request failed: host unreachable";
        case V5_REPLY_CONNECTION_REFUSED:
            return "Request failed: connection refused";
        case V5_REPLY_TTL_EXPIRED:
            return "Request failed: TTL expired";
        case V5_REPLY_COMMAND_NOT_SUPPORTED:
            return "Request failed: command not supported";
        case V5_REPLY_ADDRESS_TYPE_NOT_SUPPORTED:
            return "Request failed: address type not supported";

        default:
            return "Unknown reply code";
        }
    }
}