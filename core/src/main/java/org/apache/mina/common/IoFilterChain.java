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

import java.util.List;

import org.apache.mina.common.IoFilter.NextFilter;

/**
 * A container of {@link IoFilter}s that forwards {@link IoHandler} events
 * to the consisting filters and terminal {@link IoHandler} sequentially.
 * Every {@link IoSession} has its own {@link IoFilterChain} (1-to-1 relationship). 
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public interface IoFilterChain {
    /**
     * Returns the parent {@link IoSession} of this chain.
     * @return {@link IoSession}
     */
    IoSession getSession();
    
    /**
     * Returns the {@link Entry} with the specified <tt>name</tt> in this chain.
     * @return <tt>null</tt> if there's no such name in this chain
     */
    Entry getEntry( String name );
    
    /**
     * Returns the {@link IoFilter} with the specified <tt>name</tt> in this chain.
     * @return <tt>null</tt> if there's no such name in this chain
     */
    IoFilter get( String name );
    
    /**
     * Returns the {@link NextFilter} of the {@link IoFilter} with the
     * specified <tt>name</tt> in this chain.
     * @return <tt>null</tt> if there's no such name in this chain
     */
    NextFilter getNextFilter( String name );
    
    /**
     * Returns the list of all {@link Entry}s this chain contains.
     */
    List getAll();
    
    /**
     * Returns the reversed list of all {@link Entry}s this chain contains.
     */
    List getAllReversed();
    
    /**
     * Returns <tt>true</tt> if this chain contains an {@link IoFilter} with the
     * specified <tt>name</tt>.
     */
    boolean contains( String name );
    
    /**
     * Returns <tt>true</tt> if this chain contains the specified <tt>filter</tt>.
     */
    boolean contains( IoFilter filter );
    
    /**
     * Returns <tt>true</tt> if this chain contains an {@link IoFilter} of the
     * specified <tt>filterType</tt>.
     */
    boolean contains( Class filterType );
    
    /**
     * Adds the specified filter with the specified name at the beginning of this chain.
     * @throws IoFilterLifeCycleException
     *             if {@link IoFilter#onPostAdd(IoFilterChain, String, NextFilter)} or
     *             {@link IoFilter#init()} throws an exception.
     */
    void addFirst( String name, IoFilter filter );

    /**
     * Adds the specified filter with the specified name at the end of this chain.
     * @throws IoFilterLifeCycleException
     *             if {@link IoFilter#onPostAdd(IoFilterChain, String, NextFilter)} or
     *             {@link IoFilter#init()} throws an exception.
     */
    void addLast( String name, IoFilter filter );

    /**
     * Adds the specified filter with the specified name just before the filter whose name is
     * <code>baseName</code> in this chain.
     * @throws IoFilterLifeCycleException
     *             if {@link IoFilter#onPostAdd(IoFilterChain, String, NextFilter)} or
     *             {@link IoFilter#init()} throws an exception.
     */
    void addBefore( String baseName, String name, IoFilter filter );

    /**
     * Adds the specified filter with the specified name just after the filter whose name is
     * <code>baseName</code> in this chain.
     * @throws IoFilterLifeCycleException
     *             if {@link IoFilter#onPostAdd(IoFilterChain, String, NextFilter)} or
     *             {@link IoFilter#init()} throws an exception.
     */
    void addAfter( String baseName, String name, IoFilter filter );

    /**
     * Removes the filter with the specified name from this chain.
     * @throws IoFilterLifeCycleException
     *             if {@link IoFilter#onPostRemove(IoFilterChain, String, NextFilter)} or
     *             {@link IoFilter#destroy()} throws an exception.
     */
    IoFilter remove( String name );

    /**
     * Removes all filters added to this chain.
     * @throws Exception if {@link IoFilter#onPostRemove(IoFilterChain, String, NextFilter)} thrown an exception.
     */
    void clear() throws Exception;
    
    /**
     * Represents a name-filter pair that an {@link IoFilterChain} contains.
     *
     * @author The Apache Directory Project (dev@directory.apache.org)
     * @version $Rev$, $Date$
     */
    public interface Entry
    {
        /**
         * Returns the name of the filter.
         */
        String getName();
        
        /**
         * Returns the filter.
         */
        IoFilter getFilter();
        
        /**
         * Returns the {@link NextFilter} of the filter.
         * 
         * @throws IllegalStateException if the {@link NextFilter} is not available
         */
        NextFilter getNextFilter();
    }
}
