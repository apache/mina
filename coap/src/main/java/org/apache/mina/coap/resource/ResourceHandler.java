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

import org.apache.mina.api.IoSession;
import org.apache.mina.coap.CoapMessage;

/**
 * A resource handler for a given path.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public interface ResourceHandler {

    /**
     * The path served by this resource handler.
     */
    public String getPath();

    /**
     * Detailed title for this path (or <code>null</code>). See http://datatracker.ietf.org/doc/rfc6690/
     */
    public String getTitle();

    /**
     * Interface name of this resource (or <code>null</code>), can be an URL to a WADL file. See
     * http://datatracker.ietf.org/doc/rfc6690/
     */
    public String getInterface();

    /**
     * Resource type (or <code>null</code>). See http://datatracker.ietf.org/doc/rfc6690/
     */
    public String getResourceType();

    /**
     * Generate the response for this request.
     * 
     * @param request the request to serve
     * @param session the session which receive the request
     * @return the response
     */
    public CoapResponse handle(CoapMessage request, IoSession session);
}
