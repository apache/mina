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
package org.apache.mina.proxy.handlers.http.ntlm;

/**
 * NTLMConstants.java - All NTLM constants.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * @since MINA 2.0.0-M3
 */
public interface NTLMConstants {
    // Signature "NTLMSSP"+{0}
    byte[] NTLM_SIGNATURE = new byte[] { 0x4E, 0x54, 0x4C, 0x4D, 0x53, 0x53, 0x50, 0 };

    // Version 5.1.2600 a Windows XP version (ex: Build 2600.xpsp_sp2_gdr.050301-1519 : Service Pack 2)
    byte[] DEFAULT_OS_VERSION = new byte[] { 0x05, 0x01, 0x28, 0x0A, 0, 0, 0, 0x0F };

    /**
     * Message types
     */
    int MESSAGE_TYPE_1 = 1;

    int MESSAGE_TYPE_2 = 2;

    int MESSAGE_TYPE_3 = 3;

    /**
     * Message flags
     */

    // Indicates that Unicode strings are supported for use in security buffer data
    int FLAG_NEGOTIATE_UNICODE = 0x00000001;

    // Indicates that OEM strings are supported for use in security buffer data
    int FLAG_NEGOTIATE_OEM = 0x00000002;

    // Requests that the server's authentication realm be included in the Type 2 message
    int FLAG_REQUEST_SERVER_AUTH_REALM = 0x00000004;

    // Specifies that authenticated communication between the client 
    // and server should carry a digital signature (message integrity)
    int FLAG_NEGOTIATE_SIGN = 0x00000010;

    // Specifies that authenticated communication between the client 
    // and server should be encrypted (message confidentiality)
    int FLAG_NEGOTIATE_SEAL = 0x00000020;

    // Indicates that datagram authentication is being used
    int FLAG_NEGOTIATE_DATAGRAM_STYLE = 0x00000040;

    // Indicates that the Lan Manager Session Key should be used for signing and 
    // sealing authenticated communications
    int FLAG_NEGOTIATE_LAN_MANAGER_KEY = 0x00000080;

    // Indicates that NTLM authentication is being used
    int FLAG_NEGOTIATE_NTLM = 0x00000200;

    // Sent by the client in the Type 3 message to indicate that an anonymous context 
    // has been established. This also affects the response fields
    int FLAG_NEGOTIATE_ANONYMOUS = 0x00000800;

    // Sent by the client in the Type 1 message to indicate that the name of the domain in which 
    // the client workstation has membership is included in the message. This is used by the 
    // server to determine whether the client is eligible for local authentication
    int FLAG_NEGOTIATE_DOMAIN_SUPPLIED = 0x00001000;

    // Sent by the client in the Type 1 message to indicate that the client workstation's name 
    // is included in the message. This is used by the server to determine whether the client 
    // is eligible for local authentication
    int FLAG_NEGOTIATE_WORKSTATION_SUPPLIED = 0x00002000;

    // Sent by the server to indicate that the server and client are on the same machine.
    // Implies that the client may use the established local credentials for authentication 
    // instead of calculating a response to the challenge
    int FLAG_NEGOTIATE_LOCAL_CALL = 0x00004000;

    // Indicates that authenticated communication between the client and server should 
    // be signed with a "dummy" signature
    int FLAG_NEGOTIATE_ALWAYS_SIGN = 0x00008000;

    // Sent by the server in the Type 2 message to indicate that the target authentication 
    // realm is a domain
    int FLAG_TARGET_TYPE_DOMAIN = 0x00010000;

    // Sent by the server in the Type 2 message to indicate that the target authentication 
    // realm is a server
    int FLAG_TARGET_TYPE_SERVER = 0x00020000;

    // Sent by the server in the Type 2 message to indicate that the target authentication 
    // realm is a share. Presumably, this is for share-level authentication. Usage is unclear
    int FLAG_TARGET_TYPE_SHARE = 0x00040000;

    // Indicates that the NTLM2 signing and sealing scheme should be used for protecting 
    // authenticated communications. Note that this refers to a particular session security 
    // scheme, and is not related to the use of NTLMv2 authentication. This flag can, however, 
    // have an effect on the response calculations
    int FLAG_NEGOTIATE_NTLM2 = 0x00080000;

    // Sent by the server in the Type 2 message to indicate that it is including a Target 
    // Information block in the message. The Target Information block is used in the 
    // calculation of the NTLMv2 response
    int FLAG_NEGOTIATE_TARGET_INFO = 0x00800000;

    // Indicates that 128-bit encryption is supported
    int FLAG_NEGOTIATE_128_BIT_ENCRYPTION = 0x20000000;

    // Indicates that the client will provide an encrypted master key in the "Session Key" 
    // field of the Type 3 message
    int FLAG_NEGOTIATE_KEY_EXCHANGE = 0x40000000;

    // Indicates that 56-bit encryption is supported
    int FLAG_NEGOTIATE_56_BIT_ENCRYPTION = 0x80000000;

    // WARN : These flags usage has not been identified
    int FLAG_UNIDENTIFIED_1 = 0x00000008;

    int FLAG_UNIDENTIFIED_2 = 0x00000100; // Negotiate Netware ??!

    int FLAG_UNIDENTIFIED_3 = 0x00000400;

    int FLAG_UNIDENTIFIED_4 = 0x00100000; // Request Init Response ??!

    int FLAG_UNIDENTIFIED_5 = 0x00200000; // Request Accept Response ??!

    int FLAG_UNIDENTIFIED_6 = 0x00400000; // Request Non-NT Session Key ??!

    int FLAG_UNIDENTIFIED_7 = 0x01000000;

    int FLAG_UNIDENTIFIED_8 = 0x02000000;

    int FLAG_UNIDENTIFIED_9 = 0x04000000;

    int FLAG_UNIDENTIFIED_10 = 0x08000000;

    int FLAG_UNIDENTIFIED_11 = 0x10000000;

    // Default minimal flag set
    int DEFAULT_FLAGS = FLAG_NEGOTIATE_OEM | FLAG_NEGOTIATE_UNICODE
            | FLAG_NEGOTIATE_WORKSTATION_SUPPLIED | FLAG_NEGOTIATE_DOMAIN_SUPPLIED;

    /** 
     * Target Information sub blocks types. It may be that there are other 
     * as-yet-unidentified sub block types as well.
     */

    // Sub block terminator
    short TARGET_INFORMATION_SUBBLOCK_TERMINATOR_TYPE = 0x0000;

    // Server name
    short TARGET_INFORMATION_SUBBLOCK_SERVER_TYPE = 0x0100;

    // Domain name
    short TARGET_INFORMATION_SUBBLOCK_DOMAIN_TYPE = 0x0200;

    // Fully-qualified DNS host name (i.e., server.domain.com)
    short TARGET_INFORMATION_SUBBLOCK_FQDNS_HOSTNAME_TYPE = 0x0300;

    // DNS domain name (i.e., domain.com)
    short TARGET_INFORMATION_SUBBLOCK_DNS_DOMAIN_NAME_TYPE = 0x0400;

    // Apparently the "parent" DNS domain for servers in sub domains
    short TARGET_INFORMATION_SUBBLOCK_PARENT_DNS_DOMAIN_NAME_TYPE = 0x0500;
}