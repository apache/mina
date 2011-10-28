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
package org.apache.mina.http;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Map;

import org.apache.mina.api.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filterchain.WriteFilterChainController;
import org.apache.mina.http.api.HttpEndOfContent;
import org.apache.mina.http.api.HttpResponse;

public class HttpServerEncoder implements ProtocolEncoder {

    public Object encode(IoSession session, Object message, WriteFilterChainController controller) {
        if (message instanceof HttpResponse) {
            HttpResponse msg = (HttpResponse) message;
            StringBuilder sb = new StringBuilder(msg.getStatus().line());

            for (Map.Entry<String, String> header : msg.getHeaders().entrySet()) {
                sb.append(header.getKey());
                sb.append(": ");
                sb.append(header.getValue());
                sb.append("\r\n");
            }
            sb.append("\r\n");
            byte[] bytes = sb.toString().getBytes(Charset.forName("UTF-8"));
            controller.callWriteNextFilter(session, ByteBuffer.wrap(bytes));
        } else if (message instanceof ByteBuffer) {
            controller.callWriteNextFilter(session, message);
        } else if (message instanceof HttpEndOfContent) {
            // end of HTTP content
            // keep alive ?
            return null;
        }

        return null;
    }

    @Override
    public void dispose(IoSession session) throws Exception {
        // TODO Auto-generated method stub
    }
}
