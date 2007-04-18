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
package org.apache.mina.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.mina.common.IoFilter.NextFilter;
import org.apache.mina.common.IoFilterChain.Entry;

/**
 * The default implementation of {@link IoFilterChainBuilder} which is useful
 * in most cases.  {@link DefaultIoFilterChainBuilder} has an identical interface
 * with {@link IoFilter}; it contains a list of {@link IoFilter}s that you can
 * modify. The {@link IoFilter}s which are added to this builder will be appended
 * to the {@link IoFilterChain} when {@link #buildFilterChain(IoFilterChain)} is
 * invoked.
 * <p>
 * However, the identical interface doesn't mean that it behaves in an exactly
 * same way with {@link IoFilterChain}.  {@link DefaultIoFilterChainBuilder}
 * doesn't manage the life cycle of the {@link IoFilter}s at all, and the
 * existing {@link IoSession}s won't get affected by the changes in this builder.
 * {@link IoFilterChainBuilder}s affect only newly created {@link IoSession}s.
 * 
 * <pre>
 * IoAcceptor acceptor = ...;
 * DefaultIoFilterChainBuilder builder = acceptor.getFilterChain();
 * builder.addLast( "myFilter", new MyFilter() );
 * ...
 * </pre>
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class DefaultIoFilterChainBuilder implements IoFilterChainBuilder, Cloneable
{
    private List<Entry> entries;
    private Map<String, Entry> entriesByName;
    
    /**
     * Creates a new instance with an empty filter list.
     */
    public DefaultIoFilterChainBuilder()
    {
        init();
    }
    
    private void init()
    {
        entries = new CopyOnWriteArrayList<Entry>();
        entriesByName = new HashMap<String, Entry>();
    }

    /**
     * @see IoFilterChain#getEntry(String)
     */
    public synchronized Entry getEntry( String name )
    {
        return entriesByName.get( name );
    }

    /**
     * @see IoFilterChain#get(String)
     */
    public synchronized IoFilter get( String name )
    {
        Entry e = getEntry( name );
        if( e == null )
        {
            return null;
        }
        
        return e.getFilter();
    }
    
    /**
     * @see IoFilterChain#getAll()
     */
    public List<Entry> getAll()
    {
        return new ArrayList<Entry>( entries );
    }
    
    /**
     * @see IoFilterChain#getAllReversed()
     */
    public List<Entry> getAllReversed()
    {
        List<Entry> result = getAll();
        Collections.reverse( result );
        return result;
    }

    /**
     * @see IoFilterChain#contains(String)
     */
    public boolean contains( String name )
    {
        return getEntry( name ) != null;
    }
    
    /**
     * @see IoFilterChain#contains(IoFilter)
     */
    public boolean contains( IoFilter filter )
    {
        for (Entry e : entries) {
            if( e.getFilter() == filter )
            {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * @see IoFilterChain#contains(Class)
     */
    public boolean contains( Class<? extends IoFilter> filterType )
    {
        for (Entry e : entries) {
            if( filterType.isAssignableFrom( e.getFilter().getClass() ) )
            {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * @see IoFilterChain#addFirst(String, IoFilter)
     */
    public synchronized void addFirst( String name, IoFilter filter )
    {
        register( 0, new EntryImpl( name, filter ) );
    }
    
    /**
     * @see IoFilterChain#addLast(String, IoFilter)
     */
    public synchronized void addLast( String name, IoFilter filter )
    {
        register( entries.size(), new EntryImpl( name, filter ) );
    }

    /**
     * @see IoFilterChain#addBefore(String, String, IoFilter)
     */
    public synchronized void addBefore( String baseName, String name, IoFilter filter )
    {
        checkBaseName( baseName );
        
        for( ListIterator i = entries.listIterator(); i.hasNext(); )
        {
            Entry base = ( Entry ) i.next();
            if( base.getName().equals( baseName ) )
            {
                register( i.previousIndex(), new EntryImpl( name, filter ) );
                break;
            }
        }
    }

    /**
     * @see IoFilterChain#addAfter(String, String, IoFilter)
     */
    public synchronized void addAfter( String baseName, String name, IoFilter filter )
    {
        checkBaseName( baseName );
        
        List<Entry> entries = new ArrayList<Entry>( this.entries );
        
        for( ListIterator<Entry> i = entries.listIterator(); i.hasNext(); )
        {
            Entry base = i.next();
            if( base.getName().equals( baseName ) )
            {
                register( i.nextIndex(), new EntryImpl( name, filter ) );
                break;
            }
        }
    }

    /**
     * @see IoFilterChain#remove(String)
     */
    public synchronized IoFilter remove( String name )
    {
        if( name == null )
        {
            throw new NullPointerException( "name" );
        }

        for( ListIterator<Entry> i = entries.listIterator(); i.hasNext(); )
        {
            Entry e = i.next();
            if( e.getName().equals( name ) )
            {
                deregister( i.previousIndex(), e );
                return e.getFilter();
            }
        }
        
        throw new IllegalArgumentException( "Unknown filter name: " + name );
    }

    public synchronized IoFilter replace( String name, IoFilter newFilter ) {
        checkBaseName(name);
        EntryImpl e = (EntryImpl) get(name);
        IoFilter oldFilter = e.getFilter();
        e.setFilter(newFilter);
        return oldFilter;
    }

    public synchronized void replace( IoFilter oldFilter, IoFilter newFilter ) {
        for (Entry e: entries) {
            if (e.getFilter() == oldFilter) {
                ((EntryImpl) e).setFilter(newFilter);
                return;
            }
        }
        throw new IllegalArgumentException("Filter not found: " + oldFilter.getClass().getName());
    }

    public synchronized void replace( Class<? extends IoFilter> oldFilterType, IoFilter newFilter ) {
        for (Entry e: entries) {
            if( oldFilterType.isAssignableFrom( e.getFilter().getClass() ) )
            {
                ((EntryImpl) e).setFilter(newFilter);
                return;
            }
        }
        throw new IllegalArgumentException("Filter not found: " + oldFilterType.getName());
    }

    /**
     * @see IoFilterChain#clear()
     */
    public synchronized void clear() throws Exception
    {
        entries = new ArrayList<Entry>();
        entriesByName.clear();
    }
    
    public void buildFilterChain( IoFilterChain chain ) throws Exception
    {
        for (Entry e : entries) {
            chain.addLast( e.getName(), e.getFilter() );
        }
    }
    
    @Override
    public String toString()
    {
        StringBuffer buf = new StringBuffer();
        buf.append( "{ " );
        
        boolean empty = true;
        
        for (Entry e : entries) {
            if( !empty )
            {
                buf.append( ", " );
            }
            else
            {
                empty = false;
            }
            
            buf.append( '(' );
            buf.append( e.getName() );
            buf.append( ':' );
            buf.append( e.getFilter() );
            buf.append( ')' );
        }
        
        if( empty )
        {
            buf.append( "empty" );
        }
        
        buf.append( " }" );
        
        return buf.toString();
    }
    
    @Override
    public Object clone()
    {
        DefaultIoFilterChainBuilder ret;
        try
        {
            ret = ( DefaultIoFilterChainBuilder ) super.clone();
        }
        catch( CloneNotSupportedException e )
        {
            throw ( InternalError ) new InternalError().initCause(e);
        }
        
        ret.init();
        
        for (Entry e : entries) {
            ret.addLast( e.getName(), e.getFilter() );
        }
        return ret;
    }

    private void checkBaseName( String baseName )
    {
        if( baseName == null )
        {
            throw new NullPointerException( "baseName" );
        }
        if( !entriesByName.containsKey( baseName ) )
        {
            throw new IllegalArgumentException( "Unknown filter name: " + baseName );
        }
    }

    private void register( int index, Entry e )
    {
        if( entriesByName.containsKey( e.getName() ) )
        {
            throw new IllegalArgumentException( "Other filter is using the same name: " + e.getName() );
        }

        List<Entry> newEntries = new ArrayList<Entry>( entries );
        newEntries.add( index, e );
        this.entries = newEntries;
        entriesByName.put( e.getName(), e );
    }
    
    private void deregister( int index, Entry e )
    {
        List<Entry> newEntries = new ArrayList<Entry>( entries );
        newEntries.remove( index );
        this.entries = newEntries;
        entriesByName.remove( e.getName() );
    }

    private static class EntryImpl implements Entry
    {
        private final String name;
        private IoFilter filter;
        
        private EntryImpl( String name, IoFilter filter )
        {
            if( name == null )
            {
                throw new NullPointerException( "name" );
            }
            if( filter == null )
            {
                throw new NullPointerException( "filter" );
            }
            
            this.name = name;
            this.filter = filter;
        }

        public String getName()
        {
            return name;
        }
        
        public IoFilter getFilter()
        {
            return filter;
        }
        
        private void setFilter(IoFilter filter) {
            this.filter = filter;
        }

        public NextFilter getNextFilter()
        {
            throw new IllegalStateException();
        }
        
        @Override
        public String toString()
        {
            return "(" + getName() + ':' + filter + ')';
        }
    }
}
