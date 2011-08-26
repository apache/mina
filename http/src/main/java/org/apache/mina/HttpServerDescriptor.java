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

/**
 * This class provides a possiblity to change the tunables used by Deft for the http server configuration.
 * Do not change the values unless you know what you are doing.
 */
public class HttpServerDescriptor {

	/** The number of seconds Deft will wait for subsequent socket activity before closing the connection */
	public static int KEEP_ALIVE_TIMEOUT = 30 * 1000;	// 30s
	
	/**
	 * Size of the read (receive) buffer.
	 * "Ideally, an HTTP request should not go beyond 1 packet. 
	 * The most widely used networks limit packets to approximately 1500 bytes, so if you can constrain each request 
	 * to fewer than 1500 bytes, you can reduce the overhead of the request stream." (from: http://bit.ly/bkksUu)
	 */
	public static int READ_BUFFER_SIZE = 1024;	// 1024 bytes
	
	/**
	 * Size of the write (send) buffer.
	 */
	public static int WRITE_BUFFER_SIZE = 1024;	// 1024 bytes

}
