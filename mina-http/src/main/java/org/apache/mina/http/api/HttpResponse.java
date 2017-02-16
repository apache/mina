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
package org.apache.mina.http.api;

/**
 * An HTTP response to an HTTP request
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public interface HttpResponse extends HttpMessage {
    /**
     * The HTTP status code for the HTTP response (e.g. 200 for OK, 404 for not found, etc..)
     * 
     * @return the status of the HTTP response
     */
    HttpStatus getStatus();
}
