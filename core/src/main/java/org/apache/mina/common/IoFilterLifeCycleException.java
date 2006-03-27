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
package org.apache.mina.common;

/**
 * A {@link RuntimeException} which is thrown when {@link IoFilter#init()}
 * or {@link IoFilter#onPostAdd(IoFilterChain, String, org.apache.mina.common.IoFilter.NextFilter)}
 * failed.
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class IoFilterLifeCycleException extends RuntimeException
{
    private static final long serialVersionUID = -5542098881633506449L;

    public IoFilterLifeCycleException()
    {
    }

    public IoFilterLifeCycleException( String message )
    {
        super( message );
    }

    public IoFilterLifeCycleException( String message, Throwable cause )
    {
        super( message, cause );
    }

    public IoFilterLifeCycleException( Throwable cause )
    {
        super( cause );
    }
}
