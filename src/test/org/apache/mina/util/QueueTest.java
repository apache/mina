/*
 * @(#) $Id$
 */
package org.apache.mina.util;

import junit.framework.Assert;
import junit.framework.TestCase;

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
