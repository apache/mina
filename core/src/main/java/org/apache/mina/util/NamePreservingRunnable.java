/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.mina.util;

/**
 * A Runnable wrapper that preserves the name of the thread after the runnable is complete (for Runnables
 * that change the name of the Thread they use)
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev: 446581 $, $Date: 2006-09-15 11:36:12Z $,
 */
public class NamePreservingRunnable implements Runnable
{
    private final Runnable runnable;

    public NamePreservingRunnable( Runnable runnable )
    {
        this.runnable = runnable;
    }

    public void run()
    {
        String name = Thread.currentThread().getName();

        try
        {
            runnable.run();
        }
        finally
        {
            Thread.currentThread().setName( name );
        }
    }
}
