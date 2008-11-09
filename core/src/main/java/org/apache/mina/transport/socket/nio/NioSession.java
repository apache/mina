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
package org.apache.mina.transport.socket.nio;

import java.nio.channels.ByteChannel;
import java.nio.channels.SelectionKey;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.session.AbstractIoSession;

/**
 * An {@link IoSession} which is managed by the NIO transport.
 *  
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public abstract class NioSession extends AbstractIoSession {
    private final List<IoFilter> filterChainIn = new CopyOnWriteArrayList<IoFilter>();
    private final List<IoFilter> filterChainOut = new CopyOnWriteArrayList<IoFilter>();

    /**
     * Stores all the filters from the common chain into the incoming and outgoing 
     * chains for the current session. The chains are copied.
     * 
     * @param filters The chain (it will be used in both direction)
     */
    public void setFilterChain(List<IoFilter> filters) {
    	if (filters == null) {
    		return;
    	}
    	
		filterChainIn.addAll(filters);
		filterChainOut.addAll(filters);
    }

    /**
     * Stores all the filters from both chains (incoming and outgoing) into 
     * the current session. The chains are copied.
     * 
     * @param filtersIn The incoming chain
     * @param filtersOut The outgoing chain
     */
    public void setFilterChain(List<IoFilter> filtersIn, List<IoFilter> filtersOut) {
    	if ((filtersIn == null) || (filtersOut == null)) {
    		return;
    	}
    	
		filterChainIn.addAll(filtersIn);
		filterChainOut.addAll(filtersOut);
    }

    /**
     * {@inheritDoc}
     */
    public IoFilter getFilterIn(int index) {
        return filterChainIn.get(index);
    }

    /**
     * {@inheritDoc}
     */
    public IoFilter getFilterOut(int index) {
        return filterChainOut.get(index);
    }

    /**
     * {@inheritDoc}
     */
    public List<IoFilter> getFilterChainIn() {
        return filterChainIn;
    }
    
    /**
     * {@inheritDoc}
     */
    public List<IoFilter> getFilterChainOut() {
    	return filterChainOut;
    }
    
    public IoFilter getFilterInHead() {
    	return filterChainIn.get(0);
    }
    
    public IoFilter getFilterOutHead() {
    	return filterChainOut.get(0);
    }

    abstract ByteChannel getChannel();
    abstract SelectionKey getSelectionKey();
    abstract void setSelectionKey(SelectionKey key);
}
