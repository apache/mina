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
package org.apache.mina.util;

/**
 * Provides methods to check preconditions.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class Assert {
    /**
     * This class is not intended to be instantiated! It has no state and
     * provides only static methods
     */
    private Assert() {
    }

    /**
     * Returns <code>value</code> if it is not <code>null</code>. Otherwise a
     * {@link IllegalArgumentException} will be thrown, the given
     * <code>parameterName</code> is included in the Message. Eg. <br>
     * <br>
     * <b>Example:</b>
     * If <code>null</code> will be passed, the message of the exception is <i>"Parameter >value< must not be null !"</i>
     * <pre>
     * public void myMethod(Integer value){
     *     <b>assertNotNull(value,"value");</b>
     *     ...
     * }
     * </pre>
     * @param <T>
     *            Type of the value
     * @param value
     *            the value to check
     * @param parameterName
     *            name of the parameter to be included in a possible
     *            {@link IllegalArgumentException}
     * @return parameter <code>value</code>
     * @exception IllegalArgumentException if <code>value==null</code>
     */
    public static <T> T assertNotNull(T value, String parameterName) {
        if (parameterName == null) {
            throw new IllegalArgumentException("You must provide a parameter name!");
        }

        if (value == null) {
            throw new IllegalArgumentException("Parameter >" + parameterName + "< must not be null!");
        }

        return value;
    }
}