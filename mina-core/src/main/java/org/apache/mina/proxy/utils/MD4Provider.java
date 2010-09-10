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
package org.apache.mina.proxy.utils;

import java.security.Provider;

/**
 * MD4Provider.java - A security provider that only provides a MD4 implementation.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * @since MINA 2.0.0-M3
 */
public class MD4Provider extends Provider {

    /**
     * The serial version UID.
     */
    private final static long serialVersionUID = -1616816866935565456L;

    /**
     * Provider name.
     */
    public final static String PROVIDER_NAME = "MINA";

    /**
     * Provider version.
     */    
    public final static double VERSION = 1.00;

    /**
     * Provider information.
     */
    public final static String INFO = "MINA MD4 Provider v" + VERSION;

    /**
     * Default constructor that registers {@link MD4} as the <i>Service Provider 
     * Interface</i> (<b>SPI</b>) of the MD4 message digest algorithm.
     */
    public MD4Provider() {
        super(PROVIDER_NAME, VERSION, INFO);
        put("MessageDigest.MD4", MD4.class.getName());
    }
}
