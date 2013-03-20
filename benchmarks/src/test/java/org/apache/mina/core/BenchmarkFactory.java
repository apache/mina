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
package org.apache.mina.core;

/**
 * Common interface for client and server factories
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public interface BenchmarkFactory<T> {
    /**
     * The different types of providers
     */
    public enum Type {
        Mina3_tcp, Mina3_udp, Netty3_tcp, Netty3_udp, Netty4_tcp, Netty4_udp
    }

    /**
     * Allocate a provider.
     * 
     * @param type the provider type
     * @return the allocated provider
     */
    public T get(Type type);
}
