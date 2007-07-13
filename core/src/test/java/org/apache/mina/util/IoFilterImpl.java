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
package org.apache.mina.util;

import org.apache.mina.common.IoFilter;
import org.apache.mina.common.IoFilterAdapter;
import org.apache.mina.common.IoFilterChain;

/**
 * Bogus implementation of {@link IoFilter} to test
 * {@link IoFilterChain}.
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class IoFilterImpl extends IoFilterAdapter {
    private final char c;

    public IoFilterImpl(char c) {
        this.c = c;
    }

    public int hashCode() {
        return c;
    }

    public boolean equals(Object o) {
        if (o == null)
            return false;
        if (!(o instanceof IoFilterImpl))
            return false;
        return this.c == ((IoFilterImpl) o).c;
    }

    public String toString() {
        return "" + c;
    }
}