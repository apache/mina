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
package org.apache.mina.coap.resource;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import org.apache.mina.api.IoSession;
import org.apache.mina.coap.CoapCode;
import org.apache.mina.coap.CoapMessage;
import org.apache.mina.coap.CoapOption;
import org.apache.mina.coap.CoapOptionType;
import org.apache.mina.coap.MessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The resource registry for the different {@link ResourceHandler}.
 * 
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class ResourceRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(ResourceRegistry.class);

    private Map<String, ResourceHandler> handlers = new HashMap<String, ResourceHandler>();

    /**
     * Register a new resource handler.
     * 
     * @param handler the halder to register.
     */
    public void register(ResourceHandler handler) {
        handlers.put(handler.getPath(), handler);
    }

    /**
     * Response to a request : delegate it to the correct handler for the requested path. If not found generate 4.04
     * CoAP errors. if .well-know/core requested : generate a discover page.
     * 
     * @param request the request ot serve
     * @return the response
     */
    public CoapMessage respond(CoapMessage request, IoSession session) {
        // find the URI
        StringBuilder urlBuilder = new StringBuilder("");
        for (CoapOption opt : request.getOptions()) {
            if (opt.getType() == CoapOptionType.URI_PATH) {
                if (urlBuilder.length() > 0) {
                    urlBuilder.append("/");
                }
                urlBuilder.append(new String(opt.getData()));
            }
        }

        String url = urlBuilder.toString();
        LOG.debug("requested URL : {}", url);

        if (url.length() < 1) {
            // 4.00 !
            return new CoapMessage(1, MessageType.ACK, CoapCode.BAD_REQUEST.getCode(), request.getId(),
                    request.getToken(), new CoapOption[] { new CoapOption(CoapOptionType.CONTENT_FORMAT,
                            new byte[] { 0 }) }, "no URL !".getBytes());
        }
        if (".well-known/core".equalsIgnoreCase(url)) {
            // discovery !
            return new CoapMessage(1, MessageType.ACK, CoapCode.CONTENT.getCode(), request.getId(), request.getToken(),
                    new CoapOption[] { new CoapOption(CoapOptionType.CONTENT_FORMAT, new byte[] { 40 }) },
                    getDiscovery());
        } else {
            ResourceHandler handler = handlers.get(url);
            if (handler == null) {
                // 4.04 !
                return new CoapMessage(1, MessageType.ACK, CoapCode.NOT_FOUND.getCode(), request.getId(),
                        request.getToken(), new CoapOption[] { new CoapOption(CoapOptionType.CONTENT_FORMAT,
                                new byte[] { 0 }) }, "not found !".getBytes());
            } else {
                CoapResponse response = handler.handle(request, session);
                return new CoapMessage(1, request.getType() == MessageType.CONFIRMABLE ? MessageType.ACK
                        : MessageType.NON_CONFIRMABLE, response.getCode(), request.getId(), request.getToken(),
                        response.getOptions(), response.getContent());
            }
        }
    }

    private byte[] getDiscovery() {
        StringBuilder b = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, ResourceHandler> e : handlers.entrySet()) {
            // ex :</link1>;if="If1";rt="Type1 Type2";title="Link test resource",
            if (first) {
                first = false;
            } else {
                b.append(",");
            }
            ResourceHandler h = e.getValue();
            b.append("</").append(h.getPath()).append(">");
            if (h.getInterface() != null) {
                b.append(";if=\"").append(h.getInterface()).append("\"");
            }
            if (h.getResourceType() != null) {
                b.append(";rt=\"").append(h.getResourceType()).append("\"");
            }
            if (h.getTitle() != null) {
                b.append(";title=\"").append(h.getTitle()).append("\"");
            }
        }
        try {
            return b.toString().getBytes("UTF-8");
        } catch (UnsupportedEncodingException e1) {
            throw new IllegalStateException("no UTF-8 codec", e1);
        }
    }
}
