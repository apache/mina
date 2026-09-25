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

package org.apache.mina.filter.ssl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;

import org.apache.mina.core.session.DummySession;
import org.junit.Test;

/**
 * Tests for the {@code SslHandler}. These tests only exercise the
 * {@link SslHandler} directly, without going through the {@link SslFilter}.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class SslHandlerTest {

    /**
     * A freshly created {@link SslHandler} must have both the {@code disabled}
     * and {@code cccEnabled} flags set to {@code false} by default.
     */
    @Test
    public void testSslHandlerFlagsDefaults() {
        final SslHandler handler = new SslHandler(null, new DummySession());

        assertFalse("A new handler should not be disabled", handler.isDisabled());
        assertFalse("A new handler should not have CCC enabled", handler.isCCCEnabled());
    }

    /**
     * Each {@link SslHandler} owns a non-null CCC lock, and different handlers
     * must not share the same lock instance.
     */
    @Test
    public void testSslHandlerCccLock() {
        final SslHandler handler1 = new SslHandler(null, new DummySession());
        final SslHandler handler2 = new SslHandler(null, new DummySession());
        assertNotNull("The CCC lock must never be null", handler1.getCccLock());
        // Distinct handlers use distinct lock objects
        assertNotSame(handler1.getCccLock(), handler2.getCccLock());
    }

    /**
     * {@link SslHandler#init()} must reset the {@code disabled} and
     * {@code cccEnabled} flags back to {@code false}.
     */
    @Test
    public void testInitResetsFlags() throws NoSuchAlgorithmException, SSLException {
        // A SslFilter is only needed here to provide the SSLContext used by init()
        final SslFilter filter = new SslFilter(SSLContext.getDefault());
        final SslHandler handler = new SslHandler(filter, new DummySession());

        handler.setDisabled(true);
        handler.setCCCEnabled(true);

        handler.init();

        assertFalse("init() must reset the disabled flag", handler.isDisabled());
        assertFalse("init() must reset the cccEnabled flag", handler.isCCCEnabled());
    }
}



