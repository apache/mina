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
package org.apache.mina.transport.socket.apr;

import org.apache.tomcat.jni.Library;
import org.apache.tomcat.jni.Pool;

/**
 * Internal singleton used for initializing correctly the APR native library
 * and the associated root memory pool.
 * 
 * It'll finalize nicely the native resources (libraries and memory pools).
 * 
 * Each memory pool used in the APR transport module needs to be children of the
 * root pool {@link AprLibrary#getRootPool()}.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
class AprLibrary {

    // is APR library was initialized (load of native libraries)
    private static AprLibrary library = null;

    /**
     * get the shared instance of APR library, if none, initialize one
     * @return the current APR library singleton
     */
    static synchronized AprLibrary getInstance() {
        if (!isInitialized())
            initialize();
        return library;
    }

    /**
     * initialize the APR Library by loading the associated native libraries
     * and creating the associated singleton
     */
    private static synchronized void initialize() {
        if (library == null)
            library = new AprLibrary();
    }

    /**
     * is the APR library was initialized.
     * @return true if the Library is initialized, false otherwise
     */
    static synchronized boolean isInitialized() {
        return library != null;
    }

    // APR memory pool (package wide mother pool)
    private final long pool;

    /**
     * APR library singleton constructor. Called only when accessing the
     * singleton the first time.
     * It's initializing an APR memory pool for the whole package (a.k.a mother or root pool).
     */
    private AprLibrary() {
        try {
            Library.initialize(null);
        } catch (Throwable t) {
            throw new RuntimeException("Error loading Apache Portable Runtime (APR).", t);
        }
        pool = Pool.create(0);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        Pool.destroy(pool);
    }

    /**
     * get the package wide root pool, the mother of all the pool created 
     * in APR transport module.
     * @return number identifying the root pool 
     */
    long getRootPool() {
        return pool;
    }
}
