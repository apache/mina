/*
 *   @(#) $Id$
 *
 *   Copyright 2004 The Apache Software Foundation
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
package org.apache.mina.util;

import org.apache.mina.io.IoHandlerFilter;
import org.apache.mina.io.IoHandlerFilterAdapter;
import org.apache.mina.io.IoHandlerFilterChain;

/**
 * Bogus implementation of {@link IoHandlerFilter} to test
 * {@link IoHandlerFilterChain}.
 * 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 */
public class IoHandlerFilterImpl extends IoHandlerFilterAdapter
{
    private final char c;

    public IoHandlerFilterImpl( char c )
    {
        this.c = c;
    }

    public int hashCode()
    {
        return c;
    }

    public boolean equals( Object o )
    {
        if( o == null )
            return false;
        if( ! ( o instanceof IoHandlerFilterImpl ) )
            return false;
        return this.c == ( ( IoHandlerFilterImpl ) o ).c;
    }

    public String toString()
    {
        return "" + c;
    }
}