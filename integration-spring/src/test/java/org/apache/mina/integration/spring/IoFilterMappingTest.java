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

import junit.framework.TestCase;

import org.apache.mina.common.IoFilter;
import org.easymock.MockControl;

/**
 * Tests {@link org.apache.mina.integration.spring.IoFilterMapping}.
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class IoFilterMappingTest extends TestCase
{
    public void testConstructor() throws Exception
    {
        IoFilter filter = ( IoFilter ) MockControl.createControl(
                IoFilter.class ).getMock();

        try
        {
            new IoFilterMapping( null, filter );
            fail( "null name. IllegalArgumentException expected." );
        }
        catch( IllegalArgumentException iae )
        {
        }
        try
        {
            new IoFilterMapping( "name", null );
            fail( "null filter. IllegalArgumentException expected." );
        }
        catch( IllegalArgumentException iae )
        {
        }

        IoFilterMapping mapping = new IoFilterMapping( "name", filter );
        assertEquals( "name", mapping.getName() );
        assertSame( filter, mapping.getFilter() );
    }
}
