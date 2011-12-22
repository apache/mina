/*
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
package org.apache.mina.util;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Tests class {@link Assert}
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class AssertTest {
    /** checks Exception parameters */
    @Rule
    public final ExpectedException exception = ExpectedException.none();

    /**
     * Tests if a {@link IllegalArgumentException} is thrown when a
     * <code>null</code>-value is passed.
     * 
     * @throws Exception
     */
    @Test
    public void parameterNotNullWithNullValue() throws Exception {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Parameter >parameterName< must not be null!");
        Assert.assertNotNull(null, "parameterName");
    }

    /** Tests if a non <code>null</code> value is returned.  */
    @Test
    public void parameterNotNullWithNonNullValue() {
        Integer result = Assert.assertNotNull(123, "parameterName");
        assertThat(result, is(123));
    }

    /**
     * Tests if a {@link IllegalArgumentException} is thrown when a
     * <code>null</code>-value is passed.
     * 
     * @throws Exception
     */
    @Test
    public void parameterNotNullWithMissingParameterName() throws Exception {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("You must provide a parameter name!");
        Assert.assertNotNull("", null);
    }
}
