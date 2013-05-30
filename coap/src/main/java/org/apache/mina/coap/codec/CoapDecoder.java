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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.apache.mina.coap.CoapMessage;
import org.apache.mina.coap.CoapOption;
import org.apache.mina.coap.CoapOptionType;
import org.apache.mina.coap.MessageType;
import org.apache.mina.codec.ProtocolDecoderException;
import org.apache.mina.codec.StatelessProtocolDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decoder CoAP messages from the ByteBuffer of a received UDP Datagram.
 * 
 * Decode {@link ByteBuffer} into {@link CoapMessage}.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class CoapDecoder implements StatelessProtocolDecoder<ByteBuffer, CoapMessage> {

    private static final Logger LOG = LoggerFactory.getLogger(CoapDecoder.class);

    private static final CoapOption[] EMPTY_OPTION = new CoapOption[0];

    private static final byte[] EMPTY_PAYLOAD = new byte[0];

    /**
     * {@inheritDoc}
     */
    @Override
    public Void createDecoderState() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CoapMessage decode(ByteBuffer input, Void context) {
        LOG.debug("decode");

        if (input.remaining() <= 0) {
            LOG.debug("nothing to decode");
            return null;
        }
        int byte0 = input.get() & 0xFF;
        int version = (byte0 >> 6) & 0x3;
        // LOG.debug("version : {}", version);
        MessageType type = MessageType.fromCode((byte0 >> 4) & 0x3);
        // LOG.debug("type : {}", type);
        byte[] token = new byte[byte0 & 0xF];
        int code = input.get() & 0xFF;
        // LOG.debug("code : {}", code);
        int id = input.getShort() & 0xFFFF;
        // LOG.debug("id : {}", id);
        input.get(token);

        // if (LOG.isDebugEnabled()) {
        // LOG.debug("token : {}", ByteBufferDumper.toHex(ByteBuffer.wrap(token)));
        // }

        // decode options
        int optionCode = 0;
        byte[] payload = EMPTY_PAYLOAD;
        List<CoapOption> options = new ArrayList<CoapOption>();
        while (input.hasRemaining()) {
            int next = input.get() & 0xFF;

            // start of payload ?
            if (next == 0xFF) {
                // LOG.debug("start of payload");
                payload = new byte[input.remaining()];
                input.get(payload);
                // if (LOG.isDebugEnabled()) {
                // LOG.debug("payload : {}", ByteBufferDumper.dump(ByteBuffer.wrap(payload)));
                // }
                break;
            } else {
                int optionDeltaQuartet = (next >> 4) & 0xF;

                // decode the option type
                optionCode += optionFromQuartet(optionDeltaQuartet, input);
                // LOG.debug("optionCode : {}", optionCode);

                // decode the option length
                int optionLenQuartet = next & 0x0F;
                int optionLength = optionFromQuartet(optionLenQuartet, input);
                // LOG.debug("optionLength : {}", optionLength);

                // create the option DTO
                CoapOptionType optType = CoapOptionType.fromCode(optionCode);
                if (optType == null) {
                    throw new ProtocolDecoderException("unknown option code : " + optionCode);
                }
                // LOG.debug("option type : {}", optType);

                // get the value
                byte[] optionValue = new byte[optionLength];
                input.get(optionValue);

                options.add(new CoapOption(optType, optionValue));
                ;
            }
        }

        if (input.hasRemaining()) {
            throw new ProtocolDecoderException("trailling " + input.remaining() + " bytes in the UDP datagram");
        }
        return new CoapMessage(version, type, code, id, token, options.toArray(EMPTY_OPTION), payload);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void finishDecode(Void context) {
    }

    private int optionFromQuartet(int value, ByteBuffer input) {
        // LOG.debug("quartet value : {}", value);
        if (value < 13) {
            return value;
        } else if (value == 13) {
            // if (LOG.isDebugEnabled()) {
            // int val = input.get(input.position()) & 0xFF;
            // LOG.debug("byte : {}", val);
            // }
            return (input.get() & 0xFF) + 13;
        } else if (value == 14) {
            return (input.getShort() & 0xFFFF) + 269;
        } else {
            throw new ProtocolDecoderException("illegal option quartet value : " + value);
        }
    }
}
