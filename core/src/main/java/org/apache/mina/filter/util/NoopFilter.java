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
package org.apache.mina.filter.util;

import org.apache.mina.core.filterchain.IoFilterAdapter;

/**
 * A Noop filter. It does nothing, as all the method are already implemented
 * in the super class.<br/>
 * 
 * This class is used by tests, when some faked filter is needed to test that the 
 * chain is working properly when adding or removing a filter.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class NoopFilter extends IoFilterAdapter {
    /**
     * Default Constructor.
     */
    public NoopFilter() {
        super();
    }
}
