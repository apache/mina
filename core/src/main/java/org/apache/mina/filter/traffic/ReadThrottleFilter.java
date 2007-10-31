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
import org.apache.mina.common.IoSession;

/**
 * An {@link IoFilter} interface that provides access to the properties related
 * with inflow traffic control.  Please build the {@link IoFilterChain} with
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
    int getLocalMaxBufferSize();
    int getGlobalMaxBufferSize();
    void setLocalMaxBufferSize(int localMaxBufferSize);
    void setGlobalMaxBufferSize(int globalMaxBufferSize);
    int getLocalBufferSize(IoSession session);
    int getGlobalBufferSize();
    MessageSizeEstimator getMessageSizeEstimator();
}
