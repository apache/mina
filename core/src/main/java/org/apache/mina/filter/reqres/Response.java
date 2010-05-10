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
package org.apache.mina.filter.reqres;

/**
 * TODO Add documentation
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class Response {
    private final Request request;

    private final ResponseType type;

    private final Object message;

    public Response(Request request, Object message, ResponseType type) {
        if (request == null) {
            throw new IllegalArgumentException("request");
        }

        if (message == null) {
            throw new IllegalArgumentException("message");
        }

        if (type == null) {
            throw new IllegalArgumentException("type");
        }

        this.request = request;
        this.type = type;
        this.message = message;
    }

    public Request getRequest() {
        return request;
    }

    public ResponseType getType() {
        return type;
    }

    public Object getMessage() {
        return message;
    }

    @Override
    public int hashCode() {
        return getRequest().getId().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (o == null) {
            return false;
        }

        if (!(o instanceof Response)) {
            return false;
        }

        Response that = (Response) o;
        if (!this.getRequest().equals(that.getRequest())) {
            return false;
        }

        return this.getType().equals(that.getType());
    }

    @Override
    public String toString() {
        return "response: { requestId=" + getRequest().getId() + ", type="
                + getType() + ", message=" + getMessage() + " }";
    }
}
