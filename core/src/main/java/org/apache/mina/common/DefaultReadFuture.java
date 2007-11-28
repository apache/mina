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
package org.apache.mina.common;

import java.io.IOException;


/**
 * A default implementation of {@link WriteFuture}.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class DefaultReadFuture extends DefaultIoFuture implements ReadFuture {
    
    private static final Object CLOSED = new Object();
    
    /**
     * Creates a new instance.
     */
    public DefaultReadFuture(IoSession session) {
        super(session);
    }
    
    public Object getMessage() {
        if (isReady()) {
            Object v = getValue();
            if (v == CLOSED) {
                return null;
            }
            
            if (v instanceof ExceptionHolder) {
                v = ((ExceptionHolder) v).exception;
                if (v instanceof RuntimeException) {
                    throw (RuntimeException) v;
                }
                if (v instanceof Error) {
                    throw (Error) v;
                }
                if (v instanceof IOException || v instanceof Exception) {
                    throw new RuntimeIoException((Exception) v);
                }
            }
            
            return v;
        }

        return null;
    }


    public boolean isRead() {
        if (isReady()) {
            Object v = getValue();
            return (v != CLOSED && !(v instanceof ExceptionHolder));
        }
        return false;
    }
    
    public boolean isClosed() {
        if (isReady()) {
            return getValue() == CLOSED;
        }
        return false;
    }

    public Throwable getException() {
        if (isReady()) {
            Object v = getValue();
            if (v instanceof ExceptionHolder) {
                return ((ExceptionHolder) v).exception;
            }
        }
        return null;
    }

    public void setClosed() {
        setValue(CLOSED);
    }

    public void setRead(Object message) {
        setValue(message);
    }

    public void setException(Throwable cause) {
        setValue(new ExceptionHolder(cause));
    }

    @Override
    public ReadFuture await() throws InterruptedException {
        return (ReadFuture) super.await();
    }

    @Override
    public ReadFuture awaitUninterruptibly() {
        return (ReadFuture) super.awaitUninterruptibly();
    }

    @Override
    public ReadFuture addListener(IoFutureListener<?> listener) {
        return (ReadFuture) super.addListener(listener);
    }

    @Override
    public ReadFuture removeListener(IoFutureListener<?> listener) {
        return (ReadFuture) super.removeListener(listener);
    }
    
    private static class ExceptionHolder {
        private final Throwable exception;
        
        private ExceptionHolder(Throwable exception) {
            this.exception = exception;
        }
    }
}
