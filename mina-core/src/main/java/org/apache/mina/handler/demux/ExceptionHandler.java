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
package org.apache.mina.handler.demux;

import org.apache.mina.core.session.IoSession;

/**
 * A handler interface that {@link DemuxingIoHandler} forwards
 * <code>exceptionCaught</code> events to.  You have to register your
 * handler with the type of exception you want to get notified using
 * {@link DemuxingIoHandler#addExceptionHandler(Class, ExceptionHandler)}.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public interface ExceptionHandler<E extends Throwable> {
    /**
     * A {@link ExceptionHandler} that does nothing.  This is useful when
     * you want to ignore an exception of a specific type silently.
     */
    static ExceptionHandler<Throwable> NOOP = new ExceptionHandler<Throwable>() {
        public void exceptionCaught(IoSession session, Throwable cause) {
            // Do nothing
        }
    };

    /**
     * A {@link ExceptionHandler} that closes the session immediately.
     * This is useful when you want to close the session when an exception of
     * a specific type is raised.
     */
    static ExceptionHandler<Throwable> CLOSE = new ExceptionHandler<Throwable>() {
        public void exceptionCaught(IoSession session, Throwable cause) {
            session.close(true);
        }
    };

    /**
     * Invoked when the specific type of exception is caught from the
     * specified <code>session</code>.
     */
    void exceptionCaught(IoSession session, E cause) throws Exception;
}