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

import org.apache.mina.common.CloseFuture;
import org.apache.mina.common.IoSession;

/**
 * A default implementation of {@link CloseFuture}.
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class DefaultCloseFuture extends DefaultIoFuture implements CloseFuture {
    /**
     * Creates a new instance.
     */
    public DefaultCloseFuture(IoSession session) {
        super(session);
    }

    /**
     * Creates a new instance which uses the specified object as a lock.
     */
    public DefaultCloseFuture(IoSession session, Object lock) {
        super(session, lock);
    }

    public boolean isClosed() {
        if (isReady()) {
            return (Boolean) getValue();
        } else {
            return false;
        }
    }

    public void setClosed() {
        setValue(Boolean.TRUE);
    }
}
