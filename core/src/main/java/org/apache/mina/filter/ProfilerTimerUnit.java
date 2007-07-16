/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.apache.mina.filter;


/**
 * An Enumeration representing the unit of time values that can be used
 * by the ProfilerTimerFilter in order to calculate time.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public enum ProfilerTimerUnit
{
    SECONDS
    {
        public long timeNow()
        {
            return System.currentTimeMillis() / 1000;
        }


        public String getDescription()
        {
            return "seconds";
        }
    },
    MILLISECONDS
    {
        public long timeNow()
        {
            return System.currentTimeMillis();
        }


        public String getDescription()
        {
            return "milliseconds";
        }
    },
    NANOSECONDS
    {
        public long timeNow()
        {
            return System.nanoTime();
        }


        public String getDescription()
        {
            return "nanoseconds";
        }
    };

    /*
     * I was looking at possibly using the java.util.concurrent.TimeUnit
     * and I found this construct for writing enums.  Here is what the 
     * JDK developers say for why these methods below cannot be marked as
     * abstract, but should act in an abstract way...
     * 
     *     To maintain full signature compatibility with 1.5, and to improve the
     *     clarity of the generated javadoc (see 6287639: Abstract methods in
     *     enum classes should not be listed as abstract), method convert
     *     etc. are not declared abstract but otherwise act as abstract methods.
     */
    public long timeNow()
    {
        throw new AbstractMethodError();
    }


    public String getDescription()
    {
        throw new AbstractMethodError();
    }
}
