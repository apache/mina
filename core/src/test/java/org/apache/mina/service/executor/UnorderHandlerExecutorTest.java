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
package org.apache.mina.service.executor;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.concurrent.Executor;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

/**
 * Unit test for {@link UnorderHandlerExecutor}
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class UnorderHandlerExecutorTest {
    private UnorderHandlerExecutor handlerExecutor;

    private Executor executor;

    @Before
    public void setup() {
        executor = mock(Executor.class);
        handlerExecutor = new UnorderHandlerExecutor(executor);
    }

    @Test
    public void null_param() {
        try {
            new UnorderHandlerExecutor(null);
            Assert.fail();
        } catch (IllegalArgumentException ex) {
            // happy
        }
    }

    @Test
    public void enqueu_event() {
        // prepare
        Event e = mock(Event.class);

        // run
        handlerExecutor.execute(e);

        // verify
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).execute(captor.capture());

        captor.getValue().run();
        verify(e).visit(any(EventVisitor.class));

    }
}
