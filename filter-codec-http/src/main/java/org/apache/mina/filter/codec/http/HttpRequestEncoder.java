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

import java.net.URI;
import java.nio.charset.CharsetEncoder;

import org.apache.mina.common.IoBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoderAdapter;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

/**
 * TODO HttpRequestEncoder.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class HttpRequestEncoder extends ProtocolEncoderAdapter {

    private final CharsetEncoder asciiEncoder =
        HttpCodecUtils.US_ASCII_CHARSET.newEncoder();

    public HttpRequestEncoder() {
    }

    public void encode(
            IoSession session, Object message,
            ProtocolEncoderOutput out) throws Exception {
        
        asciiEncoder.reset();
        HttpRequest req = (HttpRequest) message;
        IoBuffer buf = IoBuffer.allocate(256).setAutoExpand(true);

        // Write request line.
        buf.putString(req.getMethod().toString(), asciiEncoder);
        buf.put((byte) ' ');
        
        URI uri = req.getRequestUri();
        buf.putString(uri.getPath(), asciiEncoder);
        String query = uri.getQuery();
        if (query != null && query.length() > 0) {
            buf.put((byte) '?');
            buf.putString(query, asciiEncoder);
        }
        buf.putString(" HTTP/1.1", asciiEncoder);
        HttpCodecUtils.appendCRLF(buf);

        HttpCodecUtils.encodeHeaders(req, buf, asciiEncoder);
        HttpCodecUtils.encodeBody(req, buf);

        buf.flip();
        out.write(buf);
    }
}
