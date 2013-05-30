/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.mina.coap.codec;

import static org.apache.mina.coap.codec.TestMessages.*;

import java.nio.ByteBuffer;

import org.apache.mina.coap.CoapMessage;
import org.apache.mina.util.ByteBufferDumper;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for {@link CoapEncoder}
 */
public class CoapEncoderTest {

    private CoapEncoder encoder = new CoapEncoder();

    @Test
    public void no_content_no_option() {
        CoapMessage message = NO_CONTENT_NO_OPTION;
        ByteBuffer encoded = encoder.encode(message, null);
        Assert.assertEquals(NO_CONTENT_NO_OPTION_HEX, ByteBufferDumper.toHex(encoded));

    }

    @Test
    public void some_content_no_option() {
        CoapMessage message = SOME_CONTENT_NO_OPTION;
        ByteBuffer encoded = encoder.encode(message, null);

        Assert.assertEquals(SOME_CONTENT_NO_OPTION_HEX, ByteBufferDumper.toHex(encoded));

    }

    @Test
    public void payload_and_one_option() {
        CoapMessage message = PAYLOAD_AND_ONE_OPTION;
        ByteBuffer encoded = encoder.encode(message, null);
        Assert.assertEquals(PAYLOAD_AND_ONE_OPTION_HEX, ByteBufferDumper.toHex(encoded));

    }

    @Test
    public void payload_and_multiple_option() {
        CoapMessage message = PAYLOAD_AND_MULTIPLE_OPTION;
        ByteBuffer encoded = encoder.encode(message, null);
        Assert.assertEquals(PAYLOAD_AND_MULTIPLE_OPTION_HEX, ByteBufferDumper.toHex(encoded));
    }

    @Test
    public void observe() {
        CoapMessage message = OBSERVE;
        ByteBuffer encoded = encoder.encode(message, null);
        Assert.assertEquals(OBSERVE_HEX, ByteBufferDumper.toHex(encoded));
    }
}
