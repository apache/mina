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
package org.apache.mina.transport.socket.nio;

import java.io.IOException;
import java.net.ServerSocket;

import org.apache.mina.common.ExceptionMonitor;
import org.apache.mina.common.IoAcceptorConfig;
import org.apache.mina.common.RuntimeIOException;
import org.apache.mina.common.support.BaseIoAcceptorConfig;

/**
 * An {@link IoAcceptorConfig} for {@link SocketAcceptor}.
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class SocketAcceptorConfig extends BaseIoAcceptorConfig {
    private SocketSessionConfig sessionConfig = new SocketSessionConfigImpl();

    private int backlog = 50;

    private boolean reuseAddress;

    /**
     * Creates a new instance.
     *
     * @throws RuntimeIOException if failed to get the default configuration
     */
    public SocketAcceptorConfig() {
        ServerSocket s = null;
        try {
            s = new ServerSocket();
            reuseAddress = s.getReuseAddress();
        } catch (IOException e) {
            throw new RuntimeIOException(
                    "Failed to get the default configuration.", e);
        } finally {
            if (s != null) {
                try {
                    s.close();
                } catch (IOException e) {
                    ExceptionMonitor.getInstance().exceptionCaught(e);
                }
            }
        }

        sessionConfig.setReuseAddress(true);
    }

    public SocketSessionConfig getSessionConfig() {
        return sessionConfig;
    }

    /**
     * @see ServerSocket#getReuseAddress()
     */
    public boolean isReuseAddress() {
        return reuseAddress;
    }

    /**
     * @see ServerSocket#setReuseAddress(boolean)
     */
    public void setReuseAddress(boolean reuseAddress) {
        this.reuseAddress = reuseAddress;
    }

    public int getBacklog() {
        return backlog;
    }

    public void setBacklog(int backlog) {
        this.backlog = backlog;
    }
}
