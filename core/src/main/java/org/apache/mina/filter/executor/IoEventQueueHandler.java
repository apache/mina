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

import java.util.EventListener;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.mina.common.IoEvent;

/**
 * Listenes and filters all event queue operations occurring in
 * {@link OrderedThreadPoolExecutor} and {@link UnorderedThreadPoolExecutor}.
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public interface IoEventQueueHandler extends EventListener {
    /**
     * Returns <tt>true</tt> if and only if the specified <tt>event</tt> is
     * allowed to be offered to the event queue.  The <tt>event</tt> is dropped
     * if <tt>false</tt> is returned.
     */
    boolean accept(ThreadPoolExecutor executor, IoEvent event);
    
    /**
     * Invoked after the specified <tt>event</tt> has been offered to the
     * event queue.
     */
    void offered(ThreadPoolExecutor executor, IoEvent event);
    
    /**
     * Invoked after the specified <tt>event</tt> has been polled from the
     * event queue.
     */
    void polled(ThreadPoolExecutor executor, IoEvent event);
}
