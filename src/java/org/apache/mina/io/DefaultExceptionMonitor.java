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
package org.apache.mina.io;

import java.lang.reflect.Method;
import java.text.DateFormat;
import java.util.Date;

/**
 * A default {@link ExceptionMonitor} implementation.  It logs uncaught
 * exceptions using <a href="http://jakarta.apache.org/commons/logging/">Apache
 * Jakarta Commons Logging</a> if available.  If not available, it prints it
 * out to {@link System#err}.
 * 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 */
public class DefaultExceptionMonitor implements ExceptionMonitor
{
    private static final Object log;

    private static final Method errorMethod;

    static
    {
        Object tempLog = null;
        Method tempErrorMethod = null;

        try
        {
            Class logCls = Class.forName( "org.apache.commons.logging.Log" );
            Class logFactoryCls = Class
                    .forName( "org.apache.commons.logging.LogFactory" );
            Method getLogMethod = logFactoryCls
                    .getMethod( "getLog", new Class[] { String.class } );
            tempLog = getLogMethod
                    .invoke( null,
                             new Object[] { DefaultExceptionMonitor.class
                                     .getPackage().getName() } );
            tempErrorMethod = logCls
                    .getMethod( "error", new Class[] { Object.class,
                                                      Throwable.class } );
        }
        catch( Exception e )
        {
            tempLog = null;
            tempErrorMethod = null;
        }

        log = tempLog;
        errorMethod = tempErrorMethod;
    }

    private final DateFormat df = DateFormat
            .getDateTimeInstance( DateFormat.MEDIUM, DateFormat.MEDIUM );

    private final Date date = new Date();

    public void exceptionCaught( Object source, Throwable cause )
    {
        if( log == null )
        {
            logToStdErr( cause );
        }
        else
        {
            logToCommonsLogging( cause );
        }
    }

    private void logToCommonsLogging( Throwable cause )
    {
        try
        {
            errorMethod.invoke( log, new Object[] { "Uncaught exception: ",
                                                   cause } );
        }
        catch( Exception e )
        {
            logToStdErr( cause );
        }
    }

    private void logToStdErr( Throwable cause )
    {
        synchronized( System.err )
        {
            date.setTime( System.currentTimeMillis() );

            System.err.print( '[' );
            System.err.print( df.format( date ) );
            System.err.print( "] [" );
            System.err.print( Thread.currentThread().getName() );
            System.err.print( "] Uncaught exception: " );
            cause.printStackTrace();
        }
    }

}