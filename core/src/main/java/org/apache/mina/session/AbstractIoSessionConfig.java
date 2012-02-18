/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
/**
 * 
 */
package org.apache.mina.session;

import org.apache.mina.api.IdleStatus;
import org.apache.mina.api.IoSessionConfig;

/**
 * Base class for session configuration.
 * Implement des session configuration properties commons to all the different transports.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public abstract class AbstractIoSessionConfig implements IoSessionConfig {

    //=====================
    // idle management
    //=====================    

    private long idleTimeRead = -1;

    private long idleTimeWrite = -1;

    /**
     * {@inheritDoc}
     */
    @Override
    public long getIdleTimeInMillis(IdleStatus status) {
        switch (status) {
        case READ_IDLE:
            return idleTimeRead;
        case WRITE_IDLE:
            return idleTimeWrite;
        default:
            throw new RuntimeException("unexpected excetion, unknown idle status : " + status);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setIdleTimeInMillis(IdleStatus status, long ildeTimeInMilli) {
        switch (status) {
        case READ_IDLE:
            this.idleTimeRead = ildeTimeInMilli;
            break;
        case WRITE_IDLE:
            this.idleTimeWrite = ildeTimeInMilli;
            break;
        default:
            throw new RuntimeException("unexpected excetion, unknown idle status : " + status);
        }
    }

}
