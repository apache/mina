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

import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.UnknownMessageTypeException;
import org.apache.mina.util.IdentityHashSet;

/**
 * A {@link IoHandler} that demuxes <code>messageReceived</code> events
 * to the appropriate {@link MessageHandler}.
 * <p>
 * You can freely register and deregister {@link MessageHandler}s using
 * {@link #addReceivedMessageHandler(Class, MessageHandler)} and
 * {@link #removeReceivedMessageHandler(Class)}.
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
 * {@link #addReceivedMessageHandler(Class, MessageHandler)} and
 * {@link #removeReceivedMessageHandler(Class)} clear this cache.
 * </p>
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class DemuxingIoHandler extends IoHandlerAdapter {
    
    private final Map<Class<?>, MessageHandler<?>> receivedMessageHandlerCache =
        new ConcurrentHashMap<Class<?>, MessageHandler<?>>();

    private final Map<Class<?>, MessageHandler<?>> receivedMessageHandlers =
        new ConcurrentHashMap<Class<?>, MessageHandler<?>>();

    private final Map<Class<?>, MessageHandler<?>> sentMessageHandlerCache =
        new ConcurrentHashMap<Class<?>, MessageHandler<?>>();

    private final Map<Class<?>, MessageHandler<?>> sentMessageHandlers =
        new ConcurrentHashMap<Class<?>, MessageHandler<?>>();

    private final Map<Class<?>, ExceptionHandler<?>> exceptionHandlerCache =
        new ConcurrentHashMap<Class<?>, ExceptionHandler<?>>();

    private final Map<Class<?>, ExceptionHandler<?>> exceptionHandlers =
        new ConcurrentHashMap<Class<?>, ExceptionHandler<?>>();

    /**
     * Creates a new instance with no registered {@link MessageHandler}s.
     */
    public DemuxingIoHandler() {
        // Do nothing
    }

    /**
     * Registers a {@link MessageHandler} that handles the received messages of
     * the specified <code>type</code>.
     *
     * @return the old handler if there is already a registered handler for
     *         the specified <tt>type</tt>.  <tt>null</tt> otherwise.
     */
    @SuppressWarnings("unchecked")
    public <E> MessageHandler<? super E> addReceivedMessageHandler(Class<E> type,
            MessageHandler<? super E> handler) {
        receivedMessageHandlerCache.clear();
        return (MessageHandler<? super E>) receivedMessageHandlers.put(type, handler);
    }

    /**
     * Deregisters a {@link MessageHandler} that handles the received messages
     * of the specified <code>type</code>.
     *
     * @return the removed handler if successfully removed.  <tt>null</tt> otherwise.
     */
    @SuppressWarnings("unchecked")
    public <E> MessageHandler<? super E> removeReceivedMessageHandler(Class<E> type) {
        receivedMessageHandlerCache.clear();
        return (MessageHandler<? super E>) receivedMessageHandlers.remove(type);
    }

    /**
     * Registers a {@link MessageHandler} that handles the sent messages of the
     * specified <code>type</code>.
     *
     * @return the old handler if there is already a registered handler for
     *         the specified <tt>type</tt>.  <tt>null</tt> otherwise.
     */
    @SuppressWarnings("unchecked")
    public <E> MessageHandler<? super E> addSentMessageHandler(Class<E> type,
            MessageHandler<? super E> handler) {
        sentMessageHandlerCache.clear();
        return (MessageHandler<? super E>) sentMessageHandlers.put(type, handler);
    }

    /**
     * Deregisters a {@link MessageHandler} that handles the sent messages of
     * the specified <code>type</code>.
     *
     * @return the removed handler if successfully removed.  <tt>null</tt> otherwise.
     */
    @SuppressWarnings("unchecked")
    public <E> MessageHandler<? super E> removeSentMessageHandler(Class<E> type) {
        sentMessageHandlerCache.clear();
        return (MessageHandler<? super E>) sentMessageHandlers.remove(type);
    }
    
    /**
     * Registers a {@link MessageHandler} that receives the messages of
     * the specified <code>type</code>.
     *
     * @return the old handler if there is already a registered handler for
     *         the specified <tt>type</tt>.  <tt>null</tt> otherwise.
     */
    @SuppressWarnings("unchecked")
    public <E extends Throwable> 
    ExceptionHandler<? super E> addExceptionHandler(
            Class<E> type, ExceptionHandler<? super E> handler) {
        exceptionHandlerCache.clear();
        return (ExceptionHandler<? super E>) exceptionHandlers.put(type, handler);
    }

    /**
     * Deregisters a {@link MessageHandler} that receives the messages of
     * the specified <code>type</code>.
     *
     * @return the removed handler if successfully removed.  <tt>null</tt> otherwise.
     */
    @SuppressWarnings("unchecked")
    public <E extends Throwable> ExceptionHandler<? super E>
    removeExceptionHandler(Class<E> type) {
        exceptionHandlerCache.clear();
        return (ExceptionHandler<? super E>) exceptionHandlers.remove(type);
    }

    /**
     * Returns the {@link MessageHandler} which is registered to process
     * the specified <code>type</code>.
     */
    @SuppressWarnings("unchecked")
    public <E> MessageHandler<? super E> getMessageHandler(Class<E> type) {
        return (MessageHandler<? super E>) receivedMessageHandlers.get(type);
    }

    /**
     * Returns the {@link Map} which contains all messageType-{@link MessageHandler}
     * pairs registered to this handler for received messages.
     */
    public Map<Class<?>, MessageHandler<?>> getReceivedMessageHandlerMap() {
        return Collections.unmodifiableMap(receivedMessageHandlers);
    }

    /**
     * Returns the {@link Map} which contains all messageType-{@link MessageHandler}
     * pairs registered to this handler for sent messages.
     */
    public Map<Class<?>, MessageHandler<?>> getSentMessageHandlerMap() {
        return Collections.unmodifiableMap(sentMessageHandlers);
    }

    /**
     * Returns the {@link Map} which contains all messageType-{@link MessageHandler}
     * pairs registered to this handler.
     */
    public Map<Class<?>, ExceptionHandler<?>> getExceptionHandlerMap() {
        return Collections.unmodifiableMap(exceptionHandlers);
    }

    /**
     * Forwards the received events into the appropriate {@link MessageHandler}
     * which is registered by {@link #addReceivedMessageHandler(Class, MessageHandler)}.
     * 
     * <b>Warning !</b> If you are to overload this method, be aware that you 
     * _must_ call the messageHandler in your own method, otherwise it won't 
     * be called.
     */
    @Override
    public void messageReceived(IoSession session, Object message)
            throws Exception {
        MessageHandler<Object> handler = findReceivedMessageHandler(message.getClass());
        if (handler != null) {
            handler.handleMessage(session, message);
        } else {
            throw new UnknownMessageTypeException(
                    "No message handler found for message type: " +
                    message.getClass().getSimpleName());
        }
    }

    /**
     * Invoked when a message written by IoSession.write(Object) is sent out.
     * 
     * <b>Warning !</b> If you are to overload this method, be aware that you 
     * _must_ call the messageHandler in your own method, otherwise it won't 
     * be called.
     */
    @Override
    public void messageSent(IoSession session, Object message) throws Exception {
        MessageHandler<Object> handler = findSentMessageHandler(message.getClass());
        if (handler != null) {
            handler.handleMessage(session, message);
        } else {
            throw new UnknownMessageTypeException(
                    "No handler found for message type: " +
                    message.getClass().getSimpleName());
        }
    }

    /**
     * Invoked when any exception is thrown by user IoHandler implementation 
     * or by MINA. If cause is an instance of IOException, MINA will close the 
     * connection automatically.
     *
     * <b>Warning !</b> If you are to overload this method, be aware that you 
     * _must_ call the messageHandler in your own method, otherwise it won't 
     * be called.
     */
    @Override
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        ExceptionHandler<Throwable> handler = findExceptionHandler(cause.getClass());
        if (handler != null) {
            handler.exceptionCaught(session, cause);
        } else {
            throw new UnknownMessageTypeException(
                    "No handler found for exception type: " +
                    cause.getClass().getSimpleName());
        }
    }

    protected MessageHandler<Object> findReceivedMessageHandler(Class<?> type) {
        return findReceivedMessageHandler(type, null);
    }

    protected MessageHandler<Object> findSentMessageHandler(Class<?> type) {
        return findSentMessageHandler(type, null);
    }

    protected ExceptionHandler<Throwable> findExceptionHandler(Class<? extends Throwable> type) {
        return findExceptionHandler(type, null);
    }

    @SuppressWarnings("unchecked")
    private MessageHandler<Object> findReceivedMessageHandler(
            Class type, Set<Class> triedClasses) {
        
        return (MessageHandler<Object>) findHandler(
                receivedMessageHandlers, receivedMessageHandlerCache, type, triedClasses);
    }

    @SuppressWarnings("unchecked")
    private MessageHandler<Object> findSentMessageHandler(
            Class type, Set<Class> triedClasses) {
        
        return (MessageHandler<Object>) findHandler(
                sentMessageHandlers, sentMessageHandlerCache, type, triedClasses);
    }

    @SuppressWarnings("unchecked")
    private ExceptionHandler<Throwable> findExceptionHandler(
            Class type, Set<Class> triedClasses) {
        
        return (ExceptionHandler<Throwable>) findHandler(
                exceptionHandlers, exceptionHandlerCache, type, triedClasses);
    }

    @SuppressWarnings("unchecked")
    private Object findHandler(
            Map handlers, Map handlerCache,
            Class type, Set<Class> triedClasses) {

        Object handler = null;

        if (triedClasses != null && triedClasses.contains(type)) {
            return null;
        }

        /*
         * Try the cache first.
         */
        handler = handlerCache.get(type);
        if (handler != null) {
            return handler;
        }

        /*
         * Try the registered handlers for an immediate match.
         */
        handler = handlers.get(type);

        if (handler == null) {
            /*
             * No immediate match could be found. Search the type's interfaces.
             */

            if (triedClasses == null) {
                triedClasses = new IdentityHashSet<Class>();
            }
            triedClasses.add(type);

            Class[] interfaces = type.getInterfaces();
            for (Class element : interfaces) {
                handler = findHandler(handlers, handlerCache, element, triedClasses);
                if (handler != null) {
                    break;
                }
            }
        }

        if (handler == null) {
            /*
             * No match in type's interfaces could be found. Search the
             * superclass.
             */
            Class superclass = type.getSuperclass();
            if (superclass != null) {
                handler = findHandler(handlers, handlerCache, superclass, null);
            }
        }

        /*
         * Make sure the handler is added to the cache. By updating the cache
         * here all the types (superclasses and interfaces) in the path which
         * led to a match will be cached along with the immediate message type.
         */
        if (handler != null) {
            handlerCache.put(type, handler);
        }

        return handler;
    }
}
