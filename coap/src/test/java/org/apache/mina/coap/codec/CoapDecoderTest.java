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

import org.apache.mina.util.ByteBufferDumper;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit test for {@link CoapDecoder}
 */
public class CoapDecoderTest {

    private CoapDecoder decoder = new CoapDecoder();

    @Test
    public void some_content_no_option() {
        Assert.assertEquals(SOME_CONTENT_NO_OPTION,
                decoder.decode(ByteBufferDumper.fromHexString(SOME_CONTENT_NO_OPTION_HEX), null));
    }

    @Test
    public void no_content_no_option() {
        Assert.assertEquals(NO_CONTENT_NO_OPTION,
                decoder.decode(ByteBufferDumper.fromHexString(NO_CONTENT_NO_OPTION_HEX), null));
    }

    @Test
    public void payload_and_one_option() {
        Assert.assertEquals(PAYLOAD_AND_ONE_OPTION,
                decoder.decode(ByteBufferDumper.fromHexString(PAYLOAD_AND_ONE_OPTION_HEX), null));
    }

    @Test
    public void payload_and_multiple_option() {
        Assert.assertEquals(PAYLOAD_AND_MULTIPLE_OPTION,
                decoder.decode(ByteBufferDumper.fromHexString(PAYLOAD_AND_MULTIPLE_OPTION_HEX), null));
    }

    @Test
    public void observe_message() {
        Assert.assertEquals(OBSERVE, decoder.decode(ByteBufferDumper.fromHexString(OBSERVE_HEX), null));
    }
}
