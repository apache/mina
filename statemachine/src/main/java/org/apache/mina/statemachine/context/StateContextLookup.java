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
package org.apache.mina.statemachine.context;

import org.apache.mina.statemachine.event.Event;

/**
 * Lookups a {@link StateContext} from a collection of event arguments.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public interface StateContextLookup {
    /**
     * Searches the arguments from an {@link Event} and returns a 
     * {@link StateContext} if any of the arguments holds one. NOTE! This method
     * must create a new {@link StateContext} if a compatible object is in
     * the arguments and the next time that same object is passed to this
     * method the same {@link StateContext} should be returned.
     */
    StateContext lookup(Object[] eventArgs);
}
