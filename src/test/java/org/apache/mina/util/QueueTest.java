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

import java.util.Iterator;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * Tests {@link Queue}
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
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
    
    public void testRandomAddOnQueue()
    {
        Queue q = new Queue();
        // Create a queue with 5 elements and capacity 8;
        for( int i = 0; i < 5; i ++ )
        {
            q.push( new Integer( i ) );
        }
        
        q.add( 0, new Integer( 100 ) );
        q.add( 3, new Integer( 200 ) );
        q.add( 7, new Integer( 300 ) );
        
        Iterator i = q.iterator();
        Assert.assertEquals( 8, q.size() );
        Assert.assertEquals( new Integer( 100 ), i.next() );
        Assert.assertEquals( new Integer( 0 ), i.next() );
        Assert.assertEquals( new Integer( 1 ), i.next() );
        Assert.assertEquals( new Integer( 200 ), i.next() );
        Assert.assertEquals( new Integer( 2 ), i.next() );
        Assert.assertEquals( new Integer( 3 ), i.next() );
        Assert.assertEquals( new Integer( 4 ), i.next() );
        Assert.assertEquals( new Integer( 300 ), i.next() );

        try
        {
            i.next();
            Assert.fail();
        }
        catch( Exception e )
        {
            // OK
        }
    }
    
    public void testRandomAddOnRotatedQueue()
    {
        Queue q = getRotatedQueue();
        
        q.add( 0, new Integer( 100 ) );  // addFirst
        q.add( 2, new Integer( 200 ) );
        q.add( 4, new Integer( 300 ) );
        q.add( 10, new Integer( 400 ) );
        q.add( 12, new Integer( 500 ) ); // addLast
        
        Iterator i = q.iterator();
        Assert.assertEquals( 13, q.size() );
        Assert.assertEquals( new Integer( 100 ), i.next() );
        Assert.assertEquals( new Integer( 0 ), i.next() );
        Assert.assertEquals( new Integer( 200 ), i.next() );
        Assert.assertEquals( new Integer( 1 ), i.next() );
        Assert.assertEquals( new Integer( 300 ), i.next() );
        Assert.assertEquals( new Integer( 2 ), i.next() );
        Assert.assertEquals( new Integer( 3 ), i.next() );
        Assert.assertEquals( new Integer( 4 ), i.next() );
        Assert.assertEquals( new Integer( 5 ), i.next() );
        Assert.assertEquals( new Integer( 6 ), i.next() );
        Assert.assertEquals( new Integer( 400 ), i.next() );
        Assert.assertEquals( new Integer( 7 ), i.next() );
        Assert.assertEquals( new Integer( 500 ), i.next() );

        try
        {
            i.next();
            Assert.fail();
        }
        catch( Exception e )
        {
            // OK
        }
    }
    
    public void testRandomRemoveOnQueue()
    {
        Queue q = new Queue();

        // Create a queue with 5 elements and capacity 8;
        for( int i = 0; i < 5; i ++ )
        {
            q.push( new Integer( i ) );
        }
        
        q.remove( 0 );
        q.remove( 2 );
        q.remove( 2 );
        
        Iterator i = q.iterator();
        Assert.assertEquals( 2, q.size() );
        Assert.assertEquals( new Integer( 1 ), i.next() );
        Assert.assertEquals( new Integer( 2 ), i.next() );

        try
        {
            i.next();
            Assert.fail();
        }
        catch( Exception e )
        {
            // OK
        }
    }
    
    public void testRandomRemoveOnRotatedQueue()
    {
        Queue q = getRotatedQueue();
        
        q.remove( 0 ); // removeFirst
        q.remove( 2 ); // removeLast in the first half
        q.remove( 2 ); // removeFirst in the first half
        q.remove( 4 ); // removeLast
        
        Iterator i = q.iterator();
        Assert.assertEquals( 4, q.size() );
        Assert.assertEquals( new Integer( 1 ), i.next() );
        Assert.assertEquals( new Integer( 2 ), i.next() );
        Assert.assertEquals( new Integer( 5 ), i.next() );
        Assert.assertEquals( new Integer( 6 ), i.next() );

        try
        {
            i.next();
            Assert.fail();
        }
        catch( Exception e )
        {
            // OK
        }
    }
    
    private Queue getRotatedQueue()
    {
        Queue q = new Queue();
        
        // Ensure capacity: 16
        for( int i = 0; i < 16; i ++ )
        {
            q.push( new Object() );
        }
        q.clear();

        // Rotate it
        for( int i = 0; i < 12; i ++ )
        {
            q.push( new Object() );
            q.pop();
        }
        
        // Now push items
        for( int i = 0; i < 8; i ++ )
        {
            q.push( new Integer( i ) );
        }
        
        return q;
    }
    
    public static void main( String[] args )
    {
        junit.textui.TestRunner.run( QueueTest.class );
    }
}
