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

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.mina.io.datagram.DatagramSessionConfig;
import org.apache.mina.io.socket.SocketSessionConfig;

/**
 * A default {@link SessionInitializer} implementation that initializes
 * default socket parameters and user-defined attributes.
 * <ul>
 *   <li><tt>ReuseAddress</tt> is set to <tt>true</tt></li>
 *   <li><tt>KeepAlive</tt> is set to <tt>true</tt></li>
 *   <li>All user-defined attributes are copied to new session's attribute map.</li>
 * </ul>
 * <p>
 * All {@link SessionManager}s have this implementation as a default session
 * initializer.
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 */
public class DefaultSessionInitializer implements SessionInitializer {
    
    private final Map attributes = new HashMap();
    
    /**
     * Creates a new instance.
     */
    public DefaultSessionInitializer()
    {
    }
    
    /**
     * Returns default attribute value with the specified key.
     */
    public Object getAttribute( String key )
    {
        return attributes.get( key );
    }
    
    /**
     * Sets default attribute value with the specified key and value.
     * 
     * @return The old value
     */
    public Object setAttribute( String key, Object value )
    {
        return attributes.put( key, value );
    }
    
    /**
     * Removed default attribute value with the specified key.
     * 
     * @return The old value
     */
    public Object removeAttribute( String key )
    {
        return attributes.remove( key );
    }
    
    /**
     * Returns the set of keys of all default attributes.
     */
    public Set getAttributeKeys()
    {
        return attributes.keySet();
    }

    public void initializeSession( Session session ) throws IOException {
        
        SessionConfig config = session.getConfig();
        if( config instanceof SocketSessionConfig )
        {
            SocketSessionConfig ssc = ( SocketSessionConfig ) config;
            ssc.setReuseAddress( true );
            ssc.setKeepAlive( true );
        }
        else if( config instanceof DatagramSessionConfig )
        {
            DatagramSessionConfig dsc = ( DatagramSessionConfig ) config;
            dsc.setReuseAddress( true );
        }
        
        Iterator it = attributes.entrySet().iterator();
        while( it.hasNext() )
        {
            Entry e = ( Entry ) it.next();
            session.setAttribute( ( String ) e.getKey(), e.getValue() );
        }
    }
}
