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
 * Socket (TCP/IP) and Datagram (UDP/IP) support based on Java NIO (New I/O) API.
 * 
 * <h2>Configuring the number of NIO selector loops</h2>
 *
 * You can specify the number of Socket I/O thread to utilize multi-processors efficiently by 
 * specifying the number of processing threads in the constructor. The default is 1
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
package org.apache.mina.transport.socket.nio;
