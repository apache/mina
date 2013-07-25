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

import java.util.Arrays;

import org.apache.mina.coap.CoapCode;
import org.apache.mina.coap.CoapOption;

/**
 * Response to a coap request.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class CoapResponse {

    private int code;
    private byte[] content;

    private CoapOption[] options;

    /**
     * Create the CoAP response for a resource request.
     * 
     * @param code the return code for this request see : {@link CoapCode}
     * @param content the content for this resource request
     * @param options the opetion to send (content-type, cacheability etc..)
     */
    public CoapResponse(int code, byte[] content, CoapOption... options) {
        super();
        this.code = code;
        this.content = content;
        this.options = options;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public CoapOption[] getOptions() {
        return options;
    }

    public void setOptions(CoapOption[] options) {
        this.options = options;
    }

    @Override
    public String toString() {
        return "CoapResponse [code=" + code + ", content=" + Arrays.toString(content) + ", options="
                + Arrays.toString(options) + "]";
    }
}
