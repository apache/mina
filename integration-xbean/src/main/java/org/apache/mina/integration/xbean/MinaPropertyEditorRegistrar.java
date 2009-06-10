/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.mina.integration.xbean;


import java.beans.PropertyEditor;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.apache.mina.integration.beans.InetAddressEditor;
import org.apache.mina.integration.beans.InetSocketAddressEditor;
import org.apache.mina.integration.beans.VmPipeAddressEditor;
import org.apache.mina.transport.vmpipe.VmPipeAddress;
import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.beans.PropertyEditorRegistry;


/**
 * A custom Spring {@link PropertyEditorRegistrar} implementation which 
 * registers by default all the {@link PropertyEditor} implementations in the 
 * MINA Integration Beans module.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class MinaPropertyEditorRegistrar implements PropertyEditorRegistrar
{
    /**
     * Registers custom {@link PropertyEditor}s in the MINA Integration Beans
     * module.
     * 
     * Note: I did not know just how useful the rest of the property editors 
     * were or if they were redundant and replicated existing functionality of
     * default editors packaged into Spring.  So presently we're only 
     * registering editors for the following classes which are not found in
     * Spring:
     * 
     * <ul>
     *   <li>java.net.InetAddress</li>
     *   <li>java.net.InetSocketAddress</li>
     *   <li>org.apache.mina.core.session.TrafficMask</li>
     *   <li>org.apache.mina.integration.beans.VmPipeAddressEditor</li>
     * </ul>
     * 
     * @see org.springframework.beans.PropertyEditorRegistrar#
     * registerCustomEditors(org.springframework.beans.PropertyEditorRegistry)
     */
    public void registerCustomEditors( PropertyEditorRegistry registry ) 
    {
        // it is expected that new PropertyEditor instances are created
        registry.registerCustomEditor( InetAddress.class, new InetAddressEditor() );
        registry.registerCustomEditor( InetSocketAddress.class, new InetSocketAddressEditor() );
        registry.registerCustomEditor( SocketAddress.class, new InetSocketAddressEditor() );
        registry.registerCustomEditor( VmPipeAddress.class, new VmPipeAddressEditor() );
        // registry.registerCustomEditor( Boolean.class, new BooleanEditor() );
    }
}
