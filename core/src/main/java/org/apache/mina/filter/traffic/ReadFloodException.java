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
package org.apache.mina.filter.traffic;

import org.apache.mina.common.RuntimeIoException;

/**
 * An exception that is thrown by {@link ReadThrottleFilter} when
 * the buffer size grows up too much.
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class ReadFloodException extends RuntimeIoException {

    private static final long serialVersionUID = -4068832261253453437L;

    public ReadFloodException() {
        super();
    }

    public ReadFloodException(String message, Throwable cause) {
        super(message, cause);
    }

    public ReadFloodException(String message) {
        super(message);
    }

    public ReadFloodException(Throwable cause) {
        super(cause);
    }
}
