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
package org.apache.mina.common;

/**
 * Represents a thread model of an {@link IoService}.  There's no essential
 * difference from {@link IoFilterChainBuilder}.  The only difference is that
 * {@link ThreadModel} is executed later than the {@link IoFilterChainBuilder}
 * you specified.  However, please don't abuse this internal behavior; it can
 * change.
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public interface ThreadModel extends IoFilterChainBuilder {
    /**
     * A {@link ThreadModel} which make MINA not manage a thread model at all.
     */
    static final ThreadModel MANUAL = new ThreadModel() {
        public void buildFilterChain(IoFilterChain chain) throws Exception {
            // Do nothing.
        }
    };
}
