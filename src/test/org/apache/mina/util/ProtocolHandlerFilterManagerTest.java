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
public class ProtocolHandlerFilterManagerTest extends TestCase
{
    private ProtocolHandlerFilterManager manager;

    private ProtocolHandlerFilterImpl filterA;

    private ProtocolHandlerFilterImpl filterB;

    private ProtocolHandlerFilterImpl filterC;

    private ProtocolHandlerFilterImpl filterD;

    private ProtocolHandlerFilterImpl filterE;

    public void setUp()
    {
        manager = new ProtocolHandlerFilterManager();
        filterA = new ProtocolHandlerFilterImpl( 'A' );
        filterB = new ProtocolHandlerFilterImpl( 'B' );
        filterC = new ProtocolHandlerFilterImpl( 'C' );
        filterD = new ProtocolHandlerFilterImpl( 'D' );
        filterE = new ProtocolHandlerFilterImpl( 'E' );
        manager.addFilter( 0, filterA );
        manager.addFilter( -2, filterB );
        manager.addFilter( 2, filterC );
        manager.addFilter( -1, filterD );
        manager.addFilter( 1, filterE );
    }

    public void testAdd()
    {
        List list;
        list = manager.filters();
        assertEquals( 6, list.size() );
        assertSame( filterC, list.get( 0 ) );
        assertSame( filterE, list.get( 1 ) );
        assertSame( filterA, list.get( 2 ) );
        assertSame( filterD, list.get( 3 ) );
        assertSame( filterB, list.get( 4 ) );

        list = manager.filtersReversed();
        assertEquals( 6, list.size() );
        assertSame( filterC, list.get( 5 ) );
        assertSame( filterE, list.get( 4 ) );
        assertSame( filterA, list.get( 3 ) );
        assertSame( filterD, list.get( 2 ) );
        assertSame( filterB, list.get( 1 ) );
    }

    public void testRemoveFirst()
    {
        manager.removeFilter( filterC );

        List list;
        list = manager.filters();
        assertEquals( 5, list.size() );
        assertSame( filterE, list.get( 0 ) );
        assertSame( filterA, list.get( 1 ) );
        assertSame( filterD, list.get( 2 ) );
        assertSame( filterB, list.get( 3 ) );

        list = manager.filtersReversed();
        assertEquals( 5, list.size() );
        assertSame( filterE, list.get( 4 ) );
        assertSame( filterA, list.get( 3 ) );
        assertSame( filterD, list.get( 2 ) );
        assertSame( filterB, list.get( 1 ) );
    }

    public void testRemoveLast()
    {
        manager.removeFilter( filterB );

        List list;
        list = manager.filters();
        assertEquals( 5, list.size() );
        assertSame( filterC, list.get( 0 ) );
        assertSame( filterE, list.get( 1 ) );
        assertSame( filterA, list.get( 2 ) );
        assertSame( filterD, list.get( 3 ) );

        list = manager.filtersReversed();
        assertEquals( 5, list.size() );
        assertSame( filterC, list.get( 4 ) );
        assertSame( filterE, list.get( 3 ) );
        assertSame( filterA, list.get( 2 ) );
        assertSame( filterD, list.get( 1 ) );
    }

    public void testRemoveMiddle()
    {
        manager.removeFilter( filterA );

        List list;
        list = manager.filters();
        assertEquals( 5, list.size() );
        assertSame( filterC, list.get( 0 ) );
        assertSame( filterE, list.get( 1 ) );
        assertSame( filterD, list.get( 2 ) );
        assertSame( filterB, list.get( 3 ) );

        list = manager.filtersReversed();
        assertEquals( 5, list.size() );
        assertSame( filterC, list.get( 4 ) );
        assertSame( filterE, list.get( 3 ) );
        assertSame( filterD, list.get( 2 ) );
        assertSame( filterB, list.get( 1 ) );
    }

    public void removeAll()
    {
    	manager.removeAllFilters();
    	assertEquals( 1, manager.filters().size() );
    }

    public static void main( String[] args )
    {
        junit.textui.TestRunner.run( ProtocolHandlerFilterManagerTest.class );
    }
}