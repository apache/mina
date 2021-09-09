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
package org.apache.mina.filter.ssl;

import org.apache.mina.core.write.DefaultWriteRequest;
import org.apache.mina.core.write.WriteRequest;

/**
 * Specialty WriteRequest which indicates that the contents has been encrypted.
 * <p>
 * This prevents a WriteRequest from being encrypted twice and allows unwrapping
 * of these WriteRequets when dispatching the messageSent events.
 * <p>
 * Users should not create their own EncryptedWriteRequest objects.
 */
public class EncryptedWriteRequest extends DefaultWriteRequest {

	// The original message
	private WriteRequest originalRequest;

	public EncryptedWriteRequest(Object encodedMessage, WriteRequest parent) {
		super(encodedMessage, parent != null ? parent.getFuture() : null);
		this.originalRequest = parent != null ? parent : this;
	}

	public WriteRequest getOriginalRequest() {
		return this.originalRequest;
	}
}