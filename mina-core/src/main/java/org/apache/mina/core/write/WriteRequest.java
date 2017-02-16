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
package org.apache.mina.core.write;

import java.net.SocketAddress;

import org.apache.mina.core.future.WriteFuture;

/**
 * Represents write request fired by {@link IoSession#write(Object)}.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public interface WriteRequest {
    /**
     * @return the {@link WriteRequest} which was requested originally,
     * which is not transformed by any {@link IoFilter}.
     */
    WriteRequest getOriginalRequest();

    /**
     * @return {@link WriteFuture} that is associated with this write request.
     */
    WriteFuture getFuture();

    /**
     * @return a message object to be written.
     */
    Object getMessage();

    /**
     * Returns the destination of this write request.
     *
     * @return <tt>null</tt> for the default destination
     */
    SocketAddress getDestination();

    /**
     * Tells if the current message has been encoded
     *
     * @return true if the message has already been encoded
     */
    boolean isEncoded();
}