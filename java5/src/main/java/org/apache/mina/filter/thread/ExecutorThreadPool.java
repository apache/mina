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
package org.apache.mina.filter.thread;

import java.util.concurrent.Executor;

/**
 * {@link ThreadPool} implementation that hands excecution off to an
 * {@link Executor}.  This pool doesn't manage the life cycle of the
 * underlying {@link Executor} at all.  {@link #isStarted()} will always
 * return <tt>true</tt>.  Other properties won't be supported, either.
 * Please get the underlying {@link Executor} by calling {@link #getExecutor()}
 * to adjust implementation-specific parameters.
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev: 400068 $, $Date: 2006-05-05 12:56:58Z $
 */
public class ExecutorThreadPool implements ThreadPool
{
    private final Executor executor;

    /**
     * Creates a new instance with the specified <tt>executor</tt>.
     */
    public ExecutorThreadPool( Executor executor )
    {
        this.executor = executor;
    }
    
    /**
     * Returns the underlying executor this thread pool wraps.
     */
    public Executor getExecutor()
    {
        return executor;
    }

    public void init()
    {
    }

    public void destroy()
    {
    }

    public void submit( Runnable runnable )
    {
        executor.execute( runnable );
    }

    public boolean isStarted()
    {
        return true;
    }

    public String getThreadNamePrefix()
    {
        return null;
    }

    public void setThreadNamePrefix( String threadNamePrefix )
    {
    }

    public int getPoolSize()
    {
        return -1;
    }

    public int getMaximumPoolSize()
    {
        return -1;
    }

    public int getKeepAliveTime()
    {
        return -1;
    }

    public void setMaximumPoolSize( int maximumPoolSize )
    {
    }

    public void setKeepAliveTime( int keepAliveTime )
    {
    }
}
