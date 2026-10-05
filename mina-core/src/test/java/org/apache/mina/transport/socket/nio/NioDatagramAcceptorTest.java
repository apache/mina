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
package org.apache.mina.transport.socket.nio;

import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.transport.socket.DatagramSessionConfig;
import org.apache.mina.util.AcceptorBindUtil;
import org.apache.mina.util.ExceptionMonitor;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class NioDatagramAcceptorTest {

    @Test
    public void shouldDisposeAcceptorSilentlyWhenExecutorIsShutdown() {
        AtomicReference<Throwable> exception = new AtomicReference<>();

        ExceptionMonitor.setInstance(new ExceptionMonitor() {
            @Override
            public void exceptionCaught(Throwable cause) {
                if (isFromNioDatagramAcceptor(cause)) {
                    exception.set(cause);
                }
            }
        });

        try {
            int iterationCount = 8;

            for (int i = 0; i < iterationCount; i++) {
                NioDatagramAcceptor acceptor = new NioDatagramAcceptor();
                acceptor.setHandler(new IoHandlerAdapter());

                DatagramSessionConfig sessionConfig = acceptor.getSessionConfig();
                sessionConfig.setReuseAddress(true);

                AcceptorBindUtil.tryBind(acceptor);
                acceptor.dispose(true);

                assertNull("Exception must not be thrown when disposing executor service", exception.get());
            }
        } finally {
            ExceptionMonitor.setInstance(null);
        }
    }

    @Test
    public void shouldThrowExceptionWhenThreadIsInterruptedAndServiceIsNotDisposing() {
        AtomicReference<Throwable> exception = new AtomicReference<>();

        ExceptionMonitor.setInstance(new ExceptionMonitor() {
            @Override
            public void exceptionCaught(Throwable cause) {
                if (isFromNioDatagramAcceptor(cause)) {
                    exception.set(cause);
                }
            }
        });

        try {
            int iterationCount = 8;

            for (int i = 0; i < iterationCount; i++) {
                ExecutorService executor = Executors.newCachedThreadPool();

                NioDatagramAcceptor acceptor = new NioDatagramAcceptor(executor);
                acceptor.setHandler(new IoHandlerAdapter());

                DatagramSessionConfig sessionConfig = acceptor.getSessionConfig();
                sessionConfig.setReuseAddress(true);

                AcceptorBindUtil.tryBind(acceptor);

                // interrupt acceptor threads and unbind without disposal
                executor.shutdownNow();
                acceptor.unbind();

                Throwable throwable = exception.get();

                if (throwable != null) {
                    // exception is not guaranteed to be thrown every time
                    assertTrue(throwable instanceof InterruptedException);
                    return;
                }
            }
        } finally {
            ExceptionMonitor.setInstance(null);
        }
    }

    private static boolean isFromNioDatagramAcceptor(Throwable cause) {
        for (StackTraceElement stackTraceElement : cause.getStackTrace()) {
            String className = stackTraceElement.getClassName();
            if (className.equals(NioDatagramAcceptor.class.getName())) {
                return true;
            }
        }
        return false;
    }
}
