/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.mina.integration.xbean;


import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


/**
 * @org.apache.xbean.XBean
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class StandardThreadPool implements Executor
{
    private final ExecutorService delegate;


    public StandardThreadPool( int maxThreads )
    {
        delegate = Executors.newFixedThreadPool( maxThreads );
    }

    
    public void execute( Runnable command )
    {
        delegate.execute( command );
    }

    
    /**
     * TODO wont this hang if some tasks are sufficiently badly behaved?
     * @org.apache.xbean.DestroyMethod
     */
    public void stop()
    {
        delegate.shutdown();
        for ( ; ; )
        {
            try
            {
                if ( delegate.awaitTermination( Integer.MAX_VALUE, TimeUnit.SECONDS ) )
                {
                    break;
                }
            }
            catch ( InterruptedException e )
            {
                //ignore
            }
        }
    }
}
