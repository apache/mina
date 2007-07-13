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
package org.apache.mina.handler.demux;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.util.IdentityHashSet;

/**
 * A {@link IoHandler} that demuxes <code>messageReceived</code> events
 * to the appropriate {@link MessageHandler}.
 * <p>
 * You can freely register and deregister {@link MessageHandler}s using
 * {@link #addMessageHandler(Class, MessageHandler)} and
 * {@link #removeMessageHandler(Class)}.
 * </p>
 * <p>
 * When <code>message</code> is received through a call to 
 * {@link #messageReceived(IoSession, Object)} the class of the 
 * <code>message</code> object will be used to find a {@link MessageHandler} for 
 * that particular message type. If no {@link MessageHandler} instance can be 
 * found for the immediate class (i.e. <code>message.getClass()</code>) the 
 * interfaces implemented by the immediate class will be searched in depth-first 
 * order. If no match can be found for any of the interfaces the search will be 
 * repeated recursively for the superclass of the immediate class 
 * (i.e. <code>message.getClass().getSuperclass()</code>).
 * </p>
 * <p>
 * Consider the following type hierarchy (<code>Cx</code> are classes while 
 * <code>Ix</code> are interfaces):
 * <pre>
 *     C3 - I7 - I9
 *      |    |   /\
 *      |   I8  I3 I4
 *      |
 *     C2 - I5 - I6
 *      |
 *     C1 - I1 - I2 - I4
 *      |         |
 *      |        I3
 *    Object          
 * </pre>
 * When <code>message</code> is of type <code>C3</code> this hierarchy will be 
 * searched in the following order:
 * <code>C3, I7, I8, I9, I3, I4, C2, I5, I6, C1, I1, I2, I3, I4, Object</code>.
 * </p>
 * <p>
 * For efficiency searches will be cached. Calls to 
 * {@link #addMessageHandler(Class, MessageHandler)} and
 * {@link #removeMessageHandler(Class)} clear this cache.
 * </p>
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class DemuxingIoHandler extends IoHandlerAdapter {
    private final Map<Class, MessageHandler> findHandlerCache = new ConcurrentHashMap<Class, MessageHandler>();

    private final Map<Class, MessageHandler> type2handler = new ConcurrentHashMap<Class, MessageHandler>();

    /**
     * Creates a new instance with no registered {@link MessageHandler}s.
     */
    public DemuxingIoHandler() {
    }

    /**
     * Registers a {@link MessageHandler} that receives the messages of
     * the specified <code>type</code>.
     * 
     * @return the old handler if there is already a registered handler for
     *         the specified <tt>type</tt>.  <tt>null</tt> otherwise.
     */
    @SuppressWarnings("unchecked")
    public <E> MessageHandler<? super E> addMessageHandler(Class<E> type,
            MessageHandler<? super E> handler) {
        findHandlerCache.clear();
        return type2handler.put(type, handler);
    }

    /**
     * Deregisters a {@link MessageHandler} that receives the messages of
     * the specified <code>type</code>.
     * 
     * @return the removed handler if successfully removed.  <tt>null</tt> otherwise.
     */
    @SuppressWarnings("unchecked")
    public <E> MessageHandler<? super E> removeMessageHandler(Class<E> type) {
        findHandlerCache.clear();
        return type2handler.remove(type);
    }

    /**
     * Returns the {@link MessageHandler} which is registered to process
     * the specified <code>type</code>. 
     */
    @SuppressWarnings("unchecked")
    public <E> MessageHandler<? super E> getMessageHandler(Class<E> type) {
        return type2handler.get(type);
    }

    /**
     * Returns the {@link Map} which contains all messageType-{@link MessageHandler}
     * pairs registered to this handler.
     */
    public Map<Class, MessageHandler> getMessageHandlerMap() {
        return Collections.unmodifiableMap(type2handler);
    }

    /**
     * Forwards the received events into the appropriate {@link MessageHandler}
     * which is registered by {@link #addMessageHandler(Class, MessageHandler)}.
     */
    public void messageReceived(IoSession session, Object message)
            throws Exception {
        MessageHandler<Object> handler = findHandler(message.getClass());
        if (handler != null) {
            handler.messageReceived(session, message);
        } else {
            throw new UnknownMessageTypeException(
                    "No message handler found for message: " + message);
        }
    }

    protected MessageHandler<Object> findHandler(Class type) {
        return findHandler(type, null);
    }

    @SuppressWarnings("unchecked")
    private MessageHandler<Object> findHandler(Class type,
            Set<Class> triedClasses) {
        MessageHandler handler = null;

        if (triedClasses != null && triedClasses.contains(type))
            return null;

        /*
         * Try the cache first.
         */
        handler = findHandlerCache.get(type);
        if (handler != null)
            return handler;

        /*
         * Try the registered handlers for an immediate match.
         */
        handler = type2handler.get(type);

        if (handler == null) {
            /*
             * No immediate match could be found. Search the type's interfaces.
             */

            if (triedClasses == null)
                triedClasses = new IdentityHashSet<Class>();
            triedClasses.add(type);

            Class[] interfaces = type.getInterfaces();
            for (int i = 0; i < interfaces.length; i++) {
                handler = findHandler(interfaces[i], triedClasses);
                if (handler != null)
                    break;
            }
        }

        if (handler == null) {
            /*
             * No match in type's interfaces could be found. Search the 
             * superclass.
             */

            Class superclass = type.getSuperclass();
            if (superclass != null)
                handler = findHandler(superclass);
        }

        /*
         * Make sure the handler is added to the cache. By updating the cache
         * here all the types (superclasses and interfaces) in the path which 
         * led to a match will be cached along with the immediate message type.
         */
        if (handler != null)
            findHandlerCache.put(type, handler);

        return handler;
    }
}
