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
 * In-VM pipe support which removes the overhead of local loopback communication.
 *
 * <h2>What is 'in-VM pipe'?</h2>
 * <p>
 *   In-VM pipe is a direct event forwarding mechanism between two
 *   <code>ProtocolHandler</code>s in the
 *   same Java Virtual Machine.  Using in-VM pipe, you can remove the overhead
 *   of encoding and decoding which is caused uselessly by local loopback
 *   network communication.  Here are some useful situations possible:
 *   <ul>
 *       <li>SMTP server and SPAM filtering server,</li>
 *       <li>web server and Servlet/JSP container.</li>
 *   </ul>
 * <p>
 *   Please refer to Tennis example.
 * </p>
 *
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
package org.apache.mina.transport.vmpipe;
