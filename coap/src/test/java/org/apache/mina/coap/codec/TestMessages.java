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
package org.apache.mina.coap.codec;

import org.apache.mina.coap.CoapMessage;
import org.apache.mina.coap.CoapOption;
import org.apache.mina.coap.CoapOptionType;
import org.apache.mina.coap.MessageType;

/**
 * Some CoAP sample messages for testing purposes.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public interface TestMessages {

    public static final CoapMessage NO_CONTENT_NO_OPTION = new CoapMessage(1, MessageType.ACK, 1, 1234, null, null,
            null);

    public static final String NO_CONTENT_NO_OPTION_HEX = "600104D2";

    public static final CoapMessage SOME_CONTENT_NO_OPTION = new CoapMessage(1, MessageType.CONFIRMABLE, 1, 1234,
            "token".getBytes(), null, "some rather large payload".getBytes());

    public static final String SOME_CONTENT_NO_OPTION_HEX = "450104D2746F6B656EFF736F6D6520726174686572206C61726765207061796C6F6164";

    public static final CoapMessage PAYLOAD_AND_ONE_OPTION = new CoapMessage(1, MessageType.NON_CONFIRMABLE, 1, 1234,
            "toto".getBytes(),
            new CoapOption[] { new CoapOption(CoapOptionType.URI_PATH, "coap://blabla".getBytes()) }, new byte[] {});

    public static final String PAYLOAD_AND_ONE_OPTION_HEX = "540104D2746F746FBD00636F61703A2F2F626C61626C61";

    public static final CoapMessage PAYLOAD_AND_MULTIPLE_OPTION = new CoapMessage(1, MessageType.NON_CONFIRMABLE, 1,
            1234, "toto".getBytes(), new CoapOption[] { //
            new CoapOption(CoapOptionType.URI_PATH, "coap://blabla".getBytes()), //
                                    new CoapOption(CoapOptionType.LOCATION_QUERY, "somewhere".getBytes()), //
                                    new CoapOption(CoapOptionType.PROXY_URI, "http://proxyuri".getBytes()), //
                                    new CoapOption(CoapOptionType.MAX_AGE, "bleh".getBytes()) }, new byte[] {});

    public static final String PAYLOAD_AND_MULTIPLE_OPTION_HEX = "540104D2746F746FBD00636F61703A2F2F626C61626C6134626C656869736F6D657768657265DD0202687474703A2F2F70726F7879757269";// "540104D2746F746FBD00636F61703A2F2F626C61626C6134626C656869736F6D657768657265DD02687474703A2F2F70726F7879757269";

    public static final CoapMessage OBSERVE = new CoapMessage(1, MessageType.CONFIRMABLE, 1, 19950, new byte[] { -28,
                            -91 }, new CoapOption[] { new CoapOption(CoapOptionType.OBSERVE, new byte[] {}),
                            new CoapOption(CoapOptionType.URI_PATH, "demo".getBytes()) }, new byte[] {});
    public static final String OBSERVE_HEX = "42014DEEE4A5605464656D6F";
}
