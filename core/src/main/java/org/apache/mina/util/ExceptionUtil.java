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

import java.io.IOException;

/**
 * Exception utility.
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class ExceptionUtil {

    public static void throwException( Throwable t ) throws IOException
    {
        if( t instanceof IOException )
        {
            throw ( IOException ) t;
        }
        else if( t instanceof RuntimeException )
        {
            throw ( RuntimeException ) t;
        }
        else if( t instanceof Error )
        {
            throw ( Error ) t;
        }
        else
        {
            throw new RuntimeException( t );
        }
    }
    
    private ExceptionUtil()
    {
    }
}
