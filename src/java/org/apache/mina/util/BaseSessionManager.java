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

import org.apache.mina.common.DefaultExceptionMonitor;
import org.apache.mina.common.DefaultSessionInitializer;
import org.apache.mina.common.ExceptionMonitor;
import org.apache.mina.common.SessionInitializer;
import org.apache.mina.common.SessionManager;

/**
 * Base implementation of {@link SessionManager}s.
 * 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 */
public class BaseSessionManager implements SessionManager {

    /**
     * Default session initializer.
     */
    protected SessionInitializer defaultInitializer = new DefaultSessionInitializer();

    /**
     * Current exception monitor.
     */
    protected ExceptionMonitor exceptionMonitor = new DefaultExceptionMonitor();
    
    protected BaseSessionManager()
    {
    }

    public SessionInitializer getDefaultSessionInitializer()
    {
        return defaultInitializer;
    }

    public void setDefaultSessionInitializer( SessionInitializer defaultInitializer )
    {
        if( defaultInitializer == null )
        {
            defaultInitializer = new DefaultSessionInitializer();
        }
        
        this.defaultInitializer = defaultInitializer;
    }

    public ExceptionMonitor getExceptionMonitor()
    {
        return exceptionMonitor;
    }

    public void setExceptionMonitor( ExceptionMonitor monitor )
    {
        if( monitor == null )
        {
            monitor = new DefaultExceptionMonitor();
        }

        this.exceptionMonitor = monitor;
    }
}
