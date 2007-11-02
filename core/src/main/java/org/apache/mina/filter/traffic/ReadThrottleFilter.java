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
package org.apache.mina.filter.traffic;

import org.apache.mina.common.IoFilter;
import org.apache.mina.common.IoFilterChain;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.executor.ExecutorFilter;

/**
 * An {@link IoFilter} interface that provides access to the properties related
 * with incoming traffic control to prevent a unwanted {@link OutOfMemoryError}
 * under heavy load.  Please build the {@link IoFilterChain} with
 * {@link ReadThrottleFilterChainBuilder} to access this filter.  Once properly
 * installed, you can access this filter using the following code:
 * <pre><code>
 * ReadThrottleFilter f = session.getFilterChain().get(ReadThrottleFilter.class);
 * int currentLocalBufferSize = f.getLocalBufferSize(session);
 * ...
 * </code></pre>
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public interface ReadThrottleFilter extends IoFilter {
    /**
     * Returns the maximum amount of data in the buffer of the {@link ExecutorFilter}
     * per {@link IoSession}.  {@code 0} means 'disabled'.
     */
    int getLocalMaxBufferSize();

    /**
     * Returns the maximum amount of data in the buffer of the {@link ExecutorFilter}
     * for all {@link IoSession} whose {@link IoFilterChain} has been configured by
     * this builder. {@code 0} means 'disabled'.
     */
    int getGlobalMaxBufferSize();

    /**
     * Sets the maximum amount of data in the buffer of the {@link ExecutorFilter}
     * per {@link IoSession}.  Specify {@code 0} or a smaller value to disable.
     */
    void setLocalMaxBufferSize(int localMaxBufferSize);
    
    /**
     * Sets the maximum amount of data in the buffer of the {@link ExecutorFilter}
     * for all {@link IoSession} whose {@link IoFilterChain} has been configured by
     * this builder. Specify {@code 0} or a smaller value to disable.
     */
    void setGlobalMaxBufferSize(int globalMaxBufferSize);

    /**
     * Returns the current amount of data in the buffer of the {@link ExecutorFilter}
     * for the specified {@link IoSession}.
     */
    int getLocalBufferSize(IoSession session);

    /**
     * Returns the current amount of data in the buffer of the {@link ExecuorFilter}
     * for all {@link IoSession} whose {@link IoFilterChain} has been configured by
     * this builder.
     */
    int getGlobalBufferSize();
    
    /**
     * Returns the size estimator currently in use.
     */
    MessageSizeEstimator getMessageSizeEstimator();
}
