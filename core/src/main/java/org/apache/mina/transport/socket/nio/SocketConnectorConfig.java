/*
 *   @(#) $Id$
 *
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.mina.transport.socket.nio;

import org.apache.mina.common.IoConnectorConfig;
import org.apache.mina.common.IoSessionConfig;
import org.apache.mina.common.RuntimeIOException;
import org.apache.mina.common.support.BaseIoConnectorConfig;

/**
 * An {@link IoConnectorConfig} for {@link SocketConnector}.
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class SocketConnectorConfig extends BaseIoConnectorConfig
{
    private IoSessionConfig sessionConfig = new SocketSessionConfigImpl();

    /**
     * Creates a new instance.
     * 
     * @throws RuntimeIOException if failed to get the default configuration
     */
    public SocketConnectorConfig()
    {
    }

    public IoSessionConfig getSessionConfig()
    {
        return sessionConfig;
    }
}
