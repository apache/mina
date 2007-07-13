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
package org.apache.mina.common;

import java.nio.channels.SelectionKey;

/**
 * A type-safe mask that is used to control the traffic of {@link IoSession}
 * with {@link IoSession#setTrafficMask(TrafficMask)}.
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class TrafficMask {
    /**
     * This mask suspends both reads and writes.
     */
    public static final TrafficMask NONE = new TrafficMask(0, "none");

    /**
     * This mask suspends writes, and resumes reads if reads were suspended.
     */
    public static final TrafficMask READ = new TrafficMask(
            SelectionKey.OP_READ, "read");

    /**
     * This mask suspends reads, and resumes writes if writes were suspended.
     */
    public static final TrafficMask WRITE = new TrafficMask(
            SelectionKey.OP_WRITE, "write");

    /**
     * This mask resumes both reads and writes if any of them were suspended.
     */
    public static final TrafficMask ALL = new TrafficMask(SelectionKey.OP_READ
            | SelectionKey.OP_WRITE, "all");

    /**
     * Returns an appropriate {@link TrafficMask} instance from the
     * specified <tt>interestOps</tt>.
     * @see SelectionKey
     */
    public static TrafficMask getInstance(int interestOps) {
        boolean read = (interestOps & SelectionKey.OP_READ) != 0;
        boolean write = (interestOps & SelectionKey.OP_WRITE) != 0;
        if (read) {
            if (write) {
                return ALL;
            } else {
                return READ;
            }
        } else if (write) {
            return WRITE;
        } else {
            return NONE;
        }
    }

    private final int interestOps;

    private final String name;

    private TrafficMask(int interestOps, String name) {
        this.interestOps = interestOps;
        this.name = name;
    }

    /**
     * Returns the name of this mask.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns <tt>true</tt> if this mask allows a read operation.
     */
    public boolean isReadable() {
        return (interestOps & SelectionKey.OP_READ) != 0;
    }

    /**
     * Returns <tt>true</tt> if this mask allows a write operation.
     */
    public boolean isWritable() {
        return (interestOps & SelectionKey.OP_WRITE) != 0;
    }

    /**
     * Returns an interestOps of {@link SelectionKey} for this mask.
     */
    public int getInterestOps() {
        return interestOps;
    }

    /**
     * Peforms an <tt>AND</tt> operation on this mask with the specified
     * <tt>mask</tt> and returns the result.
     */
    public TrafficMask and(TrafficMask mask) {
        return getInstance(interestOps & mask.interestOps);
    }

    /**
     * Peforms an <tt>OR</tt> operation on this mask with the specified
     * <tt>mask</tt> and returns the result.
     */
    public TrafficMask or(TrafficMask mask) {
        return getInstance(interestOps | mask.interestOps);
    }

    /**
     * Returns a negated mask of this one.
     */
    public TrafficMask not() {
        return getInstance(~interestOps);
    }

    /**
     * Peforms an <tt>XOR</tt> operation on this mask with the specified
     * <tt>mask</tt> and returns the result.
     */
    public TrafficMask xor(TrafficMask mask) {
        return getInstance(interestOps ^ mask.interestOps);
    }

    public String toString() {
        return name;
    }
}
