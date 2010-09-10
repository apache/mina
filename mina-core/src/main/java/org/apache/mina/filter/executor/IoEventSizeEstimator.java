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

import org.apache.mina.core.session.IoEvent;

/**
 * Estimates the amount of memory that the specified {@link IoEvent} occupies
 * in the current JVM.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public interface IoEventSizeEstimator {
    /**
     * Estimate the IoEvent size in numberof bytes
     * @param event The event we want to estimate the size of
     * @return The estimated size of this event
     */
    int estimateSize(IoEvent event);
}
