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
package org.apache.mina.core.session;

/**
 * An {@link Enum} that represents the type of I/O events and requests.
 * Most users won't need to use this class.  It is usually used by internal
 * components to store I/O events.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public enum IoEventType {
    /** The session has been created */
    SESSION_CREATED,
    
    /** The session has been opened */
    SESSION_OPENED, 
    
    /** The session has been closed */
    SESSION_CLOSED, 
    
    /** A message has been received */
    MESSAGE_RECEIVED, 
    
    /** A message has been sent */
    MESSAGE_SENT, 
    
    /** The session is idle */
    SESSION_IDLE, 
    
    /** An exception has been caught */ 
    EXCEPTION_CAUGHT, 
    
    /** A write has pccired */
    WRITE, 
    
    /** A close has occured */
    CLOSE,
}
