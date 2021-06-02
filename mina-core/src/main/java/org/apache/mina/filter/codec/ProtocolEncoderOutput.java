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

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.file.FileRegion;

/**
 * Callback for {@link ProtocolEncoder} to generate encoded messages such as
 * {@link IoBuffer}s. {@link ProtocolEncoder} must call {@link #write(Object)}
 * for each encoded message.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public interface ProtocolEncoderOutput {
	/**
	 * Callback for {@link ProtocolEncoder} to generate an encoded message such as
	 * an {@link IoBuffer}. {@link ProtocolEncoder} must call {@link #write(Object)}
	 * for each encoded message.
	 *
	 * @param message the encoded message, typically an {@link IoBuffer} or a
	 *                {@link FileRegion}.
	 */
	void write(Object message);
}