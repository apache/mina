/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.apache.mina.util;

import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;

/**
 * Simple test that checks to see if the {@link ExpiringMap} can
 * properly clean up itself.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class ExpiringMapTest {
    private ExpiringMap<String, String> theMap;

    /**
     * Create the map, populate it and then kick off 
     * the Expirer, then sleep long enough so that the
     * Expirer can clean up the map.
     *
     * @throws Exception If the setup failed
     */
    @Before
    public void setUp() throws Exception {
        theMap = new ExpiringMap<String, String>(1, 2);
        theMap.put("Apache", "MINA");
        theMap.getExpirer().startExpiringIfNotStarted();
        Thread.sleep(3000);
    }

    /**
     * Check to see if the map has been cleaned up.
     *
     */
    @Test
    public void testGet() {
        assertNull(theMap.get("Apache"));
    }
}
