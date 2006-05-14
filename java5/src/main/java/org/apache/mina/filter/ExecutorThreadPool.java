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

import java.util.concurrent.Executor;

/**
 * ThreadPool implementation that hands excecution off to a Executor
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev: 400068 $, $Date: 2006-05-05 12:56:58Z $
 */
public class ExecutorThreadPool implements ThreadPool
{
    private final Executor executor;

    public ExecutorThreadPool( Executor executor )
    {
        this.executor = executor;
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
}
