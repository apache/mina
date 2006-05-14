/*
 *   @(#) $Id:  $
 *
 *   Copyright 2006 The Apache Software Foundation
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
package org.apache.mina.filter;

/**
 * An interface for a pool of threads, capable of handling units of work that are <code>Runnable</code>.
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev: 397284 $, $Date: 2006-04-26 20:09:11Z $
 */
public interface ThreadPool
{
    /**
     * Submit a <code>Runnable</code> to this thread pool.
     *
     * @param runnable <code>Runnable</code> to submit to this pool
     */
    void submit( Runnable runnable );

    /**
     * Initialize this pool
     */
    void init();

    /**
     * Destroy this pool.
     */
    void destroy();
}
