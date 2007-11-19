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
package org.apache.mina.filter.codec.http;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

import org.apache.mina.common.IoBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

/**
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class HttpResponseEncoder implements ProtocolEncoder {

    private static final Charset US_ASCII = Charset.forName("US-ASCII");
    private final CharsetEncoder asciiEncoder = US_ASCII.newEncoder();

    public void encode(IoSession session, Object message,
            ProtocolEncoderOutput out) throws Exception {
        asciiEncoder.reset();
        HttpResponse response = (HttpResponse) message;
        IoBuffer buffer = IoBuffer.allocate(512);
        buffer.setAutoExpand(true);

        encodeStatusLine(response, buffer);
        HttpCodecUtils.encodeHeaders(response, buffer, asciiEncoder);
        HttpCodecUtils.encodeBody(response, buffer);

        buffer.flip();
        out.write(buffer);
    }

    public void dispose(IoSession session) throws Exception {
    }

    /**
     * Encodes the status line of a <code>Response</code> to a specified
     * buffer.
     * The status line takes the form:<br/>
     * <pre>
     *   HTTP-Version SP Status-Code SP Reason-Phrase CRLF
     * </pre>
     *
     * @param response  The response
     * @param buffer    The buffer
     * @throws CharacterCodingException
     */
    private void encodeStatusLine(HttpResponse response, IoBuffer buffer)
            throws CharacterCodingException {
        // Write protocol version.
        buffer.putString(
                response.getProtocolVersion().toString(), asciiEncoder);
        buffer.put(HttpCodecUtils.SP);

        // Write status code.
        HttpResponseStatus status = response.getStatus();
        // TODO: Cached buffers for response codes / descriptions?
        HttpCodecUtils.appendString(buffer, String.valueOf(status.getCode()));
        buffer.put(HttpCodecUtils.SP);

        // Write reason phrase.
        HttpCodecUtils.appendString(buffer, response.getStatusReasonPhrase());
        HttpCodecUtils.appendCRLF(buffer);
    }
}
