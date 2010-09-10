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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Iterator;

import org.junit.Before;
import org.junit.Test;

/**
 * Tests {@link org.apache.mina.util.CircularQueue}
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class CircularQueueTest {
    private volatile int pushCount;
    private volatile int popCount;

    @Before
    public void setUp() {
        pushCount = 0;
        popCount = 0;
    }

    @Test
    public void testRotation() {
        CircularQueue<Integer> q = new CircularQueue<Integer>(); // DEFAULT_CAPACITY = 4
        testRotation0(q);
    }

    @Test
    public void testExpandingRotation() {
        CircularQueue<Integer> q = new CircularQueue<Integer>(); // DEFAULT_CAPACITY = 4
        for (int i = 0; i < 10; i++) {
            testRotation0(q);

            // make expansion happen
            int oldCapacity = q.capacity();
            for (int j = q.capacity(); j >= 0; j--) {
                q.offer(new Integer(++pushCount));
            }

            assertTrue(q.capacity() > oldCapacity);
            testRotation0(q);
        }
    }

    private void testRotation0(CircularQueue<Integer> q) {
        for (int i = 0; i < q.capacity() * 7 / 4; i++) {
            q.offer(new Integer(++pushCount));
            assertEquals(++popCount, q.poll().intValue());
        }
    }

    @Test
    public void testRandomAddOnQueue() {
        CircularQueue<Integer> q = new CircularQueue<Integer>();
        // Create a queue with 5 elements and capacity 8;
        for (int i = 0; i < 5; i++) {
            q.offer(new Integer(i));
        }

        q.add(0, new Integer(100));
        q.add(3, new Integer(200));
        q.add(7, new Integer(300));

        Iterator<Integer> i = q.iterator();
        assertEquals(8, q.size());
        assertEquals(new Integer(100), i.next());
        assertEquals(new Integer(0), i.next());
        assertEquals(new Integer(1), i.next());
        assertEquals(new Integer(200), i.next());
        assertEquals(new Integer(2), i.next());
        assertEquals(new Integer(3), i.next());
        assertEquals(new Integer(4), i.next());
        assertEquals(new Integer(300), i.next());

        try {
            i.next();
            fail();
        } catch (Exception e) {
            // an exception signifies a successfull test case
            assertTrue(true);            
        }
    }

    @Test
    public void testRandomAddOnRotatedQueue() {
        CircularQueue<Integer> q = getRotatedQueue();

        q.add(0, new Integer(100)); // addFirst
        q.add(2, new Integer(200));
        q.add(4, new Integer(300));
        q.add(10, new Integer(400));
        q.add(12, new Integer(500)); // addLast

        Iterator<Integer> i = q.iterator();
        assertEquals(13, q.size());
        assertEquals(new Integer(100), i.next());
        assertEquals(new Integer(0), i.next());
        assertEquals(new Integer(200), i.next());
        assertEquals(new Integer(1), i.next());
        assertEquals(new Integer(300), i.next());
        assertEquals(new Integer(2), i.next());
        assertEquals(new Integer(3), i.next());
        assertEquals(new Integer(4), i.next());
        assertEquals(new Integer(5), i.next());
        assertEquals(new Integer(6), i.next());
        assertEquals(new Integer(400), i.next());
        assertEquals(new Integer(7), i.next());
        assertEquals(new Integer(500), i.next());

        try {
            i.next();
            fail();
        } catch (Exception e) {
            // an exception signifies a successfull test case
            assertTrue(true);
        }
    }

    @Test
    public void testRandomRemoveOnQueue() {
        CircularQueue<Integer> q = new CircularQueue<Integer>();

        // Create a queue with 5 elements and capacity 8;
        for (int i = 0; i < 5; i++) {
            q.offer(new Integer(i));
        }

        q.remove(0);
        q.remove(2);
        q.remove(2);

        Iterator<Integer> i = q.iterator();
        assertEquals(2, q.size());
        assertEquals(new Integer(1), i.next());
        assertEquals(new Integer(2), i.next());

        try {
            i.next();
            fail();
        } catch (Exception e) {
            // an exception signifies a successfull test case
            assertTrue(true);
        }
    }

    @Test
    public void testRandomRemoveOnRotatedQueue() {
        CircularQueue<Integer> q = getRotatedQueue();

        q.remove(0); // removeFirst
        q.remove(2); // removeLast in the first half
        q.remove(2); // removeFirst in the first half
        q.remove(4); // removeLast

        Iterator<Integer> i = q.iterator();
        assertEquals(4, q.size());
        assertEquals(new Integer(1), i.next());
        assertEquals(new Integer(2), i.next());
        assertEquals(new Integer(5), i.next());
        assertEquals(new Integer(6), i.next());

        try {
            i.next();
            fail();
        } catch (Exception e) {
            // an exception signifies a successfull test case
            assertTrue(true);            
        }
    }
    
    @Test
    public void testExpandAndShrink() throws Exception {
        CircularQueue<Integer> q = new CircularQueue<Integer>();
        for (int i = 0; i < 1024; i ++) {
            q.offer(i);
        }
        
        assertEquals(1024, q.capacity());
        
        for (int i = 0; i < 512; i ++) {
            q.offer(i);
            q.poll();
        }
        
        assertEquals(2048, q.capacity());
        
        for (int i = 0; i < 1024; i ++) { 
            q.poll();
        }
        
        assertEquals(4, q.capacity());
    }

    private CircularQueue<Integer> getRotatedQueue() {
        CircularQueue<Integer> q = new CircularQueue<Integer>();

        // Ensure capacity: 16
        for (int i = 0; i < 16; i++) {
            q.offer(new Integer(-1));
        }
        q.clear();

        // Rotate it
        for (int i = 0; i < 12; i++) {
            q.offer(new Integer(-1));
            q.poll();
        }

        // Now push items
        for (int i = 0; i < 8; i++) {
            q.offer(new Integer(i));
        }

        return q;
    }
}