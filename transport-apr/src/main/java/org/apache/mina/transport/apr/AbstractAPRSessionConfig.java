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
package org.apache.mina.transport.apr;

import org.apache.mina.common.AbstractIoSessionConfig;
import org.apache.mina.common.IoFuture;
import org.apache.mina.common.IoSessionConfig;

/**
 * An abstract APRSessionConfig {@link APRSessionConfig}.
 *  
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev:  $, $Date:  $
 */
abstract class AbstractAPRSessionConfig extends AbstractIoSessionConfig
        implements APRSessionConfig {

    public AbstractAPRSessionConfig() {
        super();
    }

    @Override
    protected final void doSetAll(IoSessionConfig config) {
        if (config instanceof DefaultAPRSessionConfig) {
            DefaultAPRSessionConfig cfg = (DefaultAPRSessionConfig) config;
            setKeepAlive(cfg.isKeepAlive());
            setOobInline(cfg.isOobInline());
            setReceiveBufferSize(cfg.getReceiveBufferSize());
            setReuseAddress(cfg.isReuseAddress());
            setSendBufferSize(cfg.getSendBufferSize());
            setSoLinger(cfg.getSoLinger());
            setTcpNoDelay(cfg.isTcpNoDelay());
            if (getTrafficClass() != cfg.getTrafficClass()) {
                setTrafficClass(cfg.getTrafficClass());
            }
        }
    }

}