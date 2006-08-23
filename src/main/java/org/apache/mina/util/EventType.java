/*
 *   @(#) $Id: AvailablePortFinder.java 155923 2005-03-02 14:23:42Z trustin $
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

/**
 * Enumeration for MINA event types.
 * Used by {@link ThreadPool}s when they push events to event queue.
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class EventType
{
    public static final EventType OPENED = new EventType();

    public static final EventType CLOSED = new EventType();

    public static final EventType READ = new EventType();

    public static final EventType WRITTEN = new EventType();

    public static final EventType RECEIVED = new EventType();

    public static final EventType SENT = new EventType();

    public static final EventType IDLE = new EventType();

    public static final EventType EXCEPTION = new EventType();

    private EventType()
    {
    }
}