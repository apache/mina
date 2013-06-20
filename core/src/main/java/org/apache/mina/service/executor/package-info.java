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

/**
 * <p>
 * Classes in charge of decoupling IoHandler event of the low level read/write/accept I/O threads ( {@link org.apache.mina.transport.nio.SelectorLoop} ).
 * <p>
 * Two kind of {@link org.apache.mina.service.executor.IoHandlerExecutor} are available :
 * <ul>
 * <li>in order, which will execute events for one session in order (the same thread of the pool will be picked)
 * <li> out of order, which will execute events for one session with no order consideration (can change of thread for events of the same session)
 * </ul>
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
package org.apache.mina.service.executor;

