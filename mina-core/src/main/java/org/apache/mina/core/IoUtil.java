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
package org.apache.mina.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.IoSession;

/**
 * A utility class that provides various convenience methods related with
 * {@link IoSession} and {@link IoFuture}.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public final class IoUtil {
    private static final IoSession[] EMPTY_SESSIONS = new IoSession[0];

    private IoUtil() {
        // Do nothing
    }

    /**
     * Writes the specified {@code message} to the specified {@code sessions}.
     * If the specified {@code message} is an {@link IoBuffer}, the buffer is
     * automatically duplicated using {@link IoBuffer#duplicate()}.
     * 
     * @param message The message to broadcast
     * @param sessions The sessions that will receive the message
     * @return The list of WriteFuture created for each broadcasted message
     */
    public static List<WriteFuture> broadcast(Object message, Collection<IoSession> sessions) {
        List<WriteFuture> answer = new ArrayList<>(sessions.size());
        broadcast(message, sessions.iterator(), answer);
        return answer;
    }

    /**
     * Writes the specified {@code message} to the specified {@code sessions}.
     * If the specified {@code message} is an {@link IoBuffer}, the buffer is
     * automatically duplicated using {@link IoBuffer#duplicate()}.
     * 
     * @param message The message to broadcast
     * @param sessions The sessions that will receive the message
     * @return The list of WriteFuture created for each broadcasted message
     */
    public static List<WriteFuture> broadcast(Object message, Iterable<IoSession> sessions) {
        List<WriteFuture> answer = new ArrayList<>();
        broadcast(message, sessions.iterator(), answer);
        return answer;
    }

    /**
     * Writes the specified {@code message} to the specified {@code sessions}.
     * If the specified {@code message} is an {@link IoBuffer}, the buffer is
     * automatically duplicated using {@link IoBuffer#duplicate()}.
     * 
     * @param message The message to write
     * @param sessions The sessions the message has to be written to
     * @return The list of {@link WriteFuture} for the written messages
     */
    public static List<WriteFuture> broadcast(Object message, Iterator<IoSession> sessions) {
        List<WriteFuture> answer = new ArrayList<>();
        broadcast(message, sessions, answer);
        return answer;
    }

    /**
     * Writes the specified {@code message} to the specified {@code sessions}.
     * If the specified {@code message} is an {@link IoBuffer}, the buffer is
     * automatically duplicated using {@link IoBuffer#duplicate()}.
     * 
     * @param message The message to write
     * @param sessions The sessions the message has to be written to
     * @return The list of {@link WriteFuture} for the written messages
     */
    public static List<WriteFuture> broadcast(Object message, IoSession... sessions) {
        if (sessions == null) {
            sessions = EMPTY_SESSIONS;
        }

        List<WriteFuture> answer = new ArrayList<>(sessions.length);
        if (message instanceof IoBuffer) {
            for (IoSession s : sessions) {
                answer.add(s.write(((IoBuffer) message).duplicate()));
            }
        } else {
            for (IoSession s : sessions) {
                answer.add(s.write(message));
            }
        }
        return answer;
    }

    private static void broadcast(Object message, Iterator<IoSession> sessions, Collection<WriteFuture> answer) {
        if (message instanceof IoBuffer) {
            while (sessions.hasNext()) {
                IoSession s = sessions.next();
                answer.add(s.write(((IoBuffer) message).duplicate()));
            }
        } else {
            while (sessions.hasNext()) {
                IoSession s = sessions.next();
                answer.add(s.write(message));
            }
        }
    }

    /**
     * Wait on all the {@link IoFuture}s we get, or until one of the {@link IoFuture}s is interrupted
     *  
     * @param futures The {@link IoFuture}s we are waiting on
     * @throws InterruptedException If one of the {@link IoFuture} is interrupted
     */
    public static void await(Iterable<? extends IoFuture> futures) throws InterruptedException {
        for (IoFuture f : futures) {
            f.await();
        }
    }

    /**
     * Wait on all the {@link IoFuture}s we get. This can't get interrupted.
     *  
     * @param futures The {@link IoFuture}s we are waiting on
     */
    public static void awaitUninterruptably(Iterable<? extends IoFuture> futures) {
        for (IoFuture f : futures) {
            f.awaitUninterruptibly();
        }
    }

    /**
     * Wait on all the {@link IoFuture}s we get, or until one of the {@link IoFuture}s is interrupted
     *  
     * @param futures The {@link IoFuture}s we are waiting on 
     * @param timeout The maximum time we wait for the {@link IoFuture}s to complete
     * @param unit The Time unit to use for the timeout
     * @return <tt>TRUE</TT> if all the {@link IoFuture} have been completed, <tt>FALSE</tt> if
     * at least one {@link IoFuture} haas been interrupted
     * @throws InterruptedException If one of the {@link IoFuture} is interrupted
     */
    public static boolean await(Iterable<? extends IoFuture> futures, long timeout, TimeUnit unit)
            throws InterruptedException {
        return await(futures, unit.toMillis(timeout));
    }

    /**
     * Wait on all the {@link IoFuture}s we get, or until one of the {@link IoFuture}s is interrupted
     *  
     * @param futures The {@link IoFuture}s we are waiting on 
     * @param timeoutMillis The maximum milliseconds we wait for the {@link IoFuture}s to complete
     * @return <tt>TRUE</TT> if all the {@link IoFuture} have been completed, <tt>FALSE</tt> if
     * at least one {@link IoFuture} has been interrupted
     * @throws InterruptedException If one of the {@link IoFuture} is interrupted
     */
    public static boolean await(Iterable<? extends IoFuture> futures, long timeoutMillis) throws InterruptedException {
        return await0(futures, timeoutMillis, true);
    }

    /**
     * Wait on all the {@link IoFuture}s we get.
     *  
     * @param futures The {@link IoFuture}s we are waiting on 
     * @param timeout The maximum time we wait for the {@link IoFuture}s to complete
     * @param unit The Time unit to use for the timeout
     * @return <tt>TRUE</TT> if all the {@link IoFuture} have been completed, <tt>FALSE</tt> if
     * at least one {@link IoFuture} has been interrupted
     */
    public static boolean awaitUninterruptibly(Iterable<? extends IoFuture> futures, long timeout, TimeUnit unit) {
        return awaitUninterruptibly(futures, unit.toMillis(timeout));
    }

    /**
     * Wait on all the {@link IoFuture}s we get.
     *  
     * @param futures The {@link IoFuture}s we are waiting on 
     * @param timeoutMillis The maximum milliseconds we wait for the {@link IoFuture}s to complete
     * @return <tt>TRUE</TT> if all the {@link IoFuture} have been completed, <tt>FALSE</tt> if
     * at least one {@link IoFuture} has been interrupted
     */
    public static boolean awaitUninterruptibly(Iterable<? extends IoFuture> futures, long timeoutMillis) {
        try {
            return await0(futures, timeoutMillis, false);
        } catch (InterruptedException e) {
            throw new InternalError();
        }
    }

    private static boolean await0(Iterable<? extends IoFuture> futures, long timeoutMillis, boolean interruptable)
            throws InterruptedException {
        long startTime = timeoutMillis <= 0 ? 0 : System.currentTimeMillis();
        long waitTime = timeoutMillis;

        boolean lastComplete = true;
        Iterator<? extends IoFuture> i = futures.iterator();
        
        while (i.hasNext()) {
            IoFuture f = i.next();

            do {
                if (interruptable) {
                    lastComplete = f.await(waitTime);
                } else {
                    lastComplete = f.awaitUninterruptibly(waitTime);
                }

                waitTime = timeoutMillis - (System.currentTimeMillis() - startTime);

                if (waitTime <= 0) {
                    break;
                }
            } while (!lastComplete);

            if (waitTime <= 0) {
                break;
            }
        }

        return lastComplete && !i.hasNext();
    }
}
