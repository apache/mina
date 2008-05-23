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
package org.apache.mina.queue;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * Tests {@link CircularQueue}
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class CircularQueueTest extends TestCase {
    private volatile int pushCount;
    private volatile int popCount;

    @Override
    public void setUp() {
        pushCount = 0;
        popCount = 0;
    }

    public void testRotation() {
        CircularQueue<Integer> q = new CircularQueue<Integer>(); // DEFAULT_CAPACITY = 4
        testRotation0(q);
    }

    public void testExpandingRotation() {
        CircularQueue<Integer> q = new CircularQueue<Integer>(); // DEFAULT_CAPACITY = 4
        for (int i = 0; i < 10; i++) {
            testRotation0(q);

            // make expansion happen
            int oldCapacity = q.capacity();
            for (int j = q.capacity(); j >= 0; j--) {
                q.offer(new Integer(++pushCount));
            }

            Assert.assertTrue(q.capacity() > oldCapacity);
            testRotation0(q);
        }
    }

    private void testRotation0(CircularQueue<Integer> q) {
        for (int i = 0; i < q.capacity() * 7 / 4; i++) {
            q.offer(new Integer(++pushCount));
            Assert.assertEquals(++popCount, q.poll().intValue());
        }
    }

    public void testExpandAndShrink() throws Exception {
        CircularQueue<Integer> q = new CircularQueue<Integer>();
        for (int i = 0; i < 1024; i ++) {
            q.offer(i);
        }

        Assert.assertEquals(1024, q.capacity());

        for (int i = 0; i < 512; i ++) {
            q.offer(i);
            q.poll();
        }

        Assert.assertEquals(2048, q.capacity());

        for (int i = 0; i < 1024; i ++) {
            q.poll();
        }

        Assert.assertEquals(4, q.capacity());
    }
}
