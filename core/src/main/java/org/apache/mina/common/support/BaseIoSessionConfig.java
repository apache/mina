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
package org.apache.mina.common.support;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.mina.common.IoSessionConfig;

/**
 * A base implementation of {@link IoSessionConfig}.
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public abstract class BaseIoSessionConfig implements IoSessionConfig, Cloneable
{
    private Map attributes = new HashMap();
    
    protected BaseIoSessionConfig()
    {
    }

    public Object clone()
    {
        BaseIoSessionConfig ret;
        try
        {
            ret = ( BaseIoSessionConfig ) super.clone();
            ret.attributes = new HashMap();
            ret.attributes.putAll( attributes );
        }
        catch( CloneNotSupportedException e )
        {
            throw ( InternalError ) new InternalError().initCause( e );
        }
        
        return ret;
    }
    
    public Object getAttachment()
    {
        synchronized( attributes )
        {
            return attributes.get( "" );
        }
    }

    public Object setAttachment( Object attachment )
    {
        synchronized( attributes )
        {
            return attributes.put( "", attachment );
        }
    }

    public Object getAttribute( String key )
    {
        synchronized( attributes )
        {
            return attributes.get( key );
        }
    }

    public Object setAttribute( String key, Object value )
    {
        synchronized( attributes )
        {
            return attributes.put( key, value );
        }
    }
    
    public Object setAttribute( String key )
    {
        return setAttribute( key, Boolean.TRUE );
    }
    
    public Object removeAttribute( String key )
    {
        synchronized( attributes )
        {
            return attributes.remove( key );
        }
    }
    
    public boolean containsAttribute( String key )
    {
        return getAttribute( key ) != null;
    }

    public Set getAttributeKeys()
    {
        synchronized( attributes )
        {
            return new HashSet( attributes.keySet() );
        }
    }    
}
