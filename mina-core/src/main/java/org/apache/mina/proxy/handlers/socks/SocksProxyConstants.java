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
    public final static byte SOCKS_VERSION_4 = 0x04;

    public final static byte SOCKS_VERSION_5 = 0x05;

    public final static byte TERMINATOR = 0x00;

    /**
     * The size of a server to client response in a SOCKS4/4a negotiation.
     */
    public final static int SOCKS_4_RESPONSE_SIZE = 8;
    
    /**
     * Invalid IP used in SOCKS 4a protocol to specify that the
     * client can't resolve the destination host's domain name.
     */
    public final static byte[] FAKE_IP = new byte[] { 0, 0, 0, 10 };

    /**
     * Command codes. 
     */
    public final static byte ESTABLISH_TCPIP_STREAM = 0x01;

    public final static byte ESTABLISH_TCPIP_BIND = 0x02;

    public final static byte ESTABLISH_UDP_ASSOCIATE = 0x03;

    /**
     * SOCKS v4/v4a server reply codes.
     */
    public final static byte V4_REPLY_REQUEST_GRANTED = 0x5a;

    public final static byte V4_REPLY_REQUEST_REJECTED_OR_FAILED = 0x5b;

    public final static byte V4_REPLY_REQUEST_FAILED_NO_IDENTD = 0x5c;

    public final static byte V4_REPLY_REQUEST_FAILED_ID_NOT_CONFIRMED = 0x5d;

    /**
     * SOCKS v5 server reply codes.
     */
    public final static byte V5_REPLY_SUCCEEDED = 0x00;

    public final static byte V5_REPLY_GENERAL_FAILURE = 0x01;

    public final static byte V5_REPLY_NOT_ALLOWED = 0x02;

    public final static byte V5_REPLY_NETWORK_UNREACHABLE = 0x03;

    public final static byte V5_REPLY_HOST_UNREACHABLE = 0x04;

    public final static byte V5_REPLY_CONNECTION_REFUSED = 0x05;

    public final static byte V5_REPLY_TTL_EXPIRED = 0x06;

    public final static byte V5_REPLY_COMMAND_NOT_SUPPORTED = 0x07;

    public final static byte V5_REPLY_ADDRESS_TYPE_NOT_SUPPORTED = 0x08;

    /**
     * SOCKS v5 address types.
     */
    public final static byte IPV4_ADDRESS_TYPE = 0x01;

    public final static byte DOMAIN_NAME_ADDRESS_TYPE = 0x03;

    public final static byte IPV6_ADDRESS_TYPE = 0x04;

    /**
     * SOCKS v5 handshake steps.
     */
    public final static int SOCKS5_GREETING_STEP = 0;

    public final static int SOCKS5_AUTH_STEP = 1;

    public final static int SOCKS5_REQUEST_STEP = 2;

    /**
     * SOCKS v5 authentication methods.
     */
    public final static byte NO_AUTH = 0x00;

    public final static byte GSSAPI_AUTH = 0x01;

    public final static byte BASIC_AUTH = 0x02;

    public final static byte NO_ACCEPTABLE_AUTH_METHOD = (byte) 0xFF;

    public final static byte[] SUPPORTED_AUTH_METHODS = new byte[] { NO_AUTH,
            GSSAPI_AUTH, BASIC_AUTH };

    public final static byte BASIC_AUTH_SUBNEGOTIATION_VERSION = 0x01;

    public final static byte GSSAPI_AUTH_SUBNEGOTIATION_VERSION = 0x01;

    public final static byte GSSAPI_MSG_TYPE = 0x01;

    /**
     * Kerberos providers OID's.
     */ 
    public final static String KERBEROS_V5_OID = "1.2.840.113554.1.2.2";

    public final static String MS_KERBEROS_V5_OID = "1.2.840.48018.1.2.2";

    /**
     * Microsoft NTLM security support provider.
     */ 
    public final static String NTLMSSP_OID = "1.3.6.1.4.1.311.2.2.10";

    /**
     * Return the string associated with the specified reply code.
     * 
     * @param code the reply code
     * @return the reply string
     */
    public final static String getReplyCodeAsString(byte code) {
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