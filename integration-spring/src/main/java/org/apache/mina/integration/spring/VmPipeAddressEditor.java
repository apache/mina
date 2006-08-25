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
package org.apache.mina.integration.spring;

import java.beans.PropertyEditorSupport;
import java.net.SocketAddress;

import org.apache.mina.transport.vmpipe.VmPipeAddress;
import org.springframework.util.Assert;

/**
 * Java Bean {@link java.beans.PropertyEditor} which converts Strings into 
 * {@link VmPipeAddress} objects. Valid values specify an integer port number
 * optionally prefixed with a ':'. E.g.: <code>:80</code>, <code>22</code>.
 * <p>
 * Use Spring's CustomEditorConfigurer to use this property editor in a Spring
 * configuration file. See chapter 3.14 of the Spring Reference Documentation 
 * for more info.
 * </p>
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Revision$, $Date$
 * 
 * @see org.apache.mina.transport.vmpipe.VmPipeAddress
 */
public class VmPipeAddressEditor extends PropertyEditorSupport
{
    public void setAsText( String text ) throws IllegalArgumentException
    {
        setValue( parseSocketAddress( text ) );
    }
    
    private SocketAddress parseSocketAddress( String s )
    {
        Assert.notNull( s, "null SocketAddress string" );
        s = s.trim();
        if( s.startsWith( ":" ) )
        {
            s = s.substring( 1 );
        }
        try
        {
            return new VmPipeAddress( Integer.parseInt( s.trim() ) );
        }
        catch( NumberFormatException nfe )
        {
            throw new IllegalArgumentException( "Illegal vm pipe address: " + s );
        }
    }
}
