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
package org.apache.mina.core;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.AttributeKey;

/**
 * A Netty 4 Server.
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public abstract class Netty4BenchmarkServer implements BenchmarkServer {

    protected static final AttributeKey<State> STATE_ATTRIBUTE = new AttributeKey<State>("state");

    protected static final AttributeKey<Integer> LENGTH_ATTRIBUTE = new AttributeKey<Integer>("length");

    protected static enum State {
        WAIT_FOR_FIRST_BYTE_LENGTH, WAIT_FOR_SECOND_BYTE_LENGTH, WAIT_FOR_THIRD_BYTE_LENGTH, WAIT_FOR_FOURTH_BYTE_LENGTH, READING
    }

    protected static final ByteBuf ACK = Unpooled.buffer(1);

    static {
        ACK.writeByte(0);
    }

}
