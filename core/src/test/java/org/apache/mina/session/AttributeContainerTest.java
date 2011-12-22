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
 */package org.apache.mina.session;

import static org.apache.mina.session.AttributeKey.createKey;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Tests the class {@link AttributeContainer}
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class AttributeContainerTest {

    /** the default value that is used for every test that calls {@link AttributeContainer#getAttribute(AttributeKey, Object)}*/
    private static final int DEFAULT_VALUE = 0;

    /** the class under test */
    private AttributeContainer container;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private static final AttributeKey<Integer> ATTRIBUTE_KEY = createKey(Integer.class, "myKey");

    @Before
    public void setUp() {
        container = new DefaultAttributeContainer();
    }

    /**
     * Test if a {@link IllegalArgumentException} is thrown when the key is
     * <code>null</code>.
     * 
     * @throws Exception
     */
    @Test
    public void setAttributeWithoutKey() throws Exception {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Parameter >key< must not be null!");
        container.setAttribute(null, DEFAULT_VALUE);
    }

    /**
     * Test if a {@link IllegalArgumentException} is thrown if illegal value is
     * set using an unsafe key.<br>
     * Expected is a value of type Integer, but passed is a Double-value using
     * an covariant key.
     * 
     * 
     * @throws Exception
     */
    @Test
    public void setAttributeWithUnsafeKey() throws Exception {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Invalid attribute value\r\n" + "  expected type: java.lang.Integer\r\n"
                + "  actual type  : java.lang.Double\r\n" + "  actual value : 12.3");

        AttributeKey<? extends Number> key = ATTRIBUTE_KEY;
        Double value = 12.3;
        container.setAttribute(key, value);
    }

    /**
     * Test if <code>null</code> is returned when an Attribute is set for the
     * first time.
     * 
     * @throws Exception
     */
    @Test
    public void setAttributeForTheFirstTime() throws Exception {
        Integer oldValue = container.setAttribute(ATTRIBUTE_KEY, 123);
        assertThat(oldValue, is(nullValue()));
    }

    /**
     * Test if the old value is returned, if the attribute was set before.
     * 
     * @throws Exception
     */
    @Test
    public void setAttributeForTheSecondTime() throws Exception {
        container.setAttribute(ATTRIBUTE_KEY, 123);
        Integer oldValue = container.setAttribute(ATTRIBUTE_KEY, 456);
        assertThat(oldValue, is(123));
    }

    /**
     * Test if the <code>null</code> value is returned, if the attribute has no
     * previous value.
     * 
     * @throws Exception
     */
    @Test
    public void setAttributeValueToNull() throws Exception {
        Integer oldValue = container.setAttribute(ATTRIBUTE_KEY, 456);

        assertThat(oldValue, is(nullValue()));
    }

    /**
     * Test if the old value is returned, if the attribute has a previous value.
     * 
     * @throws Exception
     */
    @Test
    public void setAttributeValueToNullIfPreviousValueIsAvailable() throws Exception {
        container.setAttribute(ATTRIBUTE_KEY, 123);
        Integer oldValue = container.setAttribute(ATTRIBUTE_KEY, null);

        assertThat(oldValue, is(123));
    }

    /**
     * Test if {@link IllegalArgumentException} is thrown when <code>null</code>
     * is passed.
     * 
     * @throws Exception
     */
    @Test
    public void getAttributeWithoutKey() throws Exception {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Parameter >key< must not be null!");
        container.getAttribute(null, DEFAULT_VALUE);
    }

    /**
     * Test if the default is returned if the value is not present.
     * 
     * @throws Exception
     */
    @Test
    public void getAttributeThatIsNotPresent() throws Exception {
        Integer value = container.getAttribute(ATTRIBUTE_KEY, DEFAULT_VALUE);
        assertThat(value, is(DEFAULT_VALUE));
    }

    /**
     * Test if {@link IllegalArgumentException} is thrown when <code>null</code>
     * is passed.
     * 
     * @throws Exception
     */
    @Test
    public void getAttributeThatIsPresent() throws Exception {
        container.setAttribute(ATTRIBUTE_KEY, 123);
        Integer value = container.getAttribute(ATTRIBUTE_KEY, DEFAULT_VALUE);
        assertThat(value, is(123));
    }

    /**
     * Test if write-operations on the Key-Set of
     * {@link AttributeContainer#getAttributeKeys()}, doesn't affect the
     * container it self.
     * 
     * @throws Exception
     */
    @Test
    public void getAttributeKeysAndRemoveKey() throws Exception {
        container.setAttribute(ATTRIBUTE_KEY, 123);
        Set<AttributeKey<?>> set = container.getAttributeKeys();

        try {
            set.remove(ATTRIBUTE_KEY);
        } catch (UnsupportedOperationException e) {
        }

        Integer value = container.getAttribute(ATTRIBUTE_KEY, DEFAULT_VALUE);
        assertThat(value, is(123));
    }

    /**
     * Test if a present attribute is returned after remove. 
     * @throws Exception
     */
    @Test
    public void removeAPresentAttribute() throws Exception {
        container.setAttribute(ATTRIBUTE_KEY, 123);
        Integer oldValue = container.removeAttribute(ATTRIBUTE_KEY);
        assertThat(oldValue, is(123));
    }

    /**
     * Test if a <code>null</code> is returned if no attribute is present. 
     * @throws Exception
     */
    @Test
    public void removeNonPresentAttribute() throws Exception {
        Integer oldValue = container.removeAttribute(ATTRIBUTE_KEY);
        assertThat(oldValue, is(nullValue()));
    }

    /**
     * Test if a {@link IllegalArgumentException} is thrown if a<code>null</code> key is passed. 
     * @throws Exception
     */
    @Test
    public void removeWithNullKey() throws Exception {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Parameter >key< must not be null!");
        container.removeAttribute(null);
    }
}
