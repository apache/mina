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
package org.apache.mina.filter.executor;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoEvent;
import org.apache.mina.core.write.WriteRequest;

/**
 * A default {@link IoEventSizeEstimator} implementation.
 * <p>
 * <a href="http://martin.nobilitas.com/java/sizeof.html">Martin's Java Notes</a>
 * was used for estimation.  For unknown types, it inspects declaring fields of the
 * class of the specified event and the parameter of the event.  The size of unknown
 * declaring fields are approximated to the specified <tt>averageSizePerField</tt>
 * (default: 64).
 * <p>
 * All the estimated sizes of classes are cached for performance improvement.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class DefaultIoEventSizeEstimator implements IoEventSizeEstimator {
    /** A map containing the estimated size of each Java objects we know for */
    private final ConcurrentMap<Class<?>, Integer> class2size = new ConcurrentHashMap<>();

    /**
     * Create a new instance of this class, injecting the known size of
     * basic java types.
     */
    public DefaultIoEventSizeEstimator() {
        class2size.put(boolean.class, 4); // Probably an integer.
        class2size.put(byte.class, 1);
        class2size.put(char.class, 2);
        class2size.put(int.class, 4);
        class2size.put(short.class, 2);
        class2size.put(long.class, 8);
        class2size.put(float.class, 4);
        class2size.put(double.class, 8);
        class2size.put(void.class, 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int estimateSize(IoEvent event) {
        return estimateSize((Object) event) + estimateSize(event.getParameter());
    }

    /**
     * Estimate the size of an Object in number of bytes
     * @param message The object to estimate
     * @return The estimated size of the object
     */
    public int estimateSize(Object message) {
        if (message == null) {
            return 8;
        }

        int answer = 8 + estimateSize(message.getClass(), null);

        if (message instanceof IoBuffer) {
            answer += ((IoBuffer) message).remaining();
        } else if (message instanceof WriteRequest) {
            answer += estimateSize(((WriteRequest) message).getMessage());
        } else if (message instanceof CharSequence) {
            answer += ((CharSequence) message).length() << 1;
        } else if (message instanceof Iterable) {
            for (Object m : (Iterable<?>) message) {
                answer += estimateSize(m);
            }
        }

        return align(answer);
    }

    private int estimateSize(Class<?> clazz, Set<Class<?>> visitedClasses) {
        Integer objectSize = class2size.get(clazz);
        
        if (objectSize != null) {
            return objectSize;
        }

        if (visitedClasses != null) {
            if (visitedClasses.contains(clazz)) {
                return 0;
            }
        } else {
            visitedClasses = new HashSet<>();
        }

        visitedClasses.add(clazz);

        int answer = 8; // Basic overhead.
        
        for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
            Field[] fields = c.getDeclaredFields();
            
            for (Field f : fields) {
                if ((f.getModifiers() & Modifier.STATIC) != 0) {
                    // Ignore static fields.
                    continue;
                }

                answer += estimateSize(f.getType(), visitedClasses);
            }
        }

        visitedClasses.remove(clazz);

        // Some alignment.
        answer = align(answer);

        // Put the final answer.
        Integer tmpAnswer = class2size.putIfAbsent(clazz, answer);

        if (tmpAnswer != null) {
            answer = tmpAnswer;
        }

        return answer;
    }

    private static int align(int size) {
        if (size % 8 != 0) {
            size /= 8;
            size++;
            size *= 8;
        }
        
        return size;
    }
}
