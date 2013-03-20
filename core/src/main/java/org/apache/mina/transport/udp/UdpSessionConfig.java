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
package org.apache.mina.transport.udp;

import java.net.DatagramSocket;

import org.apache.mina.api.IoSessionConfig;

/**
 *  A {@link IoSessionConfig} for UDP based sessions.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public interface UdpSessionConfig extends IoSessionConfig {
    /**
     * @see DatagramSocket#getBroadcast()
     */
    boolean isBroadcast();

    /**
     * @see DatagramSocket#setBroadcast(boolean)
     */
    void setBroadcast(boolean broadcast);
}
