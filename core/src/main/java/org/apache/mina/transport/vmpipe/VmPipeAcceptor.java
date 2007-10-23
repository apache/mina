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
package org.apache.mina.transport.vmpipe;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoServiceConfig;
import org.apache.mina.common.IoSessionConfig;
import org.apache.mina.common.support.BaseIoAcceptor;
import org.apache.mina.common.support.BaseIoAcceptorConfig;
import org.apache.mina.common.support.BaseIoSessionConfig;
import org.apache.mina.transport.vmpipe.support.VmPipe;

/**
 * Binds the specified {@link IoHandler} to the specified
 * {@link VmPipeAddress}.
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class VmPipeAcceptor extends BaseIoAcceptor {
    static final Map<SocketAddress, VmPipe> boundHandlers = new HashMap<SocketAddress, VmPipe>();

    private static final IoSessionConfig CONFIG = new BaseIoSessionConfig() {
    };

    private final IoServiceConfig defaultConfig = new BaseIoAcceptorConfig() {
        public IoSessionConfig getSessionConfig() {
            return CONFIG;
        }
    };

    public void bind(SocketAddress address, IoHandler handler,
            IoServiceConfig config) throws IOException {
        if (handler == null)
            throw new NullPointerException("handler");
        if (address != null && !(address instanceof VmPipeAddress))
            throw new IllegalArgumentException("address must be VmPipeAddress.");

        if (config == null) {
            config = getDefaultConfig();
        }

        synchronized (boundHandlers) {
            if (address == null || ((VmPipeAddress) address).getPort() == 0) {
                for (int i = 1; i < Integer.MAX_VALUE; i++) {
                    address = new VmPipeAddress(i);
                    if (!boundHandlers.containsKey(address)) {
                        break;
                    }
                }
            } else if (boundHandlers.containsKey(address)) {
                throw new IOException("Address already bound: " + address);
            }

            boundHandlers.put(address, new VmPipe(this,
                    (VmPipeAddress) address, handler, config, getListeners()));
        }

        getListeners().fireServiceActivated(this, address, handler, config);
    }

    public void unbind(SocketAddress address) {
        if (address == null)
            throw new NullPointerException("address");

        VmPipe pipe;
        synchronized (boundHandlers) {
            if (!boundHandlers.containsKey(address)) {
                throw new IllegalArgumentException("Address not bound: "
                        + address);
            }

            pipe = boundHandlers.remove(address);
        }

        getListeners().fireServiceDeactivated(this, pipe.getAddress(),
                pipe.getHandler(), pipe.getConfig());
    }

    public void unbindAll() {
        synchronized (boundHandlers) {
            for (SocketAddress address : new ArrayList<SocketAddress>(
                    boundHandlers.keySet())) {
                unbind(address);
            }
        }
    }

    public IoServiceConfig getDefaultConfig() {
        return defaultConfig;
    }
}
