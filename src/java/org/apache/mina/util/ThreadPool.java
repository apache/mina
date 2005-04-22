/*
 *   @(#) $Id$
 *
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.mina.util;

/**
 * A generic thread pool interface.
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 */
public interface ThreadPool {
    
    /**
     * Returns the number of threads in the thread pool.
     */
    int getPoolSize();

    /**
     * Returns the maximum size of the thread pool.
     */
    int getMaximumPoolSize();

    /**
     * Returns the keep-alive time until the thread suicides after it became
     * idle (milliseconds unit).
     */
    int getKeepAliveTime();

    /**
     * Sets the maximum size of the thread pool.
     */
    void setMaximumPoolSize( int maximumPoolSize );

    /**
     * Sets the keep-alive time until the thread suicides after it became idle
     * (milliseconds unit).
     */
    void setKeepAliveTime( int keepAliveTime );

    /**
     * Starts thread pool threads and starts forwarding events to them.
     */
    void start();

    /**
     * Stops all thread pool threads.
     */
    void stop();
}
