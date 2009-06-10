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
import org.apache.mina.core.future.WriteFuture;

/**
 * Callback for {@link ProtocolEncoder} to generate encoded messages such as
 * {@link IoBuffer}s.  {@link ProtocolEncoder} must call {@link #write(Object)}
 * for each encoded message.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public interface ProtocolEncoderOutput {
    /**
     * Callback for {@link ProtocolEncoder} to generate an encoded message such
     * as an {@link IoBuffer}. {@link ProtocolEncoder} must call
     * {@link #write(Object)} for each encoded message.
     *
     * @param encodedMessage the encoded message, typically an {@link IoBuffer}
     *                       or a {@link FileRegion}.
     */
    void write(Object encodedMessage);

    /**
     * Merges all buffers you wrote via {@link #write(Object)} into
     * one {@link IoBuffer} and replaces the old fragmented ones with it.
     * This method is useful when you want to control the way MINA generates
     * network packets.  Please note that this method only works when you
     * called {@link #write(Object)} method with only {@link IoBuffer}s.
     * 
     * @throws IllegalStateException if you wrote something else than {@link IoBuffer}
     */
    void mergeAll();

    /**
     * Flushes all buffers you wrote via {@link #write(Object)} to
     * the session.  This operation is asynchronous; please wait for
     * the returned {@link WriteFuture} if you want to wait for
     * the buffers flushed.
     *
     * @return <tt>null</tt> if there is nothing to flush at all.
     */
    WriteFuture flush();
}