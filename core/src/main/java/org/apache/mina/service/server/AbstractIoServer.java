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
package org.apache.mina.service.server;

import org.apache.mina.api.IoServer;
import org.apache.mina.service.AbstractIoService;

/**
 * Base implementation for {@link IoServer}s.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public abstract class AbstractIoServer extends AbstractIoService implements IoServer {
	
    /**
     * Create an new AbstractIoServer instance
     */
    protected AbstractIoServer() {
        super();
    }
    
    // does the reuse address flag should be positioned
    private boolean reuseAddress = false;
    
    /**
     * Set the reuse address flag on the server socket
     * @param reuseAddress <code>true</code> to enable
     */
    public void setReuseAddress(final boolean reuseAddress) {
        this.reuseAddress = reuseAddress;
    }

    /**
     * Is the reuse address enabled for this server.
     * @return
     */
    public boolean isReuseAddress() {
        return this.reuseAddress;
    }

}
