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
package org.apache.mina.session;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Date;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Tests the class {@link AttributeKey}
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class AttributeKeyTest {
    /** checks Exception parameters */
    @Rule
    public final ExpectedException exception = ExpectedException.none();

    /**
     * Tests if the constructor throws an {@link IllegalArgumentException} if
     * parameter <code>attributeType</code> is <code>null</code>.
     * 
     * @throws Exception
     */
    @Test
    public void constructorWithoutKeyType() throws Exception {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Parameter >attributeType< must not be null!");
        new AttributeKey<Number>(null, "key");
    }

    /**
     * Tests if the constructor throws an {@link IllegalArgumentException} if
     * parameter <code>attributeType</code> is <code>null</code>.
     * 
     * @throws Exception
     */
    @Test
    public void constructorWithoutKeyName() throws Exception {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Parameter >attributeName< must not be null!");
        new AttributeKey<Number>(Number.class, null);
    }

    /**
     * Test if the key-name passed into the constructor will be returned
     * 
     * @throws Exception
     */
    @Test
    public void getName() throws Exception {
        String name = new AttributeKey<Number>(Number.class, "keyName").getName();
        assertThat(name, is("keyName"));
    }

    /**
     * Test if the key-type passed into the constructor will be returned.
     * 
     * @throws Exception
     */
    @Test
    public void getType() throws Exception {
        Class<?> type = new AttributeKey<Number>(Number.class, "keyName").getType();
        assertThat(type, is((Object) Number.class));
    }

    /**
     * Test the equals method is symmetric, if two {@link AttributeKey}s  with the same attribute type and attribute-name. 
     * @throws Exception
     */
    @Test
    public void equalsSymmetric() throws Exception {
        AttributeKey<Number> key1 = new AttributeKey<Number>(Number.class, "keyName");
        AttributeKey<Number> key2 = new AttributeKey<Number>(Number.class, "keyName");

        assertThat(key1.equals(key2), is(true));
        assertThat(key2.equals(key1), is(true));
    }

    /**
     * Test if the equals method returns <code>false</code>  if the attribute type is different. 
     * @throws Exception
     */
    @Test
    public void equalsWithDifferentTypes() throws Exception {
        AttributeKey<Number> key1 = new AttributeKey<Number>(Number.class, "keyName");
        AttributeKey<Date> key2 = new AttributeKey<Date>(Date.class, "keyName");

        assertThat(key1.equals(key2), is(false));
        assertThat(key2.equals(key1), is(false));
    }

    /**
     * Test if the equals method returns <code>false</code>  if the attribute name is different. 
     * @throws Exception
     */
    @Test
    public void equalsWithDifferentName() throws Exception {
        AttributeKey<Number> key1 = new AttributeKey<Number>(Number.class, "key1");
        AttributeKey<Number> key2 = new AttributeKey<Number>(Number.class, "key2");

        assertThat(key1.equals(key2), is(false));
        assertThat(key2.equals(key1), is(false));
    }

    /**
     * Test if two {@link AttributeKey}s with the same attribute type and attribute-name have the same hashCode
     * @throws Exception
     */
    @Test
    public void hashCodeValue() throws Exception {
        AttributeKey<Number> key1 = new AttributeKey<Number>(Number.class, "keyName");
        AttributeKey<Number> key2 = new AttributeKey<Number>(Number.class, "keyName");

        assertThat(key1.hashCode(), is(key2.hashCode()));
    }
}
