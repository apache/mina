/*
 * @(#) $Id$
 */
package org.apache.mina.util;

import java.util.List;

import junit.framework.TestCase;

/**
 * TODO Document me.
 * 
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 */
public class IoHandlerFilterManagerTest extends TestCase
{
    private IoHandlerFilterManager manager;

    private IoHandlerFilterImpl filterA;

    private IoHandlerFilterImpl filterB;

    private IoHandlerFilterImpl filterC;

    private IoHandlerFilterImpl filterD;

    private IoHandlerFilterImpl filterE;
    
    private IoHandlerFilterImpl filterX;

    public void setUp()
    {
        manager = new IoHandlerFilterManager();
        filterA = new IoHandlerFilterImpl( 'A' );
        filterB = new IoHandlerFilterImpl( 'B' );
        filterC = new IoHandlerFilterImpl( 'C' );
        filterD = new IoHandlerFilterImpl( 'D' );
        filterE = new IoHandlerFilterImpl( 'E' );
        filterX = new IoHandlerFilterImpl( 'X' );
        
        manager.addFilter( 0, false, filterA );
        manager.addFilter( -2, false, filterB );
        manager.addFilter( 2, false, filterC );
        manager.addFilter( -1, false, filterD );
        manager.addFilter( 1, false, filterE );
        manager.addFilter( 3, true, filterX );
    }

    public void testAdd()
    {
        List list;
        list = manager.getAllFilters();
        assertEquals( 5, list.size() );
        assertSame( filterC, list.get( 0 ) );
        assertSame( filterE, list.get( 1 ) );
        assertSame( filterA, list.get( 2 ) );
        assertSame( filterD, list.get( 3 ) );
        assertSame( filterB, list.get( 4 ) );

        list = manager.getAllFiltersReversed();
        assertEquals( 5, list.size() );
        assertSame( filterC, list.get( 4 ) );
        assertSame( filterE, list.get( 3 ) );
        assertSame( filterA, list.get( 2 ) );
        assertSame( filterD, list.get( 1 ) );
        assertSame( filterB, list.get( 0 ) );
    }

    public void testRemoveFirst()
    {
        manager.removeFilter( filterC );

        List list;
        list = manager.getAllFilters();
        assertEquals( 4, list.size() );
        assertSame( filterE, list.get( 0 ) );
        assertSame( filterA, list.get( 1 ) );
        assertSame( filterD, list.get( 2 ) );
        assertSame( filterB, list.get( 3 ) );

        list = manager.getAllFiltersReversed();
        assertEquals( 4, list.size() );
        assertSame( filterE, list.get( 3 ) );
        assertSame( filterA, list.get( 2 ) );
        assertSame( filterD, list.get( 1 ) );
        assertSame( filterB, list.get( 0 ) );
    }

    public void testRemoveLast()
    {
        manager.removeFilter( filterB );

        List list;
        list = manager.getAllFilters();
        assertEquals( 4, list.size() );
        assertSame( filterC, list.get( 0 ) );
        assertSame( filterE, list.get( 1 ) );
        assertSame( filterA, list.get( 2 ) );
        assertSame( filterD, list.get( 3 ) );

        list = manager.getAllFiltersReversed();
        assertEquals( 4, list.size() );
        assertSame( filterC, list.get( 3 ) );
        assertSame( filterE, list.get( 2 ) );
        assertSame( filterA, list.get( 1 ) );
        assertSame( filterD, list.get( 0 ) );
    }

    public void testRemoveMiddle()
    {
        manager.removeFilter( filterA );

        List list;
        list = manager.getAllFilters();
        assertEquals( 4, list.size() );
        assertSame( filterC, list.get( 0 ) );
        assertSame( filterE, list.get( 1 ) );
        assertSame( filterD, list.get( 2 ) );
        assertSame( filterB, list.get( 3 ) );

        list = manager.getAllFiltersReversed();
        assertEquals( 4, list.size() );
        assertSame( filterC, list.get( 3 ) );
        assertSame( filterE, list.get( 2 ) );
        assertSame( filterD, list.get( 1 ) );
        assertSame( filterB, list.get( 0 ) );
    }
    
    public void removeAll()
    {
        manager.removeAllFilters();
        assertEquals( 0, manager.getAllFilters().size() );
    }

    public static void main( String[] args )
    {
        junit.textui.TestRunner.run( IoHandlerFilterManagerTest.class );
    }
}