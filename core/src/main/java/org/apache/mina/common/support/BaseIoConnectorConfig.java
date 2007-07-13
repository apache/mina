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
package org.apache.mina.common.support;

import org.apache.mina.common.IoConnectorConfig;

/**
 * A base implementation of {@link IoConnectorConfig}.
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public abstract class BaseIoConnectorConfig extends BaseIoServiceConfig
        implements IoConnectorConfig {
    private int connectTimeout = 60; // 1 minute

    protected BaseIoConnectorConfig() {
        super();
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public long getConnectTimeoutMillis() {
        return connectTimeout * 1000L;
    }

    public void setConnectTimeout(int connectTimeout) {
        if (connectTimeout <= 0) {
            throw new IllegalArgumentException("connectTimeout: "
                    + connectTimeout);
        }
        this.connectTimeout = connectTimeout;
    }
}
