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

import org.apache.mina.common.IoSession;
import org.apache.mina.common.WriteFuture;

/**
 * A default implementation of {@link WriteFuture}.
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class DefaultWriteFuture extends DefaultIoFuture implements WriteFuture {
    /**
     * Returns a new {@link DefaultWriteFuture} which is already marked as 'written'.
     */
    public static WriteFuture newWrittenFuture(IoSession session) {
        DefaultWriteFuture unwrittenFuture = new DefaultWriteFuture(session);
        unwrittenFuture.setWritten(true);
        return unwrittenFuture;
    }

    /**
     * Returns a new {@link DefaultWriteFuture} which is already marked as 'not written'.
     */
    public static WriteFuture newNotWrittenFuture(IoSession session) {
        DefaultWriteFuture unwrittenFuture = new DefaultWriteFuture(session);
        unwrittenFuture.setWritten(false);
        return unwrittenFuture;
    }

    /**
     * Creates a new instance.
     */
    public DefaultWriteFuture(IoSession session) {
        super(session);
    }

    /**
     * Creates a new instance which uses the specified object as a lock.
     */
    public DefaultWriteFuture(IoSession session, Object lock) {
        super(session, lock);
    }

    public boolean isWritten() {
        if (isReady()) {
            return (Boolean) getValue();
        } else {
            return false;
        }
    }

    public void setWritten(boolean written) {
        setValue(written ? Boolean.TRUE : Boolean.FALSE);
    }
}
