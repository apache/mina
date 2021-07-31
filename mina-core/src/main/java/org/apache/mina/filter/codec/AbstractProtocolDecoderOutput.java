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
package org.apache.mina.filter.codec;

import java.util.ArrayDeque;
import java.util.Queue;

import org.apache.mina.core.filterchain.IoFilter.NextFilter;
import org.apache.mina.core.session.IoSession;

/**
 * A {@link ProtocolDecoderOutput} based on queue.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public abstract class AbstractProtocolDecoderOutput implements ProtocolDecoderOutput {
	/** The queue where decoded messages are stored */
	protected final Queue<Object> messageQueue = new ArrayDeque<>();

	/**
	 * Creates a new instance of a AbstractProtocolDecoderOutput
	 */
	public AbstractProtocolDecoderOutput() {
		// Do nothing
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void write(Object message) {
		if (message == null) {
			throw new IllegalArgumentException("message");
		}

		messageQueue.add(message);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void flush(NextFilter nextFilter, IoSession session) {
		Object message = null;

		while ((message = messageQueue.poll()) != null) {
			nextFilter.messageReceived(session, message);
		}
	}
}
