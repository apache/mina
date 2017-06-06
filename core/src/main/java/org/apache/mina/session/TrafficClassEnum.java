/**
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

/**
 * Define the list of Trafic Class available. They are used to define the ToS (Type Of Service)
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public enum TrafficClassEnum {
    IPTOS_DEFAULT(0x00), IPTOS_LOWCOST(0x02), IPTOS_RELIABILITY(0x04), IPTOS_THROUGHPUT(0x08), IPTOS_LOWDELAY(0x10);

    /** The internal value */
    private int value;

    /**
     * The private constructor
     * @param value The interned value
     */
    private TrafficClassEnum(int value) {
        this.value = value;
    }

    /**
     * @return Get back the internal value
     */
    public int getValue() {
        return value;
    }

    /**
     * Get back the Enum value fro the integer value
     * @param value The integer value we are looking for
     * @return The Enum value
     */
    public static TrafficClassEnum valueOf(int value) {
        switch (value) {
        case 0x02:
            return IPTOS_LOWCOST;

        case 0x04:
            return IPTOS_RELIABILITY;

        case 0x08:
            return IPTOS_THROUGHPUT;

        case 0x10:
            return IPTOS_LOWDELAY;

        default:
            return IPTOS_DEFAULT;
        }
    }
}
