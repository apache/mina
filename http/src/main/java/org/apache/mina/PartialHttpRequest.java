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
package org.apache.mina;

import java.util.Map;


/**
 * Represents an unfinished "dummy" HTTP request, e.g, an HTTP POST request where the entire payload hasn't been 
 * received.
 * (E.g. because the size of the underlying (OS) socket's read buffer has a fixed size.)
 * 
 */

public class PartialHttpRequest extends HttpRequest {
	
	private final String requestLine;
	private String unfinishedBody;

	public PartialHttpRequest(String requestLine, Map<String, String> generalHeaders, String body) {
		super("POST <> Unfinished request\r\n", generalHeaders);
		this.requestLine = requestLine;
		this.unfinishedBody = body;
	}

	public void appendBody(String nextChunk) {
		unfinishedBody += nextChunk;
	}
	
	@Override
	public String getBody() {
		return unfinishedBody;
	}
	
	@Override
	public String getRequestLine() {
		return requestLine;
	}
	
}
