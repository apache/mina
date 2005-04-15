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

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * Tests {@link Queue}
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 */
public class QueueTest extends TestCase
{
    private int pushCount;
    private int popCount;
    
    public void setUp()
    {
        pushCount = 0;
        popCount = 0;
    }

    public void testRotation()
    {
        Queue q = new Queue(); // DEFAULT_CAPACITY = 4
        testRotation0( q );
    }
    
    public void testExpandingRotation()
    {
        Queue q = new Queue(); // DEFAULT_CAPACITY = 4
        for( int i = 0; i < 10; i ++ )
        {
            testRotation0( q );

            // make expansion happen
            int oldCapacity = q.capacity();
            for( int j = q.capacity(); j >= 0; j-- )
            {
                q.push( new Integer( ++pushCount ) );
            }
            
            Assert.assertTrue( q.capacity() > oldCapacity );
            testRotation0( q );
        }
    }
    
    private void testRotation0( Queue q )
    {
        for( int i = 0; i < q.capacity() * 7 / 4; i ++ )
        {
            q.push( new Integer( ++pushCount ) );
            Assert.assertEquals( ++popCount, ( ( Integer ) q.pop() ).intValue() );
        }
    }
    
    public static void main( String[] args )
    {
        junit.textui.TestRunner.run( QueueTest.class );
    }
}
