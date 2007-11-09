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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.mina.common.IoBuffer;
import org.apache.mina.common.IoEvent;

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
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class DefaultIoEventSizeEstimator implements IoEventSizeEstimator {

    private final Map<Class<?>, Integer> class2size = new ConcurrentHashMap<Class<?>, Integer>();
    private final int averageSizePerField;
    
    public DefaultIoEventSizeEstimator() {
        this(64);
    }
    
    public DefaultIoEventSizeEstimator(int averageSizePerField) {
        if (averageSizePerField <= 0) {
            throw new IllegalArgumentException("averageSizePerField: " + averageSizePerField);
        }
        this.averageSizePerField = averageSizePerField;
        
        class2size.put(boolean.class, 4); // Probably an integer.
        class2size.put(byte.class, 1);
        class2size.put(char.class, 2);
        class2size.put(int.class, 4);
        class2size.put(long.class, 8);
        class2size.put(float.class, 4);
        class2size.put(double.class, 8);
    }
    
    public int estimateSize(IoEvent event) {
        return estimateSize((Object) event) + estimateSize(event.getParameter());
    }
    
    private int estimateSize(Object message) {
        if (message == null) {
            return 8;
        }

        if (message instanceof IoBuffer) {
            return align(46 + ((IoBuffer) message).remaining());
        }
        
        if (message instanceof CharSequence) {
            return align(38 + (((CharSequence) message).length() << 1));
        }
        
        if (message instanceof Iterable) {
            int answer = estimateSize(message.getClass());
            for (Object m: (Iterable<?>) message) {
                answer += estimateSize(m);
            }
            return answer;
        }
        
        return estimateSize(message.getClass());
    }
    
    private int estimateSize(Class<?> clazz) {
        Integer objectSize = class2size.get(clazz);
        if (objectSize != null) {
            return objectSize;
        }
        
        int answer = 8; // Basic overhead.
        synchronized (class2size) {
            
            // Get the rough estimation.
            for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
                Field[] fields = c.getDeclaredFields();
                for (Field f: fields) {
                    if ((f.getModifiers() & Modifier.STATIC) != 0) {
                        // Ignore static fields.
                        continue;
                    }
                    
                    Integer fieldSize = class2size.get(f.getType());
                    if (fieldSize == null) {
                        answer += averageSizePerField;
                    } else {
                        answer += fieldSize;
                    }
                }
            }
            
            // Put the intermediate answer to prevent infinite recursion.
            class2size.put(clazz, answer);
            
            // Now include field classes, too.
            for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
                Field[] fields = c.getDeclaredFields();
                for (Field f: fields) {
                    if ((f.getModifiers() & Modifier.STATIC) != 0) {
                        // Ignore static fields.
                        continue;
                    }
                    
                    if (!class2size.containsKey(f.getType())) {
                        // Compensate previous rough estimation
                        answer += estimateSize(f.getType()) - averageSizePerField;
                    }
                }
            }
            
            if (answer <= 0) {
                answer = averageSizePerField;
            }
            
            // Some alignment.
            answer = align(answer);
            
            // Put the final answer.
            class2size.put(clazz, answer);
        }
        
        return answer;
    }
    
    private static int align(int size) {
        if (size % 8 != 0) {
            size /= 8;
            size ++;
            size *= 8;
        }
        return size;
    }
}
