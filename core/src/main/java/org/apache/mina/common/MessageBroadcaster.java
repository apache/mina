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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * A utility class that provides various convenience methods related with
 * {@link IoSession} and {@link IoFuture}.
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class MessageBroadcaster {
    
    private static final IoSession[] EMPTY_SESSIONS = new IoSession[0];

    /**
     * Writes the specified {@code message} to the specified {@code sessions}.
     * If the specified {@code message} is an {@link IoBuffer}, the buffer is
     * automatically duplicated using {@link IoBuffer#duplicate()}.
     */
    public static List<WriteFuture> broadcast(Object message, Collection<IoSession> sessions) {
        List<WriteFuture> answer = new ArrayList<WriteFuture>(sessions.size());
        broadcast(message, sessions.iterator(), answer);
        return answer;
    }

    /**
     * Writes the specified {@code message} to the specified {@code sessions}.
     * If the specified {@code message} is an {@link IoBuffer}, the buffer is
     * automatically duplicated using {@link IoBuffer#duplicate()}.
     */
    public static List<WriteFuture> broadcast(Object message, Iterable<IoSession> sessions) {
        List<WriteFuture> answer = new ArrayList<WriteFuture>();
        broadcast(message, sessions.iterator(), answer);
        return answer;
    }
    
    /**
     * Writes the specified {@code message} to the specified {@code sessions}.
     * If the specified {@code message} is an {@link IoBuffer}, the buffer is
     * automatically duplicated using {@link IoBuffer#duplicate()}.
     */
    public static List<WriteFuture> broadcast(Object message, Iterator<IoSession> sessions) {
        List<WriteFuture> answer = new ArrayList<WriteFuture>();
        broadcast(message, sessions, answer);
        return answer;
    }
    
    /**
     * Writes the specified {@code message} to the specified {@code sessions}.
     * If the specified {@code message} is an {@link IoBuffer}, the buffer is
     * automatically duplicated using {@link IoBuffer#duplicate()}.
     */
    public static List<WriteFuture> broadcast(Object message, IoSession... sessions) {
        if (sessions == null) {
            sessions = EMPTY_SESSIONS;
        }
        
        List<WriteFuture> answer = new ArrayList<WriteFuture>(sessions.length);
        if (message instanceof IoBuffer) {
            for (IoSession s: sessions) {
                answer.add(s.write(((IoBuffer) message).duplicate()));
            }
        } else {
            for (IoSession s: sessions) {
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
    
    private MessageBroadcaster() {
    }
}
